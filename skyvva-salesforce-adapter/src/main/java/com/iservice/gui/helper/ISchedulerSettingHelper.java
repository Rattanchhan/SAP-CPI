package com.iservice.gui.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class ISchedulerSettingHelper {
	
	public static final String JOB_NAME = "jobname";
	public static final String SCHEDULER_TYPE = "schedulertype";
	public static final String USERNAME = "username";
	public static final String INTEGRATION_ID = "integrationid";
	public static final String INTEGRATION_NAME = "integrationname";
	public static final String INTERFACE_ID = "interfaceid";
	public static final String INTERFACE_NAME = "interfacename";
	public static final String DIRECTION = "direction";
	public static final String REPROCESS = "reprocess";
	public static final String STATUS = "status";
	public static final String CRON_VALUE = "cronvalue";
	public static final String CRON_EXPRESSION = "cronexpression";
	public static final String SCHEDULE_BASED_CONDITION = "schedulebasedcondition";
	public static final String IS_DATA_PROCESSED = "isdataprocessed";
	
	public static final String CREATED_DATE = "createddate";
	public static final String CREATED_BY = "createdby";
	public static final String MODIFIED_DATE = "modifieddate";
	public static final String MODIFIED_BY = "modifiedby";
	public static final String SYNC_STATUS = "syncstatus";
	
	public static final String FREQUENCY = "frequency";
	public static final String RUN_AT = "runat";
	public static final String RUN_EVERY = "runevery";
	public static final String RUN_EVERY_BY = "runeveryby";
	public static final String START_MINUTE = "startminute";
	public static final String END_MINUTE = "endminute";
	public static final String START_HOUR = "starthour";
	public static final String END_HOUR = "endhour";
	public static final String DAYS_OF_WEEK = "daysOfWeek";
	public static final String DAYS_OF_MONTH = "daysOfMonth";
	public static final String MONTHLY = "monthly";
	
	public static final String MINUTES = "Minute";
	public static final String HOURS = "Hour";
	
	public static final String NEW_CRON = "NEW";
	public static final String UPD_CRON = "UPD";
	public static final String SFSYNC = "SYNC";
	public static final String ERROR = "ERROR";
	public static final String STARTED = "started";
	public static final String STOPPED = "stopped";
	
	public static final String TRUE = "true";
	public static final String FALSE = "false";
	
	public static final String RUNAT = "Run At";
	public static final String RUNEVERYMINUTE = "Every Minute";
	
	public static final String JOBNAME_AGENT_SCHEDULER = "Skyvva_Agent_Scheduler";
    public static final String PER_INTERFACE_AGENT_SCHEDULER	= "AgentProcessPerInterface";
    public static final String PER_INTEGRATION_ALL_IN_BOUND_AGENT_SCHEDULER = "AgentProcessPerIntegrationAllInBound";
    public static final String PER_INTEGRATION_ALL_IN_OUTBOUND_AGENT_SCHEDULER = "AgentProcessPerIntegrationAllOutBound";
    
	public static List<String> getFieldsArrayList() {
		return new ArrayList<String>(Arrays.asList(
				SCHEDULER_TYPE,
				USERNAME,
				INTEGRATION_ID,
				INTEGRATION_NAME,
				INTERFACE_ID,
				INTERFACE_NAME,
				DIRECTION,
				REPROCESS,
				STATUS,
				SYNC_STATUS,
				CRON_VALUE,
				CREATED_DATE,
				CREATED_BY,
				MODIFIED_DATE,
				MODIFIED_BY,
				SCHEDULE_BASED_CONDITION,
				IS_DATA_PROCESSED));
	}
	
	public static String getJobName(String sigId) {
		return JOBNAME_AGENT_SCHEDULER + (StringUtils.isNotBlank(sigId) ? ("#"+(sigId+"").substring(0,15)) : "");
	}
	
	public static String getSchedulerTypeKey(String SCH_TYPE, String sigId) {
        return StringUtils.isNotBlank(sigId)? SCH_TYPE+"#"+(sigId+"").substring(0,15) : SCH_TYPE;
    }
}
