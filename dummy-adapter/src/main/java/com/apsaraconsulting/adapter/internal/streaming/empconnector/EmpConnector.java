/*
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license.
 * For full license text, see LICENSE.TXT file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.apsaraconsulting.adapter.internal.streaming.empconnector;

import com.apsaraconsulting.adapter.internal.logger.CpiLoggingDecorator;
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.BayeuxClient.State;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author hal.hildebrand
 * @since API v37.0
 */
public class EmpConnector {
    private static final String ERROR = "error";
    private static final String FAILURE = "failure";

    private class SubscriptionImpl implements TopicSubscription {
        private final String topic;
        private final Consumer<Message> consumer;
        private final ClientSessionChannel.MessageListener messageListener;

        private SubscriptionImpl(String topic, Consumer<Message> consumer) {
            this.topic = topic;
            this.consumer = consumer;
            this.messageListener = (c, message) -> consumer.accept(message);
            subscriptions.add(this);
        }

        /*
         * (non-Javadoc)
         * @see Subscription#cancel()
         */
        @Override
        public Future<Boolean> cancel() {
            long replayFrom = getReplayFrom();
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            replay.remove(topicWithoutQueryString(topic));
            //LOG.trace("Trying to load class SendFailure");
            //LOG.trace("SendFailure class loaded: {}", SendFailure.class.getName());
            if (running.get() && client != null) {
                client.getChannel(topic).unsubscribe(messageListener, (channel, message) -> {
                    LOG.trace("{} {}: {}",
                        message.isSuccessful() ? "Success" : "Failed",
                        channel.getId(),
                        message
                    );
                    subscriptions.remove(this);
                    if (message.isSuccessful()) {
                        future.complete(true);
                    } else {
                        Object error = message.get(ERROR);
                        if (error == null) {
                            error = message.get(FAILURE);
                        }
                        future.completeExceptionally(
                            new CannotUnsubscribe(parameters.endpoint(), topic, replayFrom, error != null ? error : message));
                    }
                });
            } else {
                future.complete(true);
            }
            return future;
        }

        /*
         * (non-Javadoc)
         * @see Subscription#getReplay()
         */
        @Override
        public long getReplayFrom() {
            return replay.getOrDefault(topicWithoutQueryString(topic), REPLAY_FROM_EARLIEST);
        }

        /*
         * (non-Javadoc)
         * @see Subscription#getTopic()
         */
        @Override
        public String getTopic() {
            return topic;
        }

        @Override
        public String toString() {
            return String.format("Subscription [%s:%s]", getTopic(), getReplayFrom());
        }

        Future<TopicSubscription> subscribe() {
            long replayFrom = getReplayFrom();
            ClientSessionChannel channel = client.getChannel(topic);
            CompletableFuture<TopicSubscription> future = new CompletableFuture<>();
            channel.subscribe(messageListener, (c, message) -> {
                LOG.trace("{} {}: {}",
                    message.isSuccessful() ? "Success" : "Failed",
                    c.getId(),
                    message
                );
                if (message.isSuccessful()) {
                    future.complete(this);
                } else {
                    Object error = message.get(ERROR);
                    if (error == null) {
                        error = message.get(FAILURE);
                    }
                    future.completeExceptionally(
                            new CannotSubscribe(parameters.endpoint(), topic, replayFrom, error != null ? error : message));
                }
            });
            return future;
        }
    }

    public static long REPLAY_FROM_EARLIEST = -2L;
    public static long REPLAY_FROM_TIP = -1L;

    private static String AUTHORIZATION = "Authorization";
    private final CpiLoggingDecorator LOG;

    private volatile BayeuxClient client;
    private final HttpClient httpClient;
    private final BayeuxParameters parameters;
    private final ConcurrentMap<String, Long> replay = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean();

    private final Set<SubscriptionImpl> subscriptions = new CopyOnWriteArraySet<>();
    private final Set<MessageListenerInfo> listenerInfos = new CopyOnWriteArraySet<>();

    private Function<Boolean, String> bearerTokenProvider;
    private AtomicBoolean reauthenticate = new AtomicBoolean(false);

