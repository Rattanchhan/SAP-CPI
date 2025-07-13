package com.apsaraconsulting.skyvvaadapter.internal;

/*
  Actually, only APEX_CALL and SEARCH
 */
public enum OperationName {

    // rest API
    GET_SOBJECT("getSObject"),
    CREATE_SOBJECT("createSObject"),
    UPDATE_SOBJECT("updateSObject"),
    DELETE_SOBJECT("deleteSObject"),
    UPSERT_SOBJECT("upsertSObject"),
    QUERY("query"),
    QUERY_MORE("queryMore"),
    QUERY_ALL("queryAll"),
    APEX_CALL("apexCall");

    private final String value;

    OperationName(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static OperationName fromValue(String value) {
        for (OperationName operationName : OperationName.values()) {
            if (operationName.value.equals(value)) {
                return operationName;
            }
        }
        throw new IllegalArgumentException(value);
    }
}
