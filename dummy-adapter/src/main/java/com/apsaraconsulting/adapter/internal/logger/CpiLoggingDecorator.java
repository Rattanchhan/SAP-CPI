package com.apsaraconsulting.adapter.internal.logger;

import org.slf4j.Logger;

import java.util.Arrays;

/**
 * CPI prints logs only with ERROR level, it also doesn't add line numbers to the log entries
 * This class created to deal with these issues
 * @author Ilya Nesterov
 */
public class CpiLoggingDecorator {
    public static final LogLevel DEFAULT_LOG_LEVEL = LogLevel.WARN;
    private final Logger logger;
    private LogLevel logLevel;

    public static CpiLoggingDecorator fromLogger(Logger logger) {
        return new CpiLoggingDecorator(logger, DEFAULT_LOG_LEVEL);
    }

    public static CpiLoggingDecorator fromLogger(Logger logger, LogLevel logLevel) {
        return new CpiLoggingDecorator(logger, logLevel);
    }

    public CpiLoggingDecorator(Logger logger, LogLevel logLevel) {
        this.logger = logger;
        this.logLevel = logLevel;
    }

    public CpiLoggingDecorator withLogLevel(LogLevel logLevel) {
        return new CpiLoggingDecorator(this.logger, logLevel);
    }

    public LogLevel getLogLevel() {
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
            logger.error(buildMethodInfo(LogLevel.WARN) + msg, t);
        }
    }

    public void error(String msg) {
        logger.error(buildMethodInfo(LogLevel.ERROR) + msg);
    }

    public void error(String format, Object... arguments) {
        logger.error(buildMethodInfo(LogLevel.ERROR) + format, arguments);
    }

    public void error(String msg, Throwable t) {
        logger.error(buildMethodInfo(LogLevel.ERROR) + msg, t);
    }

    private String buildMethodInfo(LogLevel logLevel) {
        return String.format("[%s] #%s:%d ",
            logLevel,
            Thread.currentThread().getStackTrace()[3].getMethodName(),
            Thread.currentThread().getStackTrace()[3].getLineNumber()
        );
    }

    public boolean isTraceEnabled() {
        return LogLevel.TRACE.equals(logLevel);
    }

    public boolean isDebugEnabled() {
        return Arrays.asList(LogLevel.TRACE, LogLevel.DEBUG).contains(logLevel);
    }

    public boolean isInfoEnabled() {
        return Arrays.asList(LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO).contains(logLevel);
    }

    public boolean isWarnEnabled() {
        return Arrays.asList(LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN).contains(logLevel);
    }

    public enum LogLevel {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
}
