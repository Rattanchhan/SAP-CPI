package com.iservice.bulk;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;

import com.Ostermiller.util.ExcelCSVParser;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.sforce.ISFService;
import com.iservice.sforce.MapSFConnInfo;
import com.iservice.sforce.SFIntegrationService;
import com.iservice.sforce.SFService;
import com.sforce.async.BatchInfo;
import com.sforce.async.BatchStateEnum;
import com.sforce.async.BulkConnection;
import com.sforce.async.ConcurrencyMode;
import com.sforce.async.ContentType;
import com.sforce.async.JobInfo;
import com.sforce.async.JobStateEnum;
import com.sforce.async.OperationEnum;
import com.sforce.async.QueryResultList;
import com.sforce.soap.partner.fault.ApiFault;
import com.sforce.soap.schemas._class.IServices.IBean;
import com.sforce.ws.ConnectorConfig;

public class BulkQuery {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(BulkQuery.class);

	ISFService sfService;
	BulkConnection bulkConnection;
	String query;
	String sobjectType;
	JobInfo jobInfo;
	
	public BulkQuery () {}
	
	public BulkQuery(ISFService sfService, Interfaces__c intf) throws Exception {
		this.sfService = sfService;
		this.bulkConnection = getBulkConnection(sfService);
		this.query = getQuery(intf);
		this.sobjectType = getSobjectType();
		this.jobInfo = createJobInfo();
	}
	
	/**
	 * Create the BulkConnection used to call Bulk API operations.
	 */
	private BulkConnection getBulkConnection(ISFService sfService) throws Exception {
		LOGGER.trace("> BulkQuery()> getBulkConnection()> start");
		ConnectorConfig config = new ConnectorConfig();
		config.setSessionId(sfService.getSessionId());
		// The endpoint for the Bulk API service is the same as for the normal
		// SOAP uri until the /Soap/ part. From here it's '/async/versionNumber'
		String soapEndpoint = sfService.getServiceUrl();
		String apiVersion = SFService.getApiVersion(soapEndpoint);
		String restEndpoint = soapEndpoint.substring(0, soapEndpoint.indexOf("Soap/")) + "async/" + apiVersion;
		config.setRestEndpoint(restEndpoint);
		// This should only be false when doing debugging.
		config.setCompression(true);
		// Set this to true to see HTTP requests and responses on stdout
		config.setTraceMessage(false);
		BulkConnection bulkConnection = new BulkConnection(config);
		LOGGER.trace("> BulkQuery()> getBulkConnection()> done");
		return bulkConnection;
	}
	
	/**
	 * Get getSobjectType from query in Interface
	 */
	private String getSobjectType() {
		String sobjectType = "";
		Integer idxFrom = query.toLowerCase().indexOf(" from ");
		if(idxFrom>0) {
			String temp = query.substring(idxFrom+6);
			String[] arrTemp = temp.split(" ");
			sobjectType = arrTemp[0];
		}
		return sobjectType;
	}
	
	/**
	 * Get getQuery from query in Interface
	 */
	private String getQuery(Interfaces__c intf) {
		String query = intf.getQuery__c();
		query = query.replaceAll("\r", " ").replaceAll("\n", " ");
		Integer idxFrom = query.toLowerCase().indexOf(" from ");
		if(idxFrom>0) {
			query = "SELECT Id " + query.substring(idxFrom+1);
		}
		return query;
	}
	
	/**
	 * Create job used to call Bulk API operations
	 * @throws Exception 
	 */
	private JobInfo createJobInfo() throws Exception {
		LOGGER.trace("> BulkQuery()> createJobInfo()> start");
		JobInfo jobInfo = new JobInfo();
		jobInfo.setObject(sobjectType);
		jobInfo.setOperation(OperationEnum.query);
		jobInfo.setConcurrencyMode(ConcurrencyMode.Parallel);
		jobInfo.setContentType(ContentType.CSV);
		jobInfo = bulkConnection.createJob(jobInfo);
	    assert jobInfo.getId() != null;
	    jobInfo = bulkConnection.getJobStatus(jobInfo.getId());
	    LOGGER.trace("> BulkQuery()> createJobInfo()> done");
	    return jobInfo;
	}
	
