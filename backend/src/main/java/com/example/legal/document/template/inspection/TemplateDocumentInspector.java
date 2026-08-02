package com.example.legal.document.template.inspection;

public interface TemplateDocumentInspector {

    TemplateInspection inspect(byte[] docxContent);
}
