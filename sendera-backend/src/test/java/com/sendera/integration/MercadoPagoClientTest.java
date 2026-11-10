package com.sendera.integration;

import com.sendera.exception.PasarelaPagoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MercadoPagoClientTest {

    private static final String API = "https://api.mercadopago.test";
    private static final String TOKEN = "APP_USR-token-de-prueba";

    private MockRestServiceServer servidor;
    private MercadoPagoClient cliente;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(API);
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new MercadoPagoClient(builder.build(), TOKEN);
    }

    private SolicitudPreferencia solicitud(boolean autoRetorno, String urlNotificacion) {
        return new SolicitudPreferencia("itinerario-2", "Reserva SD-000015 - Descanso cafetero",
                new BigDecimal("195000.00"), "COP", "SD-000015",
                "https://sendera.app/pago/resultado?reserva=15", autoRetorno, urlNotificacion);
    }

    @Test
    void creaLaPreferenciaConElMontoLaReferenciaYLasUrlsDeRetorno() {
        servidor.expect(requestTo(API + "/checkout/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andExpect(jsonPath("$.items[0].unit_price").value(195000.00))
                .andExpect(jsonPath("$.items[0].currency_id").value("COP"))
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andExpect(jsonPath("$.external_reference").value("SD-000015"))
                .andExpect(jsonPath("$.back_urls.success").value("https://sendera.app/pago/resultado?reserva=15"))
                .andExpect(jsonPath("$.back_urls.failure").value("https://sendera.app/pago/resultado?reserva=15"))
                .andExpect(jsonPath("$.auto_return").value("approved"))
                .andExpect(jsonPath("$.notification_url").value("https://api.sendera.app/api/v1/pagos/webhook"))
                .andRespond(withSuccess("""
                        {"id":"123-abc","init_point":"https://www.mercadopago.com.co/checkout/v1/redirect?pref_id=123-abc",
                         "sandbox_init_point":"https://sandbox.mercadopago.com.co/checkout/v1/redirect?pref_id=123-abc",
                         "collector_id":99}
                        """, MediaType.APPLICATION_JSON));

        PreferenciaMercadoPago preferencia = cliente.crearPreferencia(
                solicitud(true, "https://api.sendera.app/api/v1/pagos/webhook"));

        assertEquals("123-abc", preferencia.id());
        // Las credenciales de cuentas de prueba (APP_USR-) usan el checkout normal
        assertEquals("https://www.mercadopago.com.co/checkout/v1/redirect?pref_id=123-abc",
                cliente.urlCheckout(preferencia));
        servidor.verify();
    }

    @Test
    void enLocalNoEnviaAutoReturnNiUrlDeNotificacion() {
        servidor.expect(requestTo(API + "/checkout/preferences"))
                .andExpect(jsonPath("$.auto_return").doesNotExist())
                .andExpect(jsonPath("$.notification_url").doesNotExist())
                .andRespond(withSuccess("{\"id\":\"p1\",\"init_point\":\"https://mp/p1\"}", MediaType.APPLICATION_JSON));

        cliente.crearPreferencia(solicitud(false, ""));

        servidor.verify();
    }

    @Test
    void lasCredencialesTestAntiguasUsanElCheckoutSandbox() {
        MercadoPagoClient conTokenTest = new MercadoPagoClient(RestClient.create(), "TEST-123");
        PreferenciaMercadoPago preferencia = new PreferenciaMercadoPago("p", "https://real", "https://sandbox");

        assertEquals("https://sandbox", conTokenTest.urlCheckout(preferencia));
    }

    @Test
    void unRechazoDeMercadoPagoSeConvierteEnErrorDePasarela() {
        servidor.expect(requestTo(API + "/checkout/preferences"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"invalid unit_price\"}"));

        assertThrows(PasarelaPagoException.class, () -> cliente.crearPreferencia(solicitud(false, null)));
    }

    @Test
    void unaPreferenciaSinUrlDePagoEsUnErrorDePasarela() {
        servidor.expect(requestTo(API + "/checkout/preferences"))
                .andRespond(withSuccess("{\"id\":\"p1\"}", MediaType.APPLICATION_JSON));

        assertThrows(PasarelaPagoException.class, () -> cliente.crearPreferencia(solicitud(false, null)));
    }

    @Test
    void consultaUnPago() {
        servidor.expect(requestTo(API + "/v1/payments/987"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andRespond(withSuccess("""
                        {"id":987,"status":"approved","status_detail":"accredited","external_reference":"SD-000015",
                         "transaction_amount":195000,"currency_id":"COP"}
                        """, MediaType.APPLICATION_JSON));

        PagoMercadoPago pago = cliente.obtenerPago("987").orElseThrow();

        assertEquals(987L, pago.id());
        assertEquals("approved", pago.status());
        assertEquals("SD-000015", pago.externalReference());
        assertEquals(0, new BigDecimal("195000").compareTo(pago.transactionAmount()));
        assertEquals("COP", pago.currencyId());
    }

    @Test
    void unPagoInexistenteDevuelveVacio() {
        servidor.expect(requestTo(API + "/v1/payments/123456")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertTrue(cliente.obtenerPago("123456").isEmpty());
    }

    @Test
    void unaCaidaDeMercadoPagoAlConsultarEsUnErrorDePasarela() {
        servidor.expect(requestTo(API + "/v1/payments/1")).andRespond(withServerError());

        assertThrows(PasarelaPagoException.class, () -> cliente.obtenerPago("1"));
    }

    @Test
    void buscaLosPagosDeUnaReferenciaDelMasRecienteAlMasAntiguo() {
        servidor.expect(requestTo(API + "/v1/payments/search?external_reference=SD-000015&sort=date_created&criteria=desc"))
                .andRespond(withSuccess("""
                        {"paging":{"total":2},"results":[
                          {"id":2,"status":"rejected","external_reference":"SD-000015"},
                          {"id":1,"status":"approved","external_reference":"SD-000015"}]}
                        """, MediaType.APPLICATION_JSON));

        List<PagoMercadoPago> pagos = cliente.buscarPagosPorReferencia("SD-000015");

        assertEquals(List.of(2L, 1L), pagos.stream().map(PagoMercadoPago::id).toList());
    }

    @Test
    void sinAccessTokenNoEstaConfigurado() {
        assertFalse(new MercadoPagoClient(RestClient.create(), "  ").estaConfigurado());
        assertFalse(new MercadoPagoClient(RestClient.create(), null).estaConfigurado());
        assertTrue(cliente.estaConfigurado());
    }
}
