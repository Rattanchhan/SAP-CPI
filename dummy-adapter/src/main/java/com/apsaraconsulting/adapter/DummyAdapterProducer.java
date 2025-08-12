package com.apsaraconsulting.adapter;

import com.apsaraconsulting.adapter.MPL.MessageLogFactory;
import com.apsaraconsulting.adapter.cemalRoute.AdapterRoute;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.it.api.ITApiFactory;
import com.sap.it.api.msglog.adapter.AdapterMessageLogFactory;
import com.sap.it.api.msglog.adapter.AdapterMessageLogWithStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultProducer;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

//import static com.apsaraconsulting.adapter.DummyAdapterProducer.LogLevel.INFO;
import static java.lang.String.format;

/**
 * Dummy Adapter Camel Producer
 */
@Slf4j
public class DummyAdapterProducer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(DummyAdapterProducer.class);

    public DummyAdapterProducer(DummyAdapterEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        DummyAdapterEndpoint endpoint = getEndpoint();
        /// Set default to Salesforce
        String target = endpoint.getTargetSystem();
        if ("WireMock".equalsIgnoreCase(target)) {
            invokeWiremock(exchange, endpoint);
        } else {
            invokeSalesforce(exchange,endpoint);
        }

    }

    private void createExchange(Exchange exchange, DummyAdapterEndpoint endpoint) {
        /// Prepare HTTP client
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String apiUrl = endpoint.getUrl();
            HttpPost postRequest = new HttpPost(apiUrl);

            /// Add basic auth header
            String auth = endpoint.getUsername() + ":" + endpoint.getPassword();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            postRequest.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
            postRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

            /// Set body if needed
            String inputBody = exchange.getIn().getBody(String.class);
            postRequest.setEntity(new StringEntity(inputBody != null ? inputBody : "{}"));

            /// Execute request
            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                exchange.getIn().setBody(responseBody);
                LOG.info("Processed dummy adapter message: {}", response);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DummyAdapterEndpoint getEndpoint() {
        return (DummyAdapterEndpoint) super.getEndpoint();
    }

    private void invokeSalesforce(Exchange exchange,DummyAdapterEndpoint endpoint) throws Exception {
        String authMethod = endpoint.getSalesforceAuth();
        String tokenEndpoint = endpoint.getTokenEndpoint();
        String clientId = endpoint.getClientId();
        String clientSecret = endpoint.getClientSecret();

        String accessToken;
        String instanceUrl;

        CloseableHttpClient httpClient = HttpClients.createDefault();
        ObjectMapper objectMapper = new ObjectMapper();

        HttpPost post = new HttpPost(tokenEndpoint);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");

        String payload;
        if ("password".equalsIgnoreCase(authMethod)) {
            String username = endpoint.getUsername();
            String password = endpoint.getPassword();

            payload = format(
                "grant_type=password&client_id=%s&client_secret=%s&username=%s&password=%s",
                clientId, clientSecret, username, password);

        } else if ("client_credentials".equalsIgnoreCase(authMethod)) {
            payload = format(
                "grant_type=client_credentials&client_id=%s&client_secret=%s",
                clientId, clientSecret);
        } else {
            throw new IllegalArgumentException("Unsupported Salesforce auth method: " + authMethod);
        }

        post.setEntity(new StringEntity(payload));
        HttpResponse response = httpClient.execute(post);
        String tokenJson = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        JsonNode tokenResponse = objectMapper.readTree(tokenJson);

        accessToken = tokenResponse.get("access_token").asText();
        instanceUrl = tokenResponse.has("instance_url")
            ? tokenResponse.get("instance_url").asText()
            : endpoint.getUrl();

        /// Call Salesforce API
        HttpGet get = new HttpGet(instanceUrl + "/services/data/v56.0/sobjects/Account/");
        get.setHeader("Authorization", "Bearer " + accessToken);

        HttpResponse salesforceResponse = httpClient.execute(get);
        String salesforceData = EntityUtils.toString(salesforceResponse.getEntity(), StandardCharsets.UTF_8);

        LOG.info("Salesforce API response: {}", salesforceData);
        exchange.getIn().setBody(salesforceData);

        httpClient.close();
    }

    private void invokeWiremock(Exchange exchange, DummyAdapterEndpoint endpoint) {
        //createExchange(exchange, endpoint);
        try {
            MessageLogFactory messageLogFactory = new  MessageLogFactory();
            CamelContext context = exchange.getContext();
            context.addRoutes(new AdapterRoute());
            ProducerTemplate template = context.createProducerTemplate();

            String result = template.requestBody("direct:invokeSalesforce", null, String.class);
            exchange.getIn().setBody(result);
            logging("Result: "+result,LOG);
            messageLogFactory.integrateIntoTracing(exchange);
        } catch (Exception ignored) {
        }
    }

    public void logging(String msg,Logger logger) {
        logger.error("{}", buildMethodInfo()+msg);
    }

    private String buildMethodInfo() {
        return format("%s-LEVEL #%s:%d ",
            LogLevel.INFO,
            Thread.currentThread().getStackTrace()[3].getMethodName(),
            Thread.currentThread().getStackTrace()[3].getLineNumber()
        );
    }
    public enum LogLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

}