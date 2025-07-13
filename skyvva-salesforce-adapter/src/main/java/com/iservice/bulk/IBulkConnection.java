package com.iservice.bulk;

import java.util.Map;

import com.iservice.bulk.jobinfo.IJobInfo;
import com.iservice.model.ProcessingJobInfo;
import com.sforce.ws.ConnectorConfig;

public interface IBulkConnection {
	public IJobInfo createJob(String object,String operation,String externalId,String concurrencyMode,String lineEnding) throws Exception;
	public IJobInfo getJobInfo(String jobId) throws Exception;
	public void cancelJob(String jobId) throws Exception;
	public void closeJobById(String jobId) throws Exception;
	public ConnectorConfig getConfig();
	public void createMessage(Map<String,String>sfField2CSVHeader,ProcessingJobInfo jobInfo,boolean onlyError) throws Exception;
}
