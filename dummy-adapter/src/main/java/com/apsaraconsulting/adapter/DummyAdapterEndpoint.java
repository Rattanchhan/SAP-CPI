package com.apsaraconsulting.adapter;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

/**
 * Dummy Adapter Camel Endpoint
 */
@UriEndpoint(firstVersion = "1.0.0", scheme = "dummy-adapter", title = "Dummy Adapter",
    syntax = "dummy-adapter:name", producerOnly = false, consumerOnly = false)
public class DummyAdapterEndpoint extends DefaultEndpoint {

    @UriPath
    private String name;

    @UriParam(defaultValue = "Hello")
    private String greeting = "Hello";

    @UriParam(defaultValue = "false")
    private boolean uppercase = false;

    @UriParam(defaultValue = "1000")
    private long delay = 1000;

    public DummyAdapterEndpoint(String uri, DummyAdapterComponent component) {
        super(uri, component);
    }

    @Override
    public Producer createProducer() throws Exception {
        return new DummyAdapterProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        DummyAdapterConsumer consumer = new DummyAdapterConsumer(this, processor);
        configureConsumer(consumer);
        return consumer;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public boolean isUppercase() {
        return uppercase;
    }

    public void setUppercase(boolean uppercase) {
        this.uppercase = uppercase;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }
}