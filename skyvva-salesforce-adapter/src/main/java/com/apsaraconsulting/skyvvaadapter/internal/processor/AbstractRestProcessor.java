package com.apsaraconsulting.skyvvaadapter.internal.processor;

import com.apsaraconsulting.skyvvaadapter.*;
import com.sap.it.api.ITApiFactory;
import com.sap.it.api.msglog.adapter.AdapterMessageLog;
import com.sap.it.api.msglog.adapter.AdapterMessageLogFactory;
import com.sap.it.api.msglog.adapter.AdapterTraceMessage;
import com.sap.it.api.msglog.adapter.AdapterTraceMessageType;
import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import com.apsaraconsulting.skyvvaadapter.api.NoSuchSObjectException;
import com.apsaraconsulting.skyvvaadapter.api.SalesforceException;
import com.apsaraconsulting.skyvvaadapter.api.dto.AbstractSObjectBase;
import com.apsaraconsulting.skyvvaadapter.internal.SkyvvaIntegrationMode;
import com.apsaraconsulting.skyvvaadapter.internal.client.RestClient;
import com.apsaraconsulting.skyvvaadapter.internal.client.RestClient.ResponseCallback;
import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import com.apsaraconsulting.skyvvaadapter.internal.v2throughv4.ConversionResult;
import com.apsaraconsulting.skyvvaadapter.internal.v2throughv4.V2XmlToJsonPayloadConverter;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static com.apsaraconsulting.skyvvaadapter.SalesforceEndpointConfig.*;
import static com.apsaraconsulting.skyvvaadapter.internal.SkyvvaApiVersion.V3;

public abstract class AbstractRestProcessor extends AbstractSalesforceProcessor {

    protected static final String RESPONSE_CLASS = AbstractRestProcessor.class.getName() + ".responseClass";
    private static final Pattern URL_TEMPLATE = Pattern.compile("\\{([^\\{\\}]+)\\}");

    private final CpiLoggingDecorator LOG;
    private RestClient restClient;
    private Map<String, Class<?>> classMap;
    private NotFoundBehaviour notFoundBehaviour;

    public AbstractRestProcessor(SalesforceEndpoint endpoint) throws SalesforceException {
        super(endpoint);
        LOG = CpiLoggingDecorator.fromLogger(
                LoggerFactory.getLogger(AbstractRestProcessor.class),
                endpoint.getConfiguration().getLoggingOptionAsLogLevel()
        );
        LOG.trace("context: {}", this.getClass().getSimpleName());

        final SalesforceEndpointConfig configuration = endpoint.getConfiguration();
        notFoundBehaviour = configuration.getNotFoundBehaviour();

        final SalesforceComponent salesforceComponent = endpoint.getComponent();

        this.restClient = salesforceComponent.createRestClientFor(endpoint);
        this.classMap = endpoint.getComponent().getClassMap();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        final SalesforceEndpointConfig configuration = endpoint.getConfiguration();
        this.notFoundBehaviour = configuration.getNotFoundBehaviour();
        final SalesforceComponent salesforceComponent = endpoint.getComponent();
        if (restClient == null) {
            this.restClient = salesforceComponent.createRestClientFor(endpoint);
        }
        ServiceHelper.startService(restClient);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        ServiceHelper.stopService(restClient);
    }

