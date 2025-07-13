/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apsaraconsulting.skyvvaadapter.internal.processor;

import com.apsaraconsulting.skyvvaadapter.api.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import com.apsaraconsulting.skyvvaadapter.SalesforceEndpoint;
import com.apsaraconsulting.skyvvaadapter.SalesforceEndpointConfig;
import com.apsaraconsulting.skyvvaadapter.api.SalesforceException;
import com.apsaraconsulting.skyvvaadapter.api.utils.JsonUtils;
import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class JsonRestProcessor extends AbstractRestProcessor {

    private static final String RESPONSE_TYPE = JsonRestProcessor.class.getName() + ".responseType";

    // it is ok to use a single thread safe ObjectMapper
    private final ObjectMapper objectMapper;

    private final CpiLoggingDecorator LOG;

    public JsonRestProcessor(SalesforceEndpoint endpoint) throws SalesforceException {
        super(endpoint);
        LOG = CpiLoggingDecorator.fromLogger(
            LoggerFactory.getLogger(JsonRestProcessor.class),
            endpoint.getConfiguration().getLoggingOptionAsLogLevel()
        );
        LOG.trace();

        if (endpoint.getConfiguration().getObjectMapper() != null) {
            this.objectMapper = endpoint.getConfiguration().getObjectMapper();
        } else {
            if (endpoint.getConfiguration().isSerializeNulls()) {
                this.objectMapper = JsonUtils.withNullSerialization(JsonUtils.createObjectMapper());
            } else {
                this.objectMapper = JsonUtils.createObjectMapper();
            }
        }
    }

    @Override
    protected void processRequest(Exchange exchange) throws SalesforceException {

        switch (operationName) {
            case CREATE_SOBJECT:
                // handle known response type
                exchange.setProperty(RESPONSE_CLASS, CreateSObjectResult.class);
                break;

            case UPSERT_SOBJECT:
                // handle known response type
                exchange.setProperty(RESPONSE_CLASS, CreateSObjectResult.class);
                break;

            default:
                // ignore, some operations do not require response class or type
        }
    }

    @Override
    protected InputStream getRequestStream(Exchange exchange) throws SalesforceException {
        InputStream request;
        Message in = exchange.getIn();
        request = in.getBody(InputStream.class);
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
                    request = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
                }
            }
        }

        return request;
    }

    @Override
    protected InputStream getRequestStream(final Message in, final Object object) throws SalesforceException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            prepareMapper(in).writeValue(out, object);
        } catch (IOException e) {
            final String msg = "Error marshaling request: " + e.getMessage();
            throw new SalesforceException(msg, e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    @Override
    protected void processResponse(Exchange exchange, InputStream responseEntity, Map<String, String> headers, 
        SalesforceException ex, AsyncCallback callback) {

        // process JSON response for TypeReference
        try {
            final Message out = exchange.getMessage();
            final Message in = exchange.getIn();
            out.copyFromWithNewBody(in, null);
            if (in == out) {
                // body is not cleaned by the previous statement when in and out is the same object
                out.setBody(null);
            }
            out.getHeaders().putAll(headers);

            if (ex != null) {
                // if an exception is reported we should not loose it
                if (shouldReport(ex)) {
                    exchange.setException(ex);
                }
            } else if (responseEntity != null) {
                // do we need to un-marshal a response
            	out.setBody(responseEntity); // Keeping original response as it is .. Sreeni
            } else {
                exchange.setException(ex);
            }
            // copy headers
            out.getHeaders().putAll(exchange.getIn().getHeaders());
        } catch (Exception e) { // Safe to catch if any unexcepted .. Sreeni
            String msg = "Error parsing JSON response: " + e.getMessage();
            exchange.setException(new SalesforceException(msg, e));
        } finally {
            // cleanup temporary exchange headers
            exchange.removeProperty(RESPONSE_CLASS);
            exchange.removeProperty(RESPONSE_TYPE);

            // consume response entity
            try {
                if (responseEntity != null) {
                    responseEntity.close();
                }
            } catch (IOException ignored) {
            }

            // notify callback that exchange is done
            callback.done(false);
        }

    }

    private ObjectMapper prepareMapper(final Message in) {
        final Object serializeNulls = in.getHeader(SalesforceEndpointConfig.SERIALIZE_NULLS);
        if (Boolean.TRUE.equals(serializeNulls)) {
            return JsonUtils.withNullSerialization(objectMapper);
        }

        return objectMapper;
    }
}
