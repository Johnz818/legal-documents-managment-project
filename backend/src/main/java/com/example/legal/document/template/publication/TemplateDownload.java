package com.example.legal.document.template.publication;

import java.io.InputStream;

public record TemplateDownload(
        String fileName,
        String contentType,
        long contentLength,
        InputStream content
) {
}
