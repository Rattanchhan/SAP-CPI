package com.iservice.bulk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

//import org.json.JSONArray;
//import org.json.JSONObject;
//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;

import com.iservice.task.MapIntegrationInfo;
import com.opencsv.CSVWriter;
import com.sforce.async.AsyncApiException;
import com.sforce.async.BatchInfo;
import com.sforce.async.BulkConnection;
import com.sforce.async.ContentType;
import com.sforce.async.JobInfo;
import com.sforce.async.OperationEnum;
import com.sforce.soap.schemas._class.IServices.IBean;

public class BulkTransformationHandler {

	
	public static int PACKET = 2000; // each file will contain 5000 messages

	
	protected JobInfo job;
	protected List<BatchInfo> batchs = new ArrayList<BatchInfo>();
	private int packageIndex = 0;
	private int packageSize = PACKET;
	
	private String messageId;
	private MapIntegrationInfo integrationInfo; 

	protected StringWriter swriter = null;
	protected CSVWriter csvwriter = null;
	protected ByteArrayOutputStream out = null;
	protected ZipOutputStream gzip = null;
	protected long zipLenght = 0;
	protected BulkConnection connection;
	String[] header = { "ParentId", "Name", "Body", "ContentType" };

	

	public BulkTransformationHandler(MapIntegrationInfo integrationInfo) {
		this.integrationInfo = integrationInfo;
	}

	public int getPackageIndex() {
		return packageIndex;
	}

	public void setPackageIndex(int packageIndex) {
		this.packageIndex = packageIndex;
	}

	public int getPackageSize() {
		return packageSize;
	}

	public void setPackageSize(int packageSize) {
		this.packageSize = packageSize;
	}

	public void setPackageSize(String packageSize) {
		try {
			this.packageSize = Integer.parseInt(packageSize);
		} catch (Exception e) {
			this.packageSize = PACKET;
		}
	}

	public String getInterfaceId() {
		return integrationInfo.getInterfaceId();
	}

	

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public void increasePackageIndex() {
		this.packageIndex++;
	}

	protected void initWriter() {
		out = new ByteArrayOutputStream();
		gzip = new ZipOutputStream(out);
		zipLenght = 0;
		swriter = new StringWriter();
		csvwriter = new CSVWriter(swriter);
		csvwriter.writeNext(header);	
	}

	/**
	 * 
	 * @param records
	 * @throws IOException
	 */
	/*
	public void builAttachmentJSONArray(JSONArray records) throws Exception {
		if (swriter == null) {
			initWriter();
		}
		// construct a unique attach file per record
		String attname = "INITIALLOAD-" + (new Date()).getTime()
				+ getPackageIndex()+"-1";
		String filename = attname + ".txt";
		byte[] content = records.toString().getBytes("UTF-8");
		zipLenght = zipLenght + content.length;
		double zipLenghtMB = zipLenght / (1024 * 1024);
		if (zipLenghtMB >= 15) {
			createBatch(getBulkConnection());
		}

		// adapter.appendZipLenght(content.length);
		// add the json into the zip
		gzip.putNextEntry(new ZipEntry(filename));
		gzip.write(content);
		gzip.closeEntry();

		String[] val = { getInterfaceId(), attname, "#" + filename, "txt" };
		csvwriter.writeNext(val);
		// increase
		increasePackageIndex();

	}
	*/
	
	public List<BatchInfo> createBachs(List<List<IBean>> queryList) throws Exception{
		return createBachs(queryList, false);
	}
	
	
	@SuppressWarnings("unchecked")
	public List<BatchInfo> createBachs(List<List<IBean>> queryList,boolean mustCreateBatch) throws Exception {
		// [{"oneRecord":[{"value":"XX1","name":"FIELD1"},{"value":"XX1","name":"FIELD2"}]}]
		return null;
		/*
		JSONArray records = new JSONArray();
		for (List<IBean> listBean : queryList) {
			
			JSONArray fields = new JSONArray();
			for (IBean bean : listBean) {
				
				JSONObject json = new JSONObject();
				json.put("name", bean.getName());
				json.put("value", bean.getValue());
				fields.add(json);
			}
			// one record
			JSONObject onerecord = new JSONObject();
			onerecord.put("oneRecord", fields);
			
			
			// add the record to the list
			records.add(onerecord);
			if (records.size() == packageSize) {
				builAttachmentJSONArray(records);
				records = new JSONArray();
			}
		}
		
		if (records.size() > 0) {
			builAttachmentJSONArray(records);
			
		}
		if(mustCreateBatch){
			//create last batch
			createBatch(getBulkConnection());
		}
		return batchs;
		*/
	}
	
	
	
	
	
	protected BulkConnection getBulkConnection()
	      throws Exception {
	   if(connection==null){
		   connection = new BulkV1Connection(integrationInfo.getSFIntegrationService().getBinding().getConfig());
	   }
	
	   return connection;
	}
	
	private JobInfo createJob( BulkConnection connection)
		      throws AsyncApiException {
		    JobInfo job = new JobInfo();
		    job.setObject("Attachment");
		    job.setOperation(OperationEnum.insert);
		    job.setContentType(ContentType.ZIP_CSV);
		    job = connection.createJob(job);
		    return job;
		}
	

	public JobInfo createJob()  throws Exception{
		return createJob(getBulkConnection());
	}

	public void cancelJob() throws Exception{
		if(this.job!=null){			
		    getBulkConnection().abortJob(this.job.getId());
		}
	}
	
	
	
	public void closeJob()
		      throws Exception {
			if(this.job!=null){			   
			    getBulkConnection().closeJob(this.job.getId());
			}
		}
	
	
	
	protected JobInfo getJob(BulkConnection conn)throws AsyncApiException{
		if(job==null){
			job = createJob(conn);
		}
		
		return job;
	}

	

	
	protected void createBatch( BulkConnection connection)
	          throws IOException, AsyncApiException {
		if (zipLenght>0) {
			try{
				// close the csv writer
				csvwriter.close();
				// zip the request file
				gzip.putNextEntry(new ZipEntry("request.txt"));
				gzip.write(swriter.toString().getBytes());
				gzip.closeEntry();
				gzip.close();
				byte[] zipPayload = out.toByteArray();
	
				BatchInfo batch = connection.createBatchFromZipStream(getJob(connection), new ByteArrayInputStream(zipPayload));
				batchs.add(batch);
			}finally{
				//after create job must re-init writer
				initWriter();
			}
		}
	}
	
	public MapIntegrationInfo getMapIntegrationInfo() {
		return integrationInfo;
	}

}
