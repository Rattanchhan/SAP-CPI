package com.iservice.task;

import java.util.ArrayList;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;

import com.iservice.sforce.ISFService;
import com.sforce.soap.partner.fault.ExceptionCode;
import com.sforce.soap.schemas._class.IServices.IBean;
import com.sforce.soap.schemas._class.IServices.IIntegration;

public class NormalIntegrationEvent extends AbstractIntegrationEvent {
	protected int nbProcessed;
	private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(NormalIntegrationEvent.class);

	public NormalIntegrationEvent(long totalRecord,MapIntegrationInfo mapIntgInfo){
		super(totalRecord,mapIntgInfo);
	}

	protected IIntegration createIntegration() {
		IIntegration iIntegration = new IIntegration();
		iIntegration.setFromSystem(mapIntegrationInfo.getIntegrationId());
		iIntegration.setTargetObject(mapIntegrationInfo.getInterfaceName());
		return iIntegration;
	}

	@Override
	public boolean doIntegration(boolean checkPackage)throws Exception{
		
//		if(!currentPackage.isEmpty()){
//			if(!checkPackage || currentPackage.size()>=mapIntegrationInfo.getPackageSize()){
//			
//			IIntegration itg = createIntegration();
//			itg.setRecords(mapper.map(currentPackage));
//			
//			// sent info back to caller
//			preProcess+=currentPackage.size();
//			onInProgressProcess(preProcess);
//			
//			// send the data to SF : integrate
//			integrate(itg);	
//			}
//		}
		return true;
	}
	
	public void integrate(IIntegration iInter) throws Exception {
		ISFService sfService = mapIntegrationInfo.getSFIntegrationService().getISFService();
		try {

			// 14092010
			String status = ((nbProcessed + iInter.getRecords().length) == totalRecord ? "Completed" : "In Progress");

			// mark that it is from Agent to SF
			// system,run time,total record,status
			iInter.setMappingName((org.apache.commons.lang3.StringUtils.isEmpty(iInter.getMappingName())?"":iInter.getMappingName())+"AGENT-SF," + mapIntegrationInfo.getRunTime().getTimeInMillis() + "," + totalRecord + "," + status);

			// first try
			sfService.integrate(iInter);
			nbProcessed += iInter.getRecords().length;
			
			//clear old record
			currentPackage=new ArrayList<>();
			LOGGER.trace(">doInBoundIntegration() >doIntegration() >InterfaceName: " + mapIntegrationInfo.getInterfaceName() + " > " + nbProcessed + " records have been integrated.");
			
		} catch (Exception ex) {
			if(isForceStop()) throw ex;
			// second try
			if (ex.getMessage().contains(ExceptionCode.INVALID_SESSION_ID.toString())
					|| ex.getMessage().toLowerCase().contains("connection timed out")) {

				// 04082010
				// session Expired, login again
				LOGGER.trace(">IntegrationThread>interface:" + iInter.getTargetObject() + ">Re Login");
				sfService.setSessionId(null);
				sfService.login();

				// integrate again
				LOGGER.trace(">IntegrationThread>interface:" + iInter.getTargetObject() + ">Integrate again");
				sfService.integrate(iInter);
			}
			
			// 13102010
			// case Read timed out or Your request was running for too long,
			// and has been stopped., retry
			else if (ex.getMessage().toLowerCase().contains("read timed out")
					|| ex.getMessage().toLowerCase().contains("your request was running for too long, and has been stopped.")
					//30122010
					|| ex.getMessage().toLowerCase().contains("connection was cancelled here")){
				
				// integrate again
				LOGGER.trace(">IntegrationThread>interface:"+ iInter.getTargetObject() + ">Integrate again");
				sfService.integrate(iInter);
			}
			//20072016
			//lock row and cpu limit
			else if(ex.getMessage().contains("System.LimitException") || ex.getMessage().contains("UNABLE_TO_LOCK_ROW")){
				LOGGER.trace(">IntegrationThread>interface:" + iInter.getTargetObject() + ">Integrate again by split packet size by 2");
				int totalRec = iInter.getRecords().length;
				int size = totalRec/2;
				int subPacket = ((size*2 != totalRec)?++size:size);
				if(subPacket < 1)throw ex;
				
				int numField = iInter.getRecords()[0].length;
				IBean[][] records = new IBean[subPacket][numField];
				IBean[][] recordsTmp = iInter.getRecords();
				int rowIndex = 0;
				for(int i=0;i<recordsTmp.length;i++){
					for(int j=0;j<numField;j++){
						records[rowIndex][j]=recordsTmp[i][j];
					}
					rowIndex++;
					
					iInter.setRecords(records);
					if(rowIndex == subPacket){
						integrate(iInter);
						records = new IBean[totalRec-subPacket][numField];
						rowIndex = 0;
					}
				}
				if(rowIndex > 0){
					integrate(iInter);
				}
				//fixed issue on Bug #6842
				//nbProcessed -= iInter.getRecords().length;
			}
			// other exception, re throw
			else
				throw ex;
		}
		finally {
			// 14092010
			//nbProcessed += iInter.getRecords().length;
		}

	}
}
