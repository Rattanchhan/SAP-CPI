package com.iservice.adapter;

import java.util.Arrays;
import java.util.*;

public class FileTypeHelper {
	public static final String CSV = "CSV";
	public static final String EXCEL = "Excel";
	public static final String XML = "Xml";
	public static final String JSON = "Json";
	public static final String OTHER = "Other";
	public static final String FILE = "File";
	public static final List<String> FILETYPE = Arrays.asList(CSV, XML, EXCEL, JSON, FILE);
	public static final List<String> FILETYPEEXT = Arrays.asList(".csv", ".xls|.xlsx", ".xml", ".json", "log", ".*");
	
	public static final Map<String, String> mapFileTypeExt;
	
	static
    {
		mapFileTypeExt = new HashMap<String, String>();
		mapFileTypeExt.put("csv", CSV);
		mapFileTypeExt.put("xls", EXCEL);
		mapFileTypeExt.put("xlsx", EXCEL);
		mapFileTypeExt.put("xml", XML);
		mapFileTypeExt.put("json", JSON);
    }
}
