package com.example.legal.legalcase;

class StaleCaseVersionException extends RuntimeException {

    StaleCaseVersionException(Long id) {
        super("Case was modified by another request: " + id);
    }
}
