package com.example.legal.document.upload;

public class DocumentCaseNotFoundException extends RuntimeException {

    public DocumentCaseNotFoundException(Long caseId) {
        super("Case not found: " + caseId);
    }
}