    @Override
    public final boolean process(Exchange exchange, final AsyncCallback callback) {
        // Log messages if loggingOption is set accordingly in the endpoint configuration
        String inRes = "";

        // Get in message and start bulk procedure
        inRes = exchange.getIn().getBody(String.class);

        // Set the in message back into the exchange
        exchange.getIn().setBody(inRes);

        // pre-process request message
        try {
            processRequest(exchange);
        } catch (SalesforceException e) {
            exchange.setException(e);
            callback.done(true);
            return true;
        } catch (RuntimeException e) {
            exchange.setException(new SalesforceException(e.getMessage(), e));
            callback.done(true);
            return true;
        }

        // call Salesforce asynchronously
        try {
            // call Operation using REST client
            switch (operationName) {
            case GET_SOBJECT:
                processGetSobject(exchange, callback);
                break;
            case CREATE_SOBJECT:
                processCreateSobject(exchange, callback);
                break;
            case UPDATE_SOBJECT:
                processUpdateSobject(exchange, callback);
                break;
            case DELETE_SOBJECT:
                processDeleteSobject(exchange, callback);
                break;
            case UPSERT_SOBJECT:
                processUpsertSobject(exchange, callback);
                break;
            case QUERY:
                processQuery(exchange, callback);
                break;
            case QUERY_MORE:
                processQueryMore(exchange, callback);
                break;
            case QUERY_ALL:
                processQueryAll(exchange, callback);
                break;
            case APEX_CALL:
                processApexCall(exchange, callback);
                break;
            default:
                throw new SalesforceException("Unknown operation name: " + operationName.value(), null);
            }
        } catch (SalesforceException e) {
            exchange.setException(new SalesforceException(
                    format("Error processing %s: [%s] \"%s\"",
                            operationName.value(), e.getStatusCode(), e.getMessage()),
                    e));
            callback.done(true);
            return true;
        } catch (RuntimeException e) {
            exchange.setException(new SalesforceException(
                    format("Unexpected Error processing %s: \"%s\"",
                            operationName.value(), e.getMessage()),
                    e));
            callback.done(true);
            return true;
        }

        // continue routing asynchronously
        return false;
    }

    private void processGetSobject(final Exchange exchange, final AsyncCallback callback) throws SalesforceException {
        String sObjectName;
        String sObjectIdValue;
        // determine parameters from input AbstractSObject
        final AbstractSObjectBase sObjectBase = exchange.getIn().getBody(AbstractSObjectBase.class);
        if (sObjectBase != null) {
            sObjectName = sObjectBase.getClass().getSimpleName();
            sObjectIdValue = sObjectBase.getId();
        } else {
            sObjectName = getParameter(SOBJECT_NAME, exchange, IGNORE_BODY, NOT_OPTIONAL);
            sObjectIdValue = getParameter(SOBJECT_ID, exchange, USE_BODY, NOT_OPTIONAL);
        }
        final String sObjectId = sObjectIdValue;

        // use sObject name to load class
        //setResponseClass(exchange, sObjectName); // Commenting - Sreeni - not required in CPI

        // get optional field list
        String fieldsValue = getParameter(SOBJECT_FIELDS, exchange, IGNORE_BODY, IS_OPTIONAL);
        String[] fields = null;
        if (fieldsValue != null) {
            fields = fieldsValue.split(",");
        }

        restClient.getSObject(sObjectName, sObjectId, fields, determineHeaders(exchange), processWithResponseCallback(exchange, callback));
    }

    private void processCreateSobject(final Exchange exchange, final AsyncCallback callback) throws SalesforceException {
        String sObjectName;
        // determine parameters from input AbstractSObject
        AbstractSObjectBase sObjectBase = exchange.getIn().getBody(AbstractSObjectBase.class);
        if (sObjectBase != null) {
            sObjectName = sObjectBase.getClass().getSimpleName();
        } else {
            sObjectName = getParameter(SOBJECT_NAME, exchange, IGNORE_BODY, NOT_OPTIONAL);
        }

        restClient.createSObject(sObjectName, getRequestStream(exchange), determineHeaders(exchange),
            processWithResponseCallback(exchange, callback));
    }

    private void processUpdateSobject(final Exchange exchange, final AsyncCallback callback) throws SalesforceException {
        String sObjectName;
        // determine parameters from input AbstractSObject
        final AbstractSObjectBase sObjectBase = exchange.getIn().getBody(AbstractSObjectBase.class);
        String sObjectId;
        if (sObjectBase != null) {
            sObjectName = sObjectBase.getClass().getSimpleName();
            // remember the sObject Id
            sObjectId = sObjectBase.getId();
            // clear base object fields, which cannot be updated
            sObjectBase.clearBaseFields();
        } else {
            sObjectName = getParameter(SOBJECT_NAME, exchange, IGNORE_BODY, NOT_OPTIONAL);
            sObjectId = getParameter(SOBJECT_ID, exchange, IGNORE_BODY, NOT_OPTIONAL);
        }

        final String finalsObjectId = sObjectId;
        restClient.updateSObject(sObjectName, sObjectId, getRequestStream(exchange), determineHeaders(exchange),
            new RestClient.ResponseCallback() {
                @Override
                public void onResponse(InputStream response, Map<String, String> headers, SalesforceException exception) {
                    processResponse(exchange, response, headers, exception, callback);
                    restoreFields(exchange, sObjectBase, finalsObjectId, null, null);
                }
            });
    }

