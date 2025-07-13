package com.iservice.helper;

import com.iservice.gui.helper.Helper;

public class CronEntryBean {
	
	private String schedule;
	private String classname;
	private String username;
	private String endpoint;
	private String integrationName;
	private String interfaceName;
	private String entry;
	private String mode;
	private boolean isON;
	
	public static String INBOUND="INBOUND", OUTBOUND="OUTBOUND", OUTBOUND_REPROCESS="OUTBOUND_REPROCESS"; 
	public static String OUTBOUND_CLASS = "com.iservice.integration.IntegrationSF2AgentService";
	public static String INBOUND_CLASS = "com.iservice.integration.IntegrationOnDemandService";
	
	/**
	 * Valid Cron:
	 * Inbound (On) : * * * * * OnDemandService true username¤endpoint¤integrationName[¤interfaceName]
	 * Inbound (Off): * * * * * OnDemandService false username¤endpoint¤integrationName[¤interfaceName]
	 * Outbound (On) : * * * * * SF2AgentService false true username¤endpoint¤integrationName[¤interfaceName]
	 * Outbound (Off): * * * * * SF2AgentService false false username¤endpoint¤integrationName[¤interfaceName]
	 * Outbound-Reprocess(On) : * * * * * SF2AgentService true username¤endpoint¤integrationName[¤interfaceName]
	 * Outbound-Reprocess(Off): * * * * * SF2AgentService false username¤endpoint¤integrationName[¤interfaceName]
	 * @param entry
	 * @throws Exception
	 */
	public CronEntryBean(String entry) throws Exception {
		setEntry(entry);
	}
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getEndpoint() {
		return endpoint;
	}
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	public String getSchedule() {
		return schedule;
	}
	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}
	public String getClassname() {
		return classname;
	}
	public void setClassname(String classname) {
		this.classname = classname;
	}
	public String getIntegrationName() {
		return integrationName;
	}
	public void setIntegrationName(String integrationName) {
		this.integrationName = integrationName.replace("%20", " ");
	}
	public String getInterfaceName() {
		return interfaceName;
	}
	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName.replace("%20", " ");
	}
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	public boolean isON() {
		return isON;
	}
	public void setON(boolean isON) {
		this.isON = isON;
	}
	public String getEntry() {
		return entry;
	}
	/**
	 * Set entry will manage all other properties
	 * @param entry
	 */
	public void setEntry(String entry) throws Exception{
		this.entry = validate(entry);
		manageEntry();
	}
	/**
	 * @param entry
	 * @return
	 * @throws Exception
	 */
	private String validate(String entry) throws Exception {
		if(entry.split("\\s+").length<8) throw new Exception("Invalid cron entry:" + entry);
		return entry;
	}
	private void manageEntry() {
		String[] tmp = this.entry.split("\\s+");
		this.schedule = getCronScheduleCode(this.entry);
		if(this.entry.contains(INBOUND_CLASS)) {
			// inbound
			this.mode = INBOUND;
			this.classname = INBOUND_CLASS;
		}else {
			// outbound
			this.classname = OUTBOUND_CLASS;
			if(tmp.length == 9) {
				this.mode = OUTBOUND;
			}else {
				// reprocess
				this.mode = OUTBOUND_REPROCESS;
			}
		}
		this.isON = (tmp[tmp.length-2].equals("true"))?true:false;
		manageArgument(tmp[tmp.length-1]);		
	}
	/**
	 * username¤endpoint¤integrationName[¤interfaceName]
	 * @param argument
	 */
	private void manageArgument(String argument) {
		String tmp[] = argument.split(Helper.ARGUMENT_DELIMITER);
		if(tmp.length>2) {
			setUsername(tmp[0]);
			setEndpoint(tmp[1]);
			setIntegrationName(tmp[2]);
		}
		if(tmp.length>3)setInterfaceName(tmp[3]);
		
	}
	private String getCronScheduleCode(String cronValue) {
		return cronValue.substring(0, cronValue.indexOf("com.iservice.")).trim();
	}
	@Override
	public String toString() {
		return  "entry:" + this.getEntry() +
				"\nschedule:" + this.getSchedule() +
				"\nclassname:" + this.getClassname() +
				"\nusername:" + this.getUsername() +
				"\nendpoint:" + this.getEndpoint() +
				"\nintegrationName:" + this.getIntegrationName() +
				"\ninterfaceName:" + this.getInterfaceName() +
				"\nmode:" + this.getMode() +
				"\nisON:" + this.isON();
	}
	public static void main(String[] args) throws Exception {
	
		CronEntryBean ceb = new CronEntryBean("18 11 * * * com.iservice.integration.IntegrationOnDemandService false ponnreay@test.com¤https://ap2.salesforce.com/services/Soap/class/skyvvasolutions/IServices¤Test%20AAA¤XX");
		
		System.out.println(ceb);
	}
}
