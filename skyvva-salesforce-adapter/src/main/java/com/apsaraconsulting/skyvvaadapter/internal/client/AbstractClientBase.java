/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apsaraconsulting.skyvvaadapter.internal.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import com.apsaraconsulting.skyvvaadapter.SalesforceHttpClient;
import com.apsaraconsulting.skyvvaadapter.api.SalesforceException;
import com.apsaraconsulting.skyvvaadapter.api.TypeReferences;
import com.apsaraconsulting.skyvvaadapter.api.dto.RestError;
import com.apsaraconsulting.skyvvaadapter.internal.PayloadFormat;
import com.apsaraconsulting.skyvvaadapter.internal.SalesforceSession;
import com.apsaraconsulting.skyvvaadapter.internal.dto.RestErrors;
import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import com.apsaraconsulting.skyvvaadapter.internal.logger.TokenTrimmer;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpContentResponse;
import org.eclipse.jetty.client.HttpConversation;
import org.eclipse.jetty.client.api.*;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.ByteBufferContentProvider;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public abstract class AbstractClientBase extends ServiceSupport implements SalesforceSession.SalesforceSessionListener, HttpClientHolder {

    protected static final String APPLICATION_JSON_UTF8 = "application/json;charset=utf-8";
    protected static final String APPLICATION_XML_UTF8 = "application/xml;charset=utf-8";
    private static final String REQUEST_RETRIES_ATTRIBUTE = AbstractClientBase.class.getName().concat(".retries");

    private static final int DEFAULT_TERMINATION_TIMEOUT = 10;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final SalesforceHttpClient httpClient;
    protected final SalesforceSession session;
    protected final String version;

    protected String accessToken;
    protected String instanceUrl;

    private Phaser inflightRequests;

    private long terminationTimeout;

    private final boolean enableRetry;
    private final int maxRetryAttempts = 3;

    private final CpiLoggingDecorator LOG;

    public AbstractClientBase(
        String version,
        SalesforceSession session,
        SalesforceHttpClient httpClient,
        boolean enableRetry
    ) throws SalesforceException {
        this(version, session, httpClient, DEFAULT_TERMINATION_TIMEOUT, enableRetry);
    }

    AbstractClientBase(
        String version,
        SalesforceSession session,
        SalesforceHttpClient httpClient,
        int terminationTimeout,
        boolean enableRetry
    ) throws SalesforceException {
        LOG = CpiLoggingDecorator.fromLogger(
            LoggerFactory.getLogger(AbstractClientBase.class),
            httpClient.getLogLevel()
        );
        LOG.trace("context: {}, retry enabled: {}", this.getClass().getSimpleName(), enableRetry);
        this.version = version;
        this.session = session;
        this.httpClient = httpClient;
        this.terminationTimeout = terminationTimeout;
        this.enableRetry = enableRetry;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        LOG.trace();

        // local cache
        accessToken = session.getAccessToken();
        // RestClient created in doStart method of SalesforceComponent won't have it initialized
        if (accessToken == null) {
            try {
                accessToken = session.login(null);
            } catch (SalesforceException e) {
                throw new RuntimeException(e);
            }
        }
        instanceUrl = session.getInstanceUrl();

        // also register this client as a session listener
        session.addListener(this);

        inflightRequests = new Phaser(1);
    }

    @Override
    public void doStop() throws Exception {
        super.doStop();
        if (inflightRequests != null) {
            inflightRequests.arrive();
            if (!inflightRequests.isTerminated()) {
                try {
                    inflightRequests.awaitAdvanceInterruptibly(0, terminationTimeout, TimeUnit.SECONDS);
                } catch (InterruptedException | TimeoutException ignored) {
                    // exception is ignored
                }
            }
        }

        // deregister listener
        session.removeListener(this);
    }

    @Override
    public void onLogin(String accessToken, String instanceUrl) {
        if (!accessToken.equals(this.accessToken)) {
            this.accessToken = accessToken;
            this.instanceUrl = instanceUrl;
        }
    }

    @Override
    public void onLogout() {
        // ignore, if this client makes another request with stale token,
        // SalesforceSecurityListener will auto login!
    }

    protected Request getRequest(HttpMethod method, String url, Map<String, List<String>> headers) {
        return getRequest(method.asString(), url, headers);
    }

    protected Request getRequest(String method, String url, Map<String, List<String>> headers) {
        SalesforceHttpRequest request = (SalesforceHttpRequest) httpClient.newRequest(url)
            .method(method)
            .timeout(session.getTimeout(), TimeUnit.MILLISECONDS);
        request.getConversation().setAttribute(SalesforceSecurityHandler.CLIENT_ATTRIBUTE, this);
        addHeadersTo(request, headers);

        // ======= for dbg only. Delete afterwards
        LOG.trace("Method: {}\n" +
                "URL: {}\n" +
                "Access token: {}\n" +
                "Session timeout: {}",
            method,
            url,
            TokenTrimmer.trim(session.getAccessToken()),
            session.getTimeout()
        );
        LOG.trace(stringifyRequestToPrint(request));
        // ======= for dbg only. Delete afterwards

        return request;
    }

    protected interface ClientResponseCallback {
        void onResponse(InputStream response, Map<String, String> headers, SalesforceException ex);
    }

    protected void doHttpRequest(final Request request, final ClientResponseCallback callback) {
        LOG.trace("context: {}", this.getClass().getSimpleName());
        LOG.trace(stringifyRequestToPrint(request));
        // Highly memory inefficient,
        // but buffer the request content to allow it to be replayed for authentication retries
        bufferRequestContent(request);

        inflightRequests.register();

        final AbstractClientBase that = this;

        // execute the request
        request.send(new BufferingResponseListener(httpClient.getMaxContentLength()) {
            @Override
            public void onComplete(Result result) {
                try {
                    LOG.trace("context: {}", that.getClass().getSimpleName());

                    Response response = result.getResponse();

                    final Map<String, String> headers = determineHeadersFrom(response);
                    if (result.isFailed()) {

                        // fatal error during response processing
                        // we need retry here because SalesforceSecurityHandler is not executed if response can't be parsed
                        // probably better solution exists
                        if (enableRetry && response.getStatus() == 0) {
                            SalesforceHttpRequest request = (SalesforceHttpRequest) result.getRequest();
                            HttpConversation conversation = request.getConversation();
                            Integer retries = (Integer) conversation.getAttribute(REQUEST_RETRIES_ATTRIBUTE);
                            if (retries == null) {
                                retries = 0;
                            }
                            LOG.debug("Fatal connectivity error. Retries left: {}, status: {}, reason: {}, error message: {}",
                                maxRetryAttempts - retries,
                                response.getStatus(),
                                response.getReason(),
                                ExceptionUtils.getMessage(result.getFailure())
                            );

                            if (retries < maxRetryAttempts) {
                                // processing retry
                                conversation.setAttribute(REQUEST_RETRIES_ATTRIBUTE, ++retries);
                                LOG.debug("Retry attempt {} on fatal connectivity error", retries);
                                // send new async request with a new delegate

                                Request newRequest = httpClient.copyRequestWithHeadersAndCookies(request, request.getURI());

                                conversation.updateResponseListeners(null);
                                newRequest.onRequestBegin(getRequestAbortListener(request));
                                newRequest.send(null);
                            } else {
                                LOG.debug("There are no available retry attempts", retries);
                                processFailure(result.getFailure(), callback, request.getMethod(), request.getURI(), headers);
                            }
                        } else {
                            // other errors
                            LOG.trace("result is failed");
                            // Failure!!!
                            // including Salesforce errors reported as exception from SalesforceSecurityHandler
                            Throwable failure = result.getFailure();
                            processFailure(failure, callback, request.getMethod(), request.getURI(), headers);
                        }

                        return;

                    }

                    ContentResponse contentResponse = new HttpContentResponse(response, getContent(), getMediaType(), getEncoding());

                    LOG.trace("processing asynchronous response");
                    processResponse(contentResponse, callback, headers);

                } finally {
                    inflightRequests.arriveAndDeregister();
                }
            }
        });
    }

    //TODO add retry for fatal error here if needs
    protected void doHttpRequest(final Request request, final ClientResponseCallback callback, boolean synchronousMode) {
        if (!synchronousMode) {
            doHttpRequest(request, callback);
            return;
        }

        LOG.trace("context: {}", this.getClass().getSimpleName());
        LOG.trace(stringifyRequestToPrint(request));

        bufferRequestContent(request);

        inflightRequests.register();
        ContentResponse contentResponse = null;
        Map<String, String> headers = new HashMap<>();
        // execute the request
        try {
            contentResponse = request.send();
            headers = determineHeadersFrom(contentResponse);
            LOG.trace("processing synchronous response");
            processResponse(contentResponse, callback, headers);
        } catch (Throwable ex) {
            processFailure(ex, callback, request.getMethod(), request.getURI(), headers);
        } finally {
            inflightRequests.arriveAndDeregister();
        }
    }

    private void bufferRequestContent(Request request) {
        // Highly memory inefficient,
        // but buffer the request content to allow it to be replayed for authentication retries
        final ContentProvider requestContent = request.getContent();
        if (requestContent instanceof InputStreamContentProvider) {
            LOG.trace("content is InputStreamContentProvider");
            final List<ByteBuffer> buffers = new ArrayList<>();
            for (ByteBuffer buffer : requestContent) {
                buffers.add(buffer);
            }
            request.content(new ByteBufferContentProvider(buffers.toArray(new ByteBuffer[buffers.size()])));
            buffers.clear();
        }
    }

    private void processFailure(
        Throwable failure,
        final ClientResponseCallback callback,
        final String requestMethod,
        final URI requestUri,
        Map<String, String> headers
    ) {
        if (failure instanceof SalesforceException) {
            callback.onResponse(null, headers, (SalesforceException) failure);
        } else {
            String msg;
            Throwable rootCause = ExceptionUtils.getRootCause(failure);
            if (rootCause == null || rootCause.equals(failure)) {
                msg = String.format("Unexpected error {%s} executing {%s:%s}",
                    ExceptionUtils.getMessage(failure),
                    requestMethod, requestUri
                );
            } else {
                msg = String.format("Unexpected error {%s} (root cause {%s}) executing {%s:%s}",
                    ExceptionUtils.getMessage(failure),
                    ExceptionUtils.getRootCauseMessage(failure),
                    requestMethod, requestUri
                );
            }
            LOG.error(msg);
            LOG.error("Failure: ", failure);
            callback.onResponse(null, headers, new SalesforceException(msg, 0, failure));
        }
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setInstanceUrl(String instanceUrl) {
        this.instanceUrl = instanceUrl;
    }

    @Override
    public HttpClient getHttpClient() {
        return httpClient;
    }

    final List<RestError> readErrorsFrom(
        final InputStream responseContent, final ObjectMapper objectMapper)
        throws IOException {
        final List<RestError> restErrors;
        restErrors = objectMapper.readValue(responseContent, TypeReferences.REST_ERROR_LIST_TYPE);
        return restErrors;
    }

    final List<RestError> readErrorsFrom(
        final InputStream responseContent,
        final PayloadFormat format,
        final ObjectMapper jsonMapper,
        final XmlMapper xmlMapper
    )
        throws IOException{
        final List<RestError> restErrors;
        if (PayloadFormat.JSON.equals(format)) {
            restErrors = jsonMapper.readValue(responseContent, TypeReferences.REST_ERROR_LIST_TYPE);
        } else {
            RestErrors errors = xmlMapper.readValue(responseContent, RestErrors.class);
            restErrors = errors.getErrors();
        }
        return restErrors;
    }

    protected abstract void setAccessToken(Request request);

    protected abstract SalesforceException createRestException(Response response, InputStream responseContent);

    static Map<String, String> determineHeadersFrom(final Response response) {
        final HttpFields headers = response.getHeaders();

        final Map<String, String> answer = new LinkedHashMap<>();
        for (final HttpField header : headers) {
            final String headerName = header.getName();

            if (headerName.startsWith("Sforce")) {
                answer.put(headerName, header.getValue());
            }
        }

        return answer;
    }

    private void processResponse(ContentResponse contentResponse, final ClientResponseCallback callback, Map<String, String> headers) {
        // HTTP error status
        final int status = contentResponse.getStatus();
        final SalesforceHttpRequest request = (SalesforceHttpRequest) contentResponse.getRequest();
        LOG.trace("status: {}, reason: {}", status, contentResponse.getReason());

        SalesforceHttpRequest authenticationRequest = (SalesforceHttpRequest) request
            .getConversation()
            .getAttribute(SalesforceSecurityHandler.AUTHENTICATION_REQUEST_ATTRIBUTE);

        if (status == HttpStatus.BAD_REQUEST_400 && authenticationRequest != null) {
            // parse login error
            try {
                session.parseLoginResponse(contentResponse, contentResponse.getContentAsString());
                final String msg = String.format("Error {%s:%s} executing {%s:%s}",
                    status, contentResponse.getReason(), authenticationRequest.getMethod(), authenticationRequest.getURI());
                LOG.trace(msg);
                callback.onResponse(null, headers, new SalesforceException(msg, null));

            } catch (SalesforceException e) {

                final String msg = String.format("Error {%s:%s} executing {%s:%s}",
                    status, contentResponse.getReason(), authenticationRequest.getMethod(), authenticationRequest.getURI());
                LOG.trace(msg);
                callback.onResponse(null, headers, new SalesforceException(msg, contentResponse.getStatus(), e));

            }
        } else if (status < HttpStatus.OK_200 || status >= HttpStatus.MULTIPLE_CHOICES_300) {

            // Salesforce HTTP failure!
            final String msg = String.format("Error {%s:%s} executing {%s:%s}",
                status, contentResponse.getReason(), request.getMethod(), request.getURI());
            LOG.trace(msg);
            final SalesforceException cause = createRestException(contentResponse, getContentAsInputStream(contentResponse.getContent()));
            callback.onResponse(null, headers, new SalesforceException(msg, contentResponse.getStatus(), cause));

        } else {
            LOG.trace("Request is successful");
            // Success!!!
            callback.onResponse(getContentAsInputStream(contentResponse.getContent()), headers, null);
        }
    }

    //TODO duplicated from SalesforceSecurityHandler, refactor
    private Request.BeginListener getRequestAbortListener(final SalesforceHttpRequest request) {
        return new Request.BeginListener() {
            @Override
            public void onBegin(Request redirect) {
                Throwable cause = request.getAbortCause();
                if (cause != null) {
                    redirect.abort(cause);
                }
            }
        };
    }

    private InputStream getContentAsInputStream(byte[] content) {
        if (content == null || content.length == 0) {
            return null;
        }
        return new ByteArrayInputStream(content);
    }

    private static void addHeadersTo(final Request request, final Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }

        final HttpFields requestHeaders = request.getHeaders();
        for (Entry<String, List<String>> header : headers.entrySet()) {
            requestHeaders.put(header.getKey(), header.getValue());
        }
    }

    static Map<String, List<String>> determineHeaders(final Exchange exchange) {
        final Message inboundMessage = exchange.getIn();

        final Map<String, Object> headers = inboundMessage.getHeaders();

        final Map<String, List<String>> answer = new HashMap<>();
        for (final String headerName : headers.keySet()) {
            final String headerNameLowercase = headerName.toLowerCase(Locale.US);
            if (headerNameLowercase.startsWith("sforce") || headerNameLowercase.startsWith("x-sfdc")) {
                final Object headerValue = inboundMessage.getHeader(headerName);

                if (headerValue instanceof String) {
                    answer.put(headerName, Collections.singletonList((String) headerValue));
                } else if (headerValue instanceof String[]) {
                    answer.put(headerName, Arrays.asList((String[]) headerValue));
                } else if (headerValue instanceof Collection) {
                    answer.put(headerName, ((Collection<?>) headerValue).stream().map(String::valueOf)
                        .collect(Collectors.<String>toList()));
                } else {
                    throw new IllegalArgumentException(
                        "Given value for header `" + headerName + "`, is not String, String array or a Collection");
                }
            }
        }

        return answer;
    }

    // ======= for dbg only.
    protected String stringifyRequestToPrint(Request request) {

        HttpFields fields = request.getHeaders();
        StringBuilder headersSb = new StringBuilder();
        for (HttpField field : fields) {
            if (field.getName().equals("Authorization")) {
                headersSb.append(String.format("%s:%s\n", field.getName(), TokenTrimmer.trim(field.getValue())));
            } else {
                headersSb.append(String.format("%s:%s\n", field.getName(), field.getValue()));
            }
        }

        Fields params = request.getParams();
        StringBuilder paramsSb = new StringBuilder();
        for (Fields.Field param : params) {
            paramsSb.append(String.format("%s:%s\n", param.getName(), param.getValue()));
        }

        return String.format("method: %s\n" +
                "scheme: %s\n" +
                "access token: %s\n" +
                "instance URL: %s\n" +
                "headers:\n%s" +
                "params:\n%s",
            request.getMethod() != null ? request.getMethod() : "null",
            request.getScheme() != null ? request.getScheme() : "null",
            accessToken != null ? TokenTrimmer.trim(accessToken) : "null",
            instanceUrl != null ? instanceUrl : "null",
            headersSb.toString(),
            paramsSb.toString()
        );
    }
    // ======= for dbg only.
}
