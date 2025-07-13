package com.apsaraconsulting.skyvvaadapter;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import com.apsaraconsulting.skyvvaadapter.dto.RESTStreamingAPI__c;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ilya Nesterov
 */
class StreamingApiIntegrationCdcEventTest extends AbstractSalesforceTestBase {

    private static final String cdcEventTopicName = "RESTStreamingAPI__ChangeEvent";
    private static final String loggingLevel = "Trace";
    private static final String nameOfSObjectToUpdate = "A-0012";
    private static final String idOfSObjectToUpdate = "a1Rf0000001pNIvEAM";

    @Test
    void testSubscribeAndReceiveCdcEvent() throws Exception {
        MockEndpoint rawPayloadMock = getMockEndpoint(format("mock:RawPayload%s", cdcEventTopicName));
        rawPayloadMock.expectedMessageCount(1);
        // assert expected static headers
        rawPayloadMock.expectedHeaderReceived("CamelSalesforceChannel", format("/data/%s", cdcEventTopicName));
        rawPayloadMock.expectedHeaderReceived("CamelSalesforceEventType", cdcEventTopicName);

        RESTStreamingAPI__c restStreamingAPIObj = new RESTStreamingAPI__c();
        restStreamingAPIObj.setDescription__c("RESTStreamingAPI for testing Streaming API updated on " + ZonedDateTime.now().toString());
        template().requestBodyAndHeader(
            "direct:updateSObject", restStreamingAPIObj, "sObjectId", idOfSObjectToUpdate);

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
                fromF("salesforce:foo?topic=%s&loggingOption=%s", cdcEventTopicName, loggingLevel).
                    log(LoggingLevel.INFO, "Received message ${body}").
                    toF("mock:RawPayload%s", cdcEventTopicName);

                from("direct:getSObject").
                    toF("salesforce:foo?restApi=getSObject&sObjectName=RESTStreamingAPI__c&loggingOption=%s", loggingLevel);

                from("direct:updateSObject").
                    toF("salesforce:foo?restApi=updateSObject&sObjectName=RESTStreamingAPI__c&loggingOption=%s", loggingLevel);
            }
        };
    }

}
