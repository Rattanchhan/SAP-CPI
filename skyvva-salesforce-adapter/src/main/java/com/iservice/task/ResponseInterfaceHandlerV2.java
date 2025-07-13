package com.iservice.task;

import java.util.List;
import com.sforce.soap.schemas._class.IServices.IBean;

public class ResponseInterfaceHandlerV2 extends ResponseInterfaceHandler{
	
	private List<List<IBean>> lsResponseRecords;

	public ResponseInterfaceHandlerV2(MapIntegrationInfo mapIntegrationInfo, List<List<IBean>> lsResponseRecords) throws Exception {
		super(mapIntegrationInfo);
		this.lsResponseRecords = lsResponseRecords;
		if(this.lsResponseRecords!=null && this.lsResponseRecords.size()>0) hasRecord = true;
	}

	@Override
	public void process() throws Exception {
		LOGGER.trace(">Processing a Response-Interface >"+mapIntegrationInfo.getInterfaces().getName__c());
		LOGGER.trace(">doInBoundIntegration() >with Normal Mode with total: "+lsResponseRecords.size()+(lsResponseRecords.size()>1?" records": "record")+" from Agent data source to be integrated into SalesForce.");
		new NormalIntegrationEvent(lsResponseRecords.size(), mapIntegrationInfo).processRecords(lsResponseRecords);
		LOGGER.trace(">Processing a Response-Interface >"+ mapIntegrationInfo.getInterfaces().getName__c()+" >Done");
	}
	
	public boolean hasRecordData() {
		return this.hasRecord;
	}

}
