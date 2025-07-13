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
package com.apsaraconsulting.skyvvaadapter.internal;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import com.apsaraconsulting.skyvvaadapter.AuthenticationType;
import com.apsaraconsulting.skyvvaadapter.SalesforceHttpClient;
import com.apsaraconsulting.skyvvaadapter.SalesforceLoginConfig;
import com.apsaraconsulting.skyvvaadapter.api.SalesforceException;
import com.apsaraconsulting.skyvvaadapter.api.dto.RestError;
import com.apsaraconsulting.skyvvaadapter.api.utils.JsonUtils;
import com.apsaraconsulting.skyvvaadapter.internal.dto.LoginError;
import com.apsaraconsulting.skyvvaadapter.internal.dto.LoginToken;
import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import com.apsaraconsulting.skyvvaadapter.internal.logger.TokenTrimmer;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.util.ObjectHelper;
import org.eclipse.jetty.client.HttpConversation;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.FormContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.Fields;
import org.slf4j.LoggerFactory;

public class SalesforceSession extends ServiceSupport {

    private static final String OAUTH2_REVOKE_PATH = "/services/oauth2/revoke?token=";
    private static final String OAUTH2_TOKEN_PATH = "/services/oauth2/token";

    private final SalesforceHttpClient httpClient;
    private final long timeout;

    private final SalesforceLoginConfig config;

    private final ObjectMapper objectMapper;
    private final Set<SalesforceSessionListener> listeners;

    private volatile String accessToken;
    private volatile String instanceUrl;

    private final CpiLoggingDecorator LOG;

    public SalesforceSession(CamelContext camelContext, SalesforceHttpClient httpClient, long timeout, SalesforceLoginConfig config) {

        LOG = CpiLoggingDecorator.fromLogger(
            LoggerFactory.getLogger(SalesforceSession.class),
            httpClient.getLogLevel()
        );
        LOG.trace("timeout: {}", timeout);

        // validate parameters
        ObjectHelper.notNull(httpClient, "httpClient");
        ObjectHelper.notNull(config, "SalesforceLoginConfig");
        config.validate();

        this.httpClient = httpClient;
        this.timeout = timeout;
        this.config = config;

        this.objectMapper = JsonUtils.createObjectMapper();
        this.listeners = new CopyOnWriteArraySet<>();
    }

    public synchronized String login(String oldToken) throws SalesforceException {
        LOG.trace("oldToken: {}, accessToken: {}", TokenTrimmer.trim(oldToken), TokenTrimmer.trim(accessToken));
        // check if we need a new session
        // this way there's always a single valid session
        if (accessToken == null || accessToken.equals(oldToken)) {

            // try revoking the old access token before creating a new one
            accessToken = oldToken;
            if (accessToken != null) {
                LOG.trace("logout before login, access token: {}", TokenTrimmer.trim(accessToken));
                try {
                    logout();
                } catch (SalesforceException e) {
                    LOG.error("Error revoking old access token: " + e.getMessage(), e);
                }
                accessToken = null;
            }

            // login to Salesforce and get session id
            final Request loginPost = getLoginRequest(null);
            try {

                final ContentResponse loginResponse = loginPost.send();
                parseLoginResponse(loginResponse, loginResponse.getContentAsString());

            } catch (InterruptedException e) {
                throw new SalesforceException("Login error: " + e.getMessage(), e);
            } catch (TimeoutException e) {
                throw new SalesforceException("Login request timeout: " + e.getMessage(), e);
            } catch (ExecutionException e) {
                throw new SalesforceException("Unexpected login error: " + e.getCause().getMessage(), e.getCause());
            }
        }

        return accessToken;
    }

