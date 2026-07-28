package com.example.legal.document.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class DocumentUploadConfiguration {

    @Bean
    DocumentUploadValidator documentUploadValidator(
            @Value("${legal.document.upload.max-file-size}") DataSize maximumFileSize
    ) {
        return new DocumentUploadValidator(maximumFileSize.toBytes());
    }
}
