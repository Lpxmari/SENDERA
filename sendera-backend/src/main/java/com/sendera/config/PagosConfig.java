package com.sendera.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.List;

@Configuration
public class PagosConfig {

    /** Cliente HTTP de la API de Mercado Pago con tiempos de espera acotados (el webhook debe responder en menos de 22 s). */
    @Bean
    public RestClient mercadoPagoRestClient(RestClient.Builder builder,
                                            @Value("${app.pagos.mercadopago.api-url:https://api.mercadopago.com}") String apiUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(15_000);
        return builder
                .requestFactory(requestFactory)
                .baseUrl(apiUrl)
                .defaultHeaders(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                })
                .build();
    }
}
