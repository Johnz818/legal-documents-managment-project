package com.example.legal.document.read;

class DocumentReadNotFoundException extends RuntimeException {

    DocumentReadNotFoundException() {
        super("Case document resource was not found");
    }
}
