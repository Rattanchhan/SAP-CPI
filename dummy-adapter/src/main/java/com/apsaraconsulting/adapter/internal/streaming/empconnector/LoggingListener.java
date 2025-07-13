package com.apsaraconsulting.adapter.internal.streaming.empconnector;

import com.apsaraconsulting.adapter.internal.logger.CpiLoggingDecorator;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.slf4j.LoggerFactory;

/**
 * @author Ilya Nesterov
 */
public class LoggingListener implements ClientSessionChannel.MessageListener {

    private boolean logSuccess;
    private boolean logFailure;
    private final CpiLoggingDecorator LOG;

    public LoggingListener(CpiLoggingDecorator.LogLevel logLevel) {
        this(logLevel, true, true);
    }

    public LoggingListener(CpiLoggingDecorator.LogLevel logLevel, boolean logSuccess, boolean logFailure) {
        LOG = CpiLoggingDecorator.fromLogger(
            LoggerFactory.getLogger(LoggingListener.class),
            logLevel
        );
        this.logSuccess = logSuccess;
        this.logFailure = logFailure;
    }

    @Override
    public void onMessage(ClientSessionChannel clientSessionChannel, Message message) {
        if (logSuccess && message.isSuccessful()) {
            LOG.trace("Success {}: {}", clientSessionChannel.getId(), message);
        }

        if (logFailure && !message.isSuccessful()) {
            LOG.trace("Failure {}: {}", clientSessionChannel.getId(), message);
        }
    }
}
