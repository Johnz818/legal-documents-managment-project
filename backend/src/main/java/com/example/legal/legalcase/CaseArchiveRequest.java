package com.example.legal.legalcase;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CaseArchiveRequest(
        @NotNull
        @PositiveOrZero
        Long version
) {
}
