package com.apsaraconsulting.adapter;

import lombok.Getter;
import lombok.Setter;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

/**
 * Dummy Adapter Camel Endpoint
 */
@Setter
@Getter
@UriEndpoint(firstVersion = "1.0.0", scheme = "dummy-adapter", title = "Dummy Adapter",
    syntax = "dummy-adapter:name", producerOnly = false, consumerOnly = false)
public class DummyAdapterEndpoint extends DefaultEndpoint {

    /// Getters and Setters
    @UriPath
    private String name;

    @UriParam
    private String httpMethod;

    @UriParam
    private String url;

    @UriParam
    private String username;

    @UriParam
    private String password;

    @UriParam
    private String clientId;

    @UriParam
    private String clientSecret;

    @UriParam
    private String targetSystem;

    @UriParam
    private String tokenEndpoint;

    @UriParam
    private String salesforceAuth;

    @UriParam
    private String wiremockAuth;

    @UriParam(defaultValue = "Hello")
    private String greeting;

    @UriParam(defaultValue = "false")
    private boolean uppercase;

    @UriParam(defaultValue = "1000")
    private long delay ;

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

}