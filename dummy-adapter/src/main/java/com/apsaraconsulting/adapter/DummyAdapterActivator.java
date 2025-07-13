package com.apsaraconsulting.adapter;

import org.apache.camel.spi.ComponentResolver;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * OSGi Bundle Activator for Dummy Adapter Component
 */
public class DummyAdapterActivator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(DummyAdapterActivator.class);

    private ServiceRegistration<ComponentResolver> serviceRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        LOG.info("Starting Dummy Adapter Bundle");

        // Register the component resolver service
        DummyAdapterComponentResolver resolver = new DummyAdapterComponentResolver();

        Dictionary<String, Object> props = new Hashtable<>();
        props.put("component", "dummy-adapter");

        serviceRegistration = context.registerService(
            ComponentResolver.class,
            resolver,
            props
        );

        LOG.info("Dummy Adapter Component registered successfully");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        LOG.info("Stopping Dummy Adapter Bundle");

        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }

        LOG.info("Dummy Adapter Bundle stopped");
    }
}