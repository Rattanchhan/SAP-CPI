package com.apsaraconsulting.skyvvaadapter.internal.processor;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.NoTypeConversionAvailableException;
import com.apsaraconsulting.skyvvaadapter.SalesforceComponent;
import com.apsaraconsulting.skyvvaadapter.SalesforceEndpoint;
import com.apsaraconsulting.skyvvaadapter.SalesforceEndpointConfig;
import com.apsaraconsulting.skyvvaadapter.SalesforceHttpClient;
import com.apsaraconsulting.skyvvaadapter.api.SalesforceException;
import com.apsaraconsulting.skyvvaadapter.internal.OperationName;
import com.apsaraconsulting.skyvvaadapter.internal.SalesforceSession;
import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.util.URISupport;
import org.slf4j.LoggerFactory;

import java.util.Map;

public abstract class AbstractSalesforceProcessor extends ServiceSupport implements SalesforceProcessor {

    protected static final boolean NOT_OPTIONAL = false;
    protected static final boolean IS_OPTIONAL = true;
    protected static final boolean USE_BODY = true;
    protected static final boolean IGNORE_BODY = false;

    protected final SalesforceEndpoint endpoint;
    protected final SalesforceEndpointConfig endpointConfig;
    protected final Map<String, Object> endpointConfigMap;

    protected OperationName operationName;
    protected final SalesforceSession session;
    protected final SalesforceHttpClient httpClient;
    protected final boolean rawPayload;


    private final CpiLoggingDecorator LOG;

    public AbstractSalesforceProcessor(final SalesforceEndpoint endpoint) {
        LOG = CpiLoggingDecorator.fromLogger(
                LoggerFactory.getLogger(AbstractSalesforceProcessor.class),
                endpoint.getConfiguration().getLoggingOptionAsLogLevel()
        );
        this.endpoint = endpoint;
        this.endpointConfig = endpoint.getConfiguration();
        operationName = endpoint.getOperationName();
        endpointConfigMap = endpoint.getConfiguration().toValueMap();

        final SalesforceComponent component = endpoint.getComponent();
        session = component.getSession();
        httpClient = endpoint.getConfiguration().getHttpClient();
        rawPayload = endpoint.getConfiguration().isRawPayload();

        LOG.trace("Endpoint: {}", URISupport.sanitizeUri(endpoint.getEndpointUri()));
        printEndpointConfig();
    }

    protected void printEndpointConfig() {
        LOG.trace("context: {}\n{}", this.getClass().getSimpleName(), endpoint.getStringifiedEndpointConfig());
    }

    @Override
    public abstract boolean process(Exchange exchange, AsyncCallback callback);

    /**
     * Gets String value for a parameter from header, endpoint config, or
     * exchange body (optional).
     *
     * @param exchange          exchange to inspect
     * @param convertInBody     converts In body to String value if true
     * @param propName          name of property
     * @param optional          if {@code true} returns null, otherwise throws RestException
     * @return value of property, or {@code null} for optional parameters if not found.
     * @throws SalesforceException
     *          if the property can't be found or on conversion errors.
     */
    protected final String getParameter(final String propName, final Exchange exchange, final boolean convertInBody,
        final boolean optional) throws SalesforceException {
        return getParameter(propName, exchange, convertInBody, optional, String.class);
    }

    /**
     * Gets value for a parameter from header, endpoint config, or exchange body (optional).
     *
     * @param exchange          exchange to inspect
     * @param convertInBody     converts In body to parameterClass value if true
     * @param propName          name of property
     * @param optional          if {@code true} returns null, otherwise throws RestException
     * @param parameterClass    parameter type
     * @return value of property, or {@code null} for optional parameters if not found.
     * @throws SalesforceException
     *          if the property can't be found or on conversion errors.
     */
    protected final <T> T getParameter(final String propName, final Exchange exchange, final boolean convertInBody,
        final boolean optional, final Class<T> parameterClass) throws SalesforceException {

        final Message in = exchange.getIn();
        T propValue = in.getHeader(propName, parameterClass);

        if (propValue == null) {
            // check if type conversion failed
            if (in.getHeader(propName) != null) {
                throw new IllegalArgumentException(
                    "Header " + propName + " could not be converted to type " + parameterClass.getName());
            }

            final Object value = endpointConfigMap.get(propName);

            if (value == null || parameterClass.isInstance(value)) {
                propValue = parameterClass.cast(value);
            } else {

                try {
                    propValue = exchange.getContext().getTypeConverter().mandatoryConvertTo(parameterClass, value);
                } catch (final NoTypeConversionAvailableException e) {
                    throw new SalesforceException(e);
                }
            }
        }

        propValue = propValue == null && convertInBody ? in.getBody(parameterClass) : propValue;

        // error if property was not set
        if (propValue == null && !optional) {
            final String msg = "Missing property " + propName
                + (convertInBody ? ", message body could not be converted to type " + parameterClass.getName() : "");
            throw new SalesforceException(msg, null);
        }

        return propValue;
    }

}
