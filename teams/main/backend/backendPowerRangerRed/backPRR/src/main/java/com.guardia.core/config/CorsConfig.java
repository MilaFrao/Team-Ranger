package com.guardia.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración CORS — permite peticiones del frontend React (Vite).
 *
 * Puertos habilitados:
 *   3002 → equipo rojo (módulo registro de incidentes)   ← NUEVO
 *   3001 → consumer app (módulo anterior)
 *   5173 → Vite en modo por defecto
 *
 * Si en producción el frontend está en otro dominio,
 * agrega su URL a allowedOrigins.
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")           // cubre /api/v1/** y /api/expedientes/**
                        .allowedOrigins(
                                "http://localhost:3000", // login-mfe
                                "http://localhost:3001", // consumer app
                                "http://localhost:3002", // equipo rojo ← AÑADIDO
                                "http://localhost:5173"  // Vite default
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
