package com.apsaraconsulting.skyvvaadapter;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import com.apsaraconsulting.skyvvaadapter.internal.OperationName;
import com.apsaraconsulting.skyvvaadapter.internal.SkyvvaIntegrationMode;
import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import com.apsaraconsulting.skyvvaadapter.internal.logger.TokenTrimmer;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;;
import org.apache.camel.util.URISupport;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.client.HttpClient;

@UriEndpoint(scheme = "skyvva-salesforce", title = "Skyvva Salesforce", syntax = "skyvva-salesforce:operationName:topicName", label = "api,cloud,crm", consumerClass = SalesforceConsumer.class)
public class SalesforceEndpoint extends DefaultEndpoint {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(SalesforceEndpoint.class);

    @UriPath(
        label = "producer",
        description = "The operation to use",
        enums = "getSObject,createSObject,updateSObject,deleteSObject,upsertSObject,apexCall")
    private final OperationName operationName;

    @UriPath(
        label = "consumer",
        description = "The name of the topic to use")
    private final String topicName;

    @UriParam
    private final SalesforceEndpointConfig config;

    @UriParam(
        label = "consumer",
        description = "The replayId value to use when subscribing"
    )
    private Long replayId;

    public SalesforceEndpoint(
        String uri,
        SalesforceComponent salesforceComponent,
        SalesforceEndpointConfig config,
        OperationName operationName,
        String topicName
    ) {
        super(uri, salesforceComponent);
        // not good approach, as user can theoretically configure different log levels in different receiver branches of the same IFlow
        // but at the moment it's better as we won't need to propagate logging level to large amount of new classes after addition of V4 support
        CpiLoggingDecorator.setGlobalLogLevel(config.getLoggingOptionAsLogLevel());
        CpiLoggingDecorator.setUseGlobalLogLevel(true);
        LOGGER.trace("uri: {}, operationName: {}", URISupport.sanitizeUri(uri), operationName);

        this.config = config;
        this.operationName = operationName;
        this.topicName = topicName;
    }

    public Producer createProducer() throws Exception {
        LOGGER.trace();

        // producer requires an operation, topicName must be the invalid operation name
        if (operationName == null) {
            throw new IllegalArgumentException(String.format("Invalid Operation %s", topicName));
        }

        try {
            SalesforceProducer producer = new SalesforceProducer(this);
            LOGGER.trace("producer status: {}", producer.getStatus());
            LOGGER.trace("producer endpoint: {}", producer.getEndpoint());
            return producer;
        } catch (Throwable ex) {
            String errorMessage = "Couldn't create producer: " + ExceptionUtils.getMessage(ex);
            LOGGER.error(errorMessage, ex);
            throw new RuntimeException(errorMessage, ex);
        }
    }

    private boolean isSynchronous() {
        return config.getSkyvvaIntegrationMode() == SkyvvaIntegrationMode.SYNCHRONOUS;
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        LOGGER.trace();

        // consumer requires a topicName, operation name must be the valid topic name
        if (topicName == null) {
            throw new IllegalArgumentException(String.format("Invalid topic name %s, matches a producer operation name",
                    operationName.value()));
        }

        final SalesforceConsumer consumer = new SalesforceConsumer(this, processor,
                getComponent().getSubscriptionHelper(LOGGER.getLogLevel()));
        configureConsumer(consumer);

        LOGGER.trace("consumer status: {}", consumer.getStatus());
        LOGGER.trace("consumer endpoint: {}", consumer.getEndpoint());

        return consumer;
    }

    @Override
    public SalesforceComponent getComponent() {
        return (SalesforceComponent) super.getComponent();
    }

    public boolean isSingleton() {
        // re-use endpoint instance across multiple threads
        // the description of this method is a little confusing
        return true;
    }

    public SalesforceEndpointConfig getConfiguration() {
        return config;
    }

    public OperationName getOperationName() {
        return operationName;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setReplayId(final Long replayId) {
        this.replayId = replayId;
    }

    public Long getReplayId() {
        return replayId;
    }

    public String getStringifiedEndpointConfig() {
        StringBuilder messageBuilder = new StringBuilder();

        String accessToken = getComponent().getSession().getAccessToken() != null ?
            getComponent().getSession().getAccessToken() :
            "null";

        String instanceUrl = getComponent().getSession().getInstanceUrl() != null ?
            getComponent().getSession().getInstanceUrl() :
            "null";

        messageBuilder
            .append("Access token: ").append(TokenTrimmer.trim(accessToken))
            .append("\nInstance url: ").append(instanceUrl)
            .append("\nSkyvva API version: ").append(getConfiguration().getSkyvvaApiVersion())
            .append("\nIntegration: ").append(getConfiguration().getSalesforceIntegration())
            .append("\nInterface: ").append(getConfiguration().getSalesforceInterface())
            .append("\nMode: ").append(getConfiguration().getSkyvvaIntegrationMode())
            .append("\nRequest format: ").append(getConfiguration().getRequestFormat())
            .append("\nResponse format: ").append(getConfiguration().getResponseFormat())
            .append("\nSkyvva APP version: ").append(getConfiguration().getSkyvvaAppVersion())
            .append("\nApex method: ").append(getConfiguration().getApexMethod())
            .append("\nApex url: ").append(getConfiguration().getApexUrl());

        return messageBuilder.toString();
    }

    @Override
    protected void doStart() throws Exception {
        try {
            super.doStart();
        } finally {
            // check if this endpoint has its own http client that needs to be started
            final HttpClient httpClient = getConfiguration().getHttpClient();

            if (httpClient != null && getComponent().getConfig().getHttpClient() != httpClient) {
                final String endpointUri = getEndpointUri();
                LOGGER.info("Starting http client for {} ...", endpointUri);
                httpClient.start();
                LOGGER.info("Started http client for {}", endpointUri);
            }
        }
    }

    @Override
    protected void doStop() throws Exception {
        try {
            super.doStop();
        } finally {
            // check if this endpoint has its own http client that needs to be stopped
            final HttpClient httpClient = getConfiguration().getHttpClient();
            if (httpClient != null && getComponent().getConfig().getHttpClient() != httpClient) {
                final String endpointUri = getEndpointUri();
                LOGGER.info("Stopping http client for {} ...", endpointUri);
                httpClient.stop();
                LOGGER.info("Stopped http client for {}", endpointUri);
            }
        }
    }
}
