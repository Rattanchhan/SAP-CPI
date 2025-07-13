package com.iservice.bulk.jobinfo;

public interface IJobInfo {
	
	public String getId() ;
	public void setId(String id) ;
	public String getJobStatus();
	public boolean isClose();
	public boolean isAbort();
	public boolean isFinish();
	public boolean isFailed();
	public boolean isOpen();
	public boolean isInprogress();
	public int getNumberRecordsFailed();
	public String getSObject();
	public String getExternalIdFieldName();
}
