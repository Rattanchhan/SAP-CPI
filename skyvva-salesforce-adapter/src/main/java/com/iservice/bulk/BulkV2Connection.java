package com.iservice.bulk;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.Ostermiller.util.ExcelCSVParser;
import com.Ostermiller.util.ExcelCSVPrinter;
import com.iservice.bulk.AbstractMessageCreator.TMessageStatus;
import com.iservice.bulk.jobinfo.JobV2Info;
import com.iservice.gui.helper.Helper;
import com.iservice.model.ProcessingJobInfo;
import com.sforce.async.ConcurrencyMode;
import com.sforce.async.JobStateEnum;
import com.sforce.async.OperationEnum;
import com.sforce.soap.SFConnectorConfig;
import com.sforce.soap.SalesForceHttpTransport;
import com.sforce.soap.partner.fault.ExceptionCode;
import com.sforce.soap.partner.fault.UnexpectedErrorFault;
import com.sforce.ws.ConnectorConfig;
import com.sforce.ws.util.FileUtil;

public class BulkV2Connection implements IBulkConnection {
	
		private static final JSONParser PARSER = new JSONParser();
	    private SFConnectorConfig config;
	    
	    public BulkV2Connection(SFConnectorConfig config){
	    	this.config = config;
	    }
	   
	    protected <M extends HttpRequestBase> M createBulkMethod(String targetURL, Class<M> type) throws Exception {
			M method = type.getConstructor(String.class).newInstance(targetURL);
			
			SFConnectorConfig connection = config;
			//set cookie
			method.setHeader("Authorization",	"Bearer " + connection.getSessionId());
			if(type==HttpPut.class){
				//for create batch
				method.setHeader("Content-Type", "text/csv");
			}else{
				method.setHeader("Content-Type", "application/json; charset=UTF-8");
			}
			method.setHeader("Accept", "application/json");

			return method;
		}
		
		/**
		 * aborts the job
		 * @param job
		 * @throws Exception
		 */
		public void abortJob(JobV2Info job) throws Exception{
			if(job!=null){
				//check status before abort the job
				job = getJobInfo(job.getId());
				if(job.isOpen()||job.isInprogress()){
					//we can cancel the job when it is open or inprogress
					updateJobStatus(job.getId(), JobStateEnum.Aborted);
				}
			}
			
		}
		/**
		 * Uploads data for a job using CSV data you provide.
		 * @param job
		 * @param csvContent
		 * @throws Exception
		 */
		//batch no response for bulk v2
		public void createBatch(JobV2Info job,byte[] csvContent)throws Exception{
			URL targetURL = new URL(config.getBulkV2EndPoint()+"/"+job.getId()+"/batches");
			HttpPut put = createBulkMethod(targetURL.getPath(), HttpPut.class);
			HttpHost target = new HttpHost(targetURL.getHost(),targetURL.getPort(),targetURL.getProtocol());
			HttpEntity entity =	EntityBuilder.create().setBinary(csvContent).gzipCompress().build();
			put.setEntity(entity);
			doSend(put, target);
			
		}
		
		/**
		 * close the job
		 * @param job
		 * @throws Exception
		 */
		public void closeJob(JobV2Info job) throws Exception{
			if(job!=null){
				closeJobById(job.getId());
			}
		}
		
		/**
		 * Closes or aborts a job. If you close a job, Salesforce queues the job and uploaded data for processing, and you can’t add any additional job data. If you abort a job, the job does not get queued or processed.
		 * @param job
		 * @throws Exception
		 */
		@SuppressWarnings("unchecked")
		public void updateJobStatus(String jobId,JobStateEnum status) throws Exception{
			URL targetURL = new URL(config.getBulkV2EndPoint()+"/"+jobId);
			HttpPatch method = createBulkMethod(targetURL.getPath(), HttpPatch.class);
			HttpHost target = new HttpHost(targetURL.getHost(),targetURL.getPort(),targetURL.getProtocol());
			JSONObject jsonObject = new JSONObject();		
			jsonObject.put("state", status.name());		
			//HttpEntity entity =	EntityBuilder.create().setText(jsonObject.toJSONString()).gzipCompress().build();
			method.setEntity(new StringEntity(jsonObject.toJSONString()));
			
			doSend(method, target);
		}
		
