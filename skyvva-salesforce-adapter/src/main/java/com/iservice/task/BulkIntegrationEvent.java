package com.iservice.task;

import java.util.ArrayList;
import java.util.List;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;

import com.iservice.bulk.BulkTransformationHandler;
import com.sforce.soap.schemas._class.IServices.IBean;

public class BulkIntegrationEvent extends AbstractIntegrationEvent {
    protected static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(BulkIntegrationEvent.class);
	protected BulkTransformationHandler transform;
	
	public BulkIntegrationEvent(long totalRecord,MapIntegrationInfo intInfo) {
		super(totalRecord,intInfo);
		
	}
	
	@Override
	public boolean doIntegration(boolean checkPackage)throws Exception {
		if(checkPackage){
			if(super.currentPackage.size()==mapIntegrationInfo.getBulkPackageSize()){
				//sent info back to caller
				preProcess+=currentPackage.size();
				onInProgressProcess(preProcess);
				
				getTransform().createBachs(currentPackage,false);
				
				//reset package
				currentPackage = new ArrayList<List<IBean>>();
			}
		}else{
			
			//sent info back to caller
			preProcess+=currentPackage.size();
			onInProgressProcess(preProcess);
			getTransform().createBachs(currentPackage,true);
			
			//reset package
			currentPackage = new ArrayList<List<IBean>>();
			
			
		}
		
		return true;
		
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
		
	};

	protected BulkTransformationHandler getTransform(){
		if(transform ==null){
			
			transform = new BulkTransformationHandler(mapIntegrationInfo);
			transform.setPackageSize(mapIntegrationInfo.getBulkPackageSize());
			
		}
		return transform;
	}
	
	
	

	
	

}
