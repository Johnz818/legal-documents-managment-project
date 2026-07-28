package com.example.legal.document.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class DocumentStorageConfiguration {

    @Bean
    DocumentStorage documentStorage(
            @Value("${legal.document.storage.local.root}") String storageRoot
    ) {
        return new LocalDocumentStorage(Path.of(storageRoot));
    }
}
