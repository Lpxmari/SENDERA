package com.sendera.security;

import com.sendera.exception.LimiteIntentosExcedidoException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Limita los intentos de inicio de sesión para frenar la fuerza bruta (auditoría OB-02, OM-04, OM-07).
 *
 * <p>Aplica dos baldes (token bucket) en memoria por intento: uno por la combinación IP + correo y otro por IP
 * (para que cambiar de correo no esquive el límite). Cada intento consume un token, tenga éxito o no.
 * El estado vive en la memoria de la instancia: con varias réplicas cada una lleva su propia cuenta.
 *
 * <p>Los baldes inactivos se eliminan de forma periódica (como máximo una vez por ventana, aprovechando el
 * propio tráfico) para que el mapa no crezca sin límite. Un balde inactivo durante una ventana completa ya
 * está lleno, así que descartarlo equivale a crearlo de nuevo.
 */
@Slf4j
public class LimiteLoginService {

    static final Duration VENTANA = Duration.ofMinutes(1);
    private static final int MAX_LONGITUD_CORREO = 320;

    private final int intentosPorCorreo;
    private final int intentosPorIp;
    private final Clock reloj;
    private final ConcurrentHashMap<String, Entrada> porIp = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Entrada> porIpYCorreo = new ConcurrentHashMap<>();
    private final AtomicLong ultimaLimpiezaMs;

    public LimiteLoginService(int intentosPorCorreo, int intentosPorIp) {
        this(intentosPorCorreo, intentosPorIp, Clock.systemUTC());
    }

    /** El reloj solo gobierna la limpieza de entradas inactivas; el rellenado de los baldes usa el reloj del sistema. */
    LimiteLoginService(int intentosPorCorreo, int intentosPorIp, Clock reloj) {
        if (intentosPorCorreo < 1 || intentosPorIp < 1) {
            throw new IllegalArgumentException("Los límites de intentos de login deben ser al menos 1.");
        }
        this.intentosPorCorreo = intentosPorCorreo;
        this.intentosPorIp = intentosPorIp;
        this.reloj = reloj;
        this.ultimaLimpiezaMs = new AtomicLong(reloj.millis());
    }

    /**
     * Registra un intento de login.
     *
     * @param ip     dirección del cliente ya determinada por la capa web
     * @param correo correo recibido (se normaliza con trim y minúsculas); puede ser nulo o vacío
     * @throws LimiteIntentosExcedidoException si se superó alguno de los dos límites
     */
    public void registrarIntento(String ip, String correo) {
        long ahora = reloj.millis();
        limpiarInactivos(ahora);
        String correoNormalizado = normalizar(correo);

        Bucket baldeIp = obtener(porIp, ip, intentosPorIp, ahora);
        ConsumptionProbe sondaIp = baldeIp.tryConsumeAndReturnRemaining(1);
        if (!sondaIp.isConsumed()) {
            rechazar(ip, correoNormalizado, sondaIp);
        }

        // El balde por correo se crea solo tras pasar el tope por IP: una IP no puede inflar el mapa sin límite.
        Bucket baldeCorreo = obtener(porIpYCorreo, ip + "|" + correoNormalizado, intentosPorCorreo, ahora);
        ConsumptionProbe sondaCorreo = baldeCorreo.tryConsumeAndReturnRemaining(1);
        if (!sondaCorreo.isConsumed()) {
            // Un intento rechazado no debe gastar el cupo de la IP.
            baldeIp.addTokens(1);
            rechazar(ip, correoNormalizado, sondaCorreo);
        }
    }

    /** Cantidad de baldes en memoria (para pruebas y diagnóstico). */
    int entradasActivas() {
        return porIp.size() + porIpYCorreo.size();
    }

    static String normalizar(String correo) {
        if (correo == null) {
            return "";
        }
        String limpio = correo.trim().toLowerCase(Locale.ROOT);
        return limpio.length() > MAX_LONGITUD_CORREO ? limpio.substring(0, MAX_LONGITUD_CORREO) : limpio;
    }

    /** Oculta el usuario del correo para los registros: {@code ana@correo.com} pasa a {@code a***@correo.com}. */
    static String enmascarar(String correoNormalizado) {
        if (correoNormalizado.isEmpty()) {
            return "(sin correo)";
        }
        int arroba = correoNormalizado.indexOf('@');
        String dominio = arroba >= 0 ? correoNormalizado.substring(arroba) : "";
        // Se descartan caracteres raros para que el dato del cliente no pueda falsear líneas del registro.
        String dominioSeguro = dominio.replaceAll("[^\\p{L}\\p{N}@._-]", "?");
        return correoNormalizado.charAt(0) + "***" + dominioSeguro;
    }

    private void rechazar(String ip, String correoNormalizado, ConsumptionProbe sonda) {
        long segundos = Math.max(1, (long) Math.ceil(sonda.getNanosToWaitForRefill() / (double) TimeUnit.SECONDS.toNanos(1)));
        log.warn("Límite de intentos de login excedido: ip={}, correo={}, reintentar en {} s",
                ip, enmascarar(correoNormalizado), segundos);
        throw new LimiteIntentosExcedidoException(segundos);
    }

    private static Bucket obtener(ConcurrentHashMap<String, Entrada> mapa, String clave, int capacidad, long ahoraMs) {
        Entrada entrada = mapa.compute(clave, (k, actual) -> {
            Entrada e = actual != null ? actual : new Entrada(nuevoBalde(capacidad));
            e.ultimoUsoMs = ahoraMs;
            return e;
        });
        return entrada.balde;
    }

    private static Bucket nuevoBalde(int capacidad) {
        // Rellenado por intervalos: tras agotarse, los tokens vuelven juntos al cumplirse la ventana.
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(capacidad).refillIntervally(capacidad, VENTANA).build())
                .build();
    }

    private void limpiarInactivos(long ahoraMs) {
        long ultima = ultimaLimpiezaMs.get();
        if (ahoraMs - ultima < VENTANA.toMillis() || !ultimaLimpiezaMs.compareAndSet(ultima, ahoraMs)) {
            return;
        }
        long limite = ahoraMs - VENTANA.toMillis();
        porIp.entrySet().removeIf(e -> e.getValue().ultimoUsoMs < limite);
        porIpYCorreo.entrySet().removeIf(e -> e.getValue().ultimoUsoMs < limite);
    }

    private static final class Entrada {
        private final Bucket balde;
        private volatile long ultimoUsoMs;

        private Entrada(Bucket balde) {
            this.balde = balde;
        }
    }
}
