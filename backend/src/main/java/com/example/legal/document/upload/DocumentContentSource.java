package com.example.legal.document.upload;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface DocumentContentSource {

    InputStream openStream() throws IOException;
}