		/**
		 * Retrieves detailed information about a job.
		 * @param jobId
		 * @return JobV2Info
		 * @throws Exception
		 */
		@Override
		public JobV2Info getJobInfo(String jobId) throws Exception{
			URL targetURL = new URL(config.getBulkV2EndPoint()+"/"+jobId);
			HttpGet method = createBulkMethod(targetURL.getPath(), HttpGet.class);
			HttpHost target = new HttpHost(targetURL.getHost(),targetURL.getPort(),targetURL.getProtocol());
			return doSend(method, target);
		}
		
		/**
		 * Creates a job, which represents a bulk operation (and associated data) that is sent to Salesforce for asynchronous processing.
		 * @param object
		 * @param operation
		 * @param externalId
		 * @param lineEnding
		 * @return JobV2Info
		 * @throws Exception
		 */
		public  JobV2Info createJob(String object,String operation,String externalId,String lineEnding)throws Exception{
			return createJob(object, operation, externalId, ConcurrencyMode.Parallel.name(), lineEnding);
		}
		
		/**
		 * Creates a job, which represents a bulk operation (and associated data) that is sent to Salesforce for asynchronous processing.
		 * @param object
		 * @param operation
		 * @param externalId
		 * @param concurrencyMode not use able, because bulk v2 not support it
		 * @param lineEnding
		 * @return JobV2Info
		 * @throws Exception
		 */
		@Override
		@SuppressWarnings("unchecked")
		public  JobV2Info createJob(String object,String operation,String externalId,String concurrencyMode,String lineEnding) throws Exception{
			//HttpPost httppost = null;
			SFConnectorConfig connection = config;
			URL targetURL = new URL(connection.getBulkV2EndPoint());
			HttpPost method = createBulkMethod(targetURL.getPath(), HttpPost.class);
			HttpHost target = new HttpHost(targetURL.getHost(),targetURL.getPort(),targetURL.getProtocol());
			
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("object", object);
			if(OperationEnum.upsert.toString().equals(operation)){
				jsonObject.put("externalIdFieldName", externalId);
			}
			//bulk api v2 support only csv
			jsonObject.put("contentType", "CSV");
			//we use lineEnding=CRLF because of our writer
			jsonObject.put("lineEnding", lineEnding);
			jsonObject.put("operation", operation);
			
			//HttpEntity entity =	EntityBuilder.create().setText(jsonObject.toJSONString()).gzipCompress().build();
			method.setEntity(new StringEntity(jsonObject.toJSONString()));

			return doSend(method, target);
		}

		/**
		 * Send request to Salesforce
		 * 
		 * @param method
		 * @param target
		 * @return JobV2Info 
		 * @throws Exception
		 */
		@SuppressWarnings("unchecked")
		protected JobV2Info doSend(HttpRequestBase method, HttpHost target) throws Exception {
			
			CloseableHttpClient httpcon=null;
			try {
				httpcon = SalesForceHttpTransport.createHttpClient(method,
						config);
				HttpResponse httpresponse = SalesForceHttpTransport.execute(httpcon,target, method);
				HttpEntity r_entity = httpresponse.getEntity();
				BufferedHttpEntity b_entity = new BufferedHttpEntity(r_entity);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				b_entity.writeTo(out);
				byte[] content = out.toByteArray();
				
				if (content.length > 0) {
					Object obj = PARSER.parse(new String(content, "UTF-8"));
					if (obj instanceof JSONObject) {
							JobV2Info job = new JobV2Info();
										job.putAll((JSONObject) obj);
							return job;
					} else if (obj instanceof JSONArray || httpresponse.getStatusLine().getStatusCode() >= 400) {
						//when error the response is json array 
						JSONArray errs = (JSONArray) obj;
						StringBuilder message = new StringBuilder();
						boolean first = true;
						for(Object err:errs){
							JSONObject jerr = (JSONObject)err;
							if(!first){
								message.append("\n");
							}
							message.append(jerr.get("message"));
							first = false;
						}
						
						if(StringUtils.equals(message.toString(), "Session expired or invalid")) {
							UnexpectedErrorFault invalidSessionIdEx = new UnexpectedErrorFault();
							invalidSessionIdEx.setExceptionCode(ExceptionCode.INVALID_SESSION_ID);
							throw invalidSessionIdEx;
						}
						
						throw new Exception(message.toString());
					}
				}
				
			}finally{
				if(httpcon!=null){
					httpcon.close();
				}
			}
			return null;
		}
		
