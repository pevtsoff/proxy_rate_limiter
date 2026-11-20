package com.example.targetapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Stub target API that the Proxy Rate Limiter protects. */
@SpringBootApplication
public class TargetApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TargetApiApplication.class, args);
    }
}
