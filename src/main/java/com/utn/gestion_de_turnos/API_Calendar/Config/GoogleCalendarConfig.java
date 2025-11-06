// src/main/java/com/utn/gestion_de_turnos/API_Calendar/Config/GoogleCalendarConfig.java
package com.utn.gestion_de_turnos.API_Calendar.Config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.util.Collections;

@Configuration
public class GoogleCalendarConfig {

    @Value("${google.calendar.credentials}")
    private String credentialsPath; // ej: "classpath:service-account.json"

    @Bean
    public Calendar googleCalendar(ResourceLoader loader) throws Exception {
        Resource res = loader.getResource(credentialsPath);
        try (InputStream in = res.getInputStream()) {
            GoogleCredentials creds = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singleton(CalendarScopes.CALENDAR));
            HttpRequestInitializer reqInit = new HttpCredentialsAdapter(creds);

            return new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    reqInit
            )
                    .setApplicationName("GestionDeTurnos")
                    .build();
        }
    }
}
