package com.iservice.task;

import com.iservice.bulk.BulkV2Connection;
import com.iservice.bulk.jobinfo.JobV2Info;
import com.iservice.gui.data.Interfaces__c;
import com.sforce.async.ConcurrencyMode;
import com.sforce.soap.SFConnectorConfig;

public class BulkDirectObjectV2Integration extends AbstractBulkDirectObjectEvent<JobV2Info> {


	public static final String CRLF = "CRLF";



	private static final int MAX_SIZE_IN_MB = 90;
	
	
	
	protected BulkV2Connection connection;
	public BulkDirectObjectV2Integration(long totalRecord,
			MapIntegrationInfo intInfo) {
		super(totalRecord, intInfo, TBulkVersion.SFV2);
		
	}
	
	
	protected BulkV2Connection getBulkConn() throws Exception{
		if(connection==null){
			connection= new BulkV2Connection(mapIntegrationInfo.getSalesforceConfiguration());
		}
		return connection;
	}
	
	protected SFConnectorConfig getSfConfig() throws Exception{
		return mapIntegrationInfo.getSalesforceConfiguration();
	}
	
	protected void reLoginSF() throws Exception {
		super.reLoginSF();
		connection= new BulkV2Connection(mapIntegrationInfo.getSalesforceConfiguration());
	}
		
	/**
	 * abort the job when has any error during upload data
	 */
	@Override
	public void cancelJob(JobV2Info job) throws Exception{
		if(job!=null){
			
			getBulkConn().abortJob(job);
		}
		
	}
	
	@Override	
	protected void createBatch(JobV2Info job,byte[] csvContent)throws Exception{
		getBulkConn().createBatch(job, csvContent);
		//Found multipe contents for job: <7500Y000008sf1o>, please 'Close' / 'Abort' / 'Delete' the current Job 
		//then create a new Job and make sure you only do 'PUT' once on a given Job.
		//now close the job because v2 can have only one batch
		updateJobStatus(job);
		
		//reset job
		this.job=null;
		
	}
	@Override	
	protected void closeJob(JobV2Info job) throws Exception{
		getBulkConn().closeJob(job);
		
	}
	@Override
	public  JobV2Info createJob() throws Exception{
		Interfaces__c intf = mapIntegrationInfo.getInterfaces();
		//we use lineEnding=CRLF because of our writer
		return getBulkConn().createJob(intf.getSource_Name__c(), intf.getOperationType__c(), getExternalFieldId(),ConcurrencyMode.Parallel.name(), CRLF);
		
	}


	
	@Override
	protected int getMaxContentLength() {
		
		return MAX_SIZE_IN_MB;
	}
	
	
	
	
	
	
	
	
	


}
