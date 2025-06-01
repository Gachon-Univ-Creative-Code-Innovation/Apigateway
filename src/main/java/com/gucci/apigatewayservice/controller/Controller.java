package com.gucci.apigatewayservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Slf4j
public class Controller {

    private final Environment environment;

    @GetMapping("/health-check")
    public Mono<String> healthCheck() {
        log.info("✅ health-check endpoint hit!");

        String port = environment.getProperty("local.server.port");
        return Mono.just("API Gateway Service is running on port: " + (port != null ? port : "unknown"));
    }
}