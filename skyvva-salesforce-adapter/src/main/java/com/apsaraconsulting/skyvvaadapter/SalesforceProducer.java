/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apsaraconsulting.skyvvaadapter;

import com.apsaraconsulting.skyvvaadapter.internal.processor.JsonRestProcessor;
import com.apsaraconsulting.skyvvaadapter.internal.processor.SalesforceProcessor;
import com.apsaraconsulting.skyvvaadapter.internal.processor.SkyvvaIntegrationServiceAdapter;
import com.apsaraconsulting.skyvvaadapter.internal.processor.XmlRestProcessor;
import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import com.apsaraconsulting.skyvvaadapter.api.SalesforceException;
import com.apsaraconsulting.skyvvaadapter.internal.PayloadFormat;
import com.apsaraconsulting.skyvvaadapter.internal.SkyvvaApiVersion;
import com.apsaraconsulting.skyvvaadapter.internal.SkyvvaIntegrationMode;
import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.camel.support.DefaultAsyncProducer;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;

import static com.apsaraconsulting.skyvvaadapter.internal.SkyvvaIntegrationMode.*;

/**
 * The Salesforce producer.
 */
public class SalesforceProducer extends DefaultAsyncProducer {

    private final SalesforceProcessor processor;
    private final SalesforceEndpointConfig endpointConfig;
    private final SkyvvaIntegrationMode integrationMode;
    private final CpiLoggingDecorator LOG;

    public SalesforceProducer(SalesforceEndpoint endpoint) throws SalesforceException {
        super(endpoint);
        LOG = CpiLoggingDecorator.fromLogger(
            LoggerFactory.getLogger(SalesforceProducer.class),
            endpoint.getConfiguration().getLoggingOptionAsLogLevel()
        );
        LOG.trace();

        this.endpointConfig = ((SalesforceEndpoint) getEndpoint()).getConfiguration();
        this.integrationMode = this.endpointConfig.getSkyvvaIntegrationMode();

        if (endpointConfig.getSkyvvaApiVersion().equals(SkyvvaApiVersion.V2)
            || this.integrationMode.equals(ASYNCHRONOUS)
            || this.integrationMode.equals(SYNCHRONOUS)
        ) {
            if (this.endpointConfig.getRequestFormat().equals(PayloadFormat.JSON)) {
                // Create a JSON exchange processor
                this.processor = new JsonRestProcessor(endpoint);
            } else {
                this.processor = new XmlRestProcessor(endpoint);
            }
        } else {
            this.processor = new SkyvvaIntegrationServiceAdapter(endpoint);
        }

        LOG.trace("Initialized processor: {}", this.processor);
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        LOG.trace("Processing operation {}", ((SalesforceEndpoint) getEndpoint()).getOperationName());
        return processor.process(exchange, callback);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        LOG.trace();
        try {
            // start Salesforce processor
            ServiceHelper.startService(processor);
        } catch (Throwable ex) {
            String errorMessage = "Couldn't start producer: " + ExceptionUtils.getMessage(ex);
            LOG.error(errorMessage, ex);
            throw new RuntimeException(errorMessage, ex);
        }
    }

    @Override
    protected void doStop() throws Exception {
        LOG.trace();
        try {
            // stop Salesforce processor
            ServiceHelper.stopService(processor);
        } catch (Throwable ex) {
            LOG.error("Couldn't stop producer", ex);
        }
        super.doStop();
    }
}
