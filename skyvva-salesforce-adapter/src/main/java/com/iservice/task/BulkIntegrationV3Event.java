package com.iservice.task;

import java.util.ArrayList;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;

import com.iservice.bulk.BulkTransformationHandlerV3;
import com.iservice.model.IMessageTree;

public class BulkIntegrationV3Event extends AbstractIntegrationEvent{

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(BulkIntegrationV3Event.class);
	protected int nbProcessed=0;
	protected BulkTransformationHandlerV3 transform;

	public BulkIntegrationV3Event(long totalRecord, MapIntegrationInfo intInfo) {
		super(totalRecord, intInfo);
	}

	@Override
	public boolean doIntegration(boolean checkPackage) throws Exception {
		if(checkPackage){
			if(numberOfTree==mapIntegrationInfo.getBulkPackageSize()){
				//sent info back to caller
				preProcess+=numberOfTree;
				onInProgressProcess(preProcess);
				getTransform().createBulkBasketV3(iMessage, false);
				afterPushedData();
			}
		}else{
			//sent info back to caller
			preProcess+=numberOfTree;
			onInProgressProcess(preProcess);
			getTransform().createBulkBasketV3(iMessage, true);
			afterPushedData();
		}
		return true;
	}
	
	private void afterPushedData() {
		nbProcessed += numberOfTree;
		LOGGER.trace(">doInBoundIntegrationV3() >InterfaceName: " + mapIntegrationInfo.getInterfaceName() + " > " + nbProcessed + " records have been created in the basket.");					
		//reset package
		iMessage = new ArrayList<IMessageTree>();
		numberOfTree=0;
	}
	
	public void done(){
			
			try {
				if(!isError()){
					//close the job-->start process job
					getTransform().closeJob();
				}else{
					//delete job when have an error during integration
					getTransform().cancelJob();
				}
			} catch (Exception e) {
				LOGGER.error(e.getMessage(),e);
			}
			
	}
	
	protected BulkTransformationHandlerV3 getTransform(){
		if(transform ==null){
			
			transform = new BulkTransformationHandlerV3(mapIntegrationInfo);
			transform.setPackageSize(mapIntegrationInfo.getBulkPackageSize());
			
		}
		return transform;
	}

}
