package com.apsaraconsulting.skyvvaadapter.internal.streaming;

import com.apsaraconsulting.skyvvaadapter.SalesforceConsumer;
import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.cometd.bayeux.Message.ERROR_FIELD;
import static org.cometd.bayeux.Message.SUBSCRIPTION_FIELD;

/**
 * @author Ilya Nesterov
 */
public class UnsubscribeListener implements ClientSessionChannel.MessageListener {

    private static final String FAILURE_FIELD = "failure";
    private static final String EXCEPTION_FIELD = "exception";

    private final CpiLoggingDecorator LOG;
    private final String channelName;

    // listen for unsubscribe error
    final CountDownLatch latch = new CountDownLatch(1);
    final String[] unsubscribeError = {null};
    final Exception[] unsubscribeFailure = {null};

    public UnsubscribeListener(
        String channelName,
        CpiLoggingDecorator.LogLevel logLevel
    ) {
        LOG = CpiLoggingDecorator.fromLogger(
            LoggerFactory.getLogger(SalesforceConsumer.class),
            logLevel
        );
        this.channelName = channelName;
    }

    public void onMessage(ClientSessionChannel channel, Message message) {
        LOG.debug("[CHANNEL:META_UNSUBSCRIBE]: {}", message);
        Object subscription = message.get(SUBSCRIPTION_FIELD);
        if (subscription != null) {
            String unsubscribedChannelName = subscription.toString();
            if (channelName.equals(unsubscribedChannelName)) {

                if (!message.isSuccessful()) {
                    LOG.warn("Couldn't unsubscribe from channel {}: {}", unsubscribedChannelName, message.get(ERROR_FIELD));
                    unsubscribeError[0] = (String) message.get(ERROR_FIELD);
                    unsubscribeFailure[0] = getFailure(message);
                } else {
                    // forget subscription
                    LOG.info("Successfully unsubscribed from channel {}", unsubscribedChannelName);
                }

                latch.countDown();
            }
        }
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public String[] getUnsubscribeError() {
        return unsubscribeError;
    }

    public Exception[] getUnsubscribeFailure() {
        return unsubscribeFailure;
    }

    private Exception getFailure(Message message) {
        Exception exception = null;
        if (message.get(EXCEPTION_FIELD) != null) {
            exception = (Exception) message.get(EXCEPTION_FIELD);
        } else if (message.get(FAILURE_FIELD) != null) {
            exception = (Exception) ((Map<String, Object>) message.get("failure")).get("exception");
        }
        return exception;
    }
}
