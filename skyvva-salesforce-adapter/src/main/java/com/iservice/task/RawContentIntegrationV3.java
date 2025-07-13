package com.iservice.task;

import java.util.ArrayList;
import java.util.List;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;

import com.iservice.sforce.ISFService;
import com.iservice.sforce.SFService;
import com.sforce.soap.partner.fault.ExceptionCode;
import com.sforce.soap.schemas._class.IServices.IBean;

public class RawContentIntegrationV3 extends AbstractIntegrationEvent{
	protected int nbProcessed=0;
    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(RawContentIntegrationV3.class);

	public RawContentIntegrationV3(long totalRecord, MapIntegrationInfo intInfo) {
		super(totalRecord, intInfo);
	}

	@Override
	public boolean doIntegration(boolean checkPackage) throws Exception {
		if(checkPackage) {
			// sent info back to caller
			preProcess+=currentPackage.size();
			onInProgressProcess(preProcess);
			
			// send the data to SF : integrate
			return integrate(currentPackage.get(0));	
		}
		return true;
	}
	
	public boolean integrate(List<IBean> packages) throws Exception {
		ISFService sfService = mapIntegrationInfo.getSFIntegrationService().getISFService();
		sfService.setMapIntgInfo(mapIntegrationInfo);
		try {
			// first try
			((SFService) sfService).integrateV3Inbound_RawContent(packages.get(4).getValue());
			nbProcessed+=currentPackage.size();
			currentPackage=new ArrayList<>();
			LOGGER.trace(">doInBoundIntegrationV3 for Raw Content >InterfaceName: " + mapIntegrationInfo.getInterfaceName() + " > " + nbProcessed + " files have been integrated.");		
		} catch (Exception ex) {
			
			// second try
			if (ex.getMessage().contains(ExceptionCode.INVALID_SESSION_ID.toString())
					|| ex.getMessage().toLowerCase().contains("connection timed out")) {

				// 04082010
				// session Expired, login again
				LOGGER.trace(">IntegrationThread>interface:" + mapIntegrationInfo.getInterfaceName() + ">Re Login");
				sfService.setSessionId(null);
				sfService.login();

				// integrate again
				LOGGER.trace(">IntegrationThread>interface:" + mapIntegrationInfo.getInterfaceName() + ">Integrate again");
				((SFService) sfService).integrateV3Inbound_RawContent(packages.get(4).getValue());
			}
			
			// 13102010
			// case Read timed out or Your request was running for too long,
			// and has been stopped., retry
			else if (ex.getMessage().toLowerCase().contains("read timed out")
					|| ex.getMessage().toLowerCase().contains("your request was running for too long, and has been stopped.")
					//30122010
					|| ex.getMessage().toLowerCase().contains("connection was cancelled here")){
				
				// integrate again
				LOGGER.trace(">IntegrationThread>interface:"+ mapIntegrationInfo.getInterfaceName() + ">Integrate again");
				((SFService) sfService).integrateV3Inbound_RawContent(packages.get(4).getValue());
			}
			
			//lock row and cpu limit not yet implement
//			else if(ex.getMessage().contains("System.LimitException") || ex.getMessage().contains("UNABLE_TO_LOCK_ROW")){
//				LOGGER.trace(">IntegrationThread>interface:" + iInter.getTargetObject() + ">Integrate again by split packet size by 2");
//				int totalRec = iInter.getRecords().length;
//				int size = totalRec/2;
//				int subPacket = ((size*2 != totalRec)?++size:size);
//				if(subPacket < 1)throw ex;
//				
//				int numField = iInter.getRecords()[0].length;
//				IBean[][] records = new IBean[subPacket][numField];
//				IBean[][] recordsTmp = iInter.getRecords();
//				int rowIndex = 0;
//				for(int i=0;i<recordsTmp.length;i++){
//					for(int j=0;j<numField;j++){
//						records[rowIndex][j]=recordsTmp[i][j];
//					}
//					rowIndex++;
//					
//					iInter.setRecords(records);
//					if(rowIndex == subPacket){
//						integrate(iInter);
//						records = new IBean[totalRec-subPacket][numField];
//						rowIndex = 0;
//					}
//				}
//				if(rowIndex > 0){
//					integrate(iInter);
//				}
//				//fixed issue on Bug #6842
//				//nbProcessed -= iInter.getRecords().length;
//			}
			// other exception, re throw
			else
				throw ex;
		}
		finally {
			
		}
		return true;
	}

}
