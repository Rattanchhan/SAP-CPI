package com.iservice.database;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class used to cache batch FileSystem used in SalesForce Bulk API due to SAP CPI's environment doesn't support file system.
 * So we will not write data and store it as persistence we cache it in memory.
 * It has a ConcurrentHashMap attribute which store cache batch file and has batch job Id as key.
 * 
 * @author Phanith Meas
 */
public class CacheSFBulkBatchFile {

	private final Map<String, CacheSFBulkFolder> cacheFolderJob = new ConcurrentHashMap<>();
	
	private static final CacheSFBulkBatchFile instance = new CacheSFBulkBatchFile();
	
	public static CacheSFBulkBatchFile getInstance() {
		return instance;
	}
	
	public Map<String, CacheSFBulkFolder> getCacheFolderJob(){
		return cacheFolderJob;
	}
}
