package com.apsaraconsulting.skyvvaadapter.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.apsaraconsulting.skyvvaadapter.api.dto.RestError;

import java.util.List;

/**
 * Class that holds {@link TypeReference} instances needed for Jackson mapper to support generics.
 */
public final class TypeReferences {

    public static final TypeReference<List<RestError>> REST_ERROR_LIST_TYPE = new TypeReference<List<RestError>>() {
    };

    public static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {
    };

    private TypeReferences() {
        // not meant for instantiation, only for TypeReference constants
    }

}
