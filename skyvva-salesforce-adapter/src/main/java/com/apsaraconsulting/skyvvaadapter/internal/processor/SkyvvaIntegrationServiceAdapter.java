package com.apsaraconsulting.skyvvaadapter.internal.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iservice.SkyvvaIntegrationService;
import com.iservice.model.SalesforceCredential;
import com.iservice.task.CheckSFProcessingJob;
import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import com.apsaraconsulting.skyvvaadapter.SalesforceEndpoint;
import com.apsaraconsulting.skyvvaadapter.SalesforceLoginConfig;
import com.apsaraconsulting.skyvvaadapter.api.SalesforceException;
import com.apsaraconsulting.skyvvaadapter.api.utils.JsonUtils;
import com.apsaraconsulting.skyvvaadapter.internal.SkyvvaIntegrationMode;
import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.InputStream;
import java.util.List;

import static java.lang.String.format;

/**
 * @author Ilya Nesterov
 */
public class SkyvvaIntegrationServiceAdapter extends AbstractSalesforceProcessor {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(SkyvvaIntegrationServiceAdapter.class);
    private static final ObjectMapper OBJECT_MAPPER = JsonUtils.createObjectMapper();

    private final SkyvvaIntegrationService skyvvaIntegrationService;
    private final SkyvvaIntegrationMode integrationMode;

    public SkyvvaIntegrationServiceAdapter(SalesforceEndpoint endpoint) {
        super(endpoint);
        try {
            LOGGER.trace();

            this.integrationMode = this.endpointConfig.getSkyvvaIntegrationMode();

            SalesforceLoginConfig loginConfig = endpoint.getComponent().getLoginConfig();
            SalesforceCredential salesforceCredential = new SalesforceCredential();
            //we don't have token configured separately in adapter, it's a part of loginConfig.password
            salesforceCredential.setUsername(loginConfig.getUserName());
            salesforceCredential.setPassword(loginConfig.getPassword());
            salesforceCredential.setToken("");
            salesforceCredential.setLoginUrl(loginConfig.getLoginUrl());

            LOGGER.trace("Creating SkyvvaIntegrationService...");
            this.skyvvaIntegrationService = new SkyvvaIntegrationService(salesforceCredential, this.endpointConfig.getApiVersion());
        } catch (Throwable ex) {
            String errorMsg = "Couldn't create SkyvvaIntegrationServiceAdapter: " + ExceptionUtils.getMessage(ex);
            LOGGER.error(errorMsg, ex);
            throw new RuntimeException(errorMsg, ex);
        }
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        String mplId = (String) exchange.getIn().getHeader("SAP_MessageProcessingLogID");
        LOGGER.trace("Processing MPL {}, mode: {}, integration: {}, interface: {}",
            mplId,
            integrationMode,
            endpointConfig.getSalesforceIntegration(),
            endpointConfig.getSalesforceInterface()
        );
        try {
            SkyvvaIntegrationService.Mode mode;
            switch (integrationMode) {
                case SYNCHRONOUS:
                case ASYNCHRONOUS: {
                    mode = SkyvvaIntegrationService.Mode.NORMAL;
                    break;
                }
                default: {
                    mode = SkyvvaIntegrationService.Mode.valueOf(integrationMode.name());
                }
            }
            Object result = skyvvaIntegrationService.integrateInbound(
                endpointConfig.getSalesforceIntegration(),
                endpointConfig.getSalesforceInterface(),
                SkyvvaIntegrationService.PayloadFormat.valueOf(endpointConfig.getRequestFormat().name()),
                mode,
                exchange.getIn().getBody(InputStream.class)
            );

            byte[] resultAsByteArray;
            if (result instanceof List) {
                resultAsByteArray = OBJECT_MAPPER.writeValueAsBytes(result);
            } else {
                if (result == null) {
                    // to avoid potential null pointer
                    result = new byte[0];
                }
                resultAsByteArray = (byte[]) result;
            }

            final Message out = exchange.getMessage();
            final Message in = exchange.getIn();
            out.copyFromWithNewBody(in, null);

            out.setBody(resultAsByteArray);
            // copy headers and attachments
            out.getHeaders().putAll(exchange.getIn().getHeaders());
            LOGGER.trace("Result is saved to exchange");
            return false;
        } catch (Throwable ex) {
            String msg = format("Error occurred during processing MPL %s: %s", mplId, ex.getMessage());
            exchange.setException(new SalesforceException(msg, ex));
            return true;
        } finally {
            // notify callback that exchange is done
            callback.done(false);
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        LOGGER.trace();
        if (integrationMode.equals(SkyvvaIntegrationMode.SALESFORCE_BULK)) {
            CheckSFProcessingJob.start();
        }
    }

    @Override
    public void doStop() throws Exception {
        LOGGER.trace();
        try {
            if (CheckSFProcessingJob.isStarted()) {
                CheckSFProcessingJob.stopGracefully(30000);
            }
        } catch (Throwable ex) {
            LOGGER.error("Couldn't stop SkyvvaIntegrationServiceAdapter gracefully: " + ex.getMessage(), ex);
        }
        super.doStop();
    }
}
