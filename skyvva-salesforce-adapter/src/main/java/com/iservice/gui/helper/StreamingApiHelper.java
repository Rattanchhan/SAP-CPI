package com.iservice.gui.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StreamingApiHelper {
	public static final String USERNAME = "username";
	public static final String INTEGRATION_ID = "integrationid";
	public static final String INTEGRATION_NAME = "integrationname";
	public static final String INTERFACE_ID = "interfaceid";
	public static final String INTERFACE_NAME = "interfacename";
	public static final String REPLAY_FROM = "replayFrom";
	public static final String STATUS = "status";
	public static final String MESSSAGE = "message";
	
	public static final String CREATED_DATE = "createddate";
	public static final String CREATED_BY = "createdby";
	public static final String MODIFIED_DATE = "modifieddate";
	public static final String MODIFIED_BY = "modifiedby";
	
	public static final String SUBSCRIBED = "subscribed";
	public static final String UNSUBSCRIBED = "unsubscribed";
	public static final String NEW = "new";
	public static final String ERROR = "error";
	
	public static List<String> getFieldsArrayList() {
		return new ArrayList<String>(Arrays.asList(
				USERNAME,
				INTEGRATION_ID,
				INTEGRATION_NAME,
				INTERFACE_ID,
				INTERFACE_NAME,
				REPLAY_FROM,
				STATUS,
				MESSSAGE,
				CREATED_DATE,
				CREATED_BY,
				MODIFIED_DATE,
				MODIFIED_BY));
	}
}
