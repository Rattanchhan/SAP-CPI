package com.apsaraconsulting.adapter;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dummy Adapter Camel Producer
 */
public class DummyAdapterProducer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(DummyAdapterProducer.class);

    public DummyAdapterProducer(DummyAdapterEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        DummyAdapterEndpoint endpoint = (DummyAdapterEndpoint) getEndpoint();

        // Get the message body
        String body = exchange.getIn().getBody(String.class);

        // Create the dummy adapter message
        String message;
        if (body != null && !body.trim().isEmpty()) {
            message = String.format("%s %s from %s!",
                endpoint.getGreeting(),
                body,
                endpoint.getName() != null ? endpoint.getName() : "DummyAdapter");
        } else {
            message = String.format("%s %s!",
                endpoint.getGreeting(),
                endpoint.getName() != null ? endpoint.getName() : "DummyAdapter");
        }

        // Apply uppercase if configured
        if (endpoint.isUppercase()) {
            message = message.toUpperCase();
        }

        // Set the response
        exchange.getIn().setBody(message);

        LOG.info("Processed dummy adapter message: {}", message);
    }

    @Override
    public DummyAdapterEndpoint getEndpoint() {
        return (DummyAdapterEndpoint) super.getEndpoint();
    }
}