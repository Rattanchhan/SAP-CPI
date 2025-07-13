package com.iservice.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

//import org.apache.log4j.Logger;

import com.Ostermiller.util.ExcelCSVPrinter;

public class CSVWriterUtil {

	private static final char COMMA = ',';

//	private static final Logger LOGGER = Logger.getLogger(CSVWriterUtil.class);

	protected String filename;
	protected char delemeter;
	
	public CSVWriterUtil(String file){
		this(file,COMMA);
	}
	
	public CSVWriterUtil(String filename,char delemeter){
		this.delemeter =delemeter;
		this.filename = filename;
	}
	
	public void writeHeader(String[] headers) {

		save(headers);

	}

	public void save(String[] row) {

		List<String[]> rows = new ArrayList<>(1);
		rows.add(row);
		save(rows);

	}
	public void save(List<String[]> listData) {
		if (listData != null && listData.size() > 0) {
			try {
	
				File file = new File(this.filename);
	
				OutputStreamWriter writer = new OutputStreamWriter(
						new FileOutputStream(file, true), "UTF-8");
				ExcelCSVPrinter csv = new ExcelCSVPrinter(writer);
				csv.changeDelimiter(delemeter);
				for (String[] row : listData) {
					csv.print(row);
				}
				csv.close();
				writer.close();
	
			} catch (Exception e) {
//				LOGGER.error(e.getMessage(), e);
				throw new RuntimeException(e.getMessage(), e);
			}
		}

	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public char getDelemeter() {
		return delemeter;
	}

	public void setDelemeter(char delemeter) {
		this.delemeter = delemeter;
	}
	
	
	
	
	public static void main(String[] args) {
		URL url = CSVWriterUtil.class.getResource("/testfile.txt");
		String test = new String("abcdasfsadfsfdfsdfsdfadfadfadfadfasdfasdfadfsadfasfaa");
		System.out.println(test.getBytes().length);
		System.out.println(4*(new File(url.getFile()).length()/3));
		System.out.println(Base64.getEncoder().encode(test.getBytes()).length);
	}

}
