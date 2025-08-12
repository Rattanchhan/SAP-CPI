package com.apsaraconsulting.adapter.MPL;

import com.apsaraconsulting.adapter.DummyAdapterEndpoint;
import com.sap.it.api.ITApiFactory;
import com.sap.it.api.exception.InvalidContextException;
import com.sap.it.api.msglog.adapter.*;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MessageLogFactory {
    private static final Logger LOG =  LoggerFactory.getLogger(MessageLogFactory.class);
    private final Map<String, String> headerElements = new HashMap<>();

    public void integrateIntoTracing(Exchange exchange, DummyAdapterEndpoint endpoint) throws InvalidContextException {
        this.headerElements.put("protocolType", "foo");
        this.headerElements.put("protocolVersion", "1");
        this.headerElements.put("applicationMessageFormat", "bar");

        for (Map.Entry<String, String> entry : headerElements.entrySet()) {
            exchange.getIn().setHeader(entry.getKey(), entry.getValue());
        }

        AdapterMessageLogFactory msgLogFactory = ITApiFactory.getService(AdapterMessageLogFactory.class, null);
        String greetingsMessage = endpoint.getGreeting();
        Date now = new Date();
        if (greetingsMessage == null || greetingsMessage.isEmpty()) {
            LOG.error("The message is empty! Default one will be used");
            greetingsMessage = "Hello There!! ";
        }
        StringBuilder builder = new StringBuilder(greetingsMessage);
        builder.append(" Now it is ").append(now);

        AdapterMessageLogWithStatus msgLog = null;
        try {
            msgLog = msgLogFactory.getMessageLogWithStatus(
                exchange,
                "Dummy-Adapter-MPL Receiver Log Text",
                "DUMMY-ADAPTER-ID",
                "SomeRandomString" + System.currentTimeMillis() + "-RECEIVED"
            );

            msgLog.putAdapterAttribute("GreetingsMessage", greetingsMessage);
            exchange.getIn().setBody(builder.toString());

            if (msgLog.isTraceActive()) {
                writeTraceMessage(exchange, headerElements, msgLog, AdapterTraceMessageType.RECEIVER_INBOUND);
            }

        } finally {
            assert msgLog != null;
            if (msgLog.isTraceActive()) {
                Map<String, String> outboundHeaderElements = new HashMap<>(convertMap(exchange.getIn().getHeaders()));
                outboundHeaderElements.put("isProcessed", "true");
                writeTraceMessage(exchange, outboundHeaderElements, msgLog, AdapterTraceMessageType.RECEIVER_OUTBOUND);
            }
            msgLog.close();
        }
    }

    public void  integrateIntoMPL(Exchange exchange) throws InvalidContextException {
        AdapterMessageLogFactory msgLogFactory = ITApiFactory.getService(AdapterMessageLogFactory.class, null);
        try (AdapterMessageLogWithStatus msgLog = msgLogFactory.getMessageLogWithStatus(exchange, "DummyAdapterProducer Inbound Log Text ",
            "DUMMY_ADAPTER-ID", "SomeRandomString" + System.currentTimeMillis() + "-IN")) {
            msgLog.putAdapterAttribute("PayloadResponse", exchange.getIn().getBody().toString());
        }
    }


    /**
     * Convert a Map<String, Object> to a new Map<String, String>
     * @param inputMap
     *              input Map
     *
     */
    private Map<String, String> convertMap(Map<String, Object> inputMap) {
        Map<String, String> result = new HashMap<String, String>();
        for (String key : inputMap.keySet()) {
            result.put(key, inputMap.get(key).toString());
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

        Object payload = exchange.getIn().getBody();
        byte[] payloadBytes;

        if (payload instanceof byte[]) {
            payloadBytes = (byte[]) payload;
        } else if (payload != null) {
            payloadBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            payloadBytes = new byte[0];
        }

        AdapterTraceMessage traceMsg = msgLog.createTraceMessage(type, payloadBytes, false);
        traceMsg.setHeaders(headers);

        msgLog.writeTrace(traceMsg);
    }

}
