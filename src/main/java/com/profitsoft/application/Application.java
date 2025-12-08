package com.profitsoft.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main application entry point.
 * Provides console interface for statistics generation.
 */
@Slf4j
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.profitsoft.application.repository")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        log.info("Book-Author REST API on http://localhost:8080");
        log.info("Swagger on http://localhost:8080/swagger-ui.html");
        log.info("Postman Collection in Postman_Collection.json");
    }
}