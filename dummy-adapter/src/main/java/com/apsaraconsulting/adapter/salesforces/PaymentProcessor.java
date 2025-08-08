package com.apsaraconsulting.adapter.salesforces;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.Date;

@Slf4j
public class PaymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessor.class);

    public void invokePayment(Exchange exchange) {

        // Add MDC context for better tracing
        MDC.put("exchangeId", exchange.getExchangeId());
        MDC.put("messageId", exchange.getIn().getMessageId());
        logger.info("=== PAYMENT PROCESSOR STARTING ===");
        logger.info("Exchange ID: {}", exchange.getExchangeId());
        /// Prepare HTTP client
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpPost postRequest = new HttpPost("https://q0q26.wiremockapi.cloud/mock-payment");
            logger.info("Created POST request to: https://q0q26.wiremockapi.cloud/mock-payment");
            /// Add basic auth header
            postRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            logger.debug("Added Content-Type header: application/json");
            /// Set body if needed
            String inputBody = exchange.getIn().getBody(String.class);
            postRequest.setEntity(new StringEntity(inputBody != null ? inputBody : "{}"));

            /// Execute request
            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                exchange.getIn().setBody(responseBody);
                logger.info("Request payload set: {}", responseBody);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
