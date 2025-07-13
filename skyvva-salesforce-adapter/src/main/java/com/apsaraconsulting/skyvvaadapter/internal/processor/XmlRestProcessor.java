package com.apsaraconsulting.skyvvaadapter.internal.processor;

import com.apsaraconsulting.skyvvaadapter.api.dto.*;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import com.apsaraconsulting.skyvvaadapter.SalesforceEndpoint;
import com.apsaraconsulting.skyvvaadapter.api.SalesforceException;
import com.apsaraconsulting.skyvvaadapter.api.utils.XmlUtils;
import org.eclipse.jetty.util.StringUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.apsaraconsulting.skyvvaadapter.SalesforceEndpointConfig.SOBJECT_NAME;

public class XmlRestProcessor extends AbstractRestProcessor {

    private static final String RESPONSE_ALIAS = XmlRestProcessor.class.getName() + ".responseAlias";

    public XmlRestProcessor(SalesforceEndpoint endpoint) throws SalesforceException {
        super(endpoint);
    }

    @Override
    protected void processRequest(Exchange exchange) throws SalesforceException {
        switch (operationName) {
            case GET_SOBJECT:
                // need to add alias for Salesforce XML that uses SObject name as root element
                exchange.setProperty(RESPONSE_ALIAS, getParameter(SOBJECT_NAME, exchange, IGNORE_BODY, NOT_OPTIONAL));
                break;

            case CREATE_SOBJECT:
                // handle known response type
                exchange.setProperty(RESPONSE_CLASS, CreateSObjectResult.class);
                break;

            case UPSERT_SOBJECT:
                // handle known response type
                exchange.setProperty(RESPONSE_CLASS, CreateSObjectResult.class);
                break;

            case QUERY:
            case QUERY_ALL:
            case QUERY_MORE:
                // need to add alias for Salesforce XML that uses SObject name as root element
                exchange.setProperty(RESPONSE_ALIAS, "QueryResult");
                break;

            case APEX_CALL:
                // need to add alias for Salesforce XML that uses SObject name as root element
                exchange.setProperty(RESPONSE_ALIAS, "response");
                break;

            default:
                // ignore, some operations do not require alias or class exchange properties
            }
    }

    protected InputStream getRequestStream(Exchange exchange) throws SalesforceException {
        try {
            // get request stream from In message
            Message in = exchange.getIn();
            InputStream request = in.getBody(InputStream.class);
            if (request == null) {
                AbstractDTOBase dto = in.getBody(AbstractDTOBase.class);
                if (dto != null) {
                    // marshall the DTO
                    request = getRequestStream(in, dto);
                } else {
                    // if all else fails, get body as String
                    final String body = in.getBody(String.class);
                    if (null == body) {
                        String msg = "Unsupported request message body "
                            + (in.getBody() == null ? null : in.getBody().getClass());
                        throw new SalesforceException(msg, null);
                    } else {
                        request = new ByteArrayInputStream(body.getBytes(StringUtil.__UTF8));
                    }
                }
            }
            return request;
        } catch (UnsupportedEncodingException e) {
            String msg = "Error marshaling request: " + e.getMessage();
            throw new SalesforceException(msg, e);
        }
    }

    @Override
    protected InputStream getRequestStream(final Message in, final Object object) throws SalesforceException {
        XmlMapper xmlMapper = XmlUtils.createXmlMapper(object.getClass());

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        // make sure we write the XML with the right encoding
        try {
            xmlMapper.writeValue(new OutputStreamWriter(out, StandardCharsets.UTF_8), object);
        } catch (Exception e) {
            String msg = "Error marshaling request: " + e.getMessage();
            throw new SalesforceException(msg, e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    @Override
    protected void processResponse(final Exchange exchange, final InputStream responseEntity,
        final Map<String, String> headers, final SalesforceException exception, final AsyncCallback callback) {
        try {
            final Message out = exchange.getMessage();
            final Message in = exchange.getIn();
            out.copyFromWithNewBody(in, null);
            out.getHeaders().putAll(headers);

            if (exception != null) {
                if (shouldReport(exception)) {
                    exchange.setException(exception);
                }
            } else if (responseEntity != null) {
            	out.setBody(responseEntity); // Keeping original Stream back to .. Sreeni
            } else {
                exchange.setException(exception);
            }
        } catch (Exception e) {
            String msg = "Error parsing XML response: " + e.getMessage();
            exchange.setException(new SalesforceException(msg, e));
        } finally {
            // cleanup temporary exchange headers
            exchange.removeProperty(RESPONSE_CLASS);
            exchange.removeProperty(RESPONSE_ALIAS);

            // consume response entity
            if (responseEntity != null) {
                try {
                    responseEntity.close();
                } catch (IOException ignored) {
                }
            }

            // notify callback that exchange is done
            callback.done(false);
        }
    }

}
