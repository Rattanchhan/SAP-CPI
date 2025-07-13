package com.apsaraconsulting.skyvvaadapter.internal.logger;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static java.lang.String.format;

/**
 * CPI prints logs only with ERROR level, it also doesn't add line numbers to the log entries
 * This class created to deal with these issues
 * @author Ilya Nesterov
 */
public class CpiLoggingDecorator {
    public static final LogLevel DEFAULT_LOG_LEVEL = LogLevel.WARN;
    private static volatile LogLevel globalLogLevel = DEFAULT_LOG_LEVEL;
    private static volatile boolean useGlobalLogLevel = true;
    private final Logger logger;
    @Deprecated
    private LogLevel logLevel;

    public static CpiLoggingDecorator fromLogger(Logger logger) {
        return new CpiLoggingDecorator(logger, DEFAULT_LOG_LEVEL);
    }

    public static CpiLoggingDecorator getLogger(Class clazz) {
        return CpiLoggingDecorator.fromLogger(LoggerFactory.getLogger(clazz));
    }

    @Deprecated
    public static CpiLoggingDecorator fromLogger(Logger logger, LogLevel logLevel) {
        return new CpiLoggingDecorator(logger, logLevel);
    }

    @Deprecated
    public CpiLoggingDecorator(Logger logger, LogLevel logLevel) {
        this.logger = logger;
        this.logLevel = logLevel;
    }

    public CpiLoggingDecorator withLogLevel(LogLevel logLevel) {
        return new CpiLoggingDecorator(this.logger, logLevel);
    }

    public static void setGlobalLogLevel(LogLevel logLevel) {
        globalLogLevel = logLevel;
    }

    public static void setUseGlobalLogLevel(boolean useGlobalLogLevelNewValue) {
        useGlobalLogLevel = useGlobalLogLevelNewValue;
    }

    public LogLevel getLogLevel() {
        if (useGlobalLogLevel) {
            return globalLogLevel;
        }
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    // only logs method name and line number
    public void trace() {
        if (isTraceEnabled()) {
            logger.error(buildMethodInfo(LogLevel.TRACE));
        }
    }

    public void trace(String msg) {
        if (isTraceEnabled()) {
            logger.error(buildMethodInfo(LogLevel.TRACE) + msg);
        }
    }

    public void trace(String format, Object... arguments) {
        if (isTraceEnabled()) {
            logger.error(buildMethodInfo(LogLevel.TRACE) + format, arguments);
        }
    }

    public void debug(String msg) {
        if (isDebugEnabled()) {
            logger.error(buildMethodInfo(LogLevel.DEBUG) + msg);
        }
    }

    public void debug(String format, Object... arguments) {
        if (isDebugEnabled()) {
            logger.error(buildMethodInfo(LogLevel.DEBUG) + format, arguments);
        }
    }

    public void info(String msg) {
        if (isInfoEnabled()) {
            logger.error(buildMethodInfo(LogLevel.INFO) + msg);
        }
    }

    public void info(String format, Object... arguments) {
        if (isInfoEnabled()) {
            logger.error(buildMethodInfo(LogLevel.INFO) + format, arguments);
        }
    }

    public void warn(String msg) {
        if (isWarnEnabled()) {
            logger.error(buildMethodInfo(LogLevel.WARN) + msg);
        }
    }

    public void warn(String format, Object... arguments) {
        if (isWarnEnabled()) {
            logger.error(buildMethodInfo(LogLevel.WARN) + format, arguments);
        }
    }

    public void warn(String msg, Throwable t) {
        if (isWarnEnabled()) {
            logger.error(format("%s%s%n%s", buildMethodInfo(LogLevel.WARN), msg, ExceptionUtils.getStackTrace(t)));
        }
    }

    public void error(String msg) {
        logger.error(buildMethodInfo(LogLevel.ERROR) + msg);
    }

    public void error(String format, Object... arguments) {
        logger.error(buildMethodInfo(LogLevel.ERROR) + format, arguments);
    }

    public void error(String msg, Throwable t) {
        logger.error(format("%s%s%n%s", buildMethodInfo(LogLevel.ERROR), msg, ExceptionUtils.getStackTrace(t)));
    }

    private String buildMethodInfo(LogLevel logLevel) {
        return format("[%s] #%s:%d ",
            logLevel,
            Thread.currentThread().getStackTrace()[3].getMethodName(),
            Thread.currentThread().getStackTrace()[3].getLineNumber()
        );
    }

    public boolean isTraceEnabled() {
        return LogLevel.TRACE.equals(getLogLevel());
    }

    public boolean isDebugEnabled() {
        return Arrays.asList(LogLevel.TRACE, LogLevel.DEBUG).contains(getLogLevel());
    }

    public boolean isInfoEnabled() {
        return Arrays.asList(LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO).contains(getLogLevel());
    }

    public boolean isWarnEnabled() {
        return Arrays.asList(LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN).contains(getLogLevel());
    }

    public enum LogLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
}
