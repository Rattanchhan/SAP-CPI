package com.iservice.bulk;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;

import com.Ostermiller.util.ExcelCSVParser;
import com.Ostermiller.util.ExcelCSVPrinter;
import com.google.gson.Gson;
import com.iservice.database.CacheSFBulkBatchFile;
import com.iservice.database.PropertySettingDao;
import com.iservice.gui.helper.Helper;
import com.iservice.model.ProcessingJobInfo;

public abstract class AbstractMessageCreator {
	private static final int MAX_SIZE_IN_MB = 90;
	public static final String[] MESSAGE_HEADER ={"Name","External_Id__c","Type__c","Integration__C","Interface__c","Status__c","Modification_Date__c","Comment__c","Comment2__c","Related_To__c","ExtValue__c","Data__c"};
	//public static final String[] DATA_HEADER = {"Message__r.External_Id__c","Name__c","Value2__c","ExtId__c"};
	private static final char ALPHA_CHAR_CODES[] = { 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78,
		79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90 };
	private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private static final DecimalFormat nf = new DecimalFormat("000");
	private static final SimpleDateFormat DATE_TIME_FORMAT =new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000Z'");
	protected Map<String,String>sfField2CSVHeader =null;
	protected ProcessingJobInfo jobInfo;
	protected int recCount;
	protected String currentMessgeFileName;
	protected String currentDataFilename;
	protected boolean onlyError;
	public AbstractMessageCreator(boolean onlyError,Map<String,String>sfField2CSVHeader,ProcessingJobInfo jobInfo){
		this.sfField2CSVHeader = sfField2CSVHeader;
		this.jobInfo = jobInfo;
		this.onlyError = onlyError;
	}
	
	protected String generateMessageName(int index){
		return "IM#"+df.format(new Date(System.currentTimeMillis()))+nf.format(index);
	}
	
	protected String generateMessageExtId(){
		char uid[] = new char[13];
		int index = 0;

		for (int i = 0; i < 4; i++) {
			uid[index++] = ALPHA_CHAR_CODES[(int) (Math.random() * Character.MAX_RADIX)];

		}

		uid[index++] = 45; // charCode for "-"

		long time = System.currentTimeMillis();

		String timeString = ("0000000" + Long.toString(time, Character.MAX_RADIX).toUpperCase());
		timeString = timeString.substring(timeString.length() - 8);
		for (int i = 0; i < timeString.length(); i++) {
			uid[index++] = timeString.charAt(i);
		}

		return new String(uid);
	}
	
	
	
	public void readResult(InputStream is) throws Exception{
		ExcelCSVParser csvResult = new ExcelCSVParser(new InputStreamReader(is,"UTF-8"));
		doReadResult(csvResult);
	
	}
	
	public static String getSFDateTime(Date date) {
		SimpleDateFormat sdf = DATE_TIME_FORMAT;
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(date);
	}

	protected void writeMessage(Map<String,String> datas,TMessageStatus status,String comment,String relatedTo, String externalVal) throws IOException{
		
		comment = StringUtils.trimToEmpty(comment);
		if(StringUtils.isBlank(currentMessgeFileName)){
			//create new unique file for messge
			currentMessgeFileName = "messgae-"+generateMessageExtId()+".csv";
		}
		String commentSort = comment;
		String commentLong = "";
		if(StringUtils.length(commentSort)>255){
			commentLong = comment;
			commentSort = StringUtils.substring(commentSort, 0, 254);
		}
		String externalId = generateMessageExtId();
		//"Name","External_Id__c","Type__c","Integration__C","Interface__c",
		//"Status__c","Modification_Date__c","Comment__c","Comment2__c"
		//,"Related_To__c","ExtValue__c","Data__c"
		String[] row = new String[]{generateMessageName(recCount),externalId,"Inbound"
				,jobInfo.getIntegrationId(),jobInfo.getInterfaceId(),
				status.name(),getSFDateTime(new Date()),commentSort,commentLong,
				//relatedto,external value
				relatedTo,externalVal,new Gson().toJson(datas)
				};
		ByteArrayOutputStream csvMessageContent = CacheSFBulkBatchFile.getInstance().getCacheFolderJob().get(jobInfo.getJobFolder()).lastCsvMessageContent();
		if(isExceed(csvMessageContent)){
			CacheSFBulkBatchFile.getInstance().getCacheFolderJob().get(jobInfo.getJobFolder()).setCsvMessageContent(new ByteArrayOutputStream());
			csvMessageContent = CacheSFBulkBatchFile.getInstance().getCacheFolderJob().get(jobInfo.getJobFolder()).lastCsvMessageContent();
		}
		boolean addHeader = csvMessageContent.size()==0;
		OutputStreamWriter writer = null;
		ExcelCSVPrinter xls=null;
		try{
			writer = new OutputStreamWriter(csvMessageContent, "UTF-8");
			xls = new ExcelCSVPrinter(writer);
            xls.setLineEnding("\r\n"); // TODO is it a common rule that CSV must have CRLF line ending on all OS?
			if(addHeader){
				String packageName = PropertySettingDao.getInstance().getQueryPackage();
				String[] headers =new String[MESSAGE_HEADER.length];
				for(int i=0;i<MESSAGE_HEADER.length;i++){
					if(StringUtils.endsWithIgnoreCase(MESSAGE_HEADER[i], "__c")) {
						headers[i]=packageName+MESSAGE_HEADER[i];
					}else {
						headers[i]=MESSAGE_HEADER[i];
					}
				}
				xls.writeln(headers);
			}
			xls.writeln(row);
			
		}finally{
			if(xls!=null){
				xls.close();
			}
			if(writer!=null){
				writer.close();
			}
		}
		//write data
		//writeData(datas, externalId);
	}
	
