package com.finpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class FinPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinPulseApplication.class, args);
    }
}
