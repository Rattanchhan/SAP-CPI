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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.apsaraconsulting.skyvvaadapter.SalesforceEndpointConfig;
import com.apsaraconsulting.skyvvaadapter.SalesforceHttpClient;
import com.apsaraconsulting.skyvvaadapter.api.SalesforceException;
import com.apsaraconsulting.skyvvaadapter.api.SalesforceMultipleChoicesException;
import com.apsaraconsulting.skyvvaadapter.api.TypeReferences;
import com.apsaraconsulting.skyvvaadapter.api.utils.JsonUtils;
import com.apsaraconsulting.skyvvaadapter.api.utils.XmlUtils;
import com.apsaraconsulting.skyvvaadapter.internal.PayloadFormat;
import com.apsaraconsulting.skyvvaadapter.internal.SalesforceSession;
import com.apsaraconsulting.skyvvaadapter.internal.dto.RestChoices;
import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.URISupport;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultRestClient extends AbstractClientBase implements RestClient {

    private static final String SERVICES_DATA = "/services/data/";
    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String SERVICES_APEXREST = "/services/apexrest/";
    private static final String APEX_URL_PREFIX = "skyvvasolutions/";
    private static final String EVENT_URL_PREFIX = "/services/data/v46.0/sobjects/";

    protected PayloadFormat requestFormat;
    private ObjectMapper jsonMapper;
    private ObjectMapper xmlMapper;

    private SalesforceEndpointConfig salesforceEndpointConfig;

    private final CpiLoggingDecorator LOG;

    public DefaultRestClient(final SalesforceHttpClient httpClient, final String version, final PayloadFormat requestFormat,
        final SalesforceSession session) throws SalesforceException {
        super(version, session, httpClient, false);
        LOG = CpiLoggingDecorator.fromLogger(
                LoggerFactory.getLogger(DefaultRestClient.class),
                salesforceEndpointConfig.getLoggingOptionAsLogLevel()
        );
        LOG.trace();

        this.requestFormat = requestFormat;

        // initialize error parsers for JSON and XML
        this.jsonMapper = JsonUtils.createObjectMapper();
        this.xmlMapper = XmlUtils.createXmlMapper();
    }

    public DefaultRestClient(final SalesforceEndpointConfig salesforceEndpointConfig, final String version, final PayloadFormat requestFormat,
                             final SalesforceSession session)
            throws SalesforceException {
        super(version, session, salesforceEndpointConfig.getHttpClient(), salesforceEndpointConfig.isEnableRetry());
        LOG = CpiLoggingDecorator.fromLogger(
            LoggerFactory.getLogger(DefaultRestClient.class),
            salesforceEndpointConfig.getLoggingOptionAsLogLevel()
        );
        LOG.trace();

        this.salesforceEndpointConfig = salesforceEndpointConfig;
        this.requestFormat = requestFormat;

        // initialize error parsers for JSON and XML
        this.jsonMapper = JsonUtils.createObjectMapper();
        this.xmlMapper = XmlUtils.createXmlMapper();
    }

    public void setInstanceUrl(String instanceUrl) {
        this.instanceUrl = instanceUrl;
    }

    public String getInstanceUrl() {
        return this.instanceUrl;
    }

    @Override
    protected void doHttpRequest(Request request, ClientResponseCallback callback) {
        LOG.trace();
        // set standard headers for all requests
        final String contentType = PayloadFormat.JSON.equals(requestFormat) ? APPLICATION_JSON_UTF8 : APPLICATION_XML_UTF8;
        request.header(HttpHeader.ACCEPT, contentType);
        request.header(HttpHeader.ACCEPT_CHARSET, StringUtil.__UTF8);
        // request content type and charset is set by the request entity
        super.doHttpRequest(request, callback);
    }

    @Override
    protected SalesforceException createRestException(Response response, InputStream responseContent) {
        // get status code and reason phrase
        final int statusCode = response.getStatus();
        String reason = response.getReason();
        if (reason == null || reason.isEmpty()) {
            reason = HttpStatus.getMessage(statusCode);
        }

        LOG.trace("status: {}, reason: {}", statusCode, reason);

        // try parsing response according to format
        try {
            if (responseContent != null && responseContent.available() > 0) {
                final List<String> choices;
                // return list of choices as error message for 300
                if (statusCode == HttpStatus.MULTIPLE_CHOICES_300) {
                    if (PayloadFormat.JSON.equals(requestFormat)) {
                        choices = jsonMapper.readValue(responseContent, TypeReferences.STRING_LIST_TYPE);
                    } else {
                        RestChoices restChoices = xmlMapper.readValue(responseContent, RestChoices.class);
                        choices = restChoices.getUrls();
                    }
                    return new SalesforceMultipleChoicesException(reason, statusCode, choices);
                } else {
                    return new SalesforceException(IOUtils.toString(responseContent, StandardCharsets.UTF_8), statusCode);
                }
            }
        } catch (IOException e) {
            // log and ignore
            String msg = "Unexpected Error parsing " + requestFormat
                    + " error response body + [" + responseContent + "] : " + e.getMessage();
            LOG.warn(msg, e);
        } catch (RuntimeException e) {
            // log and ignore
            String msg = "Unexpected Error parsing " + requestFormat
                    + " error response body + [" + responseContent + "] : " + e.getMessage();
            LOG.warn(msg, e);
        }

        // just report HTTP status info
        return new SalesforceException("Unexpected error: " + reason + ", with content: " + responseContent,
                statusCode);
    }

    @Override
    public void approval(final InputStream request, Map<String, List<String>> headers, final ResponseCallback callback) {
        final Request post = getRequest(HttpMethod.POST, versionUrl() + "process/approvals/", headers);

        // authorization
        setAccessToken(post);

        // input stream as entity content
        post.content(new InputStreamContentProvider(request));
        post.header(HttpHeader.CONTENT_TYPE, PayloadFormat.JSON.equals(requestFormat) ? APPLICATION_JSON_UTF8 : APPLICATION_XML_UTF8);

        doHttpRequest(post, new DelegatingClientCallback(callback));
    }

    @Override
    public void approvals(Map<String, List<String>> headers, final ResponseCallback callback) {
        final Request get = getRequest(HttpMethod.GET, versionUrl() + "process/approvals/", headers);

        doHttpRequest(get, new DelegatingClientCallback(callback));
    }

    @Override
    public void getVersions(Map<String, List<String>> headers, final ResponseCallback callback) {
        LOG.trace();
        Request get = getRequest(HttpMethod.GET, servicesDataUrl(), headers);
        // does not require authorization token

        doHttpRequest(get, new DelegatingClientCallback(callback));
    }

    @Override
    public void getResources(Map<String, List<String>> headers, ResponseCallback callback) {
        LOG.trace();
        Request get = getRequest(HttpMethod.GET, versionUrl(), headers);
        // requires authorization token
        setAccessToken(get);

        doHttpRequest(get, new DelegatingClientCallback(callback));
    }

    @Override
    public void getGlobalObjects(Map<String, List<String>> headers, ResponseCallback callback) {
        LOG.trace();
        Request get = getRequest(HttpMethod.GET, sobjectsUrl(""), headers);
        // requires authorization token
        setAccessToken(get);

        doHttpRequest(get, new DelegatingClientCallback(callback));
    }

    @Override
    public void getBasicInfo(String sObjectName, Map<String, List<String>> headers, ResponseCallback callback) {
        LOG.trace("sObjectName: {}", sObjectName);
        Request get = getRequest(HttpMethod.GET, sobjectsUrl(sObjectName + "/"), headers);
        // requires authorization token
        setAccessToken(get);

        doHttpRequest(get, new DelegatingClientCallback(callback));
    }

    @Override
    public void getDescription(String sObjectName, Map<String, List<String>> headers, ResponseCallback callback) {
        Request get = getRequest(HttpMethod.GET, sobjectsUrl(sObjectName + "/describe/"), headers);
        LOG.trace("sObjectName: {}", sObjectName);
        // requires authorization token
        setAccessToken(get);

        doHttpRequest(get, new DelegatingClientCallback(callback));
    }

    @Override
    public void getSObject(String sObjectName, String id, String[] fields, Map<String, List<String>> headers,
                           ResponseCallback callback) {

        // parse fields if set
        String params = "";
        if (fields != null && fields.length > 0) {
            StringBuilder fieldsValue = new StringBuilder("?fields=");
            for (int i = 0; i < fields.length; i++) {
                fieldsValue.append(fields[i]);
                if (i < (fields.length - 1)) {
                    fieldsValue.append(',');
                }
            }
            params = fieldsValue.toString();
        }

        LOG.trace("sObjectName: {}, id: {}, fields: {}", sObjectName, id, params);

        Request get = getRequest(HttpMethod.GET, sobjectsUrl(sObjectName + "/" + id + params), headers);
        // requires authorization token
        setAccessToken(get);

        doHttpRequest(get, new DelegatingClientCallback(callback));
    }

    @Override
    public void createSObject(String sObjectName, InputStream sObject, Map<String, List<String>> headers, 
                              ResponseCallback callback) {
        LOG.trace("sObjectName: {}", sObjectName);
        // post the sObject
        final Request post = getRequest(HttpMethod.POST, sobjectsUrl(sObjectName), headers);

        // authorization
        setAccessToken(post);

        // input stream as entity content
        post.content(new InputStreamContentProvider(sObject));
        post.header(HttpHeader.CONTENT_TYPE, PayloadFormat.JSON.equals(requestFormat) ? APPLICATION_JSON_UTF8 : APPLICATION_XML_UTF8);

        doHttpRequest(post, new DelegatingClientCallback(callback));
    }

    @Override
    public void updateSObject(String sObjectName, String id, InputStream sObject, Map<String, List<String>> headers,
                              ResponseCallback callback) {
        LOG.trace("sObjectName: {}, id: {}", sObjectName, id);
        final Request patch = getRequest("PATCH", sobjectsUrl(sObjectName + "/" + id), headers);
        // requires authorization token
        setAccessToken(patch);

        // input stream as entity content
        patch.content(new InputStreamContentProvider(sObject));
        patch.header(HttpHeader.CONTENT_TYPE, PayloadFormat.JSON.equals(requestFormat) ? APPLICATION_JSON_UTF8 : APPLICATION_XML_UTF8);

        doHttpRequest(patch, new DelegatingClientCallback(callback));
    }

    @Override
    public void deleteSObject(String sObjectName, String id, Map<String, List<String>> headers, ResponseCallback callback) {
        final Request delete = getRequest(HttpMethod.DELETE, sobjectsUrl(sObjectName + "/" + id), headers);
        LOG.trace("sObjectName: {}, id: {}", sObjectName, id);

        // requires authorization token
        setAccessToken(delete);

        doHttpRequest(delete, new DelegatingClientCallback(callback));
    }

    @Override
    public void getSObjectWithId(String sObjectName, String fieldName, String fieldValue, Map<String, List<String>> headers,
                                 ResponseCallback callback) {
        LOG.trace("sObjectName: {}, fieldName: {}, fieldValue: {}", sObjectName, fieldName, fieldValue);
        final Request get = getRequest(HttpMethod.GET,
                sobjectsExternalIdUrl(sObjectName, fieldName, fieldValue), headers);

        // requires authorization token
        setAccessToken(get);

        doHttpRequest(get, new DelegatingClientCallback(callback));
    }

    @Override
    public void upsertSObject(String sObjectName, String fieldName, String fieldValue, Map<String, List<String>> headers, InputStream sObject,
                              ResponseCallback callback) {
        LOG.trace("sObjectName: {}, fieldName: {}, fieldValue: {}", sObjectName, fieldName, fieldValue);
        final Request patch = getRequest("PATCH",
                sobjectsExternalIdUrl(sObjectName, fieldName, fieldValue), headers);

        // requires authorization token
        setAccessToken(patch);

        // input stream as entity content
        patch.content(new InputStreamContentProvider(sObject));
        // TODO will the encoding always be UTF-8??
        patch.header(HttpHeader.CONTENT_TYPE, PayloadFormat.JSON.equals(requestFormat) ? APPLICATION_JSON_UTF8 : APPLICATION_XML_UTF8);

        doHttpRequest(patch, new DelegatingClientCallback(callback));
    }

    @Override
    public void deleteSObjectWithId(String sObjectName, String fieldName, String fieldValue, Map<String, List<String>> headers,
                                    ResponseCallback callback) {
        LOG.trace("sObjectName: {}, fieldName: {}, fieldValue: {}", sObjectName, fieldName, fieldValue);
        final Request delete = getRequest(HttpMethod.DELETE,
                sobjectsExternalIdUrl(sObjectName, fieldName, fieldValue), headers);

        // requires authorization token
        setAccessToken(delete);

        doHttpRequest(delete, new DelegatingClientCallback(callback));
    }

    @Override
    public void getBlobField(String sObjectName, String id, String blobFieldName, Map<String, List<String>> headers, ResponseCallback callback) {
        LOG.trace("sObjectName: {}, id: {}, blobFieldName: {}", sObjectName, id, blobFieldName);
        final Request get = getRequest(HttpMethod.GET,
                sobjectsUrl(sObjectName + "/" + id + "/" + blobFieldName), headers);
        // TODO this doesn't seem to be required, the response is always the content binary stream
        //get.header(HttpHeader.ACCEPT_ENCODING, "base64");

        // requires authorization token
        setAccessToken(get);

        doHttpRequest(get, new DelegatingClientCallback(callback));
    }

    @Override
    public void query(String soqlQuery, Map<String, List<String>> headers, ResponseCallback callback) {
        LOG.trace("soqlQuery: {}", soqlQuery);
        try {

            String encodedQuery = urlEncode(soqlQuery);
            final Request get = getRequest(HttpMethod.GET, versionUrl() + "query/?q=" + encodedQuery, headers);

            // requires authorization token
            setAccessToken(get);

            doHttpRequest(get, new DelegatingClientCallback(callback));

        } catch (UnsupportedEncodingException e) {
            String msg = "Unexpected error: " + e.getMessage();
            callback.onResponse(null, Collections.emptyMap(), new SalesforceException(msg, e));
        }
    }

    @Override
    public void queryMore(String nextRecordsUrl, Map<String, List<String>> headers, ResponseCallback callback) {
        LOG.trace("nextRecordsUrl: {}", nextRecordsUrl);
        final Request get = getRequest(HttpMethod.GET, instanceUrl + nextRecordsUrl, headers);

        // requires authorization token
        setAccessToken(get);

        doHttpRequest(get, new DelegatingClientCallback(callback));
    }

    @Override
    public void queryAll(String soqlQuery, Map<String, List<String>> headers, ResponseCallback callback) {
        try {

            String encodedQuery = urlEncode(soqlQuery);
            final Request get = getRequest(HttpMethod.GET, versionUrl() + "queryAll/?q=" + encodedQuery, headers);

            // requires authorization token
            setAccessToken(get);

            doHttpRequest(get, new DelegatingClientCallback(callback));

        } catch (UnsupportedEncodingException e) {
            String msg = "Unexpected error: " + e.getMessage();
            callback.onResponse(null, Collections.emptyMap(), new SalesforceException(msg, e));
        }
    }

    @Override
    public void search(String soslQuery, Map<String, List<String>> headers, ResponseCallback callback) {
        try {
            LOG.trace("soslQuery: {}", soslQuery);

            String encodedQuery = urlEncode(soslQuery);
            final Request get = getRequest(HttpMethod.GET, versionUrl() + "search/?q=" + encodedQuery, headers);

            // requires authorization token
            setAccessToken(get);

            doHttpRequest(get, new DelegatingClientCallback(callback));

        } catch (UnsupportedEncodingException e) {
            String msg = "Unexpected error: " + e.getMessage();
            callback.onResponse(null, Collections.emptyMap(), new SalesforceException(msg, e));
        }
    }

    @Override
    public void apexCall(
        String httpMethod,
        String apexUrl,
        Map<String, Object> queryParams,
        byte[] requestPayload,
        Map<String, List<String>> headers,
        ResponseCallback callback
    ) {
        LOG.trace("httpMethod: {}, apexUrl: {}", httpMethod, apexUrl);
        // create APEX call request
        final Request request;
        try {
            request = getRequest(httpMethod, apexCallUrl(apexUrl, queryParams), headers);

            // set request SObject and content type
            if (requestPayload != null) {
                // guard against requests that do not support bodies
                switch (request.getMethod()) {
                    case "PUT":
                    case "PATCH":
                    case "POST":
                        request.content(new BytesContentProvider(requestPayload));
                        request.header(HttpHeader.CONTENT_TYPE,
                                PayloadFormat.JSON.equals(requestFormat) ? APPLICATION_JSON_UTF8 : APPLICATION_XML_UTF8);
                        break;
                    default:
                        // ignore body for other methods
                }
            }
            // requires authorization token
            setAccessToken(request);

            // ===== use for dbg only
            LOG.trace(stringifyRequestToPrint(request));
            doHttpRequest(request, new DelegatingClientCallback(callback));
        } catch (Exception ex) {
            String msg = "Unexpected error: " + ex.getMessage();
            LOG.error(msg);
            callback.onResponse(null, Collections.emptyMap(), new SalesforceException(msg, ex));
        }
    }

    private String apexCallUrl(String apexUrl, Map<String, Object> queryParams)
            throws UnsupportedEncodingException, URISyntaxException {

        if (queryParams != null && !queryParams.isEmpty()) {
            apexUrl = URISupport.appendParametersToURI(apexUrl, queryParams);
        }
        return instanceUrl + SERVICES_APEXREST + apexUrl;
    }

    @Override
    public void recent(final Integer limit, Map<String, List<String>> headers, final ResponseCallback responseCallback) {
        final String param = Optional.ofNullable(limit).map(v -> "?limit=" + v).orElse("");

        final Request get = getRequest(HttpMethod.GET, versionUrl() + "recent/" + param, headers);

        // requires authorization token
        setAccessToken(get);

        doHttpRequest(get, new DelegatingClientCallback(responseCallback));
    }

    @Override
    public void limits(Map<String, List<String>> headers, final ResponseCallback responseCallback) {
        if (requestFormat != PayloadFormat.JSON) {
            throw new IllegalArgumentException("Using XML format for the Limits API, to use it set the `format` endpoint property to JSON");
        }

        final Request get = getRequest(HttpMethod.GET, versionUrl() + "limits/", headers);

        // requires authorization token
        setAccessToken(get);

        doHttpRequest(get, new DelegatingClientCallback(responseCallback));
    }

    private String servicesDataUrl() {
        return instanceUrl + SERVICES_DATA;
    }

    private String versionUrl() {
        ObjectHelper.notNull(version, "version");
        return servicesDataUrl() + "v" + version + "/";
    }

    private String sobjectsUrl(String sObjectName) {
        ObjectHelper.notNull(sObjectName, "sObjectName");
        return versionUrl() + "sobjects/" + sObjectName;
    }

    private String sobjectsExternalIdUrl(String sObjectName, String fieldName, String fieldValue) {
        if (fieldName == null || fieldValue == null) {
            throw new IllegalArgumentException("External field name and value cannot be NULL");
        }
        try {
            String encodedValue = urlEncode(fieldValue);
            return sobjectsUrl(sObjectName + "/" + fieldName + "/" + encodedValue);
        } catch (UnsupportedEncodingException e) {
            String msg = "Unexpected error: " + e.getMessage();
            throw new IllegalArgumentException(msg, e);
        }
    }

    protected void setAccessToken(Request request) {
        // replace old token
        request.getHeaders().put(TOKEN_HEADER, TOKEN_PREFIX + accessToken);
    }

    private String urlEncode(String query) throws UnsupportedEncodingException {
        String encodedQuery = URLEncoder.encode(query, StringUtil.__UTF8);
        // URLEncoder likes to use '+' for spaces
        encodedQuery = encodedQuery.replace("+", "%20");
        return encodedQuery;
    }

    private static class DelegatingClientCallback implements ClientResponseCallback {
        private final ResponseCallback callback;

        DelegatingClientCallback(ResponseCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onResponse(InputStream response, Map<String, String> headers, SalesforceException ex) {
            callback.onResponse(response, headers, ex);
        }
    }

}
