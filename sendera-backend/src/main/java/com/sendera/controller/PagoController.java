package com.sendera.controller;

import com.sendera.dto.NotificacionMercadoPagoDto;
import com.sendera.dto.PreferenciaPagoDto;
import com.sendera.dto.ReservaDto;
import com.sendera.exception.FirmaWebhookInvalidaException;
import com.sendera.integration.ValidadorFirmaMercadoPago;
import com.sendera.security.UsuarioActual;
import com.sendera.service.PagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;
    private final ValidadorFirmaMercadoPago validadorFirma;

    @PostMapping("/reservas/{id}/pago")
    public ResponseEntity<PreferenciaPagoDto> iniciarPago(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(pagoService.iniciarPago(id, UsuarioActual.desde(jwt)));
    }

    @PostMapping("/reservas/{id}/pago/verificar")
    public ResponseEntity<ReservaDto> verificarPago(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(pagoService.verificarPago(id, UsuarioActual.desde(jwt)));
    }

    /**
     * Webhook público de Mercado Pago. Acepta el formato de Webhooks ({@code ?data.id=..&type=payment} y cuerpo JSON)
     * y el de IPN ({@code ?topic=payment&id=..}). Responde 200 enseguida para que Mercado Pago no reintente.
     */
    @PostMapping("/pagos/webhook")
    public ResponseEntity<Void> recibirNotificacion(
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestParam(name = "type", required = false) String tipo,
            @RequestParam(name = "topic", required = false) String topico,
            @RequestParam(name = "id", required = false) String idIpn,
            @RequestHeader(name = "x-signature", required = false) String firma,
            @RequestHeader(name = "x-request-id", required = false) String requestId,
            @RequestBody(required = false) NotificacionMercadoPagoDto cuerpo) {
        if (validadorFirma.estaHabilitado() && !validadorFirma.esValida(firma, requestId, dataId)) {
            throw new FirmaWebhookInvalidaException();
        }
        String tipoNotificacion = primeroNoVacio(tipo, cuerpo != null ? cuerpo.type() : null, topico);
        String pagoId = primeroNoVacio(dataId,
                cuerpo != null && cuerpo.data() != null ? cuerpo.data().id() : null,
                "payment".equals(topico) ? idIpn : null);
        pagoService.procesarNotificacion(tipoNotificacion, pagoId);
        return ResponseEntity.ok().build();
    }

    private static String primeroNoVacio(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor.trim();
            }
        }
        return null;
    }
}
