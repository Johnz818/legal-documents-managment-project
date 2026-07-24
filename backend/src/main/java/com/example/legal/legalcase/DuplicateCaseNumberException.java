package com.example.legal.legalcase;

class DuplicateCaseNumberException extends RuntimeException {

    DuplicateCaseNumberException(String caseNumber) {
        super("Case number already exists: " + caseNumber);
    }
}
