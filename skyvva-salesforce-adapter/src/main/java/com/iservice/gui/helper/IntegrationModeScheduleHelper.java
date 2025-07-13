package com.iservice.gui.helper;

import java.util.HashMap;
import java.util.Map;

public class IntegrationModeScheduleHelper {
	
	private static Map<String, String> mapSchedulingTypes = new HashMap<String, String>();
	
	static{
		mapSchedulingTypes.put(DirectionTypeHelper.INBOUND, "com.iservice.integration.IntegrationOnDemandService");
		mapSchedulingTypes.put(DirectionTypeHelper.OUTBOUND, "com.iservice.integration.IntegrationSF2AgentService false");
		mapSchedulingTypes = Helper.sortByValue(mapSchedulingTypes);
	}
	
	public static Map<String, String> getScheduleModeMap(){
		return mapSchedulingTypes;
	}
	
	
	public static String getClassName(String directionType){
		return mapSchedulingTypes.get(directionType);
	}
	
}
