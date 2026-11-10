package com.sendera.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sendera.exception.PasarelaPagoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de la API REST de Mercado Pago (Checkout Pro vía Preferences API). Usa la misma cuenta para pruebas
 * y producción: lo que cambia es el Access Token configurado (de una cuenta de prueba o el real).
 * Nunca registra el Access Token ni datos de pago del comprador.
 */
@Slf4j
@Component
public class MercadoPagoClient {

    private static final String DESCRIPTOR_EXTRACTO = "SENDERA";

    private final RestClient restClient;
    private final String accessToken;

    public MercadoPagoClient(@Qualifier("mercadoPagoRestClient") RestClient restClient,
                             @Value("${app.pagos.mercadopago.access-token:}") String accessToken) {
        this.restClient = restClient;
        this.accessToken = accessToken == null ? "" : accessToken.trim();
    }

    public boolean estaConfigurado() {
        return !accessToken.isEmpty();
    }

    /**
     * Las credenciales de prueba antiguas (prefijo TEST-) abren el checkout sandbox; las de cuentas de prueba
     * (prefijo APP_USR-, el formato actual) y las de producción usan el checkout normal.
     */
    public String urlCheckout(PreferenciaMercadoPago preferencia) {
        if (accessToken.startsWith("TEST-") && preferencia.sandboxInitPoint() != null) {
            return preferencia.sandboxInitPoint();
        }
        return preferencia.initPoint();
    }

    public PreferenciaMercadoPago crearPreferencia(SolicitudPreferencia solicitud) {
        try {
            PreferenciaMercadoPago preferencia = restClient.post()
                    .uri("/checkout/preferences")
                    .headers(this::autenticar)
                    .header("X-Idempotency-Key", UUID.randomUUID().toString())
                    .body(cuerpoPreferencia(solicitud))
                    .retrieve()
                    .body(PreferenciaMercadoPago.class);
            if (preferencia == null || preferencia.id() == null || urlCheckout(preferencia) == null) {
                log.error("Mercado Pago respondió una preferencia sin identificador o sin URL de pago.");
                throw new PasarelaPagoException();
            }
            return preferencia;
        } catch (RestClientResponseException e) {
            log.error("Mercado Pago rechazó la preferencia de pago. HTTP {}. Respuesta: {}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new PasarelaPagoException();
        } catch (RestClientException e) {
            log.error("No se pudo crear la preferencia en Mercado Pago: {}", e.getMessage());
            throw new PasarelaPagoException();
        }
    }

    /** Vacío si Mercado Pago no conoce el pago (p. ej. la notificación simulada desde "Tus integraciones"). */
    public Optional<PagoMercadoPago> obtenerPago(String pagoId) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/v1/payments/{id}", pagoId)
                    .headers(this::autenticar)
                    .retrieve()
                    .body(PagoMercadoPago.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientResponseException e) {
            log.error("No se pudo consultar el pago {} en Mercado Pago. HTTP {}", pagoId, e.getStatusCode().value());
            throw new PasarelaPagoException();
        } catch (RestClientException e) {
            log.error("No se pudo consultar el pago {} en Mercado Pago: {}", pagoId, e.getMessage());
            throw new PasarelaPagoException();
        }
    }

    /** Pagos asociados a una referencia externa, del más reciente al más antiguo. */
    public List<PagoMercadoPago> buscarPagosPorReferencia(String referenciaExterna) {
        try {
            ResultadoBusqueda resultado = restClient.get()
                    .uri(uri -> uri.path("/v1/payments/search")
                            .queryParam("external_reference", referenciaExterna)
                            .queryParam("sort", "date_created")
                            .queryParam("criteria", "desc")
                            .build())
                    .headers(this::autenticar)
                    .retrieve()
                    .body(ResultadoBusqueda.class);
            return resultado == null || resultado.results() == null ? List.of() : resultado.results();
        } catch (RestClientResponseException e) {
            log.error("No se pudieron buscar los pagos de {} en Mercado Pago. HTTP {}",
                    referenciaExterna, e.getStatusCode().value());
            throw new PasarelaPagoException();
        } catch (RestClientException e) {
            log.error("No se pudieron buscar los pagos de {} en Mercado Pago: {}", referenciaExterna, e.getMessage());
            throw new PasarelaPagoException();
        }
    }

    private void autenticar(HttpHeaders headers) {
        headers.setBearerAuth(accessToken);
    }

    private Map<String, Object> cuerpoPreferencia(SolicitudPreferencia solicitud) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", solicitud.itemId());
        item.put("title", solicitud.titulo());
        item.put("quantity", 1);
        item.put("currency_id", solicitud.moneda());
        item.put("unit_price", solicitud.monto());

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("items", List.of(item));
        cuerpo.put("external_reference", solicitud.referenciaExterna());
        cuerpo.put("statement_descriptor", DESCRIPTOR_EXTRACTO);
        cuerpo.put("back_urls", Map.of(
                "success", solicitud.urlRetorno(),
                "pending", solicitud.urlRetorno(),
                "failure", solicitud.urlRetorno()));
        if (solicitud.autoRetorno()) {
            cuerpo.put("auto_return", "approved");
        }
        if (solicitud.urlNotificacion() != null && !solicitud.urlNotificacion().isBlank()) {
            cuerpo.put("notification_url", solicitud.urlNotificacion());
        }
        return cuerpo;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResultadoBusqueda(List<PagoMercadoPago> results) {
    }
}