    private void processDeleteSobject(final Exchange exchange, final AsyncCallback callback) throws SalesforceException {
        String sObjectName;
        // determine parameters from input AbstractSObject
        final AbstractSObjectBase sObjectBase = exchange.getIn().getBody(AbstractSObjectBase.class);
        String sObjectIdValue;
        if (sObjectBase != null) {
            sObjectName = sObjectBase.getClass().getSimpleName();
            sObjectIdValue = sObjectBase.getId();
        } else {
            sObjectName = getParameter(SOBJECT_NAME, exchange, IGNORE_BODY, NOT_OPTIONAL);
            sObjectIdValue = getParameter(SOBJECT_ID, exchange, USE_BODY, NOT_OPTIONAL);
        }
        final String sObjectId = sObjectIdValue;

        restClient.deleteSObject(sObjectName, sObjectId, determineHeaders(exchange), new RestClient.ResponseCallback() {
            @Override
            public void onResponse(InputStream response, Map<String, String> headers, SalesforceException exception) {
                processResponse(exchange, response, headers, exception, callback);
                restoreFields(exchange, sObjectBase, sObjectId, null, null);
            }
        });
    }

    private void processUpsertSobject(final Exchange exchange, final AsyncCallback callback) throws SalesforceException {
        String sObjectName;
        String sObjectExtIdValue;
        final String sObjectExtIdName = getParameter(SOBJECT_EXT_ID_NAME, exchange,
                IGNORE_BODY, NOT_OPTIONAL);

        // determine parameters from input AbstractSObject
        Object oldValue = null;
        final AbstractSObjectBase sObjectBase = exchange.getIn().getBody(AbstractSObjectBase.class);
        if (sObjectBase != null) {
            sObjectName = sObjectBase.getClass().getSimpleName();
            oldValue = getAndClearPropertyValue(sObjectBase, sObjectExtIdName);
            sObjectExtIdValue = oldValue.toString();
            // clear base object fields, which cannot be updated
            sObjectBase.clearBaseFields();
        } else {
            sObjectName = getParameter(SOBJECT_NAME, exchange, IGNORE_BODY, NOT_OPTIONAL);
            sObjectExtIdValue = getParameter(SOBJECT_EXT_ID_VALUE, exchange, IGNORE_BODY, NOT_OPTIONAL);
        }

        final Object finalOldValue = oldValue;
        restClient.upsertSObject(sObjectName, sObjectExtIdName, sObjectExtIdValue, determineHeaders(exchange), getRequestStream(exchange),
            new RestClient.ResponseCallback() {
                @Override
                public void onResponse(InputStream response, Map<String, String> headers, SalesforceException exception) {
                    processResponse(exchange, response, headers, exception, callback);
                    restoreFields(exchange, sObjectBase, null, sObjectExtIdName, finalOldValue);
                }
            });
    }

    private void processQuery(final Exchange exchange, final AsyncCallback callback) throws SalesforceException {
        //final String sObjectQuery = getParameter(SOBJECT_QUERY, exchange, USE_BODY, NOT_OPTIONAL);
    	final String sObjectQuery = getParameter(SOBJECT_QUERY, exchange, IGNORE_BODY, IS_OPTIONAL); // Trying from parameters .. Sreeni
        //String apexMethod = getParameter(APEX_METHOD, exchange, IGNORE_BODY, IS_OPTIONAL); apexCall

        // use custom response class property
        //setResponseClass(exchange, null); // Not required any map back to class .. Sreeni

        restClient.query(sObjectQuery, determineHeaders(exchange), processWithResponseCallback(exchange, callback));
    }

    private void processQueryMore(final Exchange exchange, final AsyncCallback callback) throws SalesforceException {
        // reuse SOBJECT_QUERY parameter name for nextRecordsUrl
        final String nextRecordsUrl = getParameter(SOBJECT_QUERY, exchange, USE_BODY, NOT_OPTIONAL);

        // use custom response class property
       // setResponseClass(exchange, null); // Commenting out .. Sreeni

        restClient.queryMore(nextRecordsUrl, determineHeaders(exchange), processWithResponseCallback(exchange, callback));
    }

