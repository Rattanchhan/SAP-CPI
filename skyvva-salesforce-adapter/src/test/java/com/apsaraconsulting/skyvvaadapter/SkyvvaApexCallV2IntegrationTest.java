package com.apsaraconsulting.skyvvaadapter;

import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import static com.apsaraconsulting.skyvvaadapter.utils.TestDataLoader.loadTestData;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ilya Nesterov
 */
class SkyvvaApexCallV2IntegrationTest extends AbstractSalesforceTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(SkyvvaApexCallV2IntegrationTest.class);

    @Test
    void test_integrate_s18() throws Exception {
        byte[] requestPayload = loadTestData("test-data/v2/integrate_s18/request.json");
        Map<String, Object> headers = defaultHeadersForV2ReceiverAdapterRequests();
        headers.put("apexUrl", "skyvvasolutions/Integrate");
        Object response = template().sendBodyAndHeaders("direct:testApexCall",
            ExchangePattern.InOut,
            requestPayload,
            headers
        );
        String responseAsString = IOUtils.toString((ByteArrayInputStream) response, UTF_8);
        assertEquals( "{\"records\":\"1\",\"success\":\"true\"}", responseAsString);
    }

    @Test
    void test_integrateSynchronous_s18() throws Exception {
        byte[] requestPayload = loadTestData("test-data/v2/integrateSynchronous_s18/request.json");
        Map<String, Object> headers = defaultHeadersForV2ReceiverAdapterRequests();
        headers.put("apexUrl", "skyvvasolutions/IntegrateSynchronous");
        Object response = template().sendBodyAndHeaders("direct:testApexCall",
            ExchangePattern.InOut,
            requestPayload,
            headers
        );
        String responseAsString = IOUtils.toString((ByteArrayInputStream) response, UTF_8)
            .replaceAll("\"id\":\"\\w+\"", "\"id\":\"STUB\"")
            .replaceAll("\"externalId\":\"\\w+\"", "\"externalId\":\"STUB\"");
        assertEquals( "[{\"success\":true,\"objectType\":\"Account\",\"name\":\"Ahmed Keihel AG\",\"id\":\"STUB\",\"externalId\":\"STUB\",\"errorMessage\":null}]", responseAsString);
    }

    @Test
    void test_integrateBatch_s18() throws Exception {
        byte[] requestPayload = loadTestData("test-data/v2/integrateBatch_s18/request.json");
        Map<String, Object> headers = defaultHeadersForV2ReceiverAdapterRequests();
        headers.put("apexUrl", "skyvvasolutions/integrateBatch");
        Object response = template().sendBodyAndHeaders("direct:testApexCall",
            ExchangePattern.InOut,
            requestPayload,
            headers
        );
        String responseAsString = IOUtils.toString((ByteArrayInputStream) response, UTF_8)
            .replaceAll("\"Basket Name\":\"[\\w-]+\"", "\"Basket Name\":\"STUB\"")
            .replaceAll("\"Basket Id\":\"[\\w-]+\"", "\"Basket Id\":\"STUB\"");
        assertEquals( "{\"Number Of Records\":\"2\",\"Basket Name\":\"STUB\",\"Basket Id\":\"STUB\"}", responseAsString);
    }

    @Test
    void test_searchService_s18() throws Exception {
        byte[] requestPayload = loadTestData("test-data/v2/searchService_s18/request.json");
        Map<String, Object> headers = defaultHeadersForV2ReceiverAdapterRequests();
        headers.put("apexUrl", "skyvvasolutions/SearchService");
        Object response = template().sendBodyAndHeaders("direct:testApexCall",
            ExchangePattern.InOut,
            requestPayload,
            headers
        );
        String responseAsString = IOUtils.toString((ByteArrayInputStream) response, UTF_8)
            .replaceAll("\\n|\\r\\n", System.getProperty("line.separator"));
        byte[] expectedResponse = loadTestData("test-data/v2/searchService_s18/response.json");
        String expectedResponseAsString = IOUtils.toString(expectedResponse, UTF_8.name())
            .replaceAll("\\n|\\r\\n", System.getProperty("line.separator"));
        assertEquals(expectedResponseAsString, responseAsString);
    }

    @Override
    protected RouteBuilder doCreateRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:testApexCall")
                    .toD("salesforce:foo?" +
                        "loginUrl=${header.loginUrl}&" +
                        "apexUrl=${header.apexUrl}&" +
                        "loggingOption=${header.loggingOption}&" +
                        "enableRetry=${header.enableRetry}&" +
                        "httpClientTimeout=${header.httpClientTimeout}"
                    );
            }
        };
    }

    private Map<String, Object> defaultHeadersForV2ReceiverAdapterRequests() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("loginUrl", "https://test.salesforce.com");
        headers.put("apexMethod", "POST");
        headers.put("loggingOption", "Trace");
        headers.put("enableRetry", "false");
        headers.put("httpClientTimeout", "60");
        return headers;
    }
}
