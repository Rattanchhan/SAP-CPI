package com.iservice.database;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * We cache batch data as ByteArrayOutputStream. So we can store it in just memory.
 * @author Phanith Meas
 *
 */
		
public class CacheSFBulkFolder {
	ByteArrayOutputStream csvContent;
	List<ByteArrayOutputStream> csvMessageContent = new ArrayList<ByteArrayOutputStream>();
	
	int csvMessageContentIndex = 0;
	
	public ByteArrayOutputStream getCsvContent() {
		return csvContent;
	}
	
	public void setCsvContent(ByteArrayOutputStream csvContent) {
		this.csvContent = csvContent;
	}
	
	/**
	 * CSV Message file could reach SF Limit. So this method will return the last one that not yet reach the limit.
	 * @return
	 */
	public ByteArrayOutputStream lastCsvMessageContent() {
		if(csvMessageContent.size()==0) {
			csvMessageContent.add(new ByteArrayOutputStream());
			csvMessageContentIndex = 0;
		}
		return csvMessageContent.get(csvMessageContentIndex);
	}
	
	public List<ByteArrayOutputStream> getCsvMessageContent(){
		return csvMessageContent;
	}
	
	public void setCsvMessageContent(ByteArrayOutputStream csvMessageContent) {
		this.csvMessageContent.add(csvMessageContent);
		csvMessageContentIndex++;
	}

}
