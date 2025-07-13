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
package com.apsaraconsulting.adapter;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import com.apsaraconsulting.adapter.internal.HelloPrinter;
import com.apsaraconsulting.adapter.internal.logger.CpiLoggingDecorator;
import com.apsaraconsulting.adapter.internal.streaming.empconnector.*;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;
import org.cometd.client.BayeuxClient;
import org.eclipse.jetty.client.SendFailure;
import org.slf4j.LoggerFactory;

import static com.apsaraconsulting.adapter.internal.logger.CpiAdapterLoggingLevel.LOG_TRACE;
import static com.apsaraconsulting.adapter.internal.logger.CpiAdapterLoggingLevel.getLoggingOptionAsLogLevel;
import static org.cometd.bayeux.Channel.*;


public class DummyAdapterConsumer extends DefaultConsumer {

    private final CpiLoggingDecorator LOG;
    private final DummyAdapterEndpoint endpoint;
    private BearerTokenProvider tokenProvider;
    private EmpConnector empConnector;
    private TopicSubscription topicSubscription;
    private String topicName = "RESTStreamingAPITest";

    public DummyAdapterConsumer(final DummyAdapterEndpoint endpoint, final Processor processor) throws Exception {
        super(endpoint, processor);
        LOG = CpiLoggingDecorator.fromLogger(
            LoggerFactory.getLogger(DummyAdapterConsumer.class),
            getLoggingOptionAsLogLevel(LOG_TRACE)
        );
        LOG.trace("Creating consumer");
        this.endpoint = endpoint;
        // TODO commented temporary
        //initEmpConnector();
    }

    private void initEmpConnector() throws Exception {
        tokenProvider = new BearerTokenProvider(() -> {
            try {
                return LoginHelper.login(
                    new URL("https://test.salesforce.com"),
                    "xxx",
                    "xxx"
                );
            } catch (Exception ex) {
                LOG.error("Couldn't get access token: " + ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
        });

        LoggingListener loggingListener = new LoggingListener(LOG.getLogLevel());

        empConnector = new EmpConnector(tokenProvider.login(), LOG.getLogLevel());
        empConnector.addListener(META_HANDSHAKE, loggingListener)
            .addListener(META_CONNECT, loggingListener)
            .addListener(META_DISCONNECT, loggingListener);
        empConnector.setBearerTokenProvider(tokenProvider);
        empConnector.start().get(5, TimeUnit.SECONDS);
    }

    @Override
    protected void doStart() throws Exception {
        LOG.trace("Starting consumer");
        super.doStart();

        //testLoadClasses("Consumer starting phase:");

        // TODO commented temporary
        // topicSubscription = empConnector.subscribe(
        //    getChannelName(topicName),
        //    -1,
        //    message -> LOG.info("Received message: {}", message)
        //).get(5, TimeUnit.SECONDS);
    }

    @Override
    protected void doStop() throws Exception {
        LOG.trace("Stopping consumer");
        super.doStop();

        try {
            new HelloPrinter().printHello(LOG);
        } catch (Throwable ex) {
            LOG.error("Separate class case. Couldn't print Hello: " + ex.getMessage(), ex);
        }

        try {
            new InnerHelloPrinter().printHello(LOG);
        } catch (Throwable ex) {
            LOG.error("Inner class case. Couldn't print Hello: " + ex.getMessage(), ex);
        }

        try {
            new NestedHelloPrinter().printHello(LOG);
        } catch (Throwable ex) {
            LOG.error("Nested class case. Couldn't print Hello: " + ex.getMessage(), ex);
        }

        //testLoadClasses("Consumer stopping phase:");

        // TODO commented temporary
        //try {
        //    topicSubscription.cancel().get(5, TimeUnit.SECONDS);
        //    LOG.trace("Unsubscribed successfully from topic {}", topicName);
        //} catch (Throwable ex) {
        //    LOG.error("Couldn't unsubscribe from topic: " + topicName, ex);
        //}
        //try {
        //    empConnector.stop();
        //} catch (Throwable ex) {
        //    LOG.error("Couldn't stop EmpConnector: " + ex.getMessage(), ex);
        //}
        LOG.trace("Consumer is stopped");
    }

    private String getChannelName(final String topicName) {
        final StringBuilder channelName = new StringBuilder();
        if (topicName.charAt(0) != '/') {
            channelName.append('/');
        }

        if (topicName.indexOf('/', 1) > 0) {
            channelName.append(topicName);
        } else {
            channelName.append("topic/");
            channelName.append(topicName);
        }

        final int typeIdx = channelName.indexOf("/", 1);
        if ("event".equals(channelName.substring(1, typeIdx)) && !topicName.endsWith("__e")) {
            channelName.append("__e");
        }

        return channelName.toString();
    }

    private void testLoadClasses(String logPrefix) {
        try {
            LOG.trace("{} Trying to load class SendFailure", logPrefix);
            LOG.trace("{} SendFailure class is loaded: {}", logPrefix, SendFailure.class.getName());
        } catch (Exception ex) {
            LOG.error("Error occurred while loading class SendFailure: " + ex.getMessage(), ex);
        }
        try {
            LOG.trace("{} Trying to load class BayeuxClient.State", logPrefix);
            LOG.trace("{} {} is loaded", logPrefix, BayeuxClient.State.class);
        } catch (Exception ex) {
            LOG.error("Error occurred while loading class BayeuxClient.State: " + ex.getMessage(), ex);
        }
    }

    private class InnerHelloPrinter {
        public void printHello(CpiLoggingDecorator logger) {
            logger.debug("Hello!");
        }
    }

    private static class NestedHelloPrinter {
        public void printHello(CpiLoggingDecorator logger) {
            logger.debug("Hello!");
        }
    }
}
