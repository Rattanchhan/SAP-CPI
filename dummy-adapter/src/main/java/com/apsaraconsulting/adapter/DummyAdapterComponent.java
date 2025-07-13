package com.apsaraconsulting.adapter;

import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;

import java.util.Map;

/**
 * Dummy Adapter Camel Component
 */
public class DummyAdapterComponent extends DefaultComponent {

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        DummyAdapterEndpoint endpoint = new DummyAdapterEndpoint(uri, this);

        // Set endpoint properties from URI parameters
        setProperties(endpoint, parameters);

        return endpoint;
    }
}