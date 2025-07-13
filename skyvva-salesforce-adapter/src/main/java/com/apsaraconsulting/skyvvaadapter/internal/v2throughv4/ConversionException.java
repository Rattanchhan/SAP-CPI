package com.apsaraconsulting.skyvvaadapter.internal.v2throughv4;

/**
 * @author Ilya Nesterov
 */
public class ConversionException extends RuntimeException {

    public ConversionException() {
    }

    public ConversionException(String message) {
        super(message);
    }

    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
