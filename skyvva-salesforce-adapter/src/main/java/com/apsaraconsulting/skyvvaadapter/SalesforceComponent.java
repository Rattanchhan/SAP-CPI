package com.apsaraconsulting.skyvvaadapter;

import lombok.Getter;
import lombok.Setter;
import org.apache.camel.*;
import com.apsaraconsulting.skyvvaadapter.api.SalesforceException;
import com.apsaraconsulting.skyvvaadapter.api.dto.AbstractSObjectBase;
import com.apsaraconsulting.skyvvaadapter.internal.OperationName;
import com.apsaraconsulting.skyvvaadapter.internal.PayloadFormat;
import com.apsaraconsulting.skyvvaadapter.internal.SalesforceSession;
import com.apsaraconsulting.skyvvaadapter.internal.client.DefaultRestClient;
import com.apsaraconsulting.skyvvaadapter.internal.client.RestClient;
import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiAdapterLoggingLevel;
import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import com.apsaraconsulting.skyvvaadapter.internal.streaming.SubscriptionHelper;
import com.apsaraconsulting.skyvvaadapter.internal.util.AdapterInitializationUtils;
import org.apache.camel.spi.Metadata;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.PropertyBindingSupport;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.util.*;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.String.format;

/**
 * Represents the component that manages {@link SalesforceEndpoint}.
 */
public class SalesforceComponent extends DefaultComponent {

    private static final CpiLoggingDecorator LOG = CpiLoggingDecorator.fromLogger(
        LoggerFactory.getLogger(SalesforceComponent.class),
        CpiLoggingDecorator.LogLevel.TRACE
    );

    static {
        AdapterInitializationUtils.loadDependenciesEagerly(LOG);
    }

    static final int CONNECTION_TIMEOUT = 60000;
    static final int IDLE_TIMEOUT = 60000;

    @Getter @Setter
    @Metadata(description = "All authentication configuration in one nested bean, all properties set there can be set"
            + " directly on the component as well", label = "common,security")
    private SalesforceLoginConfig loginConfig;


    @Getter @Setter
    @Metadata(description = "Global endpoint configuration - use to set values that are common to all endpoints",
        label = "common,advanced")
    private SalesforceEndpointConfig config;

    @Getter @Setter
    @Metadata(description = "Used to set any properties that can be configured on the LongPollingTransport used by the"
        + " BayeuxClient (CometD) used by the streaming api",
        label = "common,advanced")
    private Map<String, Object> longPollingTransportProperties;

    @Getter @Setter
    @Metadata(description = "In what packages are the generated DTO classes. Typically the classes would be generated"
        + " using camel-salesforce-maven-plugin. Set it if using the generated DTOs to gain the benefit of using short "
        + " SObject names in parameters/header values.", label = "common")
    private String[] packages;

    // component state
    private SalesforceHttpClient httpClient;

    @Getter
    private SalesforceSession session;

    @Getter
    private Map<String, Class<?>> classMap;

    // Lazily created helper for consumer endpoints
    private SubscriptionHelper subscriptionHelper;
    //private BearerTokenProvider tokenProvider;
    //private EmpConnector empConnector;

    public SalesforceComponent() {
        this(null);
    }

    public SalesforceComponent(CamelContext context) {
        super(context);
    }

    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        LOG.trace("uri: {}", URISupport.sanitizeUri(uri));

        OperationName operationName;
        if (parameters.get("restApi") != null) {
            // in fact restApi parameter is used only for Streaming API integration tests where it's needed to call some sObject API
            // as for Skyvva Salesforce Receiver Adapter UI, it only uses APEX_CALL
            operationName = OperationName.fromValue((String)parameters.get("restApi"));
        } else {
            operationName = OperationName.APEX_CALL;
        }
        String topicName = (String) parameters.get("topic");
        String apexUrl = (String) parameters.get("apexUrl");

        // create endpoint config
        if (config == null) {
            config = new SalesforceEndpointConfig();
        }

        // adding default replayId to configuration
        config.setDefaultReplayId(-1L);
        // enabling rawPayload by default if parameter is missing
        // because on CPI we don't have all possible classes in classpath
        if (parameters.get("rawPayload") == null) {
            config.setRawPayload(true);
        }

        // create a deep copy and map parameters
        final SalesforceEndpointConfig copyOfSalesforceEndpointConfig = config.copy();
        setProperties(copyOfSalesforceEndpointConfig, parameters);

        if (this.loginConfig == null) {
            loginConfig = copyOfSalesforceEndpointConfig.getLoginConfig();
        }

        // set apexUrl in endpoint config
        if (apexUrl != null) {
            copyOfSalesforceEndpointConfig.setApexUrl(apexUrl);
        }

        final SalesforceEndpoint endpoint = new SalesforceEndpoint(
                uri,
                this,
                copyOfSalesforceEndpointConfig,
                operationName,
                topicName
        );

        // map remaining parameters to endpoint (specifically, synchronous)
        setProperties(endpoint, parameters);


        // create a Jetty HttpClient if not already set
        createHttpClientIfNeeds(CpiAdapterLoggingLevel.getLoggingOptionAsLogLevel(copyOfSalesforceEndpointConfig.getLoggingOption()));

