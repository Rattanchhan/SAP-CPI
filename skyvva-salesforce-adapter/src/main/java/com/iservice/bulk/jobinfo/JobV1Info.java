package com.iservice.bulk.jobinfo;

import java.util.Calendar;

import com.sforce.async.ConcurrencyMode;
import com.sforce.async.ContentType;
import com.sforce.async.JobInfo;
import com.sforce.async.JobStateEnum;
import com.sforce.async.OperationEnum;

public class JobV1Info extends JobInfo implements IJobInfo {
	protected JobInfo currentJob = null;
	protected String currentBatchId;
	public JobV1Info(){
		
	}
	public JobV1Info(JobInfo job){
		this.currentJob = job;
	}
	@Override
	public String getId() {
		if(currentJob!=null){
			return currentJob.getId();
		}
		return super.getId();
	}
	@Override
	public void setId(String id) {
		if(currentJob!=null){
			currentJob.setId(id);
		}
		super.setId(id);
	}
	@Override
	public OperationEnum getOperation() {
		if(currentJob!=null){
			return currentJob.getOperation();
		}
		return super.getOperation();
	}
	@Override
	public void setOperation(OperationEnum operation) {
		if(currentJob!=null){
			currentJob.setOperation(operation);
		}
		super.setOperation(operation);
	}
	@Override
	public String getObject() {
		if(currentJob!=null){
			return currentJob.getObject();
		}
		return super.getObject();
	}
	@Override
	public void setObject(String object) {
		if(currentJob!=null){
			currentJob.setObject(object);
		}
		super.setObject(object);
	}
	@Override
	public String getCreatedById() {
		if(currentJob!=null){
			return currentJob.getCreatedById();
		}
		return super.getCreatedById();
	}
	@Override
	public void setCreatedById(String createdById) {
		if(currentJob!=null){
			currentJob.setCreatedById(createdById);
		}
		super.setCreatedById(createdById);
	}
	@Override
	public Calendar getCreatedDate() {
		if(currentJob!=null){
			return currentJob.getCreatedDate();
		}
		return super.getCreatedDate();
	}
	@Override
	public void setCreatedDate(Calendar createdDate) {
		if(currentJob!=null){
			currentJob.setCreatedDate(createdDate);
		}
		super.setCreatedDate(createdDate);
	}
	@Override
	public Calendar getSystemModstamp() {
		if(currentJob!=null){
			return currentJob.getSystemModstamp();
		}
		return super.getSystemModstamp();
	}
	@Override
	public void setSystemModstamp(Calendar systemModstamp) {
		if(currentJob!=null){
			currentJob.setSystemModstamp(systemModstamp);
		}
		super.setSystemModstamp(systemModstamp);
	}
	@Override
	public JobStateEnum getState() {
		if(currentJob!=null){
			return currentJob.getState();
		}
		return super.getState();
	}
	@Override
	public void setState(JobStateEnum state) {
		if(currentJob!=null){
			currentJob.setState(state);
		}
		super.setState(state);
	}
	@Override
	public String getExternalIdFieldName() {
		if(currentJob!=null){
			return currentJob.getExternalIdFieldName();
		}
		return super.getExternalIdFieldName();
	}
	@Override
	public void setExternalIdFieldName(String externalIdFieldName) {
		if(currentJob!=null){
			currentJob.setExternalIdFieldName(externalIdFieldName);
		}
		super.setExternalIdFieldName(externalIdFieldName);
	}
	@Override
	public ConcurrencyMode getConcurrencyMode() {
		if(currentJob!=null){
			return currentJob.getConcurrencyMode();
		}
		return super.getConcurrencyMode();
	}
	@Override
	public void setConcurrencyMode(ConcurrencyMode concurrencyMode) {
		if(currentJob!=null){
			currentJob.setConcurrencyMode(concurrencyMode);
		}
		super.setConcurrencyMode(concurrencyMode);
	}
	@Override
	public boolean getFastPathEnabled() {
		if(currentJob!=null){
			return this.currentJob.getFastPathEnabled();
		}
		return super.getFastPathEnabled();
	}
	@Override
	public void setFastPathEnabled(boolean fastPathEnabled) {
		if(currentJob!=null){
			this.currentJob.setFastPathEnabled(fastPathEnabled);
		}
		super.setFastPathEnabled(fastPathEnabled);
	}
	@Override
	public int getNumberBatchesQueued() {
		if(currentJob!=null){
			return this.currentJob.getNumberBatchesQueued();
		}
		return super.getNumberBatchesQueued();
	}
	@Override
	public void setNumberBatchesQueued(int numberBatchesQueued) {
		if(currentJob!=null){
			this.currentJob.setNumberBatchesQueued(numberBatchesQueued);
		}
		super.setNumberBatchesQueued(numberBatchesQueued);
	}
	@Override
	public int getNumberBatchesInProgress() {
		if(currentJob!=null){
			return this.currentJob.getNumberBatchesInProgress();
		}
		return super.getNumberBatchesInProgress();
	}
	@Override
	public void setNumberBatchesInProgress(int numberBatchesInProgress) {
		if(currentJob!=null){
			currentJob.setNumberBatchesInProgress(numberBatchesInProgress);
		}
		super.setNumberBatchesInProgress(numberBatchesInProgress);
	}
	@Override
	public int getNumberBatchesCompleted() {
		if(currentJob!=null){
			return currentJob.getNumberBatchesCompleted();
		}
		return super.getNumberBatchesCompleted();
	}
	@Override
	public void setNumberBatchesCompleted(int numberBatchesCompleted) {
		if(currentJob!=null){
			this.currentJob.setNumberBatchesCompleted(numberBatchesCompleted);
		}
		super.setNumberBatchesCompleted(numberBatchesCompleted);
	}
	@Override
	public int getNumberBatchesFailed() {
		if(currentJob!=null){
			return currentJob.getNumberBatchesFailed();
		}
		return super.getNumberBatchesFailed();
	}
	@Override
	public void setNumberBatchesFailed(int numberBatchesFailed) {
		if(currentJob!=null){
			this.currentJob.setNumberBatchesFailed(numberBatchesFailed);
		}
		super.setNumberBatchesFailed(numberBatchesFailed);
	}
	@Override
	public int getNumberBatchesTotal() {
		if(currentJob!=null){
			return this.currentJob.getNumberBatchesTotal();
		}
		return super.getNumberBatchesTotal();
	}
	@Override
	public void setNumberBatchesTotal(int numberBatchesTotal) {
		if(currentJob!=null){
			this.currentJob.setNumberBatchesTotal(numberBatchesTotal);
		}
		super.setNumberBatchesTotal(numberBatchesTotal);
	}
	@Override
	public int getNumberRecordsProcessed() {
		if(currentJob!=null){
			return this.currentJob.getNumberRecordsProcessed();
		}
		return super.getNumberRecordsProcessed();
	}
	@Override
	public void setNumberRecordsProcessed(int numberRecordsProcessed) {
		if(currentJob!=null){
			this.currentJob.setNumberRecordsProcessed(numberRecordsProcessed);
		}
		super.setNumberRecordsProcessed(numberRecordsProcessed);
	}
	@Override
	public int getNumberRetries() {
		if(currentJob!=null){
			return this.currentJob.getNumberRetries();
		}
		return super.getNumberRetries();
	}
	@Override
	public void setNumberRetries(int numberRetries) {
		if(currentJob!=null){
			this.currentJob.setNumberRetries(numberRetries);
		}
		super.setNumberRetries(numberRetries);
	}
	@Override
	public ContentType getContentType() {
		if(currentJob!=null){
			return this.currentJob.getContentType();
		}
		return super.getContentType();
	}
	@Override
	public void setContentType(ContentType contentType) {
		if(currentJob!=null){
			this.currentJob.setContentType(contentType);
		}
		super.setContentType(contentType);
	}
	@Override
	public double getApiVersion() {
		if(currentJob!=null){
			return this.currentJob.getApiVersion();
		}
		return super.getApiVersion();
	}
	@Override
	public void setApiVersion(double apiVersion) {
		if(currentJob!=null){
			this.currentJob.setApiVersion(apiVersion);
		}
		super.setApiVersion(apiVersion);
	}
	@Override
	public String getAssignmentRuleId() {
		if(currentJob!=null){
			return this.currentJob.getAssignmentRuleId();
		}
		return super.getAssignmentRuleId();
	}
	@Override
	public void setAssignmentRuleId(String assignmentRuleId) {
		if(currentJob!=null){
			this.currentJob.setAssignmentRuleId(assignmentRuleId);
		}
		super.setAssignmentRuleId(assignmentRuleId);
	}
	@Override
	public int getNumberRecordsFailed() {
		if(currentJob!=null){
			return this.currentJob.getNumberRecordsFailed();
		}
		return super.getNumberRecordsFailed();
	}
	@Override
	public void setNumberRecordsFailed(int numberRecordsFailed) {
		if(currentJob!=null){
			this.currentJob.setNumberRecordsFailed(numberRecordsFailed);
		}
		super.setNumberRecordsFailed(numberRecordsFailed);
	}
	@Override
	public long getTotalProcessingTime() {
		if(currentJob!=null){
			return this.currentJob.getTotalProcessingTime();
		}
		return super.getTotalProcessingTime();
	}
	@Override
	public void setTotalProcessingTime(long totalProcessingTime) {
		if(currentJob!=null){
			this.currentJob.setTotalProcessingTime(totalProcessingTime);
		}
		super.setTotalProcessingTime(totalProcessingTime);
	}
	@Override
	public long getApiActiveProcessingTime() {
		if(currentJob!=null){
			return this.currentJob.getApiActiveProcessingTime();
		}
		return super.getApiActiveProcessingTime();
	}
	@Override
	public void setApiActiveProcessingTime(long apiActiveProcessingTime) {
		if(currentJob!=null){
			this.currentJob.setApiActiveProcessingTime(apiActiveProcessingTime);
		}
		super.setApiActiveProcessingTime(apiActiveProcessingTime);
	}
	@Override
	public long getApexProcessingTime() {
		if(currentJob!=null){
			return currentJob.getApexProcessingTime();
		}
		return super.getApexProcessingTime();
	}
	@Override
	public void setApexProcessingTime(long apexProcessingTime) {
		if(currentJob!=null){
			this.currentJob.setApexProcessingTime(apexProcessingTime);
		}
		super.setApexProcessingTime(apexProcessingTime);
	}
	
