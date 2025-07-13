package com.apsaraconsulting.adapter;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.support.ScheduledPollConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dummy Adapter Camel Consumer
 */
public class DummyAdapterConsumer extends ScheduledPollConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(DummyAdapterConsumer.class);

    private int counter = 0;

    public DummyAdapterConsumer(DummyAdapterEndpoint endpoint, Processor processor) {
        super(endpoint, processor);

        // Set the polling delay from endpoint configuration
        setDelay(endpoint.getDelay());
    }

    @Override
    protected int poll() throws Exception {
        DummyAdapterEndpoint endpoint = (DummyAdapterEndpoint) getEndpoint();

        counter++;

        // Create the dummy adapter message
        String message = String.format("%s %s! (Message #%d)",
            endpoint.getGreeting(),
            endpoint.getName() != null ? endpoint.getName() : "DummyAdapter",
            counter);

        // Apply uppercase if configured
        if (endpoint.isUppercase()) {
            message = message.toUpperCase();
        }

        // Create exchange and set message
        Exchange exchange = createExchange(false);
        exchange.getIn().setBody(message);
        exchange.getIn().setHeader("messageNumber", counter);
        exchange.getIn().setHeader("greeting", endpoint.getGreeting());
        exchange.getIn().setHeader("name", endpoint.getName());

        LOG.info("Generated dummy adapter message: {}", message);

        try {
            // Process the exchange
            getProcessor().process(exchange);
            return 1; // Indicate that 1 message was processed
        } catch (Exception e) {
            LOG.error("Error processing dummy adapter message", e);
            exchange.setException(e);
            return 0; // Indicate no messages were processed due to error
        }
    }

    @Override
    public DummyAdapterEndpoint getEndpoint() {
        return (DummyAdapterEndpoint) super.getEndpoint();
    }
}