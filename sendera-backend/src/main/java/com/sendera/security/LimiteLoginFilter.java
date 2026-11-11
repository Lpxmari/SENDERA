package com.sendera.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sendera.exception.LimiteIntentosExcedidoException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Aplica {@link LimiteLoginService} solo a {@code POST /auth/login}. Se encarga de lo propio de HTTP (IP del cliente,
 * lectura del correo del cuerpo y respuesta); las reglas del límite viven en el servicio.
 *
 * <p>Cuando se excede el límite delega en el {@link HandlerExceptionResolver} de Spring para que
 * {@code GlobalExceptionHandler} produzca el 429 con el formato {@code ApiError} y la cabecera {@code Retry-After}.
 */
public class LimiteLoginFilter extends OncePerRequestFilter {

    private static final RequestMatcher LOGIN = new AntPathRequestMatcher("/auth/login", HttpMethod.POST.name());
    /** Un correo válido cabe de sobra; un cuerpo mayor no se analiza (cuenta solo con correo vacío). */
    private static final int MAX_BYTES_CUERPO = 8 * 1024;
    private static final Pattern IP_VALIDA = Pattern.compile("[0-9a-fA-F:.]{1,45}");

    private final LimiteLoginService limite;
    private final HandlerExceptionResolver resolvedorExcepciones;
    private final ObjectMapper objectMapper;

    public LimiteLoginFilter(LimiteLoginService limite, HandlerExceptionResolver resolvedorExcepciones,
                             ObjectMapper objectMapper) {
        this.limite = limite;
        this.resolvedorExcepciones = resolvedorExcepciones;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !LOGIN.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        SolicitudConCuerpoLeido solicitud = new SolicitudConCuerpoLeido(request, MAX_BYTES_CUERPO + 1);
        try {
            limite.registrarIntento(ipCliente(request), extraerCorreo(solicitud.prefijo()));
        } catch (LimiteIntentosExcedidoException ex) {
            resolvedorExcepciones.resolveException(request, response, null, ex);
            return;
        }
        chain.doFilter(solicitud, response);
    }

    /**
     * Railway está detrás de un proxy: se usa el primer valor de {@code X-Forwarded-For}. Si falta o no parece una IP,
     * se usa la dirección de la conexión.
     */
    static String ipCliente(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        if (reenviada != null) {
            String primera = reenviada.split(",", 2)[0].trim();
            if (IP_VALIDA.matcher(primera).matches()) {
                return primera;
            }
        }
        return request.getRemoteAddr();
    }

    private String extraerCorreo(byte[] prefijo) {
        if (prefijo.length == 0 || prefijo.length > MAX_BYTES_CUERPO) {
            return "";
        }
        try {
            JsonNode correo = objectMapper.readTree(prefijo).path("correo");
            return correo.isTextual() ? correo.asText() : "";
        } catch (IOException ex) {
            // Cuerpo ilegible: no hay correo que contar. El controlador responderá 400 al validarlo.
            return "";
        }
    }

    /** Lee como máximo {@code limite} bytes del cuerpo para inspeccionarlos y deja el cuerpo completo disponible aguas abajo. */
    private static final class SolicitudConCuerpoLeido extends HttpServletRequestWrapper {

        private final byte[] prefijo;
        private final InputStream restante;

        SolicitudConCuerpoLeido(HttpServletRequest request, int limite) throws IOException {
            super(request);
            InputStream original = request.getInputStream();
            this.prefijo = original.readNBytes(limite);
            this.restante = original;
        }

        byte[] prefijo() {
            return prefijo;
        }

        @Override
        public ServletInputStream getInputStream() {
            InputStream completo = new SequenceInputStream(new ByteArrayInputStream(prefijo), restante);
            return new ServletInputStream() {
                private boolean terminado;

                @Override
                public int read() throws IOException {
                    int b = completo.read();
                    terminado = b == -1;
                    return b;
                }

                @Override
                public int read(byte[] destino, int desde, int largo) throws IOException {
                    int n = completo.read(destino, desde, largo);
                    terminado = n == -1;
                    return n;
                }

                @Override
                public boolean isFinished() {
                    return terminado;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("Lectura asíncrona no soportada en el login.");
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String codificacion = getCharacterEncoding();
            Charset charset = codificacion != null ? Charset.forName(codificacion) : StandardCharsets.UTF_8;
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }
}
