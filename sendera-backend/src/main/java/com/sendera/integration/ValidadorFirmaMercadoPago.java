package com.sendera.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Verifica la cabecera x-signature de los webhooks de Mercado Pago: HMAC-SHA256, con la clave secreta de
 * "Tus integraciones > Webhooks", del manifiesto {@code id:{data.id};request-id:{x-request-id};ts:{ts};}.
 */
@Component
public class ValidadorFirmaMercadoPago {

    private final String secreto;

    public ValidadorFirmaMercadoPago(@Value("${app.pagos.mercadopago.webhook-secret:}") String secreto) {
        this.secreto = secreto == null ? "" : secreto.trim();
    }

    /** Sin secreto configurado no se valida la firma (solo recomendable en local). */
    public boolean estaHabilitado() {
        return !secreto.isEmpty();
    }

    public boolean esValida(String xSignature, String xRequestId, String dataId) {
        if (xSignature == null || xSignature.isBlank()) {
            return false;
        }
        String ts = null;
        String v1 = null;
        for (String parte : xSignature.split(",")) {
            String[] claveValor = parte.split("=", 2);
            if (claveValor.length != 2) {
                continue;
            }
            switch (claveValor[0].trim()) {
                case "ts" -> ts = claveValor[1].trim();
                case "v1" -> v1 = claveValor[1].trim();
                default -> { }
            }
        }
        if (ts == null || v1 == null) {
            return false;
        }

        // Las partes ausentes en la notificación se omiten del manifiesto, según la documentación de Mercado Pago.
        StringBuilder manifiesto = new StringBuilder();
        if (dataId != null && !dataId.isBlank()) {
            manifiesto.append("id:").append(dataId.toLowerCase(Locale.ROOT)).append(';');
        }
        if (xRequestId != null && !xRequestId.isBlank()) {
            manifiesto.append("request-id:").append(xRequestId).append(';');
        }
        manifiesto.append("ts:").append(ts).append(';');

        try {
            byte[] recibida = HexFormat.of().parseHex(v1);
            return MessageDigest.isEqual(firmar(manifiesto.toString()), recibida);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] firmar(String manifiesto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(manifiesto.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 no está disponible en la JVM.", e);
        }
    }
}
