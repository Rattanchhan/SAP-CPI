package com.apsaraconsulting.adapter.MPL;

import com.sap.it.api.ITApiFactory;
import com.sap.it.api.exception.InvalidContextException;
import com.sap.it.api.msglog.adapter.*;
import org.apache.camel.Exchange;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MessageLogFactory {
    private final Map<String, String> headerElements = new HashMap<>();
    private static  final Logger logger  = LoggerFactory.getLogger(MessageLogFactory.class);

    public void integrateIntoTracing(Exchange exchange) throws InvalidContextException {
        this.headerElements.put("protocolType", "foo");
        this.headerElements.put("protocolVersion", "1");
        this.headerElements.put("applicationMessageFormat", "bar");

        for (Map.Entry<String, String> entry : headerElements.entrySet()) {
            exchange.getIn().setHeader(entry.getKey(), entry.getValue());
        }

        AdapterMessageLogFactory msgLogFactory = ITApiFactory.getService(AdapterMessageLogFactory.class, null);
        String logBuilder = createLogMessage(exchange);
        AdapterMessageLogWithStatus msgLog = null;
        try {
            msgLog = msgLogFactory.getMessageLogWithStatus(
                exchange,
                "Dummy-Adapter-MPL Receiver Log Text",
                "DUMMY-ADAPTER-ID",
                "SomeRandomString" + System.currentTimeMillis() + "-RECEIVED"
            );

            msgLog.putAdapterAttribute("GreetingsMessage", logBuilder);
            /// Check if trace is active before writing
            if (msgLog.isTraceActive()) {
                writeTraceMessage(exchange, headerElements, msgLog, AdapterTraceMessageType.RECEIVER_INBOUND);

                /// Process and write outbound trace
                Map<String, String> outboundHeaderElements = new HashMap<>(convertMap(exchange.getIn().getHeaders()));
                outboundHeaderElements.put("isProcessed", "true");
                writeTraceMessage(exchange, outboundHeaderElements, msgLog, AdapterTraceMessageType.RECEIVER_OUTBOUND);
            }

        } finally {
            if (msgLog != null) {
                msgLog.close();
            }
        }
        logger.error("INFO-LEVEL | Trace method called | Exchange ID: {}", exchange.getExchangeId());
        logger.error("INFO-LEVEL | Trace active: {} ", msgLog.isTraceActive());
    }

    public void  integrateIntoMPL(Exchange exchange) throws InvalidContextException {
        AdapterMessageLogFactory msgLogFactory = ITApiFactory.getService(AdapterMessageLogFactory.class, null);
        try (AdapterMessageLogWithStatus msgLog = msgLogFactory.getMessageLogWithStatus(exchange, "DummyAdapterProducer Inbound Log Text ",
            "DUMMY_ADAPTER-ID", "SomeRandomString" + System.currentTimeMillis() + "-IN")) {
            msgLog.putAdapterAttribute("PayloadResponse", exchange.getIn().getBody().toString());
        }
    }

    /// Instead of JsonObject, use a more reliable approach:
    private String createLogMessage(Exchange exchange) {
        Date now = new Date();
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("Prefix: Hello There!! Now it is ").append(now).append("\n");
        logBuilder.append("Timestamp: ").append(now.getTime()).append("\n");

        Object body = exchange.getIn().getBody();
        if (body != null) {
            logBuilder.append("PayloadResponse: ").append(body.toString());
        } else {
            logBuilder.append("PayloadResponse: No payload");
        }

        return logBuilder.toString();
    }


    /**
     * Convert a Map<String, Object> to a new Map<String, String>
     * @param inputMap
     *              input Map
     *
     */
    private Map<String, String> convertMap(Map<String, Object> inputMap) {
        Map<String, String> result = new HashMap<>();
        if (inputMap != null) {
            for (Map.Entry<String, Object> entry : inputMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                result.put(key, value != null ? value.toString() : "null");
            }
        }
        return result;
    }

    /**
     * Write a trace message.
     *
     * @param msgLog
     *            message log
     * @param type
     *            message type
     */
    private void writeTraceMessage(final Exchange exchange, Map<String, String> headers,
                                   final AdapterMessageLogWithStatus msgLog, final AdapterTraceMessageType type) {

        byte[] payloadBytes = getPayloadBytes(exchange);
        AdapterTraceMessage traceMsg = msgLog.createTraceMessage(type, payloadBytes, false);
        traceMsg.setHeaders(headers);
        msgLog.writeTrace(traceMsg);
    }

    private byte[] getPayloadBytes(Exchange exchange) {
        try {
            Object body = exchange.getIn().getBody();
            if (body == null) {
                return "No payload".getBytes(StandardCharsets.UTF_8);
            }

            /// Handle different body types
            if (body instanceof byte[]) {
                return (byte[]) body;
            } else if (body instanceof String) {
                return ((String) body).getBytes(StandardCharsets.UTF_8);
            } else if (body instanceof InputStream) {
                return IOUtils.toByteArray((InputStream) body);
            } else {
                return body.toString().getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            String errorMsg = "Error reading payload: " + e.getMessage();
            return errorMsg.getBytes(StandardCharsets.UTF_8);
        }
    }

}
