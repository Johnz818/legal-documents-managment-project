package com.example.legal.document.template.docx;

public record TemplatePackageLimits(
        long maximumFileSize,
        int maximumEntryCount,
        long maximumEntrySize,
        long maximumExpandedSize,
        double maximumCompressionRatio,
        long compressionRatioMinimumExpandedSize
) {

    public TemplatePackageLimits {
        if (maximumFileSize <= 0
                || maximumEntryCount <= 0
                || maximumEntrySize <= 0
                || maximumExpandedSize <= 0
                || maximumCompressionRatio <= 0
                || compressionRatioMinimumExpandedSize < 0) {
            throw new IllegalArgumentException("Template package limits must be positive");
        }
    }
}