	@Override
	public String toString() {
		if(currentJob!=null){
			return this.currentJob.toString();
		}
		return super.toString();
	}
	public String getCurrentBatchId() {
		return currentBatchId;
	}
	public void setCurrentBatchId(String currentBatchId) {
		this.currentBatchId = currentBatchId;
	}
	@Override
	public String getJobStatus() {
		if(getState()!=null)
			return this.getState().name();
		return null;
	}
	@Override
	public boolean isClose() {
		
		return this.getState()==JobStateEnum.Closed;
	}
	@Override
	public boolean isFinish() {
		
		if( this.getState()==JobStateEnum.Closed){
			//but we need to check total batch and batch completed
			return this.getNumberBatchesTotal()<=(this.getNumberBatchesCompleted()+this.getNumberBatchesFailed());
		}
		
		return false;
	}
	@Override
	public boolean isAbort() {
		return this.getState()==JobStateEnum.Aborted;
	}
	@Override
	public boolean isFailed() {
		// TODO Auto-generated method stub
		return this.getState()==JobStateEnum.Failed;
	}
	@Override
	public boolean isOpen() {
		// TODO Auto-generated method stub
		return this.getState()==JobStateEnum.Open;
	}
	@Override
	public boolean isInprogress() {
		// TODO Auto-generated method stub
		return this.getState()==JobStateEnum.InProgress;
	}
	
	@Override
	public String getSObject() {
		return getObject();
	}

}
