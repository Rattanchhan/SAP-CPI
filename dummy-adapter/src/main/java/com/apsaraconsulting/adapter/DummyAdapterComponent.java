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
        // Fail if URI path is missing or empty
        if (remaining == null || remaining.isEmpty()) {
            throw new IllegalArgumentException(
                "Invalid dummy-adapter endpoint URI: missing required path. Example: dummy-adapter://myTarget");
        }

        // Optionally enforce required parameter
        if (!parameters.containsKey("name")) {
            throw new IllegalArgumentException("Missing required parameter 'name' in dummy-adapter URI.");
        }
        DummyAdapterEndpoint endpoint = new DummyAdapterEndpoint(uri, this);
        // Set endpoint properties from URI parameters
        setProperties(endpoint, parameters);

        return endpoint;
    }
}