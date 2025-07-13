package com.iservice.adapter.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.Ostermiller.util.ExcelCSVParser;
import com.iservice.adapter.FileMapAdapter;
import com.iservice.gui.helper.Helper;
import com.iservice.task.MapIntegrationInfo;
import com.sforce.soap.schemas._class.IServices.IBean;

public class CsvFileReader extends AbstractFileReader {
	protected char sparater;
	public CsvFileReader(char separater,FileMapAdapter adapter, MapIntegrationInfo integrationInfo) {
		super(adapter,integrationInfo);
		this.sparater = separater;
		
	}

	@SuppressWarnings("resource")
	@Override
	public boolean doProceFile(File f,IProcessRecord processor, boolean isReadOnly)throws Exception {
		InputStream is = new FileInputStream(f);
		ExcelCSVParser csvParser = new ExcelCSVParser(new InputStreamReader(is,"UTF-8"));
		
		try {
			//change the CSV separator
			csvParser.changeDelimiter(sparater);
			List<String> orderHeaders = getOrderHeader();
			if(orderHeaders==null||orderHeaders.isEmpty()){
				//when there is no define header mean that first line is header
				String[] csvHeaders = csvParser.getLine();
				
				orderHeaders = new ArrayList<String>();
				if(csvHeaders!=null){
					boolean firstLine =true;
					for(String h:csvHeaders){
						// replace ﻿ hidden char (for the first column)
						if(firstLine) h = h.replaceAll(Helper.INVALID_CHARS, "");
						orderHeaders.add(FileMapAdapter.manageSPChar(h));
						firstLine = false;
					}
				}
			}
			
			//start create record
			String[] row;
			while((row = csvParser.getLine()) != null){
				List<IBean> rec = new ArrayList<IBean>();
				for(int i=0;i<row.length;i++){
					if(orderHeaders.size()>i){
						rec.add(new IBean(orderHeaders.get(i), row[i]));
					}
				}
				if(!processor.doProcess(rec)){
					return false;
				}
//				if(integrationInfo.getInterfaces().getMessageType__c()!=null && !isReadOnly) {
//					if(!processV3__IBean(processor, rec)) {
//						return false;
//					}
//				}else {
//					if(!processor.doProcess(rec)){
//						return false;
//					}
//				}
			}
			
		}
		finally {
			csvParser.close();
			is.close();
			csvParser=null;
			is=null;
		}
		return true;
	}

	@Override
	protected long doCountRecord(File f) throws Exception{
		InputStream is = new FileInputStream(f);
		ExcelCSVParser csvParser = new ExcelCSVParser(new InputStreamReader(is,"UTF-8"));
		try {
			//change the CSV separator
			csvParser.changeDelimiter(sparater);
			List<String> orderHeaders = getOrderHeader();
			if(orderHeaders==null||orderHeaders.isEmpty()){
				//when there is no define header mean that first line is header
				String[] csvHeaders = csvParser.getLine();
				orderHeaders = new ArrayList<String>();
				if(csvHeaders!=null){
					boolean firstLine = true;
					for(String h:csvHeaders){
						// replace ﻿ hidden char (for the first column)
						if(firstLine) h = h.replaceAll(Helper.INVALID_CHARS, "");
						orderHeaders.add(FileMapAdapter.manageSPChar(h));
						firstLine = false;
					}
				}
				doCompareHeader(orderHeaders);
			}
			//start create record
			long total =0;
			while(csvParser.getLine() != null){
				total++;
			}
			return total;
		} finally {
			csvParser.close();
			is.close();
			csvParser=null;
			is=null;
		}
	}

}
