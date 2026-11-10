package com.sendera.integration;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF-014: solo se procesan webhooks firmados por Mercado Pago con la clave secreta de la aplicación. */
class ValidadorFirmaMercadoPagoTest {

    private static final String SECRETO = "secreto-de-prueba";
    private final ValidadorFirmaMercadoPago validador = new ValidadorFirmaMercadoPago(SECRETO);

    static String firmar(String secreto, String manifiesto) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(manifiesto.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void aceptaUnaFirmaCalculadaConElManifiestoOficial() throws Exception {
        String v1 = firmar(SECRETO, "id:123456;request-id:abc-1;ts:1742505638683;");

        assertTrue(validador.esValida("ts=1742505638683,v1=" + v1, "abc-1", "123456"));
    }

    @Test
    void elIdDelPagoSeComparaEnMinusculas() throws Exception {
        String v1 = firmar(SECRETO, "id:abc9;request-id:r;ts:1;");

        assertTrue(validador.esValida("ts=1, v1=" + v1, "r", "ABC9"));
    }

    @Test
    void omiteDelManifiestoLasPartesAusentes() throws Exception {
        String v1 = firmar(SECRETO, "ts:99;");

        assertTrue(validador.esValida("ts=99,v1=" + v1, null, null));
    }

    @Test
    void rechazaFirmasAlteradasOConOtroSecreto() throws Exception {
        String otroSecreto = firmar("otro-secreto", "id:123456;request-id:abc-1;ts:1;");
        String correcta = firmar(SECRETO, "id:123456;request-id:abc-1;ts:1;");

        assertFalse(validador.esValida("ts=1,v1=" + otroSecreto, "abc-1", "123456"));
        assertFalse(validador.esValida("ts=1,v1=" + correcta, "abc-1", "999999"));
        assertFalse(validador.esValida("ts=2,v1=" + correcta, "abc-1", "123456"));
    }

    @Test
    void rechazaCabecerasAusentesOMalFormadas() {
        assertFalse(validador.esValida(null, "r", "1"));
        assertFalse(validador.esValida("", "r", "1"));
        assertFalse(validador.esValida("ts=1", "r", "1"));
        assertFalse(validador.esValida("v1=abcd", "r", "1"));
        assertFalse(validador.esValida("ts=1,v1=no-es-hexadecimal", "r", "1"));
    }

    @Test
    void sinSecretoLaValidacionQuedaDeshabilitada() {
        assertFalse(new ValidadorFirmaMercadoPago("").estaHabilitado());
        assertFalse(new ValidadorFirmaMercadoPago(null).estaHabilitado());
        assertTrue(validador.estaHabilitado());
    }
}
