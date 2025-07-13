package com.iservice.gui.helper;

import java.util.HashMap;
import java.util.Map;

public class ScheduleModeHelper {

	public static String PROCESSING = "Processing";
	public static String REPROCESSING = "Reprocessing";
	
	private static Map<String, String> mapScheduleMode = new HashMap<String, String>();
	
	static{
		mapScheduleMode.put(PROCESSING, "com.iservice.integration.IntegrationSF2AgentService");
		mapScheduleMode.put(REPROCESSING, "com.iservice.integration.IntegrationSF2AgentService");
		mapScheduleMode = Helper.sortByValue(mapScheduleMode);
	}
	
	public static Map<String, String> getScheduleModeMap(){
		return mapScheduleMode;
	}
	
	public static String getClassName(String scheduleMode){
		return mapScheduleMode.get(scheduleMode);
	}
	


}
