package com.apsaraconsulting.adapter.internal;

import com.apsaraconsulting.adapter.internal.logger.CpiLoggingDecorator;

/**
 * @author Ilya Nesterov
 */
public class HelloPrinter {

    public void printHello(CpiLoggingDecorator logger) {
        logger.debug("Hello!");
    }
}
