package com.example.legal.document.generation;

import java.util.Map;

public interface TemplateDocumentRenderer {

    byte[] render(byte[] templateContent, Map<String, String> values);
}