    private void processQueryAll(final Exchange exchange, final AsyncCallback callback) throws SalesforceException {
        final String sObjectQuery = getParameter(SOBJECT_QUERY, exchange, USE_BODY, NOT_OPTIONAL);

        // use custom response class property
        setResponseClass(exchange, null);

        restClient.queryAll(sObjectQuery, determineHeaders(exchange), processWithResponseCallback(exchange, callback));
    }

    private void processApexCall(final Exchange exchange, final AsyncCallback callback) throws SalesforceException {

        String apexMethod;
        String apexUrl;
        Map<String, Object> queryParams;

        LOG.trace("skyvvaApiVersion: {}", endpointConfig.getSkyvvaApiVersion());

        byte[] requestPayload;
        if (V3.equals(endpointConfig.getSkyvvaApiVersion())) {
            apexMethod = "POST";

            String integrateUrl = "integrate";
            apexUrl = endpointConfig.getSkyvvaApiVersion().getApiUrlPrefix() + integrateUrl;

            LOG.trace("apexUrl: {}", apexUrl);
            LOG.trace("apexMethod: {}", apexMethod);

            queryParams = new HashMap<>();

            queryParams.put("integration", endpointConfig.getSalesforceIntegration());
            queryParams.put("interface", endpointConfig.getSalesforceInterface());
            queryParams.put("mode", endpointConfig.getSkyvvaIntegrationMode().value());
            queryParams.put("request-format", endpointConfig.getRequestFormat());
            queryParams.put("response-format", endpointConfig.getResponseFormat());
            queryParams.put("transferId", exchange.getIn().getHeader("SAP_MessageProcessingLogID"));

            requestPayload = exchange.getIn().getBody(byte[].class);
        } else {
            apexUrl = getApexUrl(exchange);
            apexMethod = getParameter(AdapterUiParameters.APEX_METHOD.getParameterName(), exchange, IGNORE_BODY, IS_OPTIONAL);
            // default to GET
            if (apexMethod == null) {
                apexMethod = "GET";
                LOG.debug("Using HTTP GET method by default for APEX REST call for {}", apexUrl);
            }

            queryParams = new HashMap<>();

            if (apexUrl.contains("/V4/")) {
                // this is special case when V2 payload is used together with V4 API
                String originalRequestPayload = exchange.getIn().getBody(String.class);

                V2XmlToJsonPayloadConverter converter = new V2XmlToJsonPayloadConverter();
                ConversionResult conversionResult = converter.convert(originalRequestPayload);

                String skyvvaApi = StringUtils.substringAfterLast(apexUrl, "/V4/");
                SkyvvaIntegrationMode skyvvaIntegrationMode = determineMode(skyvvaApi);

                queryParams.put("integration", conversionResult.getSkyvvaIntegration());
                queryParams.put("interface", conversionResult.getSkyvvaInterface());
                queryParams.put("mode", skyvvaIntegrationMode.value());
                queryParams.put("request-format", "JSON");
                queryParams.put("response-format", "JSON");
                queryParams.put("runtime-param", format(
                    "{\"__SKYVVA__API_Version\": \"V2\", \"__SKYVVA__V2_Mode\":\"%s\" }",
                    skyvvaApi
                ));
                queryParams.put("transferId", exchange.getIn().getHeader("SAP_MessageProcessingLogID"));

                traceConvertedPayload(exchange,
                    conversionResult,
                    queryParams
                );

                // original XML is UTF-8 encoded
                requestPayload = conversionResult.getV2BasedJsonPayload().getBytes(StandardCharsets.UTF_8);
            } else {
                requestPayload = exchange.getIn().getBody(byte[].class);
            }

            LOG.trace("apexUrl: {}", apexUrl);
            LOG.trace("apexMethod: {}", apexMethod);


            LOG.trace("query params: {}", queryParams);
        }

        restClient.apexCall(
            apexMethod,
            apexUrl,
            queryParams,
            requestPayload,
            determineHeaders(exchange),
            processWithResponseCallback(exchange, callback)
        );
    }

