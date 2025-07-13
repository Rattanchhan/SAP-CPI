package com.iservice.bulk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.opencsv.CSVWriter;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

//import org.json.simple.JSONArray;
//import org.json.simple.parser.JSONParser;

import com.google.gson.Gson;
import com.iservice.model.IMessageTree;
import com.iservice.task.MapIntegrationInfo;
import com.sforce.async.AsyncApiException;
import com.sforce.async.BatchInfo;
import com.sforce.async.BulkConnection;
import com.sforce.async.ContentType;
import com.sforce.async.JobInfo;
import com.sforce.async.OperationEnum;

public class BulkTransformationHandlerV3 extends BulkTransformationHandler{
	
	String[] header = {"FirstPublishLocationId", "Title", "PathOnClient", "VersionData"};

	public BulkTransformationHandlerV3(MapIntegrationInfo integrationInfo) {
		super(integrationInfo);
	}
	
	@SuppressWarnings("unchecked")
	public List<BatchInfo> createBulkBasketV3(List<IMessageTree> iMessage, boolean mustCreateBatch) throws Exception {
		
		JSONArray records = (JSONArray) new JSONParser().parse(new Gson().toJson(iMessage));
		
		if (records.size() > 0) {
			builAttachmentJSONArrayV3(records);
		}
		
		if(mustCreateBatch){
			//create last batch
			createBatch(getBulkConnection());
		}
		return batchs;
	}
	
	public void builAttachmentJSONArrayV3(JSONArray records) throws Exception {
		if (swriter == null) {
			initWriter();
		}
		// construct a unique attach file per record
		String attname = "INITIALLOAD__BULK_V3-" + (new Date()).getTime()
				+ getPackageIndex();
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

		String[] val = { getInterfaceId(), filename, filename, "#" + filename };
		csvwriter.writeNext(val);
		// increase
		increasePackageIndex();

	}
	
	
	protected void initWriter() {
		out = new ByteArrayOutputStream();
		gzip = new ZipOutputStream(out);
		zipLenght = 0;
		swriter = new StringWriter();
		csvwriter = new CSVWriter(swriter);
		csvwriter.writeNext(header);
	}
	
	private JobInfo createJob( BulkConnection connection)
		      throws AsyncApiException {
		    JobInfo job = new JobInfo();
		    job.setObject("ContentVersion");
		    job.setOperation(OperationEnum.insert);
		    job.setContentType(ContentType.ZIP_CSV);
		    job = connection.createJob(job);
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
				this.getMapIntegrationInfo().setResponseObject(batchs);
			}finally{
				//after create job must re-init writer
				initWriter();
			}
		}
	}
	
	protected JobInfo getJob(BulkConnection conn)throws AsyncApiException{
		if(job==null){
			job = createJob(conn);
		}
		
		return job;
	}

}
