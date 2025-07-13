package com.apsaraconsulting.skyvvaadapter;

import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.apsaraconsulting.skyvvaadapter.utils.TestDataLoader.loadTestData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ilya Nesterov
 */
class SkyvvaApexCallV2ThroughV4IntegrationTest extends AbstractSalesforceTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(SkyvvaApexCallV2ThroughV4IntegrationTest.class);

    @Test
    void test_accounts_hierarchical_level2() throws Exception {
        byte[] requestPayload = loadTestData("test-data/v2/throughv4/hierarchical/level-2/accounts-v2-hierarchical.xml");
        Map<String, Object> headers = defaultHeadersForV2ReceiverAdapterRequests();
        headers.put("apexUrl", "skyvvasolutions/V4/integrate");
        Object response = template().sendBodyAndHeaders("direct:testApexCall",
            ExchangePattern.InOut,
            requestPayload,
            headers
        );
        // there are no response body
        assertThat(response).isNull();
    }

    @Test
    void test_orders_hierarchical_level3() throws Exception {
        byte[] requestPayload = loadTestData("test-data/v2/throughv4/hierarchical/level-3/orders-v2-hierarchical.xml");
        Map<String, Object> headers = defaultHeadersForV2ReceiverAdapterRequests();
        headers.put("apexUrl", "skyvvasolutions/V4/integrate");
        Object response = template().sendBodyAndHeaders("direct:testApexCall",
            ExchangePattern.InOut,
            requestPayload,
            headers
        );
        // there are no response body
        assertThat(response).isNull();
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
        headers.put("SAP_MessageProcessingLogID", "123456789");
        headers.put("apexMethod", "POST");
        headers.put("loggingOption", "Trace");
        headers.put("enableRetry", "false");
        headers.put("httpClientTimeout", "60");
        return headers;
    }
}