    private void traceConvertedPayload(
        Exchange exchange,
        ConversionResult conversionResult,
        Map<String, Object> queryParams
    ) {
        try {
            AdapterMessageLogFactory adapterMessageLogFactory = getAdapterMessageLogFactory();
            if (adapterMessageLogFactory == null) {
                // service is unavailable on local environment
                return;
            }

            AdapterMessageLog mplLog = adapterMessageLogFactory.getMessageLog(
                exchange,
                "Prepared V2-based JSON for sending through V4 API",
                "SkyvvaSalesforce",
                UUID.randomUUID().toString()
            );
            if (!mplLog.isTraceActive()) {
                return;
            }
            // if you have a fault inbound message then specify AdapterTraceMessageType.RECEIVER_INBOUND_FAULT,
            // if you have a fault outbound message then specify AdapterTraceMessageType.SENDER_OUTBOUND_FAULT
            // for synchronous adapters you may also need AdapterTraceMessageType.SENDER_OUTBOUND and AdapterTraceMessageType.RECEIVER_INBOUND
            AdapterTraceMessageType type = AdapterTraceMessageType.RECEIVER_OUTBOUND;

            AdapterTraceMessage traceMessage = mplLog.createTraceMessage(
                type,
                conversionResult.getV2BasedJsonPayload().getBytes(StandardCharsets.UTF_8),
                false
            );//Setting isTruncated as false assuming traceData is less than 25MB.

            // Encoding is optional, but should be set if available.
            traceMessage.setEncoding("UTF-8");

            Map<String, String> traceMessageHeaders = new HashMap<>();
            queryParams.forEach((key, value) -> traceMessageHeaders.put(key, value.toString()));
            traceMessage.setHeaders(traceMessageHeaders);

            mplLog.writeTrace(traceMessage);
        } catch (Exception ex) {
            LOG.error("Couldn't trace V2-based JSON payload: " + ex.getMessage(), ex);
        }
    }

    @Deprecated
    private String getApexUrl(Exchange exchange) throws SalesforceException {
        final String apexUrl = getParameter(AdapterUiParameters.APEX_URL.getParameterName(), exchange, IGNORE_BODY, NOT_OPTIONAL);

        final Matcher matcher = URL_TEMPLATE.matcher(apexUrl);
        StringBuilder result = new StringBuilder();
        int start = 0;
        while (matcher.find()) {
            // append part before parameter template
            result.append(apexUrl.substring(start, matcher.start()));
            start = matcher.end();

            // append template value from exchange header
            final String parameterName = matcher.group(1);
            final Object value = exchange.getIn().getHeader(parameterName);
            if (value == null) {
                throw new IllegalArgumentException("Missing APEX URL template header " + parameterName);
            }
            try {
                result.append(URLEncoder.encode(String.valueOf(value), "UTF-8").replaceAll("\\+", "%20"));
            } catch (UnsupportedEncodingException e) {
                throw new SalesforceException("Unexpected error: " + e.getMessage(), e);
            }
        }
        if (start != 0) {
            // append remaining URL
            result.append(apexUrl.substring(start));
            final String resolvedUrl = result.toString();
            LOG.trace("Resolved APEX URL {} to {}", apexUrl, resolvedUrl);
            return resolvedUrl;
        }
        return apexUrl;
    }

    private SkyvvaIntegrationMode determineMode(String skyvvaApi) {
        switch (skyvvaApi) {
            case "integrate": return SkyvvaIntegrationMode.ASYNCHRONOUS;
            case "integrateSynchronous": return SkyvvaIntegrationMode.SYNCHRONOUS;
            case "integrateBatch": return SkyvvaIntegrationMode.SKYVVA_BATCH;
            default: throw new IllegalArgumentException(format("Unsupported skyvva Api %s for V4 mode detection. " +
                "Only integrate, integrateSynchronous and integrateBatch are supported", skyvvaApi));
        }
    }

    private void restoreFields(Exchange exchange, AbstractSObjectBase sObjectBase,
                               String sObjectId, String sObjectExtIdName, Object oldValue) {
        // restore fields
        if (sObjectBase != null) {
            // restore the Id if it was cleared
            if (sObjectId != null) {
                sObjectBase.setId(sObjectId);
            }
            // restore the external id if it was cleared
            if (sObjectExtIdName != null && oldValue != null) {
                try {
                    setPropertyValue(sObjectBase, sObjectExtIdName, oldValue);
                } catch (SalesforceException e) {
                    // YES, the exchange may fail if the property cannot be reset!!!
                    exchange.setException(e);
                }
            }
        }
    }

