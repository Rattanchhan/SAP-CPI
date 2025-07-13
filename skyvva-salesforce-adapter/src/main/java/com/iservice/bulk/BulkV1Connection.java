package com.iservice.bulk;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.Ostermiller.util.ExcelCSVParser;
import com.iservice.bulk.jobinfo.IJobInfo;
import com.iservice.bulk.jobinfo.JobV1Info;
import com.iservice.database.CacheSFBulkBatchFile;
import com.iservice.gui.helper.Helper;
import com.iservice.model.ProcessingJobInfo;
import com.sforce.async.AsyncApiException;
import com.sforce.async.BatchInfo;
import com.sforce.async.BatchInfoList;
import com.sforce.async.BatchStateEnum;
import com.sforce.async.BulkConnection;
import com.sforce.async.ConcurrencyMode;
import com.sforce.async.ContentType;
import com.sforce.async.OperationEnum;
import com.sforce.ws.ConnectorConfig;

public class BulkV1Connection extends BulkConnection implements IBulkConnection{

	public BulkV1Connection(ConnectorConfig config) throws AsyncApiException {
		super(config);
		
	}

	@Override
	public IJobInfo getJobInfo(String jobId) throws Exception{
		
		return new JobV1Info( super.getJobStatus(jobId));
	}

	@Override
	public void cancelJob(String jobId) throws Exception {
		IJobInfo job = getJobInfo(jobId);
		//check status first before cancel the job
		if(job.isOpen()||job.isInprogress()){
			super.abortJob(jobId);
		}
		
	}

	

	@Override
	public void closeJobById(String jobId) throws Exception {
		super.closeJob(jobId);
		
	}
	
	protected void retrieveResult(String jobId,String batchId,AbstractMessageCreator reader) throws Exception{
		reader.readResult(getBatchResultStream(jobId, batchId));
		
		
	}
	
	

	@Override
	public void createMessage(Map<String,String>sfField2CSVHeader,ProcessingJobInfo job,boolean onlyError) throws Exception {
		String jobId = job.getJobid();
		BatchInfoList lst = getBatchInfoList(jobId);
		File jobFolder = Helper.getJobFolder(job);

		for(final BatchInfo b:lst.getBatchInfo()){
			if(b.getState()==BatchStateEnum.NotProcessed){
				File reprocessFolder = Helper.getReprocessFolder(job);
				FileUtils.moveFile(new File(jobFolder,b.getId()+".csv"), new File(reprocessFolder,b.getId()+".csv"));					
			}else{		
				if(b.getNumberRecordsFailed()==0 && onlyError){
					//there is no error message to create
					continue;
				}
				retrieveResult(jobId, b.getId(), new BulkV1MessageCreator(b.getId(),onlyError, sfField2CSVHeader, job));
			}
		}		
	}
	
	public static class BulkV1MessageCreator extends AbstractMessageCreator{
		protected String batchId;
		
		public BulkV1MessageCreator(String batchId,boolean onlyError,Map<String, String> sfField2CSVHeader,
				ProcessingJobInfo jobInfo) {
			super(onlyError,sfField2CSVHeader, jobInfo);
			this.batchId = batchId;
		}

		@Override
		protected void doReadResult(ExcelCSVParser csvReader) throws Exception {
			ByteArrayInputStream is = new ByteArrayInputStream(CacheSFBulkBatchFile.getInstance().getCacheFolderJob().get(jobInfo.getJobFolder()).getCsvContent().toByteArray());
			ExcelCSVParser batchContent =null;
			try{
				batchContent = new ExcelCSVParser(new InputStreamReader(is,"UTF-8"));
				//lines of result and batch are match
				String[] resultHeader = csvReader.getLine();
				String[] batchHeader = batchContent.getLine();
				String[] resultRow = csvReader.getLine();
				while(resultRow!=null){
					//next row of the batch match to the result
					String[] batchRow = batchContent.getLine();
					try{
						Map<String,String> result = new HashMap<String, String>(0);
						for(int i=0;i<resultHeader.length;i++){
							result.put(resultHeader[i], resultRow[i]);
						}
						boolean error =!Boolean.valueOf( result.get("Success"));
						if(!onlyError||error){
							Map<String, String> datas = new HashMap<String, String>();
							String externalVal =  "";
							// for skyvva mapping we create data with only source field to msg
							if(this.sfField2CSVHeader!=null && this.sfField2CSVHeader.size()>0) {
								for (int i = 0; i < batchHeader.length; i++) {
									// set business externalId
									if(StringUtils.equals(this.jobInfo.getExternalFieldId(), batchHeader[i])) {
										externalVal = batchRow[i];
									}
									String sourceField = this.sfField2CSVHeader.get(batchHeader[i]);
									if(StringUtils.isNotEmpty(sourceField)) {
										datas.put(sourceField, batchRow[i]);
									}
								}
							}else { // external mapping we create data with target field
								for(int i=0;i<batchHeader.length;i++){
									datas.put(batchHeader[i], batchRow[i]);
								}
							}
							//set status
							TMessageStatus status = TMessageStatus.Completed;
							if(error){
							 status = TMessageStatus.Failed;
							}
							
							//set comment
							String comment = result.get("Error");
							
							if(StringUtils.isBlank(comment) && result.get("Created")!=null) {
								if(Boolean.valueOf(result.get("Created")) == true) {
//									comment = "Creation of "+ this.jobInfo.getSObject();
								}else if(Boolean.valueOf(result.get("Created")) == false) {
//									comment = "Modification of "+ this.jobInfo.getSObject();
								}
							}
							externalVal = datas.get(this.jobInfo.getExternalFieldId())!=null?datas.get(this.jobInfo.getExternalFieldId()): externalVal;
							writeMessage(datas, status, comment,result.get("Id"),externalVal);
							
							
						}
					}finally{
						//next row of the result
						resultRow = csvReader.getLine();
					}
				}
			}finally{
				if(is!=null){
					is.close();
				}
				if(batchContent!=null){
					batchContent.close();
				}
			}
			
		
			
		
			
		}
		
	}
	protected ConcurrencyMode getConcurrencyMode(String mode){
		
		if(StringUtils.isNotBlank(mode)){
			try{
				return ConcurrencyMode.valueOf(mode);
			}catch(Exception e){
				//nothing to do
			}
		}
		//default mode is paralel
		return ConcurrencyMode.Parallel;
	}
	@Override
	public IJobInfo createJob(String object, String operation,
			String externalId,String concurrencyMode, String lineEnding) throws Exception {
		JobV1Info job = new JobV1Info();
		
		    job.setObject(object);
		    OperationEnum type = OperationEnum.valueOf(operation);
		    job.setOperation(type);
		    if(type==OperationEnum.upsert){
		    	job.setExternalIdFieldName(externalId);
		    }
		    job.setConcurrencyMode(getConcurrencyMode(concurrencyMode));
		    job.setContentType(ContentType.CSV);
		    return new JobV1Info(createJob(job));
		
	}

}
