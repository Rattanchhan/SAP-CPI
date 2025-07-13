package com.iservice.task;

import java.util.ArrayList;
import java.util.List;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;

import com.iservice.model.IMessageTree;
import com.iservice.sforce.ISFService;
import com.sforce.soap.partner.fault.ExceptionCode;
import org.apache.commons.lang3.exception.ExceptionUtils;

import static java.lang.String.format;

public class IntegrationV3Event extends AbstractIntegrationEvent {
	
	protected int nbProcessed=0;
    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(IntegrationV3Event.class);
	
	public IntegrationV3Event(long totalRecord, MapIntegrationInfo intInfo) throws Exception {
		super(totalRecord, intInfo);
	}

	@Override
	public boolean doIntegration(boolean checkPackage) throws Exception {
		if(!iMessage.isEmpty()){
			if(!checkPackage || numberOfTree>=mapIntegrationInfo.getPackageSize()){
			// sent info back to caller
			preProcess+=numberOfTree;
			onInProgressProcess(preProcess);
			
			// send the data to SF : integrate
			integrate(iMessage, numberOfTree, getInterfaceMode());	
			}
		}
		return true;
	}
	
	public void integrate(List<IMessageTree> msgTrees, int numOfTree, String mode) throws Exception {
		ISFService sfService = mapIntegrationInfo.getSFIntegrationService().getISFService();
		sfService.setMapIntgInfo(mapIntegrationInfo);
		try {
			// first try
			sfService.integrateV3Inbound(iMessage, numberOfTree, mode);
			nbProcessed+=numberOfTree;
			iMessage = new ArrayList<IMessageTree>();
			numberOfTree=0;
			
			LOGGER.trace(mode+" mode >InterfaceName: " + mapIntegrationInfo.getInterfaceName() + " > " + nbProcessed + " records have been integrated.");
		} catch (Exception ex) {
            LOGGER.error(format("Error occurred during execution of 'integrate' for interface %s: %s",
                mapIntegrationInfo.getInterfaceName(),
                ExceptionUtils.getMessage(ex)
            ));
			
			// second try
			if (ex.getMessage().contains(ExceptionCode.INVALID_SESSION_ID.toString())
					|| ex.getMessage().toLowerCase().contains("connection timed out")) {

				// 04082010
				// session Expired, login again
				LOGGER.trace("interface:" + mapIntegrationInfo.getInterfaceName() + ">Re Login");
				sfService.setSessionId(null);
				sfService.login();

				// integrate again
				LOGGER.trace("interface:" + mapIntegrationInfo.getInterfaceName() + ">Integrate again");
				sfService.integrateV3Inbound(iMessage, numberOfTree, mode);
			}
			
			// 13102010
			// case Read timed out or Your request was running for too long,
			// and has been stopped., retry
			else if (ex.getMessage().toLowerCase().contains("read timed out")
					|| ex.getMessage().toLowerCase().contains("your request was running for too long, and has been stopped.")
					//30122010
					|| ex.getMessage().toLowerCase().contains("connection was cancelled here")
            ) {
				// integrate again
				LOGGER.trace(">IntegrationThread>interface:"+ mapIntegrationInfo.getInterfaceName() + ">Integrate again");
				sfService.integrateV3Inbound(iMessage, numberOfTree, mode);
			} else {
                throw ex;
            }
		}
	}
	
	private String getInterfaceMode() {
		if(mapIntegrationInfo!=null && mapIntegrationInfo.getInterfaces()!=null) {
			if(mapIntegrationInfo.isBatchMode()) return "Batch";
			else return mapIntegrationInfo.getInterfaces().getInterface_Mode__c();
		}
		return "";
	}

}
