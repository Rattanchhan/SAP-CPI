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
package com.apsaraconsulting.skyvvaadapter.internal.client;

import com.apsaraconsulting.skyvvaadapter.SalesforceHttpClient;
import com.apsaraconsulting.skyvvaadapter.api.SalesforceException;
import com.apsaraconsulting.skyvvaadapter.internal.SalesforceSession;
import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import com.apsaraconsulting.skyvvaadapter.internal.logger.TokenTrimmer;
import org.eclipse.jetty.client.HttpContentResponse;
import org.eclipse.jetty.client.HttpConversation;
import org.eclipse.jetty.client.ProtocolHandler;
import org.eclipse.jetty.client.ResponseNotifier;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class SalesforceSecurityHandler implements ProtocolHandler {

    static final String CLIENT_ATTRIBUTE = SalesforceSecurityHandler.class.getName().concat("camel-salesforce-client");
    static final String AUTHENTICATION_REQUEST_ATTRIBUTE = SalesforceSecurityHandler.class.getName().concat(".request");

    private static final String AUTHENTICATION_RETRIES_ATTRIBUTE = SalesforceSecurityHandler.class.getName().concat(".retries");

    private final SalesforceHttpClient httpClient;
    private final SalesforceSession session;
    private final int maxAuthenticationRetries;
    private final int maxContentLength;
    private final ResponseNotifier notifier;

    private final CpiLoggingDecorator LOG;

    public SalesforceSecurityHandler(SalesforceHttpClient httpClient) {

        LOG = CpiLoggingDecorator.fromLogger(
            LoggerFactory.getLogger(SalesforceSecurityHandler.class),
            httpClient.getLogLevel()
        );

        LOG.trace();
        this.httpClient = httpClient;
        this.session = httpClient.getSession();

        this.maxAuthenticationRetries = httpClient.getMaxRetries();
        this.maxContentLength = httpClient.getMaxContentLength();
        this.notifier = new ResponseNotifier();
    }

    @Override
    public boolean accept(Request request, Response response) {
        HttpConversation conversation = ((SalesforceHttpRequest) request).getConversation();
        Integer retries = (Integer) conversation.getAttribute(AUTHENTICATION_RETRIES_ATTRIBUTE);

        LOG.trace("{} {} {} => {}:{}, retries: {}", request.getMethod(), request.getPath(), request.getVersion(), response.getStatus(), response.getReason(), retries);

        // is this an authentication response for a previously handled conversation?
        if (conversation.getAttribute(AUTHENTICATION_REQUEST_ATTRIBUTE) != null
            && (retries == null || retries <= maxAuthenticationRetries)) {
            LOG.trace("<- true");
            return true;
        }

        final int status = response.getStatus();
        // handle UNAUTHORIZED and BAD_REQUEST for Bulk API,
        // the actual InvalidSessionId Bulk API error is checked and handled in the listener
        // also check retries haven't exceeded maxAuthenticationRetries
        return (status == HttpStatus.UNAUTHORIZED_401 || status == HttpStatus.BAD_REQUEST_400)
            && (retries == null || retries <= maxAuthenticationRetries);
    }

    @Override
    public Response.Listener getResponseListener() {
        return new SecurityListener(maxContentLength);
    }

    private class SecurityListener extends BufferingResponseListener {

        SecurityListener(int maxLength) {
            super(maxLength);
            LOG.trace();
        }

        @Override
        public void onComplete(Result result) {
            LOG.trace();

            SalesforceHttpRequest request = (SalesforceHttpRequest)result.getRequest();
            ContentResponse response = new HttpContentResponse(result.getResponse(), getContent(), getMediaType(), getEncoding());

            // get number of retries
            HttpConversation conversation = request.getConversation();
            Integer retries = (Integer) conversation.getAttribute(AUTHENTICATION_RETRIES_ATTRIBUTE);

            LOG.trace("retries: {}, status: {}, reason: {}", retries, response.getStatus(), response.getReason());

            if (retries == null) {
                retries = 0;
            }

            // get AbstractClientBase if request originated from one, for updating token and setting auth header
            final AbstractClientBase client = (AbstractClientBase) conversation.getAttribute(CLIENT_ATTRIBUTE);
            LOG.trace("client: {}", client);

            // Handling exception response
            // We've got a case when request failed with ClosedChannelException but with 401 status code, it was expected to get 401,
            // but the root cause of exception is not clear, it's thrown here (org.eclipse.jetty.io.sslSslConnection):
            // if (_cannotAcceptMoreAppDataToFlush)
            // {
            //     if (_sslEngine.isOutboundDone())
            //         throw new EofException(new ClosedChannelException());
            //     return false;
            // }
            // To get session updated for that case, we need to check the status code of the result
            if (result.isFailed() && response.getStatus() == HttpStatus.UNAUTHORIZED_401) {
                // 401 error handling
                Throwable failure = result.getFailure();
                LOG.error("Request is failed but response has 401 status. Re-login will be processed. Error message: {}\nResponse: {}",
                    failure.getMessage(),
                    response.getContentAsString()
                );
                LOG.error("Failure: ", failure);
            } else if (result.isFailed()) {
                // other statuses
                Throwable failure = result.getFailure();
                LOG.error("Request is failed, error message: {}\nResponse: {}",
                    failure.getMessage(),
                    response.getContentAsString()
                );
                retryOnFailure(request, conversation, retries, client, failure);
                return;
            }

            // response to a re-login request
            SalesforceHttpRequest origRequest = (SalesforceHttpRequest) conversation.getAttribute(AUTHENTICATION_REQUEST_ATTRIBUTE);
            if (origRequest != null) {
                LOG.trace("original request is not null");
                // parse response
                try {
                    session.parseLoginResponse(response, response.getContentAsString());
                } catch (SalesforceException e) {
                    // retry login request on error if we have login attempts left
                    if (retries < maxAuthenticationRetries) {
                        LOG.trace("retries ({}) < maxAuthenticationRetries ({})", retries, maxAuthenticationRetries);
                        retryOnFailure(request, conversation, retries, client, e);
                    } else {
                        LOG.trace("retries ({}) = maxAuthenticationRetries ({})", retries, maxAuthenticationRetries);
                        forwardFailureComplete(origRequest, null, response, e);
                    }
                    return;
                }

                // retry original request on success
                conversation.removeAttribute(AUTHENTICATION_REQUEST_ATTRIBUTE);
                retryRequest(origRequest, client, retries, conversation, true);
                return;
            } else {
                LOG.trace("original request is null");
            }

            // response to an original request
            final int status = response.getStatus();
            final String reason = response.getReason();

            // check if login retries left
            if (retries >= maxAuthenticationRetries) {
                // forward current response
                LOG.trace("retries () >= maxAuthenticationRetries ()", retries, maxAuthenticationRetries);
                forwardSuccessComplete(request, response);
                return;
            }

            // request failed authentication?
            if (status == HttpStatus.UNAUTHORIZED_401) {

                // REST token expiry
                LOG.warn("Retrying on Salesforce authentication error [{}]: [{}]", status, reason);

                // remember original request and send a relogin request in current conversation
                retryLogin(request, retries);

            } else if (status < HttpStatus.OK_200 || status >= HttpStatus.MULTIPLE_CHOICES_300) {

                // HTTP failure status
                // get detailed cause, if request comes from an AbstractClientBase
                final InputStream inputStream = getContent().length == 0 ? null : getContentAsInputStream();
                final SalesforceException cause = client != null
                    ? client.createRestException(response, inputStream) : null;

                if (status == HttpStatus.BAD_REQUEST_400 && cause != null && isInvalidSessionError(cause)) {
                    // retry Bulk API call
                    LOG.warn("Retrying on Bulk API Salesforce authentication error [{}]: [{}]", status, reason);
                    retryLogin(request, retries);
                } else {
                    LOG.error("forwardSuccessComplete(...)");
                    // forward Salesforce HTTP failure!
                    forwardSuccessComplete(request, response);
                }
            }
        }

        protected void retryOnFailure(SalesforceHttpRequest request, HttpConversation conversation, Integer retries, AbstractClientBase client, Throwable failure) {
            LOG.trace("Retrying on failure " + failure.getMessage(), failure);

            // retry request
            retryRequest(request, client, retries, conversation, true);
        }

        private boolean isInvalidSessionError(SalesforceException e) {
            return e.getErrors() != null && e.getErrors().size() == 1
                && "InvalidSessionId".equals(e.getErrors().get(0).getErrorCode());
        }

        private void retryLogin(SalesforceHttpRequest request, Integer retries) {
            LOG.trace("Retrying login, retries: {}", retries);

            final HttpConversation conversation = request.getConversation();
            // remember the original request to resend
            conversation.setAttribute(AUTHENTICATION_REQUEST_ATTRIBUTE, request);

            retryRequest((SalesforceHttpRequest)session.getLoginRequest(conversation), null, retries, conversation, false);
        }

        private void retryRequest(
            SalesforceHttpRequest request,
            AbstractClientBase client,
            Integer retries,
            HttpConversation conversation,
            boolean copy
        ) {
            LOG.trace("retries: {}, copy: {}", retries, copy);
            // copy the request to resend
            // TODO handle a change in Salesforce instanceUrl, right now we retry with the same destination
            final Request newRequest;
            if (copy) {
                newRequest = httpClient.copyRequestWithHeadersAndCookies(request, request.getURI());
            } else {
                newRequest = request;
            }

            conversation.setAttribute(AUTHENTICATION_RETRIES_ATTRIBUTE, ++retries);

            Object originalRequest = conversation.getAttribute(AUTHENTICATION_REQUEST_ATTRIBUTE);
            LOG.debug("Retry attempt {} on authentication error for {}", retries, originalRequest != null ? originalRequest : newRequest);

            // update currentToken for original request
            if (originalRequest == null) {
                LOG.trace("original request is null");

                String currentToken = session.getAccessToken();
                LOG.trace("new access token: {}", TokenTrimmer.trim(currentToken));
                if (client != null) {
                    // update client cache for this and future requests
                    client.setAccessToken(currentToken);
                    client.setInstanceUrl(session.getInstanceUrl());
                    client.setAccessToken(newRequest);
                    LOG.trace("client is not null");
                } else {
                    // plain request not made by an AbstractClientBase
                    newRequest.header(HttpHeader.AUTHORIZATION, "OAuth " + currentToken);
                    LOG.trace("client is null");
                }
            } else {
                LOG.trace("original request is not null");
            }

            // send new async request with a new delegate
            conversation.updateResponseListeners(null);
            newRequest.onRequestBegin(getRequestAbortListener(request));
            newRequest.send(null);
        }

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

        private void forwardSuccessComplete(SalesforceHttpRequest request, Response response) {
            LOG.trace();
            HttpConversation conversation = request.getConversation();
            conversation.updateResponseListeners(null);
            notifier.forwardSuccessComplete(conversation.getResponseListeners(), request, response);
        }

        private void forwardFailureComplete(SalesforceHttpRequest request, Throwable requestFailure,
                                            Response response, Throwable responseFailure) {
            LOG.trace();
            HttpConversation conversation = request.getConversation();
            conversation.updateResponseListeners(null);
            notifier.forwardFailureComplete(conversation.getResponseListeners(), request, requestFailure,
                response, responseFailure);
        }
    }

    // no @Override annotation here to keep it compatible with Jetty 9.2, getName was added in 9.3
    public String getName() {
        return "CamelSalesforceSecurityHandler";
    }
}
