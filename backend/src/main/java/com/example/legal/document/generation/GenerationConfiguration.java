package com.example.legal.document.generation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class GenerationConfiguration {

    @Bean
    Clock generationClock() {
        return Clock.systemUTC();
    }
}
