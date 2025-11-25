package com.utn.gestion_de_turnos.service;

import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import com.utn.gestion_de_turnos.model.Reserva;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class MercadoPagoService {

    public String crearPreferenciaParaReserva(Reserva reserva) {
        try {
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title("Reserva sala " + reserva.getSala().getNumero())
                    .quantity(1)
                    .unitPrice(
                            reserva.getMonto() != null
                                    ? reserva.getMonto()
                                    : BigDecimal.ZERO
                    )
                    .currencyId("ARS")
                    .build();

            PreferencePayerRequest payer = PreferencePayerRequest.builder()
                    .email(reserva.getCliente().getEmail())
                    .name(reserva.getCliente().getNombre())
                    .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success("http://localhost:4200/pago/success")
                    .failure("http://localhost:4200/pago/failure")
                    .pending("http://localhost:4200/pago/pending")
                    .build();

            PreferenceRequest request = PreferenceRequest.builder()
                    .items(List.of(item))
                    .payer(payer)
                    .backUrls(backUrls)
                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(request);

            return preference.getInitPoint();

        } catch (MPApiException e) {
            log.error("MPApiException status={} body={}",
                    e.getApiResponse().getStatusCode(),
                    e.getApiResponse().getContent(), e);

            throw new RuntimeException("Error Mercado Pago: " + e.getApiResponse().getContent(), e);

        } catch (MPException e) {
            log.error("Error en SDK Mercado Pago", e);
            throw new RuntimeException("Error SDK Mercado Pago: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Error general al crear preferencia de pago", e);
            throw new RuntimeException("Error al crear preferencia de pago: " + e.getMessage(), e);
        }
    }
}
