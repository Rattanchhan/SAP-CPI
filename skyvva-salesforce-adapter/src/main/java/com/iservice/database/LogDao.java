package com.iservice.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.iservice.gui.data.ILogs__c;

public class LogDao extends BaseDao {

	public static final String TABLE_NAME = "ilog";
	@Override
	public List<String> getFields() {
		return new ArrayList<String>(Arrays.asList("ParentId", "level","source","sourceType","description"));
	}

	@Override
	public String getTableName() {
		return TABLE_NAME;
	}

	@Override
	public String[] getIndexes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getUniques() {
		return new String[] {"ParentId,sourceType"};
	}
	public List<ILogs__c> getLogs(String parentId,String pkage) {
		List<ILogs__c> ilogs = new ArrayList<ILogs__c>();
		Map<String,Object> param = new HashMap<String,Object>();
		param.put("ParentId", parentId);
		List<Map<String, Object>> result = getDb().query("SELECT * FROM " + getTableName() + " where ParentId=:ParentId" ,param);
		if(result != null && result.size()>0) {
			for(Map<String,Object> map : result) {
				ILogs__c log = new ILogs__c(pkage);
				log.setError_Level__c((String)map.get("level"));
				log.setDescription__c((String)map.get("description"));
				log.setIntegration__c((String)map.get("ParentId"));
				log.setSource__c((String)map.get("source"));
				ilogs.add(log);
			}
			
		}
		return ilogs;
	}
}
