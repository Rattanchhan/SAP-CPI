package com.apsaraconsulting.adapter;

/**
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

import java.util.Map;

import com.apsaraconsulting.adapter.internal.logger.CpiLoggingDecorator;
import com.apsaraconsulting.adapter.internal.util.AdapterInitializationUtils;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.ComponentResolver;
import org.apache.camel.support.DefaultComponent;
import org.slf4j.LoggerFactory;

import static com.apsaraconsulting.adapter.internal.logger.CpiAdapterLoggingLevel.LOG_TRACE;
import static com.apsaraconsulting.adapter.internal.logger.CpiAdapterLoggingLevel.getLoggingOptionAsLogLevel;

/**
 * Represents the component that manages.
 */
public class DummyAdapterComponent extends DefaultComponent implements ComponentResolver {

    private static final CpiLoggingDecorator LOG = CpiLoggingDecorator.fromLogger(
        LoggerFactory.getLogger(DummyAdapterComponent.class),
        getLoggingOptionAsLogLevel(LOG_TRACE)
    );

    static {
        AdapterInitializationUtils.loadDependenciesEagerly(LOG);
    }

    public DummyAdapterComponent() {
        super();
        LOG.trace("Creating component");
    }

    public DummyAdapterComponent(CamelContext context) {
        super(context);
        LOG.trace("Creating component");
    }

    protected Endpoint createEndpoint(final String uri, final String remaining, final Map<String, Object> parameters) throws Exception {
        LOG.info("Creating the endpoint");
        final Endpoint endpoint = new DummyAdapterEndpoint(uri, remaining, this);
        setProperties(endpoint, parameters);
        return endpoint;
    }

    @Override
    public Component resolveComponent(String name, org.apache.camel.CamelContext context) throws Exception {
        return this;
    }
}
