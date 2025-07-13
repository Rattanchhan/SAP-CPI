package com.iservice.gui.helper;

import java.util.HashMap;
import java.util.Map;

import com.iservice.task.GenericSFTask;

public class ServerHelper {

    private static Map<String, String> mapServerEnvironment;
	
	public static String SANDBOX = "Sandbox";
	public static String PRODUCTION_DEVELOPER = "Production / Developer";
	public static String WWW= "www";

	static{
		mapServerEnvironment = new HashMap<String, String>();
		mapServerEnvironment.put(SANDBOX, "test");
		mapServerEnvironment.put(PRODUCTION_DEVELOPER, "login");
		mapServerEnvironment.put(WWW, "www");
	}
		
	public static String getEnvironment(String environment){
		return mapServerEnvironment.get(environment);
	}

}