    private void setPropertyValue(Object sObjectBase, String name, Object value) throws SalesforceException {
        try {
            // set the value with the set method
            Method setMethod = sObjectBase.getClass().getMethod("set" + name, value.getClass());
            setMethod.invoke(sObjectBase, value);
        } catch (NoSuchMethodException e) {
            throw new SalesforceException(
                    format("SObject %s does not have a field %s",
                            sObjectBase.getClass().getName(), name),
                    e);
        } catch (InvocationTargetException e) {
            throw new SalesforceException(
                    format("Error setting value %s.%s",
                            sObjectBase.getClass().getSimpleName(), name),
                    e);
        } catch (IllegalAccessException e) {
            throw new SalesforceException(
                    format("Error accessing value %s.%s",
                            sObjectBase.getClass().getSimpleName(), name),
                    e);
        }
    }

    private Object getAndClearPropertyValue(AbstractSObjectBase sObjectBase, String propertyName) throws SalesforceException {
        try {
            // obtain the value using the get method
            Method getMethod = sObjectBase.getClass().getMethod("get" + propertyName);
            Object value = getMethod.invoke(sObjectBase);

            // clear the value with the set method
            Method setMethod = sObjectBase.getClass().getMethod("set" + propertyName, getMethod.getReturnType());
            setMethod.invoke(sObjectBase, new Object[]{null});

            return value;
        } catch (NoSuchMethodException e) {
            throw new SalesforceException(
                    format("SObject %s does not have a field %s",
                            sObjectBase.getClass().getSimpleName(), propertyName),
                    e);
        } catch (InvocationTargetException e) {
            throw new SalesforceException(
                    format("Error getting/setting value %s.%s",
                            sObjectBase.getClass().getSimpleName(), propertyName),
                    e);
        } catch (IllegalAccessException e) {
            throw new SalesforceException(
                    format("Error accessing value %s.%s",
                            sObjectBase.getClass().getSimpleName(), propertyName),
                    e);
        }
    }

    // pre-process request message
    protected abstract void processRequest(Exchange exchange) throws SalesforceException;

    // get request stream from In message
    protected abstract InputStream getRequestStream(Exchange exchange) throws SalesforceException;

    /**
     * Returns {@link InputStream} to serialized form of the given object.
     * 
     * @param object
     *            object to serialize
     * @return stream to read serialized object from
     */
    protected abstract InputStream getRequestStream(Message in, Object object) throws SalesforceException;

    private void setResponseClass(Exchange exchange, String sObjectName) throws SalesforceException {

        // nothing to do if using rawPayload
        if (rawPayload) {
            return;
        }

        Class<?> sObjectClass;

        if (sObjectName != null) {
            // lookup class from class map
            sObjectClass = classMap.get(sObjectName);
            if (null == sObjectClass) {
                throw new SalesforceException(format("No class found for SObject %s", sObjectName), null);
            }

        } else {

            // use custom response class property
            final String className = getParameter(SOBJECT_CLASS, exchange, IGNORE_BODY, NOT_OPTIONAL);
            try {
                sObjectClass = endpoint.getComponent().getCamelContext()
                        .getClassResolver().resolveMandatoryClass(className);
            } catch (ClassNotFoundException e) {
                throw new SalesforceException(
                        format("SObject class not found %s, %s",
                                className, e.getMessage()),
                        e);
            }
        }
        exchange.setProperty(RESPONSE_CLASS, sObjectClass);
    }

    final ResponseCallback processWithResponseCallback(final Exchange exchange, final AsyncCallback callback) {
        return (response, headers, exception) -> processResponse(exchange, response, headers, exception, callback);
    }

    // process response entity and set out message in exchange
    protected abstract void processResponse(Exchange exchange, InputStream responseEntity, Map<String, String> headers, 
        SalesforceException ex, AsyncCallback callback);

    final boolean shouldReport(SalesforceException ex) {
        return !(ex instanceof NoSuchSObjectException && notFoundBehaviour == NotFoundBehaviour.NULL);
    }

    private AdapterMessageLogFactory getAdapterMessageLogFactory() {
        try {
            return ITApiFactory.getService(AdapterMessageLogFactory.class, null);
        } catch (Exception ex) {
            LOG.warn("Couldn't load AdapterMessageLogFactory service: " + ex.getMessage());
        }
        return null;
    }
}
