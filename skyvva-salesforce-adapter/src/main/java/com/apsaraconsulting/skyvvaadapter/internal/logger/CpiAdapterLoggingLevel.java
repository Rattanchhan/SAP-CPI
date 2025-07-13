package com.apsaraconsulting.skyvvaadapter.internal.logger;

import org.slf4j.LoggerFactory;

public class CpiAdapterLoggingLevel {

    private static final CpiLoggingDecorator LOG = CpiLoggingDecorator.fromLogger(
        LoggerFactory.getLogger(CpiAdapterLoggingLevel.class)
    );
    // Values used to get settings from HCI config
    public static final String LOG_TRACE = "Trace";
    public static final String LOG_DEBUG = "Debug";
    public static final String LOG_INFO = "Info";
    public static final String LOG_WARNING = "Warning";
    public static final String LOG_ERROR = "Error";

    public static CpiLoggingDecorator.LogLevel getLoggingOptionAsLogLevel(String loggingOption) {
        try {
            if (loggingOption == null) {
                return CpiLoggingDecorator.DEFAULT_LOG_LEVEL;
            }

            switch (loggingOption) {
                case LOG_TRACE: return CpiLoggingDecorator.LogLevel.TRACE;
                case LOG_DEBUG: return CpiLoggingDecorator.LogLevel.DEBUG;
                case LOG_INFO: return CpiLoggingDecorator.LogLevel.INFO;
                case LOG_WARNING: return CpiLoggingDecorator.LogLevel.WARN;
                case LOG_ERROR: return CpiLoggingDecorator.LogLevel.ERROR;
                default: return CpiLoggingDecorator.DEFAULT_LOG_LEVEL;
            }
        } catch (Exception ex) {
            LOG.error("Couldn't convert logging option as log level");
        }

        return CpiLoggingDecorator.DEFAULT_LOG_LEVEL;
    }
}
