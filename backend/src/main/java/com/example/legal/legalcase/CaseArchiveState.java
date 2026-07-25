package com.example.legal.legalcase;

public enum CaseArchiveState {
    ACTIVE(false),
    ARCHIVED(true);

    private final boolean archived;

    CaseArchiveState(boolean archived) {
        this.archived = archived;
    }

    public boolean isArchived() {
        return archived;
    }
}