    /**
     * Creates login request, allows SalesforceSecurityHandler to create a login request for a failed authentication
     * conversation
     *
     * @return login POST request.
     */
    public Request getLoginRequest(HttpConversation conversation) {
        final String loginUrl = (instanceUrl == null ? config.getLoginUrl() : instanceUrl) + OAUTH2_TOKEN_PATH;
        LOG.info("Login at Salesforce loginUrl: {}", loginUrl);
        final Fields fields = new Fields(true);

        fields.put("client_id", config.getClientId());
        fields.put("format", "json");

        final AuthenticationType type = config.getType();
        switch (type) {
            case USERNAME_PASSWORD:
                fields.put("client_secret", config.getClientSecret());
                fields.put("grant_type", "password");
                fields.put("username", config.getUserName());
                fields.put("password", config.getPassword());
                break;
            default:
                throw new IllegalArgumentException("Unsupported login configuration type: " + type);
        }

        final Request post;
        if (conversation == null) {
            LOG.trace("conversation is null");
            post = httpClient.POST(loginUrl);
        } else {
            LOG.trace("conversation will be reused");
            post = httpClient.newHttpRequest(conversation, URI.create(loginUrl))
                .method(HttpMethod.POST).agent("SAP-CPI");// added agent
        }

        return post.content(new FormContentProvider(fields)).timeout(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Parses login response, allows SalesforceSecurityHandler to parse a login request for a failed authentication
     * conversation.
     */
    public synchronized void parseLoginResponse(ContentResponse loginResponse, String responseContent)
        throws SalesforceException {
        final int responseStatus = loginResponse.getStatus();

        try {
            switch (responseStatus) {
                case HttpStatus.OK_200:
                    // parse the response to get token
                    LoginToken token = objectMapper.readValue(responseContent, LoginToken.class);

                    // don't log token or instance URL for security reasons
                    accessToken = token.getAccessToken();
                    instanceUrl = Optional.ofNullable(config.getInstanceUrl()).orElse(token.getInstanceUrl());
                    // strip trailing '/'
                    int lastChar = instanceUrl.length() - 1;
                    if (instanceUrl.charAt(lastChar) == '/') {
                        instanceUrl = instanceUrl.substring(0, lastChar);
                    }

                    LOG.debug("Login is successful");
                    LOG.trace("accessToken: {}", TokenTrimmer.trim(accessToken));

                    // notify all session listeners
                    for (SalesforceSessionListener listener : listeners) {
                        try {
                            listener.onLogin(accessToken, instanceUrl);
                        } catch (Throwable t) {
                            LOG.error("Unexpected error from listener {}: {}", listener, t.getMessage());
                        }
                    }

                    break;

                case HttpStatus.BAD_REQUEST_400:
                    // parse the response to get error
                    final LoginError error = objectMapper.readValue(responseContent, LoginError.class);
                    final String errorCode = error.getError();
                    final String msg = String.format("Login error code:[%s] description:[%s]", error.getError(),
                        error.getErrorDescription());
                    final List<RestError> errors = new ArrayList<>();
                    errors.add(new RestError(errorCode, msg));
                    throw new SalesforceException(errors, HttpStatus.BAD_REQUEST_400);

                default:
                    throw new SalesforceException(
                        String.format("Login error status:[%s] reason:[%s]", responseStatus, loginResponse.getReason()),
                        responseStatus);
            }
        } catch (IOException e) {
            String msg = "Login error: response parse exception " + e.getMessage();
            throw new SalesforceException(msg, e);
        }
    }

    public synchronized void logout() throws SalesforceException {
        if (accessToken == null) {
            return;
        }

        try {
            String logoutUrl = (instanceUrl == null ? config.getLoginUrl() : instanceUrl) + OAUTH2_REVOKE_PATH + accessToken;
            final Request logoutGet = httpClient.newRequest(logoutUrl)
                .timeout(timeout, TimeUnit.MILLISECONDS)
                .idleTimeout(timeout, TimeUnit.MILLISECONDS);

            LOG.trace("accessToken: {}, method: {}, logout timeout(ms): {}", TokenTrimmer.trim(accessToken), logoutGet.getMethod(), timeout);

            final ContentResponse logoutResponse = logoutGet.send();

            final int statusCode = logoutResponse.getStatus();
            final String reason = logoutResponse.getReason();

            if (statusCode == HttpStatus.OK_200) {
                LOG.debug("Logout successful");
            } else {
                LOG.debug("Failed to revoke OAuth token. This is expected if the token is invalid or already expired");
                throw new SalesforceException(
                    String.format("Logout error, code: [%s] reason: [%s]", statusCode, reason), statusCode);
            }

        } catch (InterruptedException e) {
            String msg = "Logout error: " + e.getMessage();
            throw new SalesforceException(msg, e);
        } catch (ExecutionException e) {
            final Throwable ex = e.getCause();
            throw new SalesforceException("Unexpected logout exception: " + ex.getMessage(), ex);
        } catch (TimeoutException e) {
            throw new SalesforceException("Logout request TIMEOUT! Original message:" + e.getMessage(), null);
        } finally {
            // reset session
            accessToken = null;
            instanceUrl = null;
            // notify all session listeners about logout
            for (SalesforceSessionListener listener : listeners) {
                try {
                    listener.onLogout();
                } catch (Exception t) {
                    LOG.error("Unexpected error from listener {}: {}", listener, t.getMessage());
                }
            }
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public void setInstanceUrl(String instanceUrl) {
        this.instanceUrl = instanceUrl;
    }

    public boolean addListener(SalesforceSessionListener listener) {
        return listeners.add(listener);
    }

    public boolean removeListener(SalesforceSessionListener listener) {
        return listeners.remove(listener);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        LOG.trace();
        // auto-login at start if needed
        login(accessToken);
    }

    @Override
    public void doStop() throws Exception {
        LOG.trace();
        // logout
        logout();
        super.doStop();
    }

    public long getTimeout() {
        return timeout;
    }

    public interface SalesforceSessionListener {
        void onLogin(String accessToken, String instanceUrl);
        void onLogout();
    }
}
