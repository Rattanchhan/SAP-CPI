package com.apsaraconsulting.skyvvaadapter.internal;

/**
 * @author Ilya Nesterov
 */
public enum SkyvvaIntegrationMode {
    SYNCHRONOUS("Synchronous"),
    ASYNCHRONOUS("Asynchronous"),
    SKYVVA_BATCH("Skyvva Batch"),
    SKYVVA_BULK("Skyvva Bulk"),
    SALESFORCE_BULK("Salesforce Bulk"),
    AUTO_SWITCH("Auto-Switch");

    private final String value;

    SkyvvaIntegrationMode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
