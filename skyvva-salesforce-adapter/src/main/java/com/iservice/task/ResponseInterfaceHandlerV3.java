package com.iservice.task;

import java.util.List;

import com.iservice.xmlparser.XmlNode;

public class ResponseInterfaceHandlerV3 extends ResponseInterfaceHandler{

	private List<XmlNode> lsResponseRecords;
	
	public ResponseInterfaceHandlerV3(MapIntegrationInfo mapIntegrationInfo, List<XmlNode> lsResponseRecords) throws Exception {
		super(mapIntegrationInfo);
		
		if(lsResponseRecords!=null && lsResponseRecords.size()>0) {
			this.lsResponseRecords = lsResponseRecords;
			hasRecord = true;
		}
	}

	@Override
	public void process() throws Exception {
//		logProcessing.trace(">Processing a Response-Interface >"+mapIntegrationInfo.getInterfaces().getName__c());
//	
//		IMsgGeneratorV3 imgGeneratorV3 = new XmlToImgV3(null, mapIntegrationInfo);
//		IntegrationV3Event IntegrationV3Event = new IntegrationV3Event(lsResponseRecords.size(), mapIntegrationInfo, null);
//		logProcessing.trace(">doInBoundIntegrationV3() >with Normal Mode with total: "+lsResponseRecords.size()+(lsResponseRecords.size()>1?" records": "record")+" from Agent data source to be integrated into SalesForce.");
//		
//		for(XmlNode oneNode : lsResponseRecords) {
//			List<XmlNode> lsChosenNode = new ArrayList<XmlNode>();
//			lsChosenNode.add(oneNode);
//			List<IMessageTree> msgTree =  imgGeneratorV3.generateIMessage(lsChosenNode);
//			IntegrationV3Event.processIMessagesV3(msgTree, lsChosenNode.size());
//		}
//		// do send the last package
//		IntegrationV3Event.doIntegration(false);
//		logProcessing.trace(">Processing a Response-Interface >"+ mapIntegrationInfo.getInterfaces().getName__c()+" >Done");
	}

	@Override
	public boolean hasRecordData() {
		return hasRecord;
	}

}