        if (copyOfSalesforceEndpointConfig.getHttpClient() == null) {
            // set the component's httpClient as default
            copyOfSalesforceEndpointConfig.setHttpClient(this.httpClient);
            config.setHttpClient(this.httpClient);//TODO potentially dangerous initialization
        }

        // override timeout settings of HTTP client if needs
        overrideTimeoutSettingsIfNeeds(endpoint);

        createSalesforceSessionAndStartHttpClient();

        return endpoint;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        LOG.trace();
        if (packages != null && packages.length > 0) {
            // parse the packages to create SObject name to class map
            classMap = parsePackages();
            LOG.info("Found {} generated classes in packages: {}", classMap.size(), Arrays.asList(packages));
        } else {
            // use an empty map to avoid NPEs later
            LOG.warn("Missing property packages, getSObject* operations will NOT work without property rawPayload=true");
            classMap = new HashMap<>(0);
        }
    }

    @Override
    protected void doStop() throws Exception {
        LOG.trace();
        if (classMap != null) {
            classMap.clear();
        }

        try {
            if (subscriptionHelper != null) {
                // shutdown all streaming connections
                // note that this is done in the component, and not in consumer
                try {
                    LOG.trace("Stopping subscription helper");
                    ServiceHelper.stopService(subscriptionHelper);
                    subscriptionHelper = null;
                } catch (Throwable ex) {
                    LOG.error("Couldn't stop subscriptionHelper: " + ex.getMessage(), ex);
                }
            }
            //if (empConnector != null) {
            //    try {
            //        empConnector.stop();
            //    } catch (Throwable ex) {
            //        LOG.error("Couldn't stop empConnector");
            //    }
            //}
            //if (session != null && session.getAccessToken() != null) {
            //    try {
            //        LOG.trace("Stopping Salesforce session");
            //        // logout of Salesforce
            //        ServiceHelper.stopService(session);
            //    } catch (SalesforceException ignoredEx) {
            //        LOG.error("Couldn't logout from Salesforce: " + ignoredEx.getMessage());
            //    }
            //}
        } finally {
            if (httpClient != null) {
                LOG.trace("Stopping HTTP client {}", httpClient);
                // shutdown http client connections
                try {
                    httpClient.stop();
                } catch (Throwable ex) {
                    LOG.error(
                        format("Couldn't stop http client %s at the component level: %s", httpClient, ex.getMessage()),
                        ex
                    );
                }
                // destroy http client if it was created by the component
                try {
                    if (config != null && config.getHttpClient() == null) {
                        LOG.error("Destroying HTTP client");
                        httpClient.destroy();
                    } if (config != null) {
                        config.setHttpClient(null);
                    }
                } catch (Throwable ex) {
                    LOG.error(
                        format("Couldn't destroy http client %s at the component level: %s", httpClient, ex.getMessage()),
                        ex
                    );
                }
                httpClient = null;
            }
            super.doStop();
        }
    }

    //public EmpConnector getEmpConnector(CpiLoggingDecorator.LogLevel logLevel) throws Exception {
    //    if (empConnector == null) {
    //        tokenProvider = new BearerTokenProvider(() -> {
    //            try {
    //                return LoginHelper.login(
    //                    new URL(loginConfig.getLoginUrl()),
    //                    loginConfig.getUserName(),
    //                    loginConfig.getPassword()
    //                );
    //            } catch (Exception ex) {
    //                LOG.error("Couldn't get access token: " + ex.getMessage(), ex);
    //                throw new RuntimeException(ex);
    //            }
    //        });

    //        LoggingListener loggingListener = new LoggingListener(logLevel);

    //        empConnector = new EmpConnector(tokenProvider.login(), logLevel);
    //        empConnector.addListener(META_HANDSHAKE, loggingListener)
    //            .addListener(META_CONNECT, loggingListener)
    //            .addListener(META_DISCONNECT, loggingListener);
    //        empConnector.setBearerTokenProvider(tokenProvider);
    //        empConnector.start().get(5, TimeUnit.SECONDS);
    //    }
    //    return empConnector;
    //}

    public SubscriptionHelper getSubscriptionHelper(CpiLoggingDecorator.LogLevel logLevel) throws Exception {
        if (subscriptionHelper == null) {
            // lazily create subscription helper
            subscriptionHelper = new SubscriptionHelper(this, logLevel);

            // also start the helper to connect to Salesforce
            LOG.trace("Starting subscription helper");
            ServiceHelper.startService(subscriptionHelper);
        }
        return subscriptionHelper;
    }

    public RestClient createRestClientFor(final SalesforceEndpoint endpoint) throws SalesforceException {
        final SalesforceEndpointConfig endpointConfig = endpoint.getConfiguration();

        return createRestClientFor(endpointConfig);
    }

    RestClient createRestClientFor(SalesforceEndpointConfig endpointConfig) throws SalesforceException {
        final String version = endpointConfig.getApiVersion();
        final PayloadFormat format = endpointConfig.getRequestFormat();

        return new DefaultRestClient(endpointConfig, version, format, session);
    }

    RestClient createRestClient(final Map<String, Object> properties) throws Exception {
        final SalesforceEndpointConfig modifiedConfig = Optional.ofNullable(config).map(SalesforceEndpointConfig::copy)
            .orElseGet(() -> new SalesforceEndpointConfig());
        final CamelContext camelContext = getCamelContext();

        PropertyBindingSupport.bindProperties(camelContext, modifiedConfig, properties);

        return createRestClientFor(modifiedConfig);
    }

    static RestClient createRestClient(final CamelContext camelContext, final Map<String, Object> properties)
        throws Exception {

        final SalesforceEndpointConfig config = new SalesforceEndpointConfig();
        // let's work with a copy so original properties are intact
        PropertyBindingSupport.bindProperties(camelContext, config, new HashMap<>(properties));

        final SalesforceLoginConfig loginConfig = new SalesforceLoginConfig();
        // let's work with a copy so original properties are intact
        PropertyBindingSupport.bindProperties(camelContext, loginConfig, new HashMap<>(properties));

        final SSLContextParameters sslContextParameters = Optional.ofNullable(camelContext.getSSLContextParameters())
            .orElseGet(() -> new SSLContextParameters());
        // let's work with a copy so original properties are intact
        PropertyBindingSupport.bindProperties(camelContext, sslContextParameters, new HashMap<>(properties));

        final SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setSslContext(sslContextParameters.createSSLContext(camelContext));

        final SalesforceHttpClient httpClient = createHttpClient(sslContextFactory, CpiLoggingDecorator.LogLevel.INFO);

        final SalesforceSession session = new SalesforceSession(camelContext, httpClient, httpClient.getTimeout(),
            loginConfig);
        httpClient.setSession(session);
        config.setHttpClient(httpClient);

        return new DefaultRestClient(config, config.getApiVersion(), config.getRequestFormat(), session);
    }

    static SalesforceHttpClient createHttpClient(final SslContextFactory sslContextFactory, CpiLoggingDecorator.LogLevel logLevel) {
        final SalesforceHttpClient httpClient = new SalesforceHttpClient(sslContextFactory, logLevel);
        LOG.debug("Setting default timeout settings to the HTTP client {}", httpClient);
        LOG.debug("Default connectTimeout: {} ms", CONNECTION_TIMEOUT);
        LOG.debug("Default idleTimeout: {} ms", IDLE_TIMEOUT);
        LOG.debug("Default timeout: {} ms", CONNECTION_TIMEOUT);
        httpClient.setConnectTimeout(CONNECTION_TIMEOUT);
        httpClient.setIdleTimeout(IDLE_TIMEOUT);
        httpClient.setTimeout(CONNECTION_TIMEOUT);

        return httpClient;
    }

    private void createHttpClientIfNeeds(CpiLoggingDecorator.LogLevel logLevel) throws Exception {
        // create a Jetty HttpClient if not already set
        if (httpClient == null) {
            final SSLContextParameters contextParameters = new SSLContextParameters();

            final SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setSslContext(contextParameters.createSSLContext(getCamelContext()));

            httpClient = createHttpClient(sslContextFactory, logLevel);
        }
    }

    private void createSalesforceSessionAndStartHttpClient() throws Exception {
        ObjectHelper.notNull(loginConfig, "loginConfig");

        // support restarts
        if (null == this.session) {
            LOG.trace("Creating new Salesforce session");
            this.session = new SalesforceSession(getCamelContext(), this.httpClient, this.httpClient.getTimeout(), loginConfig);
        }
        // set session before calling start()
        httpClient.setSession(session);

        // start the Jetty client to initialize thread pool, etc.
        httpClient.start();

        // login at startup if lazyLogin is disabled
        if (!loginConfig.isLazyLogin()) {
            ServiceHelper.startService(session);
        }
    }

    private Map<String, Class<?>> parsePackages() {
        Map<String, Class<?>> result = new HashMap<>();
        Set<Class<?>> classes = getCamelContext().adapt(ExtendedCamelContext.class).getPackageScanClassResolver().
                findImplementations(AbstractSObjectBase.class, packages);
        for (Class<?> aClass : classes) {
            // findImplementations also returns AbstractSObjectBase for some reason!!!
            if (AbstractSObjectBase.class != aClass) {
                result.put(aClass.getSimpleName(), aClass);
            }
        }

        return result;
    }

    private void overrideTimeoutSettingsIfNeeds(SalesforceEndpoint endpoint) {
        if (StringUtils.isBlank(endpoint.getConfiguration().getHttpClientTimeout())) {
            return;
        }

        try {
            int httpClientTimeout = Integer.parseInt(endpoint.getConfiguration().getHttpClientTimeout()) * 1000;
            this.httpClient.setConnectTimeout(httpClientTimeout);
            this.httpClient.setTimeout(httpClientTimeout);
            this.httpClient.setIdleTimeout(httpClientTimeout);
            LOG.debug("Http client timeout has been overridden. New value (ms): {}", httpClientTimeout);
        } catch (Exception ex) {
            LOG.warn("Couldn't override http client timeout, default will be used. Cause: {}", ex.getMessage());
        }
    }
}
