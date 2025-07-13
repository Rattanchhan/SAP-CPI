package com.apsaraconsulting.skyvvaadapter;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ilya Nesterov
 */
class StreamingApiIntegrationPlatformEventTest extends AbstractSalesforceTestBase {

    private static final String platformEventTopicName = "event/Low_Ink";
    private static final String loggingLevel = "Trace";

    @Test
    void testSubscribeAndReceivePlatformEvent() throws Exception {
        MockEndpoint rawPayloadMock = getMockEndpoint("mock:rawPayload");
        rawPayloadMock.expectedMessageCount(1);
        // assert expected static headers
        rawPayloadMock.expectedHeaderReceived("CamelSalesforceChannel", "/event/Low_Ink__e");
        rawPayloadMock.expectedHeaderReceived("CamelSalesforceEventType", "Low_Ink");

        String body = "{\n" +
            "    \"Printer_Model__c\": \"test\",\n" +
            "    \"Serial_Number__c\": 100\n" +
            "}";
        template().requestBody(
            "direct:createSObject", body);

        // wait for Salesforce notification
        // validate raw payload message
        rawPayloadMock.assertIsSatisfied();
        final Message inRaw = rawPayloadMock.getExchanges().get(0).getIn();
        assertTrue(inRaw.getBody() instanceof String, "Expected String message body for Raw Payload");

        // validate dynamic message headers
        // clientId is not received in the event
        //assertNotNull(inRaw.getHeader("CamelSalesforceClientId"), "Missing header CamelSalesforceClientId");
        assertNotNull(inRaw.getHeader("CamelSalesforcePlatformEventSchema"), "Missing header CamelSalesforcePlatformEventSchema");
        assertNotNull(inRaw.getHeader("CamelSalesforceReplayId"), "Missing header CamelSalesforceReplayId");
    }

    @Override
    protected RouteBuilder doCreateRouteBuilder() throws Exception {

        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                // CPI system only creates endpoints with the name 'foo'
                // to make test fair we need to do the same thing here
                // topic name should be propagated as a parameter
                fromF("salesforce:foo?topic=%s&loggingOption=%s", platformEventTopicName, loggingLevel).
                    log(LoggingLevel.INFO, "Received message ${body}").
                    toF("mock:rawPayload");

                from("direct:createSObject").
                    toF("salesforce:foo?restApi=createSObject&sObjectName=Low_Ink__e&loggingOption=%s", loggingLevel);
            }
        };
    }

}
