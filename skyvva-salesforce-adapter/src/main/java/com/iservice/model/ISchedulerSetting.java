package com.iservice.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iservice.gui.helper.DirectionTypeHelper;
import com.iservice.gui.helper.Helper;
import com.iservice.gui.helper.ISchedulerSettingHelper;
import com.iservice.gui.helper.JCrontabHelper;
import com.iservice.gui.helper.JCrontabHelper.HourMinute;

public class ISchedulerSetting extends AbstractDataModel {
	
	public static final String OUTBOUND_CLASS = "com.iservice.integration.IntegrationSF2AgentService";
	public static final String INBOUND_CLASS = "com.iservice.integration.IntegrationOnDemandService";
	
	public static final String INBOUND = "InBound";
	public static final String OUTBOUND = "OutBound";
	
	public static final String PROCESS_PER_INTERFACE_GROUP = "AgentProcessPerInterfaceGroup";

	private static final String EMPTY = "";
	// cron data
	private String jobname = EMPTY;
	private String schedulertype = EMPTY;
	private String username = EMPTY;
	private String integrationid = EMPTY;
	private String integrationname = EMPTY;
	private String interfaceid = EMPTY;
	private String interfacename = EMPTY;
	private String direction = EMPTY;
	private String reprocess = EMPTY;
	private String status = EMPTY;
	private String schedulebasedcondition = EMPTY;
	private String isdataprocessed = EMPTY;
	
	// reference
	private String createddate = EMPTY;
	private String createdby = EMPTY;
	private String modifieddate = EMPTY;
	private String modifiedby = EMPTY;
	private String syncstatus = EMPTY;
	
	// cron value
	private String cronvalue;
	
	public String getSchedulertype() {
		if(StringUtils.isEmpty(schedulertype)) {
			if(StringUtils.isEmpty(getInterfacename())) {
				String type = StringUtils.equalsIgnoreCase(getDirection(), DirectionTypeHelper.INBOUND) ? ISchedulerSettingHelper.PER_INTEGRATION_ALL_IN_BOUND_AGENT_SCHEDULER : ISchedulerSettingHelper.PER_INTEGRATION_ALL_IN_OUTBOUND_AGENT_SCHEDULER;
				this.schedulertype = ISchedulerSettingHelper.getSchedulerTypeKey(type, getIntegrationid());
				this.jobname = ISchedulerSettingHelper.getJobName(getIntegrationid());
			}else {
				this.schedulertype = ISchedulerSettingHelper.getSchedulerTypeKey(ISchedulerSettingHelper.PER_INTERFACE_AGENT_SCHEDULER, getInterfaceid());
				this.jobname = ISchedulerSettingHelper.getJobName(getInterfaceid());
			}
			this.createddate = getCreateddate();
			this.modifieddate = getModifieddate();
			this.syncstatus = ISchedulerSettingHelper.NEW_CRON;
		}else {
			this.modifieddate = getModifieddate();
			if(StringUtils.equalsIgnoreCase(syncstatus, ISchedulerSettingHelper.SFSYNC)) {
				this.syncstatus = ISchedulerSettingHelper.UPD_CRON;
			}
		}
		return schedulertype;
	}
	
	public String getIsdataprocessed() {
		return isdataprocessed;
	}

	public void setIsdataprocessed(String isdataprocessed) {
		this.isdataprocessed = isdataprocessed;
	}
	
	public String getSchedulebasedcondition() {
		return schedulebasedcondition;
	}

	public void setSchedulebasedcondition(String schedulebasedcondition) {
		this.schedulebasedcondition = schedulebasedcondition;
	}

