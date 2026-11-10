package com.sendera.security;

import com.sendera.exception.LimiteIntentosExcedidoException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reglas del límite de intentos de login (sin Spring ni HTTP). */
class LimiteLoginServiceTest {

    /** Reloj manual: solo gobierna la limpieza de entradas inactivas. */
    private static final class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-11-10T12:00:00Z");

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    @Test
    void cincoIntentosPasanYElSextoSeRechazaConSegundosDeEspera() {
        LimiteLoginService limite = new LimiteLoginService(5, 20);

        for (int i = 0; i < 5; i++) {
            limite.registrarIntento("1.1.1.1", "ana@correo.com");
        }

        LimiteIntentosExcedidoException ex = assertThrows(LimiteIntentosExcedidoException.class,
                () -> limite.registrarIntento("1.1.1.1", "ana@correo.com"));
        assertTrue(ex.getSegundosEspera() >= 1 && ex.getSegundosEspera() <= 60, "espera: " + ex.getSegundosEspera());
        assertEquals("Demasiados intentos de inicio de sesión. Espera un minuto e inténtalo de nuevo.", ex.getMessage());
    }

    @Test
    void elCorreoSeNormalizaConTrimYMinusculas() {
        LimiteLoginService limite = new LimiteLoginService(2, 20);

        limite.registrarIntento("1.1.1.1", "  ANA@Correo.com ");
        limite.registrarIntento("1.1.1.1", "ana@correo.com");

        assertThrows(LimiteIntentosExcedidoException.class, () -> limite.registrarIntento("1.1.1.1", "Ana@CORREO.com  "));
    }

    @Test
    void otroCorreoDeLaMismaIpSigueHastaElTopePorIp() {
        LimiteLoginService limite = new LimiteLoginService(5, 20);

        for (int c = 0; c < 4; c++) {
            for (int i = 0; i < 5; i++) {
                limite.registrarIntento("1.1.1.1", "usuario" + c + "@correo.com");
            }
        }

        // 20 intentos aceptados: el tope por IP corta aunque el correo sea nuevo.
        assertThrows(LimiteIntentosExcedidoException.class, () -> limite.registrarIntento("1.1.1.1", "nuevo@correo.com"));
    }

    @Test
    void unIntentoRechazadoPorCorreoNoGastaElCupoDeLaIp() {
        LimiteLoginService limite = new LimiteLoginService(1, 3);

        limite.registrarIntento("1.1.1.1", "a@correo.com");
        for (int i = 0; i < 10; i++) {
            assertThrows(LimiteIntentosExcedidoException.class, () -> limite.registrarIntento("1.1.1.1", "a@correo.com"));
        }

        // Quedan 2 cupos de IP aunque hubo 10 rechazos del mismo correo.
        assertDoesNotThrow(() -> limite.registrarIntento("1.1.1.1", "b@correo.com"));
        assertDoesNotThrow(() -> limite.registrarIntento("1.1.1.1", "c@correo.com"));
        assertThrows(LimiteIntentosExcedidoException.class, () -> limite.registrarIntento("1.1.1.1", "d@correo.com"));
    }

    @Test
    void otraIpNoSeVeAfectada() {
        LimiteLoginService limite = new LimiteLoginService(1, 1);
        limite.registrarIntento("1.1.1.1", "ana@correo.com");
        assertThrows(LimiteIntentosExcedidoException.class, () -> limite.registrarIntento("1.1.1.1", "ana@correo.com"));

        assertDoesNotThrow(() -> limite.registrarIntento("2.2.2.2", "ana@correo.com"));
    }

    @Test
    void sinCorreoSeCuentaComoCorreoVacio() {
        LimiteLoginService limite = new LimiteLoginService(2, 20);

        limite.registrarIntento("1.1.1.1", null);
        limite.registrarIntento("1.1.1.1", "   ");

        assertThrows(LimiteIntentosExcedidoException.class, () -> limite.registrarIntento("1.1.1.1", ""));
    }

    @Test
    void laLimpiezaEliminaLasEntradasInactivasYConservaLasRecientes() {
        RelojManual reloj = new RelojManual();
        LimiteLoginService limite = new LimiteLoginService(5, 20, reloj);
        limite.registrarIntento("1.1.1.1", "viejo@correo.com");
        limite.registrarIntento("2.2.2.2", "viejo@correo.com");
        assertEquals(4, limite.entradasActivas());

        reloj.avanzar(Duration.ofSeconds(50));
        limite.registrarIntento("3.3.3.3", "reciente@correo.com"); // aún no toca limpiar
        assertEquals(6, limite.entradasActivas());

        reloj.avanzar(Duration.ofSeconds(20)); // los dos primeros llevan 70 s inactivos; el reciente, 20 s
        limite.registrarIntento("3.3.3.3", "reciente@correo.com");

        assertEquals(2, limite.entradasActivas());
    }

    @Test
    void unaLimpiezaNoPierdeElLimiteDeUnaEntradaActiva() {
        RelojManual reloj = new RelojManual();
        LimiteLoginService limite = new LimiteLoginService(2, 20, reloj);
        limite.registrarIntento("1.1.1.1", "ana@correo.com");
        reloj.avanzar(Duration.ofSeconds(30));
        limite.registrarIntento("1.1.1.1", "ana@correo.com");
        reloj.avanzar(Duration.ofSeconds(40)); // dispara la limpieza, pero la entrada se usó hace 40 s

        assertThrows(LimiteIntentosExcedidoException.class, () -> limite.registrarIntento("1.1.1.1", "ana@correo.com"));
    }

    @Test
    void losLimitesDebenSerPositivos() {
        assertThrows(IllegalArgumentException.class, () -> new LimiteLoginService(0, 20));
        assertThrows(IllegalArgumentException.class, () -> new LimiteLoginService(5, 0));
    }

    @Test
    void elCorreoSeEnmascaraParaLosRegistros() {
        assertEquals("a***@correo.com", LimiteLoginService.enmascarar("ana@correo.com"));
        assertEquals("s***", LimiteLoginService.enmascarar("sinarroba"));
        assertEquals("(sin correo)", LimiteLoginService.enmascarar(""));
        assertFalse(LimiteLoginService.enmascarar("ana@co\nrreo.com").contains("\n"));
        assertFalse(LimiteLoginService.enmascarar("ana@correo.com").contains("ana"));
    }
}
