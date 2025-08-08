package com.apsaraconsulting.adapter.cemalRoute;

import org.apache.camel.builder.RouteBuilder;
import com.apsaraconsulting.adapter.salesforces.PaymentProcessor;

public class AdapterRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {

        /// === Route 1: Handle Salesforce Request ===
        from("direct://invokeSalesforce")
            .routeId("salesforceRoute")
            .to("direct:invokeFakePayment"); /// Static forward

        /// === Route 2: Fake Payment API Integration ===
        from("direct://invokeFakePayment")
            .routeId("fakePaymentRoute")
            .log("Calling fake payment API...")
            .bean(PaymentProcessor.class,"invokePayment")
            .log("Payment API response: ${body}");
    }
}