    public EmpConnector(BayeuxParameters parameters, CpiLoggingDecorator.LogLevel logLevel) {
        LOG = CpiLoggingDecorator.fromLogger(
            LoggerFactory.getLogger(EmpConnector.class),
            logLevel
        );
        this.parameters = parameters;
        httpClient = new HttpClient(parameters.sslContextFactory());
        httpClient.getProxyConfiguration().getProxies().addAll(parameters.proxies());
    }

    /**
     * Start the connector.
     * @return true if connection was established, false otherwise
     */
    public Future<Boolean> start() {
        if (running.compareAndSet(false, true)) {
            addListener(Channel.META_CONNECT, new AuthFailureListener());
            addListener(Channel.META_HANDSHAKE, new AuthFailureListener());
            replay.clear();
            return connect();
        }
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.complete(true);
        return future;
    }

    /**
     * Disconnecting Bayeux Client in Emp Connector
     */
    private void disconnect() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        if (client != null) {
            LOG.info("Disconnecting Bayeux Client in EmpConnector");
            try {
                client.disconnect();
                boolean disconnected = client.waitFor(60000, State.DISCONNECTED);
                if (!disconnected) {
                    LOG.warn("Could not disconnect client after {} msec", 60000);
                    client.abort();
                }
            } catch (Throwable ex) {
                LOG.error("Error occurred while disconnecting client: " + ex.getMessage(), ex);
            }
            client = null;
        }
    }

    /**
     * Stop the connector
     */
    public void stop() {
        try {
            disconnect();
        } catch (Exception ex) {
            LOG.error("Unable to disconnect streaming client: " + ex.getMessage(), ex);
        }
        if (httpClient != null) {
            try {
                LOG.info("Stopping the http client!");
                httpClient.stop();
            } catch (Exception e) {
                LOG.error("Unable to stop HTTP transport[{}]", parameters.endpoint(), e);
            }
        }
    }

    /**
     * Set a bearer token / session id provider function that takes a boolean as input and returns a valid token.
     * If the input is true, the provider function is supposed to re-authenticate with the Salesforce server
     * and get a fresh session id or token.
     *
     * @param bearerTokenProvider a bearer token provider function.
     */
    public void setBearerTokenProvider(Function<Boolean, String> bearerTokenProvider) {
        this.bearerTokenProvider = bearerTokenProvider;
    }

    /**
     * Subscribe to a topic, receiving events after the replayFrom position
     *
     * @param topic
     *            - the topic to subscribe to
     * @param replayFrom
     *            - the replayFrom position in the event stream
     * @param consumer
     *            - the consumer of the events
     * @return a Future returning the Subscription - on completion returns a Subscription or throws a CannotSubscribe
     *         exception
     */
    public Future<TopicSubscription> subscribe(String topic, long replayFrom, Consumer<Message> consumer) {
        if (!running.get()) {
            throw new IllegalStateException(String.format("Connector[%s} has not been started",
                    parameters.endpoint()));
        }
        topic = topic.replaceAll("/$", "");

        final String topicWithoutQueryString = topicWithoutQueryString(topic);
        if (replay.putIfAbsent(topicWithoutQueryString, replayFrom) != null) {
            throw new IllegalStateException(String.format("Already subscribed to %s [%s]",
                    topic, parameters.endpoint()));
        }

        SubscriptionImpl subscription = new SubscriptionImpl(topic, consumer);

        return subscription.subscribe();
    }

    /**
     * Unsubscribe to a topic subscription
     *
     * @param topic
     *            - the topic subscribed
     */
    public void unsubscribe(String topic) {
        subscriptions.stream()
                .filter(subscription -> subscription.getTopic().equalsIgnoreCase(topic))
                .findAny()
                .ifPresent(SubscriptionImpl::cancel);
    }

    /**
     * Subscribe to a topic, receiving events from the earliest event position in the stream
     *
     * @param topic
     *            - the topic to subscribe to
     * @param consumer
     *            - the consumer of the events
     * @return a Future returning the Subscription - on completion returns a Subscription or throws a CannotSubscribe
     *         exception
     */
    public Future<TopicSubscription> subscribeEarliest(String topic, Consumer<Message> consumer) {
        return subscribe(topic, REPLAY_FROM_EARLIEST, consumer);
    }

    /**
     * Subscribe to a topic, receiving events from the latest event position in the stream
     *
     * @param topic
     *            - the topic to subscribe to
     * @param consumer
     *            - the consumer of the events
     * @return a Future returning the Subscription - on completion returns a Subscription or throws a CannotSubscribe
     *         exception
     */
    public Future<TopicSubscription> subscribeTip(String topic, Consumer<Message> consumer) {
        return subscribe(topic, REPLAY_FROM_TIP, consumer);
    }

    public EmpConnector addListener(String channel, ClientSessionChannel.MessageListener messageListener) {
        listenerInfos.add(new MessageListenerInfo(channel, messageListener));
        return this;
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public boolean isDisconnected() {
        return client == null || client.isDisconnected();
    }

    public boolean isHandshook() {
        return client != null && client.isHandshook();
    }

    public long getLastReplayId(String topic) {
        return replay.get(topic);
    }

    private static String topicWithoutQueryString(String fullTopic) {
        return fullTopic.split("\\?")[0];
    }

    private Future<Boolean> connect() {
        LOG.info("EmpConnector connecting");
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            if (!httpClient.isStarted()) {
                httpClient.start();
            }
        } catch (Exception e) {
            LOG.error("Unable to start HTTP transport[{}]", parameters.endpoint(), e);
            running.set(false);
            future.complete(false);
            return future;
        }

        String bearerToken = bearerToken();

        LongPollingTransport httpTransport = new LongPollingTransport(parameters.longPollingOptions(), httpClient) {
            @Override
            protected void customize(Request request) {
                request.header(AUTHORIZATION, bearerToken);
            }
        };

        client = new BayeuxClient(parameters.endpoint().toExternalForm(), httpTransport);

        client.addExtension(new ReplayExtension(replay));

        addListeners(client);

        client.handshake((channel, m) -> {
            if (!m.isSuccessful()) {
                Object error = m.get(ERROR);
                if (error == null) {
                    error = m.get(FAILURE);
                }
                future.completeExceptionally(new ConnectException(
                        String.format("Cannot connect [%s] : %s", parameters.endpoint(), error)));
                running.set(false);
            } else {
                subscriptions.forEach(SubscriptionImpl::subscribe);
                future.complete(true);
            }
        });

        return future;
    }

    private void addListeners(BayeuxClient client) {
        for (MessageListenerInfo info : listenerInfos) {
            client.getChannel(info.getChannelName()).addListener(info.getMessageListener());
        }
    }

    private String bearerToken() {
        String bearerToken;
        if (bearerTokenProvider != null) {
            bearerToken = bearerTokenProvider.apply(reauthenticate.get());
            reauthenticate.compareAndSet(true, false);
        } else {
            bearerToken = parameters.bearerToken();
        }

        return bearerToken;
    }

    private void reconnect() {
        if (running.compareAndSet(false, true)) {
            connect();
        } else {
            LOG.error("The current value of running is not as we expect, this means our reconnection may not happen");
        }
    }

    /**
     * Listens to /meta/connect channel messages and handles 401 errors, where client needs
     * to reauthenticate.
     */
    private class AuthFailureListener implements ClientSessionChannel.MessageListener {
        private static final String ERROR_401 = "401";
        private static final String ERROR_403 = "403";

        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            if (!message.isSuccessful()) {
                if (isError(message, ERROR_401) || isError(message, ERROR_403)) {
                    reauthenticate.set(true);
                    disconnect();
                    reconnect();
                }
            }
        }

        private boolean isError(Message message, String errorCode) {
            String error = (String)message.get(Message.ERROR_FIELD);
            String failureReason = getFailureReason(message);

            return (error != null && error.startsWith(errorCode)) ||
                    (failureReason != null && failureReason.startsWith(errorCode));
        }

        private String getFailureReason(Message message) {
            String failureReason = null;
            Map<String, Object> ext = message.getExt();
            if (ext != null) {
                Map<String, Object> sfdc = (Map<String, Object>)ext.get("sfdc");
                if (sfdc != null) {
                    failureReason = (String)sfdc.get("failureReason");
                }
            }
            return failureReason;
        }
    }

    private static class MessageListenerInfo {
        private String channelName;
        private ClientSessionChannel.MessageListener messageListener;

        MessageListenerInfo(String channelName, ClientSessionChannel.MessageListener messageListener) {
            this.channelName = channelName;
            this.messageListener = messageListener;
        }

        String getChannelName() {
            return channelName;
        }

        ClientSessionChannel.MessageListener getMessageListener() {
            return messageListener;
        }
    }
}
