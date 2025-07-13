package com.apsaraconsulting.adapter;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.spi.ComponentResolver;

/**
 * Component Resolver for Dummy Adapter Component
 */
public class DummyAdapterComponentResolver implements ComponentResolver {

    @Override
    public Component resolveComponent(String name, CamelContext context) throws Exception {
        if ("dummy-adapter".equals(name)) {
            return new DummyAdapterComponent();
        }
        return null;
    }
}