	public void setSchedulertype(String schedulertype) {
		this.schedulertype = schedulertype;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getIntegrationid() {
		return integrationid;
	}

	public void setIntegrationid(String integrationid) {
		this.integrationid = integrationid;
	}

	public String getIntegrationname() {
		return integrationname;
	}

	public void setIntegrationname(String integrationname) {
		this.integrationname = integrationname.replace("%20", " ");
	}

	public String getInterfaceid() {
		return interfaceid;
	}

	public void setInterfaceid(String interfaceid) {
		this.interfaceid = interfaceid;
	}

	public String getInterfacename() {
		return interfacename;
	}

	public void setInterfacename(String interfacename) {
		this.interfacename = interfacename.replace("%20", " ");
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public String getReprocess() {
		return reprocess;
	}

	public void setReprocess(String reprocess) {
		this.reprocess = reprocess;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getCreateddate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		this.createddate = dateFormat.format(new Date());
		return createddate;
	}

	public void setCreateddate(String createddate) {
		this.createddate = createddate;
	}

	public String getCreatedby() {
		return createdby;
	}

	public void setCreatedby(String createdby) {
		this.createdby = createdby;
	}

	public String getModifieddate() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		this.modifieddate = dateFormat.format(new Date());
		return modifieddate;
	}
	
	public String getModifiedDate() {
		return this.modifieddate;
	}

	public void setModifieddate(String modifieddate) {
		this.modifieddate = modifieddate;
	}

	public String getModifiedby() {
		return modifiedby;
	}

	public void setModifiedby(String modifiedby) {
		this.modifiedby = modifiedby;
	}

	public String getSyncstatus() {
		return syncstatus;
	}

	public void setSyncstatus(String syncstatus) {
		this.syncstatus = syncstatus;
	}

	public String getCronvalue() {
		return cronvalue;
	}

	public void setCronvalue(String cronvalue) {
		this.cronvalue = cronvalue;
	}
	
	public String getJobName() {
		return jobname;
	}
	
	public void getJobName(String jobname) {
		this.jobname = jobname;
	}

	public ISchedulerSetting() {
		super();
	}
	
	public ISchedulerSetting(String entry) throws Exception {
		entry = validate(entry);
		manageEntry(entry);
		
		setSchedulertype(getSchedulertype());
		setCreatedby(getUsername());
		setModifiedby(getUsername());
	}
	
	private void manageEntry(String entry) {
		String[] tmp = entry.split("\\s+");		
		this.cronvalue = getCronvalue(entry);
		if(entry.contains(INBOUND_CLASS)) {
			// inbound
			this.direction = DirectionTypeHelper.INBOUND;
			this.reprocess = ISchedulerSettingHelper.FALSE;
		}else if(entry.contains(OUTBOUND_CLASS)) {
			// outbound
			if(tmp.length == 9) {
				this.direction = DirectionTypeHelper.OUTBOUND;
				this.reprocess = ISchedulerSettingHelper.FALSE;
			}else {
				// reprocess
				this.direction = DirectionTypeHelper.OUTBOUND;
				this.reprocess = ISchedulerSettingHelper.TRUE;
			}
		} else {
			if(entry.contains(INBOUND)) {
				this.direction = DirectionTypeHelper.INBOUND;
				this.reprocess = ISchedulerSettingHelper.FALSE;
			} else {
				if(tmp.length == 9) {
					this.direction = DirectionTypeHelper.OUTBOUND;
					this.reprocess = ISchedulerSettingHelper.FALSE;
				} else {
					this.direction = DirectionTypeHelper.OUTBOUND;
					this.reprocess = ISchedulerSettingHelper.TRUE;
				} 
			}
		}
		setStatus((tmp[tmp.length-2].equals("true"))?ISchedulerSettingHelper.STARTED:ISchedulerSettingHelper.STOPPED);
		manageArgument(tmp[tmp.length-1]);
		
	}
	
	//New Update bug fix: 7/19/2019
	private void manageArgument(String argument) {	
		String tmp[] = argument.split(Helper.ARGUMENT_DELIMITER);
		
			//for old corntab
		if(tmp.length == 2) {
			this.username = tmp[0];
			setIntegrationname(tmp[1]);
			//if add new field when import old version crontab we need to set default for new field 
			setSchedulebasedcondition(ISchedulerSettingHelper.FALSE);
			setIsdataprocessed(ISchedulerSettingHelper.FALSE);  
			return;
		}
		if(tmp.length <= 4 && tmp[1].contains("salesforce.com/services/Soap/class/skyvvasolutions/IServices")) {
			if(tmp.length>2) {
				this.username = tmp[0];
				setIntegrationname(tmp[2]);
			}
			if(tmp.length>3)setInterfacename(tmp[3]);
			//if add new field when import old version crontab we need to set default for new field 
			setSchedulebasedcondition(ISchedulerSettingHelper.FALSE);
			setIsdataprocessed(ISchedulerSettingHelper.FALSE);  
		
			//for new crontab
		}else {
			this.username = tmp[0];
			setIntegrationname(tmp[1]);
			int index = 2;
			if(tmp.length == 5) {  //if have a new field u just increase current length by 1 (e.g the value "5" increase by 1)
				setInterfacename(tmp[2]);
				index = 3;
			}
			//add new field here by increasing the index by 1 for e.g new will be index+2
			setSchedulebasedcondition(tmp[index]);
			setIsdataprocessed(tmp[index+1]);
		}
	}
	
	public String getCrontabEntry() {
		//we need to replace " " with "%20" when exporting to make the correct format of crontab, once we import we will replace "%20" to the original " "
		//7/19/2019 update
		String integrationName = getIntegrationname().replace(" ", "%20");
		String interfaceName = getInterfacename().replace(" ","%20");
		if(this.direction.equals(INBOUND)) {
			return getCronvalue() + " " + getDirection() + " " + (getStatus().equals(ISchedulerSettingHelper.STARTED) ? "true" : "false") + " " + getUsername() 
				+ Helper.ARGUMENT_DELIMITER + (interfaceName.isEmpty() ? integrationName : integrationName + Helper.ARGUMENT_DELIMITER + interfaceName)
				+ Helper.ARGUMENT_DELIMITER + getSchedulebasedcondition() + Helper.ARGUMENT_DELIMITER + getIsdataprocessed();
		} else {
			if(this.reprocess.equals("true")) {
				return getCronvalue() + " " + getDirection() + " " + (getStatus().equals(ISchedulerSettingHelper.STARTED) ? "true" : "false") + " " + getUsername() 
					+ Helper.ARGUMENT_DELIMITER + (interfaceName.isEmpty() ? integrationName : integrationName + Helper.ARGUMENT_DELIMITER + interfaceName)
					+ Helper.ARGUMENT_DELIMITER + getSchedulebasedcondition() + Helper.ARGUMENT_DELIMITER + getIsdataprocessed();
			} else {
				return getCronvalue() + " " + getDirection() + " false " + (getStatus().equals(ISchedulerSettingHelper.STARTED) ? "true" : "false") + " " + getUsername() 
					+ Helper.ARGUMENT_DELIMITER + (interfaceName.isEmpty() ? integrationName : integrationName + Helper.ARGUMENT_DELIMITER + interfaceName)
					+ Helper.ARGUMENT_DELIMITER + getSchedulebasedcondition() + Helper.ARGUMENT_DELIMITER + getIsdataprocessed();
			}
		}
	}
	
	private String getCronvalue(String cronValue) {
		if(cronValue.contains(INBOUND_CLASS) || cronValue.contains(OUTBOUND_CLASS)) {
			return cronValue.substring(0, cronValue.indexOf("com.iservice.")).trim();
		} else {
			return cronValue.substring(0, (cronValue.contains(INBOUND) ? cronValue.indexOf(INBOUND) : cronValue.indexOf(OUTBOUND))).trim();
		}
	}
	
	private String validate(String entry) throws Exception {
		if(entry.split("\\s+").length<8) throw new Exception("Invalid cron entry:" + entry);
		return entry;
	}
	
	public String getCronvalue(JsonObject jsonObject) {
		String cronexpression = com.iservice.utils.StringUtils.getString(jsonObject,ISchedulerSettingHelper.CRON_EXPRESSION);
		JsonParser parser = new JsonParser();
		JsonObject cronData = (JsonObject) parser.parse(cronexpression);
		setReprocess(com.iservice.utils.StringUtils.getString(cronData,ISchedulerSettingHelper.REPROCESS));
		String daily = getDailyData(jsonObject, cronData);
		String monthly = com.iservice.utils.StringUtils.getString(cronData,ISchedulerSettingHelper.MONTHLY);
		String daysOfWeek = com.iservice.utils.StringUtils.getString(cronData,ISchedulerSettingHelper.DAYS_OF_WEEK);
		String daysOfMonth = com.iservice.utils.StringUtils.getString(cronData,ISchedulerSettingHelper.DAYS_OF_MONTH);
		return daily + " " + daysOfMonth + " " + monthly + " " + daysOfWeek;
	}
	
	public JsonObject getCronExpression() {
		JsonObject jsonObject = new JsonObject();
		String[] cronValue = this.cronvalue.split("\\s+");
		jsonObject = renderDaily(cronValue[0]+" "+cronValue[1]); // daily
		jsonObject.addProperty(ISchedulerSettingHelper.DAYS_OF_MONTH, cronValue[2]);
		jsonObject.addProperty(ISchedulerSettingHelper.MONTHLY, cronValue[3]);
		jsonObject.addProperty(ISchedulerSettingHelper.DAYS_OF_WEEK, cronValue[4]);
		jsonObject.addProperty(ISchedulerSettingHelper.DIRECTION, getDirection());
		jsonObject.addProperty(ISchedulerSettingHelper.STATUS, getStatus());
		jsonObject.addProperty(ISchedulerSettingHelper.REPROCESS, getReprocess());
		return jsonObject;
	}
	
	private JsonObject renderDaily(String data) {
		JsonObject jsonObject = new JsonObject();
		String times[] = data.split(" ");
		String minutes = times[0];
		String hours = times[1];
		
		if(minutes.equals("*")) minutes = "*/1";
		if(hours.equals("*") && minutes.equals("0")) hours = "*/1";
		
		int every = 1;
		int minEveryIndex = minutes.indexOf("/");
		int hrEveryIndex = hours.indexOf("/");
		
		String mins[] = JCrontabHelper.parseRange("*", HourMinute.minute);
		String hrs [] = JCrontabHelper.parseRange("*", HourMinute.hour);
		
		if(!minutes.equals("0")) {//minute
			String[] mm = minutes.split(",");
			if(mm.length>1) {
				every =  Integer.parseInt(mm[1])-Integer.parseInt(mm[0]);
				mins[0] = mm[0];
				mins[1] = mm[mm.length-1];
			}else {
				mm = minutes.split("-");
				if(mm.length>1) {
					every = 1;
					mins[0] = mm[0];
					mins[1] = mm[mm.length-1];
				}else
					every = Integer.valueOf(minutes.substring(minEveryIndex + 1));
			}
			
			String[] hh = hours.split(",");
			if(hh.length>1) {
				hrs[0] = hh[0];
				hrs[1] = hh[hh.length-1];
			}else {
				hh = hours.split("-");
				if(hh.length>1) {
					hrs[0] = hh[0];
					hrs[1] = hh[hh.length-1];
				}
			}
			
			jsonObject.addProperty(ISchedulerSettingHelper.RUN_EVERY_BY, ISchedulerSettingHelper.MINUTES);
	
		}else {//hour
			String[] hh = hours.split(",");
			if(hh.length>1) {
				every =  Integer.parseInt(hh[1])-Integer.parseInt(hh[0]);
				hrs[0] = hh[0];
				hrs[1] = hh[hh.length-1];
			}else {
				hh = hours.split("-");
				if(hh.length>1) {
					every = 1;
					hrs[0] = hh[0];
					hrs[1] = hh[hh.length-1];
				}else
					every = Integer.valueOf(hours.substring(hrEveryIndex + 1));
			}
			
			jsonObject.addProperty(ISchedulerSettingHelper.RUN_EVERY_BY, ISchedulerSettingHelper.HOURS);
			
		}
		
		if(minEveryIndex < 0 && hrEveryIndex <0 && minutes.indexOf("-")<0 && (hours.indexOf(",")<0 && hours.indexOf("-")<0 && !hours.equals("*"))){
			//Run at HH:mm
			jsonObject.addProperty(ISchedulerSettingHelper.RUN_AT, hours + ":" + minutes);
			jsonObject.addProperty(ISchedulerSettingHelper.FREQUENCY, ISchedulerSettingHelper.RUNAT);
			jsonObject.addProperty(ISchedulerSettingHelper.RUN_EVERY, String.valueOf(0));
			jsonObject.addProperty(ISchedulerSettingHelper.RUN_EVERY_BY, "");
			return jsonObject;
		}
		
		jsonObject.addProperty(ISchedulerSettingHelper.FREQUENCY, ISchedulerSettingHelper.RUNEVERYMINUTE);
		jsonObject.addProperty(ISchedulerSettingHelper.RUN_EVERY, String.valueOf(every));
		jsonObject.addProperty(ISchedulerSettingHelper.START_MINUTE, String.valueOf(mins[0]));
		jsonObject.addProperty(ISchedulerSettingHelper.END_MINUTE, String.valueOf(mins[1]));
		jsonObject.addProperty(ISchedulerSettingHelper.START_HOUR, String.valueOf(hrs[0]));
		jsonObject.addProperty(ISchedulerSettingHelper.END_HOUR, String.valueOf(hrs[1]));
		
		return jsonObject;
	}
	
	private String getDailyData(JsonObject jsonObject, JsonObject cronData) {
		String hh = "*";
		String mm = "*";
		String frequency = com.iservice.utils.StringUtils.getString(jsonObject,ISchedulerSettingHelper.FREQUENCY);
		if(StringUtils.equalsIgnoreCase(frequency, ISchedulerSettingHelper.RUNAT)){
			String times[] = com.iservice.utils.StringUtils.getString(cronData,ISchedulerSettingHelper.RUN_AT).split(":");
			hh = times[0];
			mm = times[1];
			
		}else{	
			String runeveryby = com.iservice.utils.StringUtils.getString(cronData,ISchedulerSettingHelper.RUN_EVERY_BY);
			if(StringUtils.equalsIgnoreCase(runeveryby, ISchedulerSettingHelper.MINUTES)){
				mm = JCrontabHelper.retrieveHourMinute(
						com.iservice.utils.StringUtils.getString(cronData,ISchedulerSettingHelper.START_MINUTE),
						com.iservice.utils.StringUtils.getString(cronData,ISchedulerSettingHelper.END_MINUTE),
						com.iservice.utils.StringUtils.getString(jsonObject,ISchedulerSettingHelper.RUN_EVERY), HourMinute.minute, 
						com.iservice.utils.StringUtils.getString(cronData,ISchedulerSettingHelper.RUN_EVERY_BY));
				hh = JCrontabHelper.retrieveHourMinute(
						com.iservice.utils.StringUtils.getString(cronData,ISchedulerSettingHelper.START_HOUR), 
						com.iservice.utils.StringUtils.getString(cronData,ISchedulerSettingHelper.END_HOUR),
						com.iservice.utils.StringUtils.getString(jsonObject,ISchedulerSettingHelper.RUN_EVERY), HourMinute.hour, 
						com.iservice.utils.StringUtils.getString(cronData,ISchedulerSettingHelper.RUN_EVERY_BY));
			}else{
				mm = "0";
				hh = JCrontabHelper.retrieveHourMinute(
						com.iservice.utils.StringUtils.getString(cronData,ISchedulerSettingHelper.START_HOUR),
						com.iservice.utils.StringUtils.getString(cronData,ISchedulerSettingHelper.END_HOUR),
						com.iservice.utils.StringUtils.getString(jsonObject,ISchedulerSettingHelper.RUN_EVERY), HourMinute.hour, 
						com.iservice.utils.StringUtils.getString(cronData,ISchedulerSettingHelper.RUN_EVERY_BY));
			}
		}
		return mm + " " + hh;
	}	
}	