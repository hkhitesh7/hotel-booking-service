package com.rupeek.hotelbooking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hotelBookingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Hotel Booking API")
                .version("v1")
                .description("Discover, onboard, book, pay for and cancel hotel stays."));
    }
}