		/**
		 * Retrieves error record from salesforce by jobid
		 * 
		 * @param jobId
		 * @param reader read content csv from salesforce
		 * @throws Exception
		 */
		public void doRetrievErrorRecords(String jobId,AbstractMessageCreator reader) throws Exception{
			doGetResult(jobId,reader,TResultType.error);
		}
		/**
		 * Retrieves success record from salesforce by jobid
		 * 
		 * @param jobId
		 * @param reader read content csv from salesforce
		 * @throws Exception
		 */
		public void doRetrievSuccessRecords(String jobId,AbstractMessageCreator reader) throws Exception{
			doGetResult(jobId,reader,TResultType.success);
		}
		/**
		 * Retrieves unprocess record from salesforce by jobid
		 * 
		 * @param jobId
		 * @param reader read content csv from salesforce
		 * @throws Exception
		 */
		public void doRetrievUnProcessRecords(String jobId,AbstractMessageCreator reader) throws Exception{
			doGetResult(jobId,reader,TResultType.unprocess);
		}
		/**
		 * Retrieves a list of  processed records for a job.
		 * @param jobId
		 * @param reader
		 * @param type
		 * @throws Exception
		 */
		protected void doGetResult(String jobId,AbstractMessageCreator reader,TResultType type) throws Exception{
			URL targetURL =getResultUrl(jobId, type);
			HttpGet method = createBulkMethod(targetURL.getPath(), HttpGet.class);
			HttpHost target = new HttpHost(targetURL.getHost(),targetURL.getPort(),targetURL.getProtocol());
			CloseableHttpClient httpcon=null;
			try {
				httpcon = SalesForceHttpTransport.createHttpClient(method,
						config);
				HttpResponse httpresponse = SalesForceHttpTransport.execute(httpcon,target, method);
				HttpEntity r_entity = httpresponse.getEntity();
				InputStream csvContent = r_entity.getContent();
				reader.readResult(csvContent);
			}finally{
				if(httpcon!=null){
					httpcon.close();
				}
			}
		
		
		}
		/**
		 * generate result url by type
		 * @param jobId
		 * @param type
		 * @return URL endpoint of url result
		 * @throws MalformedURLException
		 */
		protected URL getResultUrl(String jobId,TResultType type) throws MalformedURLException{
			return new URL(config.getBulkV2EndPoint()+"/"+jobId+"/"+ type.getUri());
		}
		
		
		public static enum TResultType{
			error("failedResults"),success("successfulResults"),unprocess("unprocessedrecords");
			protected String uri;
			private TResultType(String uri){
				this.uri = uri;
			}
			public String getUri(){
				return this.uri;
			}
		}
		
		
		public static void main(String[] args) {
			/*
			SQLite.getInstance();
			PropertySettingDao dao = PropertySettingDao.getInstance();
			
			MapSFConnInfo conn = dao.getAllValueByFileName("integration.properties");
			conn.setUsername("lyna.phu@apsara-consulting.com.takeo");
			conn.setPassword("SKYvva2=18");
			conn.setToken("pYuIWWyodPBPPxlMQb2qhUfk");
			System.out.println(conn.get("proxyUse"));
			SFIntegrationService service = new SFIntegrationService(conn);
			try {
				service.login();
				SFConnectorConfig config =(SFConnectorConfig)service.getISFService().getSFPartner().getConfig();
				System.out.println(config.getBulkV2EndPoint());
				Interfaces__c intf = new Interfaces__c("");
				intf.setSource_Name__c("Account");
				intf.setOperationType__c(OperationEnum.upsert.toString());				
				BulkV2Connection bconn = new BulkV2Connection(config);
				//JobV2Info job = bconn.createJob(intf.getSource_Name__c(),intf.getOperationType__c(), "ExternalId__c", "CRLF");
				//System.out.println(job);
				//bconn.createBatch(job, getContent());
				//bconn.closeJob(job);
				System.out.println(bconn.getJobInfo("7500Y000008Ozdd"));
				bconn.doRetrievSuccessRecords("7500Y000008Ozdd", new AbstractMessageCreator(false,null,null) {
					
					@Override
					protected void doReadResult(ExcelCSVParser csvReader) throws Exception {
						String[] row;
						List<String> ids = new ArrayList<String>();
						while((row = csvReader.getLine()) != null){
							
							for(int i=0;i<row.length;i++){
								System.out.print(row[i]+"\t");
							}
							System.out.println();
							ids.add(row[0]);
						}
						
						//service.getSFService().getSFPartner().delete(ids.toArray(new String[ids.size()]));
						
					}
				});
	bconn.doRetrievErrorRecords("7500Y000008Ozdd", new AbstractMessageCreator(false,null,null) {
						
						@Override
						protected void doReadResult(ExcelCSVParser csvReader) throws Exception {
							String[] row;
							while((row = csvReader.getLine()) != null){
								
								for(int i=0;i<row.length;i++){
									System.out.print(row[i]+"\t");
								}
								System.out.println();
							}
							
						}
					});
			bconn.doRetrievUnProcessRecords("7500Y000008Ozdd", new AbstractMessageCreator(false,null,null) {
				
				@Override
				protected void doReadResult(ExcelCSVParser csvReader) throws Exception {
					String[] row;
					while((row = csvReader.getLine()) != null){
						
						for(int i=0;i<row.length;i++){
							System.out.print(row[i]+"\t");
						}
						System.out.println();
					}
					
				}
			});
				
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
		}
		
		protected static byte[] getContent() throws IOException{
			return FileUtil.toBytes(BulkV2Connection.class.getResourceAsStream("/account_test.csv"));
		}

		@Override
		public void cancelJob(String jobId) throws Exception {
			JobV2Info job = new JobV2Info();
			job.setId(jobId);
			abortJob(job);
			
		}

		
		@Override
		public void closeJobById(String jobId) throws Exception {
			
			updateJobStatus(jobId, JobStateEnum.UploadComplete);
			
		}

		@Override
		public void createMessage(Map<String,String>sfField2CSVHeader,ProcessingJobInfo job,boolean onlyError) throws Exception {
			BulkV2MessageCreator creater =  new BulkV2MessageCreator(TMessageStatus.Failed,onlyError,sfField2CSVHeader,job); 
			doRetrievErrorRecords(job.getJobid(), creater);
			if(!onlyError){
				creater.setStatus(TMessageStatus.Completed);
				doRetrievSuccessRecords(job.getJobid(), creater );
			}
			
			//write record not process to file
			doRetrievUnProcessRecords(job.getJobid(), new AbstractMessageCreator(onlyError,sfField2CSVHeader,job) {
				
				@Override
				protected void doReadResult(ExcelCSVParser csvReader) throws Exception {
					//write new csv for reprocess
					String[] headers = csvReader.getLine();
					String[] row = csvReader.getLine();
					if(row!=null){
						File reprocessFolder = Helper.getReprocessFolder(job);
						File reporceFile = new File(reprocessFolder,"reprocess_v2_"+generateMessageExtId()+".csv");
						OutputStreamWriter writer=null ;
						ExcelCSVPrinter csvWriter=null;
						try{
							reporceFile.createNewFile();
							writer =  new OutputStreamWriter(new FileOutputStream(reporceFile), "UTF-8");
							csvWriter = new ExcelCSVPrinter(writer);
                            csvWriter.setLineEnding("\r\n"); // TODO is it a common rule that CSV must have CRLF line ending on all OS?
							csvWriter.writeln(headers);
							while(row!=null){
								csvWriter.writeln(row);
								row = csvReader.getLine();
							}
							writer.close();
							csvWriter.close();
						}catch(Exception e){
							if(writer!=null){
								writer.close();
							}
							if(csvWriter!=null){
								csvWriter.close();
							}
							//delete when has any error
							FileUtils.deleteDirectory(reprocessFolder);
							throw e;
						}
					}
					
					
				}
			});
		}
		
		public static class BulkV2MessageCreator extends AbstractMessageCreator{
			private static final String[] SF_FIELDS ={"sf__Error","sf__Id","sf__Created"};
			protected TMessageStatus status;
			public BulkV2MessageCreator(TMessageStatus status,boolean onlyError,Map<String, String> sfField2CSVHeader,
					ProcessingJobInfo jobInfo) {
				super(onlyError,sfField2CSVHeader, jobInfo);
				this.status = status;
			}
			
			public void setStatus(TMessageStatus status){
				this.status = status;
			}

		@Override
		protected void doReadResult(ExcelCSVParser csvReader) throws Exception {
			

			// lines of result and batch are match
			String[] resultHeader = csvReader.getLine();

			String[] resultRow = csvReader.getLine();
			while (resultRow != null) {
				// next row of the batch match to the result

				try {
					Map<String, String> datas = new HashMap<String, String>(0);
					String externalVal =  "";
					// for skyvva mapping we create data with only source field to msg
					if(this.sfField2CSVHeader!=null && this.sfField2CSVHeader.size()>0) {
						for (int i = 0; i < resultHeader.length; i++) {
							// set business externalId
							if(StringUtils.equals(this.jobInfo.getExternalFieldId(), resultHeader[i])) {
								externalVal = resultRow[i];
							}
							
							String sourceField = this.sfField2CSVHeader.get(resultHeader[i]);
							if(StringUtils.isNotEmpty(sourceField)) {
								datas.put(sourceField, resultRow[i]);
							}else if(ArrayUtils.contains( SF_FIELDS, resultHeader[i] )){
								datas.put(resultHeader[i], resultRow[i]);
							}
						}
					}else { // external mapping we create data with target field
						for (int i = 0; i < resultHeader.length; i++) {
							datas.put(resultHeader[i], resultRow[i]);
						}
					}
					
					
					boolean error = status==TMessageStatus.Failed;
					if (!onlyError || error) {
						String comment = datas.get("sf__Error");
						
						if(comment==null && datas.get("sf__Created")!=null) {
							if(Boolean.valueOf(datas.get("sf__Created")) == true) {
//								comment = "Creation of "+ this.jobInfo.getSObject();
							}else if(Boolean.valueOf(datas.get("sf__Created")) == false) {
//								comment = "Modification of "+ this.jobInfo.getSObject();
							}
						}
						
						String relatedTo = datas.get("sf__Id");
						for (String sfField:SF_FIELDS) {
							datas.remove(sfField);
						}
						//TMessageStatus status = TMessageStatus.Completed;
						//if (error) {
						//	status = TMessageStatus.Failed;
						//}
						externalVal = datas.get(this.jobInfo.getExternalFieldId())!=null?datas.get(this.jobInfo.getExternalFieldId()): externalVal;
						writeMessage(datas, status, comment,relatedTo,externalVal);

					}
				} finally {
					// next row of the result
					resultRow = csvReader.getLine();
				}
			}

		}
			
		}

		@Override
		public ConnectorConfig getConfig() {		
			return config;
		}
		
}
