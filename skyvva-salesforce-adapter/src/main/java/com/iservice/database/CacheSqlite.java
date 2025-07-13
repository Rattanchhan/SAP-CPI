package com.iservice.database;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.iservice.model.Prefs;
import com.iservice.model.ProcessingJobInfo;

public class CacheSqlite {
	private Map<Long, ProcessingJobInfo> processingJobInfor = new ConcurrentHashMap<>();
	private Map<String, List<Prefs>> propertySetting = new ConcurrentHashMap<>();
	
	private static CacheSqlite instance;
	
	public static CacheSqlite getInstance() {
		if(instance==null) {
			instance = new CacheSqlite();
		}
		return instance;
	}	
	
	public Map<Long, ProcessingJobInfo> getProcessingJobInfor() {
		return processingJobInfor;
	}
	
	public void saveProcessingJobInfor(ProcessingJobInfo job) {
		processingJobInfor.put(job.getAgent_id(), job);
	}
	
	public Map<String, List<Prefs>> getPropertySetting(){
		return propertySetting;
	}
	
	public void savePropertySetting(List<Prefs> prefs, String username) {
		propertySetting.put(username, prefs);
	}
}
