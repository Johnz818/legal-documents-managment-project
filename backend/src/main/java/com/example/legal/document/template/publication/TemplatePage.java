package com.example.legal.document.template.publication;

import org.springframework.data.domain.Page;

import java.util.List;

public record TemplatePage<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> TemplatePage<T> from(Page<T> page) {
        return new TemplatePage<>(
                List.copyOf(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