	/*
	protected void writeData(Map<String, String> datas,String messageId) throws IOException{
		if(StringUtils.isBlank(currentDataFilename)){
			//create new unique file for messge
			currentDataFilename = "data-"+generateMessageExtId()+".csv";
		}
		File file = createDataFile(currentDataFilename);
		OutputStreamWriter writer = null;
		ExcelCSVPrinter xls=null;
		try{
			boolean addHeader = file.length()==0;
			writer = new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8");
			xls = new ExcelCSVPrinter(writer);
			if(addHeader){
				String packageName = PropertySettingDao.getInstance().getQueryPackage();
				String[] headers =new String[DATA_HEADER.length];
				for(int i=0;i<DATA_HEADER.length;i++){
					if(StringUtils.endsWithIgnoreCase(DATA_HEADER[i], "__c")) {
						if(StringUtils.contains(DATA_HEADER[i], ".")){
							String part[] = StringUtils.split(DATA_HEADER[i],".");
							String relation = packageName+part[0];
							String fieldName =packageName+part[1];
							
							headers[i]=relation+"."+fieldName;
						}else{
							headers[i]=packageName+DATA_HEADER[i];
						}
					}else{
						headers[i]=DATA_HEADER[i];
					}
					
				}
				xls.writeln(headers);
			}
			for(String field:datas.keySet()){
				String name = sfField2CSVHeader.get(field);
				if(StringUtils.isBlank(name)){
					name = field;
				}
				String externalId = messageId+name;
				//"Message__r.External_Id__c","Name__c","Value2__c","ExtId__c"
				String[] row = new String[]{messageId,name,datas.get(field),externalId};
				if(isExceed(file, row)){
					currentDataFilename = "data-"+generateMessageExtId()+".csv";
					file = createDataFile(currentDataFilename);
					writer = new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8");
					xls = new ExcelCSVPrinter(writer);
					xls.writeln(DATA_HEADER);
				}
				xls.writeln(row);
			}
		}finally{
			if(xls!=null){
				xls.close();
			}
			if(writer!=null){
				writer.close();
			}
		}
		
		
	}*/

	protected File createMessageFile(String filename) throws IOException {
		File messageFolder = Helper.getJobMessageFolder(jobInfo);
		return createFile(filename, messageFolder);
	}
	protected File createDataFile(String filename) throws IOException {
		File messageFolder = Helper.getJobDataFolder(jobInfo);
		return createFile(filename, messageFolder);
	}
	protected File createFile(String filename, File folder)
			throws IOException {
		File newFile = new File(folder,filename);
		if(!newFile.exists()){
			newFile.createNewFile();
		}
		
		return newFile;
	}
	
	protected boolean isExceed(ByteArrayOutputStream file) throws IOException{
			
		long sizeMB= file.size()/(1024*1024);
			
		return sizeMB>=MAX_SIZE_IN_MB;
		
	}

	
	protected abstract void doReadResult(ExcelCSVParser csvResult)throws Exception;
	
	public static enum TMessageStatus{
		New,Completed,Failed,Pending,Cancelled;
	}
}
