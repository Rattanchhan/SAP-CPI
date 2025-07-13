/*
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
package com.apsaraconsulting.skyvvaadapter;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public abstract class AbstractSalesforceTestBase extends CamelTestSupport {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected SalesforceComponent component;

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        // create the test component
        createComponent();

        return doCreateRouteBuilder();
    }

    protected RouteBuilder doCreateRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
            }
        };
    }

    protected void createComponent() throws Exception {
        // create the component
        component = new SalesforceComponent();
        final SalesforceEndpointConfig config = new SalesforceEndpointConfig();
        config.setApiVersion(System.getProperty("apiVersion", salesforceApiVersionToUse()));
        component.setConfig(config);
        component.setLoginConfig(LoginConfigHelper.getLoginConfig());

        // add it to context
        context().addComponent("salesforce", component);
    }

    protected String salesforceApiVersionToUse() {
        return SalesforceEndpointConfig.DEFAULT_SALESFORCE_API_VERSION;
    }

    // CPI has shutdown timeout around 1 sec
    @Override
    protected int getShutdownTimeout() {
        return 1;
    }
}