	/**
	 * close job
	 * @throws Exception
	 */
	private void closeJob() throws Exception {
		LOGGER.trace("> BulkQuery()> closeJob()> start");
	    JobInfo job = new JobInfo();
	    job.setId(jobInfo.getId());
	    job.setState(JobStateEnum.Closed);
	    bulkConnection.updateJob(job);
	    LOGGER.trace("> BulkQuery()> closeJob()> done");
	}

	/**
	 * doBulkQuery get results.
	 */
	public List<List<IBean>> doBulkQuery() throws Exception {
		LOGGER.trace("> BulkQuery()> doBulkQuery()> start");
		List<List<IBean>> lstResult = new ArrayList<List<IBean>>();
	    BatchInfo batchInfo = null;
	    ByteArrayInputStream bout = new ByteArrayInputStream(query.getBytes());
	    batchInfo = bulkConnection.createBatchFromStream(jobInfo, bout);
	    
	    String[] queryResults = null;
	    
	    for(int i=0; i<10000; i++) {
	    	Thread.sleep(30000); //30 sec
	    	batchInfo = bulkConnection.getBatchInfo(jobInfo.getId(), batchInfo.getId());
	      	if (batchInfo.getState() == BatchStateEnum.Completed) {
	      		QueryResultList list =bulkConnection.getQueryResultList(jobInfo.getId(), batchInfo.getId());
	      		queryResults = list.getResult();
	      		break;
	      	}else if (batchInfo.getState() == BatchStateEnum.Failed) {
	      		System.out.println("-------------- failed ----------" + batchInfo);
	      		break;
	      	}else {
	      		System.out.println("-------------- waiting ----------" + batchInfo);
	      	}
	    }
	    
	    if(queryResults != null && queryResults.length > 0) {
	    	for (String resultId : queryResults) {
	        	InputStream input = bulkConnection.getQueryResultStream(jobInfo.getId(), batchInfo.getId(), resultId);
	        	ExcelCSVParser csvParser = new ExcelCSVParser(new InputStreamReader(input,"UTF-8"));
	        	try {
	        		//start create header
	        		List<String> resultHeader = new ArrayList<>();
	        		String[] csvHeaders = csvParser.getLine();
	        		if(csvHeaders!=null){
						for(String h:csvHeaders){
							resultHeader.add(h);
						}
					}
	    			//start create record
	    			String[] row;
	    			while((row = csvParser.getLine()) != null){
	    				List<IBean> rec = new ArrayList<IBean>();
	    				for(int i=0;i<row.length;i++){
	    					rec.add(new IBean(resultHeader.get(i), row[i]));
	    				}
	    				lstResult.add(rec);
	    			}
	    		}finally {
	    			csvParser.close();
	    			input.close();
	    			csvParser=null;
	    			input=null;
	    		}
	    	}
	    }
	    LOGGER.trace("> BulkQuery()> doBulkQuery()> done");
	    LOGGER.trace("> BulkQuery()> doBulkQuery()> lstResult: " + lstResult.size());
	    closeJob();
	    return lstResult;
	}
	
	public static void main(String[] args) throws Exception {
		
		Map<String, String> mapProps =new HashMap<String, String>();
		mapProps.put("package", "skyvvasolutions");
		mapProps.put("username", "von@test.com");
		mapProps.put("password", "skyvva123");
		mapProps.put("token", "MTFenP5WgLvKJcbgAjsOgJVnp");
		mapProps.put("serverEnvironment", "Sandbox");
		
		MapSFConnInfo mapSFConnInfo = new MapSFConnInfo(mapProps);
		BulkQuery bulkQuery = new BulkQuery();
		SFIntegrationService intService = new SFIntegrationService(mapSFConnInfo);
		try {
			intService.login();
			try {
				bulkQuery.runSample("Select Id From Account limit 10", intService.getISFService());
			}catch(Exception ex) {
				System.err.println(ex.getMessage());
				bulkQuery.closeJob();
			}
		}catch (ApiFault ex) {
			System.err.println(ex.getMessage());
		}
		
	}

	public void runSample(String query, ISFService sfService) throws Exception {
		this.bulkConnection = getBulkConnection(sfService);
		this.query = query;
		this.sobjectType = getSobjectType();
		this.jobInfo = createJobInfo();
		doBulkQuery();
	}

}