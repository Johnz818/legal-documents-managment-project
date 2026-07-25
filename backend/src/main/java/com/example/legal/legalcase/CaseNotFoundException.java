package com.example.legal.legalcase;

class CaseNotFoundException extends RuntimeException {

    CaseNotFoundException(Long id) {
        super("Case not found: " + id);
    }
}
