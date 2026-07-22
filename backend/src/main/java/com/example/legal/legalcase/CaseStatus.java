package com.example.legal.legalcase;

public enum CaseStatus {
    PENDING_FILING("待立案"),
    PRE_TRIAL_PREPARATION("审理准备"),
    IN_TRIAL("审理中"),
    CLOSED("已结案");

    private final String displayName;

    CaseStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
