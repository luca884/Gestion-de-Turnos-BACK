package com.utn.gestion_de_turnos.config;

import com.google.api.client.util.Value;
import com.mercadopago.MercadoPagoConfig;
import jakarta.annotation.PostConstruct;

public class MercadoPagoConfigApp {
    @Value("${mercadopago.access-token}")
    private String accessToken;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(accessToken);
    }
}
