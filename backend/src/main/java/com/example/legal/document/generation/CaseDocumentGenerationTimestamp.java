package com.example.legal.document.generation;

import java.time.LocalDateTime;

public interface CaseDocumentGenerationTimestamp {

    Long getCaseDocumentId();

    LocalDateTime getCreatedAt();
}
