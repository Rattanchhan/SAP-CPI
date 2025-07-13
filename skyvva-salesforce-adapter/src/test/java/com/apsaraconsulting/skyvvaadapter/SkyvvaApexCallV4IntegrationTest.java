package com.apsaraconsulting.skyvvaadapter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.iservice.database.CacheSFBulkBatchFile;
import com.iservice.model.v4response.IntegrateMessageResponse;
import com.iservice.task.CheckSFProcessingJob;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import com.apsaraconsulting.skyvvaadapter.internal.PayloadFormat;
import com.apsaraconsulting.skyvvaadapter.internal.SkyvvaIntegrationMode;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.apsaraconsulting.skyvvaadapter.utils.TestDataLoader.loadTestData;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Naming is a bit confusing, the requirement was to have a new API version as V3,
 * but at the same time it's often called as V4 in sample Postman requests and during discussions.
 * Consider V3 and V4 as the same thing.
 * @author Ilya Nesterov
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class SkyvvaApexCallV4IntegrationTest extends AbstractSalesforceTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(SkyvvaApexCallV4IntegrationTest.class);

    @Test
    void test_normal_sync_json() throws Exception {
        executeTestForV4Normal(
            "test-data/v4/normal-sync/request.json",
            "Ly-Na",
            "V4_External_Mapping_Hierachical",
            PayloadFormat.JSON,
            SkyvvaIntegrationMode.SYNCHRONOUS
        );
    }

    @Test
    void test_normal_sync_xml() throws Exception {
        executeTestForV4Normal(
            "test-data/v4/normal-sync/request.xml",
            "Ly-Na",
            "V4_BusinessMessageType_Salesforce_sObject_IN",
            PayloadFormat.XML,
            SkyvvaIntegrationMode.SYNCHRONOUS
        );
    }

    @Test
    void test_normal_async_json() throws Exception {
        executeTestForV4Normal(
            "test-data/v4/normal-async/request.json",
            "Ly-Na",
            "CPI_Integrate_Async_NoResponse_IN",
            PayloadFormat.JSON,
            SkyvvaIntegrationMode.ASYNCHRONOUS
        );
    }

    @Test
    void test_skyvva_batch_xml() throws Exception {
        executeTestForV4SkyvvaBatch(
            "test-data/v4/skyvva-batch/accounts-request.xml",
            "Phanith_SAP_CPI",
            "SkyvvaBatchV4",
            PayloadFormat.XML
        );
    }

    @Test
    void test_skyvva_batch_json_accounts() throws Exception {
        executeTestForV4SkyvvaBatch(
            "test-data/v4/skyvva-batch/accounts-request.json",
            "Phanith_SAP_CPI",
            "SkyvvaBatchV4",
            PayloadFormat.JSON
        );
    }

    @Test
    void test_skyvva_batch_json_products() throws Exception {
        executeTestForV4SkyvvaBatch(
            "test-data/v4/skyvva-batch/products-request.json",
            "Ly-Na",
            "CPI_Skyvva_Batch_IN",
            PayloadFormat.JSON
        );
    }

    @Test
    void test_skyvva_bulk_xml_accounts() throws Exception {
        executeTestForV4SkyvvaBulk(
            "test-data/v4/skyvva-bulk/accounts-request.xml",
            "Ly-Na",
            "SKYVVA-Bulk",
            PayloadFormat.XML
        );
    }

    @Test
    void test_skyvva_bulk_json_accounts() throws Exception {
        executeTestForV4SkyvvaBulk(
            "test-data/v4/skyvva-bulk/accounts-request.json",
            "Phanith_SAP_CPI",
            "skyvvaBulk",
            PayloadFormat.JSON
        );
    }

    // no actual test data at the moment, we decided to skip that test
    @Disabled
    @Test
    void test_skyvva_bulk_json_products() throws Exception {
        executeTestForV4SkyvvaBulk(
            "test-data/v4/skyvva-bulk/products-request.json",
            "Ly-Na",
            "SKYVVA-Bulk",
            PayloadFormat.JSON
        );
    }

    @Test
    void test_autoswitch_xml() throws Exception {
        executeTestForV4AutoSwitch(
            "test-data/v4/autoswitch/accounts-request.xml",
            "Phanith_SAP_CPI",
            "AutoSwitch",
            PayloadFormat.XML
        );
    }

    @Test
    void test_autoswitch_json() throws Exception {
        executeTestForV4AutoSwitch(
            "test-data/v4/autoswitch/accounts-request.json",
            "Phanith_SAP_CPI",
            "AutoSwitch",
            PayloadFormat.JSON
        );
    }

    @Test
    void test_salesforce_bulk_xml() throws Exception {
        executeTestForV4SalesforceBulk(
            "test-data/v4/salesforce-bulk/accounts-request.xml",
            "Phanith_SAP_CPI",
            "SalesForceBulkMode",
            PayloadFormat.XML
        );
    }

    @Test
    void test_salesforce_bulk_json_accounts() throws Exception {
        executeTestForV4SalesforceBulk(
            "test-data/v4/salesforce-bulk/accounts-request.json",
            "Phanith_SAP_CPI",
            "SalesForceBulkMode",
            PayloadFormat.JSON
        );
    }

    @Test
    void test_salesforce_bulk_products() throws Exception {
        executeTestForV4SalesforceBulk(
            "test-data/v4/salesforce-bulk/products-request.json",
            "Ly-Na",
            "CPI_Skyvva_Batch_IN",
            PayloadFormat.JSON
        );
    }

    @Override
    protected RouteBuilder doCreateRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

            from("direct:testApexCall")
                .toD("salesforce:foo?" +
                    "loginUrl=${header.loginUrl}&" +
                    "salesforceIntegration=${header.salesforceIntegration}&" +
                    "salesforceInterface=${header.salesforceInterface}&" +
                    "skyvvaApiVersion=${header.skyvvaApiVersion}&" +
                    "skyvvaAppVersion=${header.skyvvaAppVersion}&" +
                    "skyvvaIntegrationMode=${header.skyvvaIntegrationMode}&" +
                    "requestFormat=${header.requestFormat}&" +
                    "responseFormat=${header.responseFormat}&" +
                    "loggingOption=${header.loggingOption}&" +
                    "enableRetry=${header.enableRetry}&" +
                    "httpClientTimeout=${header.httpClientTimeout}"
                );
            }
        };
    }

    private void executeTestForV4Normal(
        String inputFile,
        String salesforceIntegration,
        String salesforceInterface,
        PayloadFormat requestFormat,
        SkyvvaIntegrationMode mode
    ) throws Exception {
        byte[] requestPayload = loadTestData(inputFile);
        Map<String, Object> headers = defaultHeadersForV3ReceiverAdapterRequests();
        headers.put("salesforceIntegration", salesforceIntegration);
        headers.put("salesforceInterface", salesforceInterface);
        headers.put("requestFormat", requestFormat.name());
        headers.put("skyvvaIntegrationMode", mode);
        InputStream response = (InputStream) template().sendBodyAndHeaders("direct:testApexCall",
            ExchangePattern.InOut,
            requestPayload,
            headers
        );
        JSONObject responseAsJsonObject  = new Gson().fromJson(IOUtils.toString(response, UTF_8), JSONObject.class);
        assertEquals("Successful" , responseAsJsonObject.get("StatusMsg"));
        assertEquals("OK" , responseAsJsonObject.get("StatusCode"));
    }

    private void executeTestForV4SkyvvaBatch(String inputFile, String salesforceIntegration, String salesforceInterface, PayloadFormat requestFormat) throws Exception {
        byte[] requestPayload = loadTestData(inputFile);
        Map<String, Object> headers = defaultHeadersForV3ReceiverAdapterRequests();
        headers.put("salesforceIntegration", salesforceIntegration);
        headers.put("salesforceInterface", salesforceInterface);
        headers.put("skyvvaIntegrationMode", SkyvvaIntegrationMode.SKYVVA_BATCH);
        headers.put("requestFormat", requestFormat.name());
        byte[] response = (byte[]) template().sendBodyAndHeaders("direct:testApexCall",
            ExchangePattern.InOut,
            requestPayload,
            headers
        );
        String responseAsString = new String(response, UTF_8);
        JSONObject integrateMessageResponse  = new Gson().fromJson(responseAsString, JSONObject.class);
        assertEquals("Basket has been created successfully" , integrateMessageResponse.get("message"));
    }

    private void executeTestForV4SkyvvaBulk(String inputFile, String salesforceIntegration, String salesforceInterface, PayloadFormat requestFormat) throws Exception {
        byte[] requestPayload = loadTestData(inputFile);
        Map<String, Object> headers = defaultHeadersForV3ReceiverAdapterRequests();
        headers.put("salesforceIntegration", salesforceIntegration);
        headers.put("salesforceInterface", salesforceInterface);
        headers.put("skyvvaIntegrationMode", SkyvvaIntegrationMode.SKYVVA_BULK);
        headers.put("requestFormat", requestFormat.name());
        byte[] response = (byte[]) template().sendBodyAndHeaders("direct:testApexCall",
            ExchangePattern.InOut,
            requestPayload,
            headers
        );
        String responseAsString = new String(response, UTF_8);
        List<JSONObject> integrateMessageResponse  = new Gson().fromJson(responseAsString, new TypeToken<ArrayList<JSONObject>>(){}.getType());
        assertEquals(1, integrateMessageResponse.size());
    }

    private void executeTestForV4AutoSwitch(String inputFile, String salesforceIntegration, String salesforceInterface, PayloadFormat requestFormat) throws Exception {
        byte[] requestPayload = loadTestData(inputFile);
        Map<String, Object> headers = defaultHeadersForV3ReceiverAdapterRequests();
        headers.put("salesforceIntegration", salesforceIntegration);
        headers.put("salesforceInterface", salesforceInterface);
        headers.put("skyvvaIntegrationMode", SkyvvaIntegrationMode.AUTO_SWITCH);
        headers.put("requestFormat", requestFormat.name());
        byte[] response = (byte[]) template().sendBodyAndHeaders("direct:testApexCall",
            ExchangePattern.InOut,
            requestPayload,
            headers
        );
        String responseAsString = new String(response, UTF_8);
        if (responseAsString.startsWith("[")) {
            List<JSONObject> integrateMessageResponse  = new Gson().fromJson(responseAsString, new TypeToken<ArrayList<JSONObject>>(){}.getType());
            assertEquals(1, integrateMessageResponse.size());
        } else if (responseAsString.contains("Basket has been created successfully")) {
            JSONObject integrateMessageResponse  = new Gson().fromJson(responseAsString, JSONObject.class);
            assertEquals("Basket has been created successfully" , integrateMessageResponse.get("message"));
        } else {
            IntegrateMessageResponse integrateMessageResponse  = new Gson().fromJson(responseAsString, IntegrateMessageResponse.class);
            assertEquals("Successful" , integrateMessageResponse.getStatusMsg());
        }
    }

    private void executeTestForV4SalesforceBulk(String inputFile, String salesforceIntegration, String salesforceInterface, PayloadFormat requestFormat) throws Exception {
        byte[] requestPayload = loadTestData(inputFile);
        Map<String, Object> headers = defaultHeadersForV3ReceiverAdapterRequests();
        headers.put("salesforceIntegration", salesforceIntegration);
        headers.put("salesforceInterface", salesforceInterface);
        headers.put("skyvvaIntegrationMode", SkyvvaIntegrationMode.SALESFORCE_BULK);
        headers.put("requestFormat", requestFormat.name());
        CheckSFProcessingJob.setLinearBackoffForReprocessingEnabled(false); // to speedup unit test execution
        template().sendBodyAndHeaders("direct:testApexCall",
            ExchangePattern.InOut,
            requestPayload,
            headers
        );

        // after integrated to SF, we have cache SF Bulk processing job. it used for creating Message
        assertEquals(1 , CacheSFBulkBatchFile.getInstance().getCacheFolderJob().size());

        try {
            // setup max estimated time of the whole request processing
            Thread.sleep(180000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        // after message created, we deleted from the cache.
        assertEquals(0 , CacheSFBulkBatchFile.getInstance().getCacheFolderJob().size());
    }

    private Map<String, Object> defaultHeadersForV3ReceiverAdapterRequests() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("loginUrl", "https://test.salesforce.com");
        headers.put("skyvvaApiVersion", "V3");
        headers.put("skyvvaAppVersion", "2.48.0");
        headers.put("requestFormat", "JSON");
        headers.put("responseFormat", "JSON");
        headers.put("SAP_MessageProcessingLogID", "123456789");
        headers.put("loggingOption", "Trace");
        headers.put("enableRetry", "false");
        headers.put("httpClientTimeout", "60");

        return headers;
    }
}
