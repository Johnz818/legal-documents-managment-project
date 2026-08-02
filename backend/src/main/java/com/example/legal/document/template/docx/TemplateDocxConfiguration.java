package com.example.legal.document.template.docx;

import com.example.legal.document.template.inspection.TemplateDocumentInspector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class TemplateDocxConfiguration {

    @Bean
    TemplatePackageLimits templatePackageLimits(
            @Value("${legal.document.template.max-file-size}") DataSize maximumFileSize,
            @Value("${legal.document.template.max-entry-count}") int maximumEntryCount,
            @Value("${legal.document.template.max-entry-size}") DataSize maximumEntrySize,
            @Value("${legal.document.template.max-expanded-size}") DataSize maximumExpandedSize,
            @Value("${legal.document.template.max-compression-ratio}") double maximumCompressionRatio,
            @Value("${legal.document.template.compression-ratio-min-expanded-size}") DataSize ratioMinimumSize
    ) {
        return new TemplatePackageLimits(
                maximumFileSize.toBytes(),
                maximumEntryCount,
                maximumEntrySize.toBytes(),
                maximumExpandedSize.toBytes(),
                maximumCompressionRatio,
                ratioMinimumSize.toBytes()
        );
    }

    @Bean
    TemplatePackagePreflight templatePackagePreflight(TemplatePackageLimits limits) {
        return new TemplatePackagePreflight(limits);
    }

    @Bean
    TemplateDocumentInspector templateDocumentInspector(TemplatePackagePreflight preflight) {
        return new Docx4jTemplateDocumentInspector(preflight);
    }
}
