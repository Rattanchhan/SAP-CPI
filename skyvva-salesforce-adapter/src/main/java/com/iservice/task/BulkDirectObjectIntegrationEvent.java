package com.iservice.task;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.iservice.bulk.BulkV1Connection;
import com.iservice.bulk.jobinfo.JobV1Info;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.utils.CSVWriterUtil;
import com.sforce.async.BatchInfo;
import com.sforce.async.BulkConnection;
import com.sforce.async.ConcurrencyMode;
import com.sforce.async.ContentType;
import com.sforce.async.OperationEnum;

public class BulkDirectObjectIntegrationEvent extends AbstractBulkDirectObjectEvent<JobV1Info> {
	
	private static final int MAX_RECORD_PER_BATCH = 9999;

	private static final int MAX_SIZE_IN_MB = 9;
	
	
	protected List<String> batchIds ;
	protected CSVWriterUtil currentCsv;
	protected List<String> sfFieldOrders;
	protected BulkConnection connection;
	public BulkDirectObjectIntegrationEvent(long totalRecord,
			MapIntegrationInfo intInfo) {
		super(totalRecord, intInfo, TBulkVersion.SFV1); 
		
	}

	protected void reLoginSF() throws Exception {
		super.reLoginSF();
		connection= new BulkV1Connection(mapIntegrationInfo.getSalesforceConfiguration());
	}
	
	protected BulkConnection getBulkConn() throws Exception{
		if(connection==null){
			connection= new BulkV1Connection(mapIntegrationInfo.getSalesforceConfiguration());
		}
		return connection;
	}
	
	@Override
	public void cancelJob(JobV1Info job) throws Exception{
		if(job!=null){			
		    getBulkConn().abortJob(this.job.getId());
		}
	}

	@Override
	protected String getBatchFileName(){
		if(job!=null && StringUtils.isNotBlank(job.getCurrentBatchId())){
			return this.job.getCurrentBatchId()+".csv";
		}
		return super.getBatchFileName();
	}
	@Override
	protected void createBatch(JobV1Info job,byte[] csvContent) throws Exception {
		if (csvContent.length>0) {
			// close the csv writer	
			BatchInfo batch=	getBulkConn().createBatchFromStream(job, new ByteArrayInputStream(csvContent));
			this.job.setCurrentBatchId(batch.getId());
		}
		
	}
	@Override
	protected void closeJob(JobV1Info job) throws Exception {
		if(this.job!=null){		  
		   getBulkConn().closeJob(this.job.getId());
		}
		
	}

	@Override
	protected JobV1Info createJob() throws Exception {
		JobV1Info job = new JobV1Info();
		Interfaces__c intf = mapIntegrationInfo.getInterfaces();
		    job.setObject(intf.getSource_Name__c());
		    OperationEnum type = getOpperation(intf.getOperationType__c());
		    job.setOperation(type);
		    if(type==OperationEnum.upsert){
		    	job.setExternalIdFieldName(getExternalFieldId());
		    }
		    job.setConcurrencyMode(getConcurrencyMode());
		    job.setContentType(ContentType.CSV);
		    return new JobV1Info(getBulkConn().createJob(job));
		    
	}
	//only bulk version 1.0 we can change concurrencyMode
	protected ConcurrencyMode getConcurrencyMode(){
		Interfaces__c intf = mapIntegrationInfo.getInterfaces();
		String processMode = intf.getBulk_Processing_Mode__c();
		if(StringUtils.isBlank(processMode)){
			//try to read process mode from  adapter
			Map<String,String> adapterProps = mapIntegrationInfo.getAdapterProperties();
			//this is mean that skyvva version is older than 2.41, then try to check bulkd process mode from adapter
			if(adapterProps!=null){
				processMode = adapterProps.get("bulk_processing_mode");
			}
		}
		if(StringUtils.isNotBlank(processMode)){
			try{
				return ConcurrencyMode.valueOf(processMode);
			}catch(Exception e){
				//nothing to do
			}
		}
		
		//default mode is paralel
		return ConcurrencyMode.Parallel;
	}
	/**
	 * convert skyvva's operation to bulk's operation
	 * @param skyvvaOperation
	 * @return bulk operation
	 */
	protected OperationEnum getOpperation(String skyvvaOperation){
		return OperationEnum.valueOf(skyvvaOperation);
	}



	@Override
	protected boolean isMaxLenght() {
		//bulk version max record per batch is 10000
		if(recordBatchCount==MAX_RECORD_PER_BATCH){
			return true;
		}
		
		return super.isMaxLenght();
	}



	@Override
	protected int getMaxContentLength() {
		
		return MAX_SIZE_IN_MB;
	}
	
	
	
	
	
	
	
	
	
}
