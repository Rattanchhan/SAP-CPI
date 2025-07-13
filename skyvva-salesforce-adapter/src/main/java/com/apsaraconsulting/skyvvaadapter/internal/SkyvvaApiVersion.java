package com.apsaraconsulting.skyvvaadapter.internal;

/**
 * @author Ilya Nesterov
 */
public enum SkyvvaApiVersion {
    V2("skyvvasolutions/"),
    V3("skyvvasolutions/V4/"); // don't ask why do we have V4 here, it's not a mistake

    private final String apiUrlPrefix;

    SkyvvaApiVersion(String apiUrlPrefix) {
        this.apiUrlPrefix = apiUrlPrefix;
    }

    public String getApiUrlPrefix() {
        return apiUrlPrefix;
    }
}
