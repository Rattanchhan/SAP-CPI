package com.iservice.adapter.reader;

import java.util.List;
import java.util.Objects;

import org.apache.camel.util.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

public class FilterContextNode {
	
	
	@SuppressWarnings("unchecked")
	private static JsonObject getDataValue(List<JsonObject> datas, String filterField, Object filterValue) {
		for(JsonObject data : datas) {
			if(data.get(filterField)!=null) {
				if(filterValue instanceof String && StringUtils.equalsIgnoreCase((String) filterValue, String.valueOf(data.get(filterField)))) {
					return data;
				}else if(filterValue instanceof List){
					List<Object> filterValues = (List<Object>) filterValue;
					for (Object fValue : filterValues) {
						if (Objects.equals(fValue, data.get(filterField))) {
							return data;
						}
					}
				}
			}
		}	
		return null;		
	}
	
	public static JsonObject getRecordByFilter(JsonObject filter, List<JsonObject> data) {
		boolean isORCondition = StringUtils.equalsIgnoreCase("OR", String.valueOf(getValue(filter, "condition_type")));
		boolean isMatch = false;
		JsonObject parentRecord = new JsonObject();
		for (String filterField : filter.keySet()) {
			if (StringUtils.equalsIgnoreCase(filterField, "condition_type")) {
				continue;
			}
			boolean found = false;
			Object filterValue = filter.get(filterField);
			JsonObject dataValue = getDataValue(data, filterField, filterValue);

			if(dataValue!=null) {
				found = true;
				parentRecord = dataValue;							
			}
			
			if (found) {
				isMatch = true;
				if (isORCondition) {					
					break;
				}
			} else {
				if (!isORCondition) {
					isMatch = false;
					break;
				}
			}
		}
		
		if (isMatch) {
			parentRecord = reAssignData(data, parentRecord);
		}else {
			parentRecord = null;
		}
		
		return parentRecord;
	}
	
	private static JsonObject reAssignData(List<JsonObject> datas, JsonObject parentRecord) {
		for(JsonObject data : datas) {
			for(String key : data.keySet()) {
				if(parentRecord.get(key)==null) parentRecord.put(key, data.get(key));
			}			
		}
		return parentRecord;
	}
	
	protected static Object getValue(JsonObject filter, String key) {
		Object val = null;
		if (filter != null) {
			val = filter.get(key);
			if (val == null) {
				key = convertSkyvvaSpecialCharToDot(key);
				val = filter.get(key);
			}
			if (val == null) {
				for (String f : filter.keySet()) {
					if (StringUtils.equalsIgnoreCase(key, f)) {
						val = filter.get(f);
						break;
					}
				}
			}
		}
		return val;
	}
	
	public static String convertSkyvvaSpecialCharToDot(String nodeName) {
		if (StringUtils.isNotBlank(nodeName)) {
			nodeName = StringUtils.replaceIgnoreCase(nodeName, IMsgGeneratorV3.DOT_REPLACING, IMsgGeneratorV3.SEP);
		}
		return nodeName;
	}
}
