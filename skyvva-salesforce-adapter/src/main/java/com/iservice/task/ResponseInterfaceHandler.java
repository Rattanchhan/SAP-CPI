package com.iservice.task;


import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;


public abstract class ResponseInterfaceHandler {
	
	MapIntegrationInfo mapIntegrationInfo;
	protected boolean hasRecord = false;
    protected static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(ResponseInterfaceHandler.class);
	
	public ResponseInterfaceHandler(MapIntegrationInfo mapIntegrationInfo) throws Exception {
		this.mapIntegrationInfo = mapIntegrationInfo;
	}
	
	public abstract void process() throws Exception;
	public abstract boolean hasRecordData();
}
