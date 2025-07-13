package com.iservice.task;

import static java.lang.management.ManagementFactory.getMemoryMXBean;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.LimitExceededException;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;
import com.iservice.adapter.ConnectionFactory;
import com.iservice.adapter.DatabaseTypeHelper;
import com.iservice.adapter.IDBConnection;
import com.iservice.database.BaseDao;
import com.iservice.database.LogDao;
import com.iservice.database.PropertySettingDao;
import com.iservice.gui.data.ILogs__c;
import com.iservice.gui.data.IMessage__c;
import com.iservice.gui.data.ISFIntegrationObject;
import com.iservice.gui.data.Integration__c;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.gui.helper.DirectionTypeHelper;
import com.iservice.gui.helper.Helper;
import com.iservice.gui.helper.PropertyNameHelper;
import com.iservice.gui.helper.ScheduleModeHelper;
import com.iservice.helper.FileHelper;
import com.iservice.helper.FolderUtils;
import com.iservice.helper.SFIntegrationObjectUtils;
//import com.iservice.jcron.CronExpressionBean;
import com.iservice.model.ISchedulerSetting;
import com.iservice.sforce.ISFService;
import com.iservice.sforce.MapSFConnInfo;
import com.iservice.sforce.SFIntegrationService;
import com.iservice.sforce.SFService;
import com.iservice.sqlite.SQLite;
import com.iservice.xmlparser.XmlField;
import com.iservice.xmlparser.XmlNode;
import com.model.iservice.Adapter;
import com.model.iservice.Property;
import com.sforce.soap.partner.DeleteResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.UpsertResult;
import com.sforce.soap.partner.fault.ApiFault;
import com.sforce.soap.partner.fault.InvalidFieldFault;
import com.sforce.soap.partner.fault.InvalidSObjectFault;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.soap.schemas._class.IServices.IBean;

/**
 * LOGS
 * 
 * 20012011: Add function Last Data on the Interface (Overview Page)
 * 20012011: SF2Agent >10000 records
 * 
 * 24012011: Fine tune method traceError2() for create Logs
 * 			 Test $PARENTLOOKUP() function in Agent2SF interface's SELECT statement
 * 			 Test SF2Agent Last Data	   		
 * 			 Test SF2Agent query(); queryMore()
 * 
 * 25012011: Fine tune error logic handling for $PARENTLOOKUP 
 * 				(check parentInterfaceId, check foreignTableExternalFieldName in the parent query list)	
 * 
 * 23022011: Search() and integrate() for Professional Edition
 * 
 * 
 * 20110725 method integrate() 
 * 				- enable support of $PARENTLOOKUP with the multiple interface adapter
 *				- use mapAdapterInterface instead of mapAdapter
 *				- release from memory
 *			IntegrationThread - run() : release from memory
 *			method traceError(); createLogs() : release from memory
 * 
 * 
 * 20110805 clearMessageAfterProcessing (v1.12): will support with Integration Suite v1.52
 * 			Case 00002234 - RE: Processing taking extremely long?
 * 
 * 
 **/

public class GenericSFTask {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(GenericSFTask.class);
	
	/*
		"1.7.3"; 	//with UI, Last Data
	 	"1.7.4.1"; 	//with updated UI (update data source), update source data
	 	"1.7.4.2"; 	//fine tune code update UI; change iodservice to skyvvaagent
	 	"1.7.4.3"; 	//24012011 SF2Agent>10000records; SF2Agent last data; Agent2SF $ILOOKUP
	 	"1.8"; 		//24022011
	 	"1.9";		//23032011
	 	"1.10";		//10052011
	 	"1.11";		//16062011
	 	"1.12";		//28072011
	 	"1.13";		//01-11-2011
	 	"1.14";		//21-11-2011
	 	"1.15";		//06-12-2011
	 	"1.16";		//06-04-2012
	  	"1.17";		//17-05-2012
	  	"1.18";		//6-02-2013
	  	"1.19"; 	//21-06-2013
	  	"1.20"; 	//12-08-2013
	  	"1.21"; 	//06-09-2013
	  	"1.22"; 	//30-09-2013
	  	"1.23"; 	//24-10-2013
	  	"1.24"; 	//01-11-2013
	  	"1.25"; 	//22-11-2013
	  	"1.26"; 	//10-12-2013
	  	"1.27"; 	//17-01-2014
	  	"1.28"; 	//20-02-2014
	  	"1.29"; 	//13-03-2014
	  	"1.30"; 	//29-04-2014
	  	"1.31"; 	//19-08-2014
	  	"1.32"; 	//10-12-2014
	  	"1.33"; 	//06-02-2015
	  	"1.34"; 	//25-03-2015
	  	"1.35"; 	//27-05-2015
	  	"1.36"; 	//21-07-2015
	  	"1.38";     //29-09-2015
	  	"1.39";     //09-02-2016
	  	"1.40";		//01-03-2016
	  	"1.41";		//13-06-2016
	  	"1.42"; 	//04-07-2016
	  	"1.43";		//12-07-2016
	  	"1.44";     //26-07-2016
	  	"1.45";     //16-12-2016
	  	"1.46";     //06-06-2017
	  	"1.47";     //06-03-2018
	  	"1.47.0.1"; //29-03-2018
 	  	"1.47.0.2"; //24-04-2018
 	  	"1.47.0.3"; //16-05-2018
 	  	"1.47.0.4"; //29-05-2018
 	  	"1.47.0.5"; //31-07-2018
 	  	"1.47.0.6"; //20-08-2018
 	  	"1.48.0.0"; //07-12-2018
 	  	"1.47.0.9"; //11-01-2019
 	  	"1.47.1.0"; //23-04-2019
 	  	"1.47.1.1"; //20-05-2019
 	  	"1.47.1.2"; //19-06-2019
 	  	"1.47.1.3"; //19-02-2020
 	  	"1.47.1.4"; //16-04-2020
 	  	"1.47.1.5"; //01-07-2020
 	  	"1.49.0.0"; //28-06-2019
 	  	"1.49.0.1"; //08-10-2019
 	  	"1.49.0.2"; //21-10-2019
 	  	"1.49.0.3"; //18-12-2019
 	  	"1.49.0.4"; //06-01-2020
 	  	"1.49.0.5"; //20-01-2020
 	  	"1.50"; //10-06-2020
	 */

	public static String version ="1.51.6";
	public static class GenericSFTaskException extends Exception {
		private static final long serialVersionUID = -8450362584607773808L;
		public GenericSFTaskException(String msg) {
			super(msg);
		}
	}
	
	public static boolean DEMO32 = false;//set this to true for work with development package false true

	// 17-08-2017
	public static final String TMP_FILE ="tmpfiles";
	
	private String scheduledIntegrationId;
	private String scheduledInterfaceId;
	
	private List<ILogs__c> ilogs = new ArrayList<ILogs__c>();	

	protected MapSFConnInfo mapSFConnInfo = null;
	public MapSFConnInfo getMapSFConnInfo() {
		return mapSFConnInfo;
	}
	public void setMapSFConnInfo(MapSFConnInfo mapSFConnInfo) {
		this.mapSFConnInfo = mapSFConnInfo;
	}
	
	protected ISchedulerSetting iSchedulerSetting = null;
	public ISchedulerSetting getiSchedulerSetting() {
		return iSchedulerSetting;
	}
	public void setiSchedulerSetting(ISchedulerSetting iSchedulerSetting) {
		this.iSchedulerSetting = iSchedulerSetting;
	}

	private String scheduledPropertyFile = null;
	private static int PACKET = 20;
	private String sessionID = null;
	public boolean isFirstLoad = true;
	
	protected SFIntegrationService sfIntegrationService;
	
	//17-11-2011
	public static final String LAST_INTEGRATION_DATETIME="Integration_Operation_Cache.txt";
	
	static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	//For Integrate SF - Agent
	public static Set<String> supportedBatchExecutors = new HashSet<String>();
	
	//20012011
	private Map<Interfaces__c, String> mapInterfaceLastData = new HashMap<Interfaces__c, String>();
	public static final String SF_AGENT = "SF-AGENT";
	public static final String AGENT_SF = "AGENT-SF";
	
	//28-04-2017		
	private List<String> lstInterfaceId = new ArrayList<String>();

	static {
		supportedBatchExecutors.add(DatabaseTypeHelper.ORACLE);
		supportedBatchExecutors.add(DatabaseTypeHelper.SQL_SERVER);
		supportedBatchExecutors.add(DatabaseTypeHelper.MYSQL);
		supportedBatchExecutors.add(DatabaseTypeHelper.MS_ACCESS);
		supportedBatchExecutors.add(DatabaseTypeHelper.POSTGRESQL);
	}
	int numberInterface__Processed = 0;

	public IBean[][] addFilter(String setup, String key, String id, IBean[][] resources) {

		IBean[] beans = readFilter(resources, setup);
		if (beans != null) {
			for (int j = 0; j < beans.length; j++) {
				if (beans[j].getName().equals(key)) {
					beans[j].setValue(id);
					break;
				}
			}
		}

		// No session filter in the resources
		// add new session filter
		if (beans == null) {
			beans = createBeans(setup, key, id);
			Integer n = (resources == null ? 1 : resources.length + 1);
			IBean[][] temp = new IBean[n][];
			if (resources != null) {
				System.arraycopy(resources, 0, temp, 0, resources.length);
			}
			temp[n - 1] = beans;
			resources = temp;
			temp = null;
		}
		return resources;
	}
	
	public IBean[] createBeans(String setup, String key, String value) {
		IBean[] bean = new IBean[2];
		IBean sessSetup = new IBean(setup, setup);
		IBean sessVal = new IBean(key, value);
		bean[0] = sessSetup;
		bean[1] = sessVal;
		return bean;
	}
	
	public IBean[] readFilter(IBean[][] resources, String key) {
		if (resources != null) {
			for (IBean[] bs : resources) {
				if (bs != null) {
					// for each filter
					for (int j = 0; j < bs.length; j++) {
						IBean b = bs[j];
						if (b != null && b.getName().equals(key)) {
							return bs;
						}
					}
				}
			}
		}
		return null;
	}
	
	public MapSFConnInfo getResource() {
		if (mapSFConnInfo == null) {
			mapSFConnInfo = BaseDao.getDao(PropertySettingDao.class).getAllValueByFileName(getScheduledPropertyFile());
		}
		return mapSFConnInfo;
	}
	
	public MapSFConnInfo getResource(String filename) {
		return BaseDao.getDao(PropertySettingDao.class).getAllValueByFileName(filename);
	}
	
	public GenericSFTask() throws Exception{}
	
	public GenericSFTask(ISchedulerSetting iSchedulerSetting) {
		setiSchedulerSetting(iSchedulerSetting);
	}
	
	public static void setSF_CONNECT_MAX_TIMEOUT(Map<String,String> res) {
		if(res==null) return;
		try {
			String sftimeout = res.get("SF_CONNECT_MAX_TIMEOUT");
			LOGGER.trace(">setSF_CONNECT_MAX_TIMEOUT sftimeout:" + sftimeout);
			if( sftimeout!=null && !sftimeout.trim().isEmpty() ) {
				SFService.TIMEOUT = Integer.parseInt(sftimeout);
			}
		} catch (Exception e) {
		}
	}
	public void setPacket(String p) {

		try {
			PACKET = Integer.valueOf(p).intValue();
		} catch (Exception e) {

			// default
			PACKET = 20;
		}
	}

	public static String buildSet(Set<String> ids, String query) {

		StringBuffer res = new StringBuffer();

		if (ids != null) {

			for (Iterator<String> iter = ids.iterator(); iter.hasNext();) {

				String element = iter.next();
				res.append(element);
				res.append(",");

			}
		}

		String result = res.toString();

		int index = result.lastIndexOf(",");
		if (index != -1 && !result.equals("")) {

			result = result.substring(0, index);

		}

		return query.replaceAll("$1", result);

	}
	
	/**
	 * Agent Schedule Service
	 */
	public void doIntegrationService() {
		
		try {
			
			setRunTime(Calendar.getInstance());

			LOGGER.trace(">doIntegrationService > SchedulerType: " + iSchedulerSetting.getSchedulertype());

			setMapSFConnInfo(BaseDao.getDao(PropertySettingDao.class).getAllValues(iSchedulerSetting.getUsername()));

			if(mapSFConnInfo != null && !mapSFConnInfo.isEmpty()){
				
				Integration__c integration = getIntegrationToSchedule();
				
				LOGGER.trace(">doIntegrationService > Start with SchedulerType: " + iSchedulerSetting.getSchedulertype());
				
				if(StringUtils.isNotBlank(iSchedulerSetting.getInterfaceid()) && StringUtils.isNotBlank(iSchedulerSetting.getInterfacename())) {
					if(!iSchedulerSetting.getSchedulertype().contains(ISchedulerSetting.PROCESS_PER_INTERFACE_GROUP)) {
						LOGGER.trace(">doIntegrationService > Schedule per InterfaceName: "+ iSchedulerSetting.getInterfacename() + " >in IntegrationName: " + iSchedulerSetting.getIntegrationname());
					}else {
						LOGGER.trace(">doIntegrationService > Schedule per InterfaceGroupName: "+ iSchedulerSetting.getInterfacename() + 
								" >Type: "+integration.getInterface_groups().get(0).getType__c()+" >Direction: "+integration.getInterface_groups().get(0).getDirection_Type__c()+" >in IntegrationName: " + iSchedulerSetting.getIntegrationname());
					}
				}else {
					LOGGER.trace(">doIntegrationService > Schedule per IntegrationName: "+ iSchedulerSetting.getIntegrationname());
				}
				
				setScheduledIntegrationId(iSchedulerSetting.getIntegrationid());
				if(!iSchedulerSetting.getSchedulertype().contains(ISchedulerSetting.PROCESS_PER_INTERFACE_GROUP)) setScheduledInterfaceId(iSchedulerSetting.getInterfaceid());
				
				if(StringUtils.equalsIgnoreCase(iSchedulerSetting.getDirection(), DirectionTypeHelper.INBOUND)) {
					integrationInBound(integration, iSchedulerSetting);
				}else {
					integrationOutBound(integration, iSchedulerSetting);
				}
				
				LOGGER.trace(">doIntegrationService > End");
				
			}else {
				LOGGER.trace(">doIntegrationService > Invalid Username: " + iSchedulerSetting.getUsername() + " in propertysetting.");
			}
			
		}catch (Exception ex) {
			if (SFService.isErrorConnection(ex)) {
				LOGGER.error(">doIntegrationService> Error Connection!");
			}
			else {
				LOGGER.error(">doIntegrationService > error: "+ ex, ex);
				LOGGER.error(">doIntegrationService > error: "+ ex, ex);
			}
		}
	}

	/**
    * Schedule Agent-SF
    */
	public void integrationInBound(Integration__c integration,ISchedulerSetting iSchedulerSetting) {
	
		boolean stillProcessing = false;
		boolean runPerIntegration = StringUtils.isBlank(getScheduledInterfaceId());
		//20111116 v.14 Initial ilogs list
		if(ilogs==null) {
			ilogs = new ArrayList<ILogs__c>();
		}
		else {
			ilogs.clear();
		}
		
		//clear interfaceId for update interface last next run date
		lstInterfaceId.clear();

		try {
			
			LOGGER.trace(">integrationInBound() >IntegrationName: " + integration.getName() + " >Start");
			
			//has interface(s)
			if(integration!=null && integration.getInterfaces().size()>0) {
				
				if(runPerIntegration) {
					//prevent integration running overlap
					if(FolderUtils.isFileExisting(TMP_FILE + "\\" + integration.getId())){
						LOGGER.trace(">integrationAgent2SF() >Integration Name: "+integration.getName()+" >Still Processing.");
						stillProcessing = true;
						return;
					}
					// create empty file with integrationId as name, integration in processing
					new File(TMP_FILE + "\\" +integration.getId()).createNewFile();
				}
				
				String lastruntimeFileName = FileHelper.getFilename(GenericSFTask.LAST_INTEGRATION_DATETIME);
				LastRunDateCache lastRunDateCache = FileHelper.readLastRunDateCache(lastruntimeFileName);
				
				if(lastRunDateCache == null){//in case do new integration (File not found)
					lastRunDateCache = new LastRunDateCache();
				}	
				
				integrationAgent2SF(integration, lastRunDateCache); 
				
				//17-11-2011 Create Last run data cache
				//add to cache file
				FileHelper.saveLastRunDateCache(lastruntimeFileName, lastRunDateCache);
				
				try{
					if(iSchedulerSetting.getSchedulertype().contains(ISchedulerSetting.PROCESS_PER_INTERFACE_GROUP)){
						updateInterfaceGroupLast_Next_Rundate(integration, sfIntegrationService.getISFService(), iSchedulerSetting);
					}else if(StringUtils.isBlank(getScheduledInterfaceId())) {
						updateIntegrationLast_Next_Rundate(integration, sfIntegrationService.getISFService(), iSchedulerSetting);
					}else {
						updateInterface_Last_Next_Rundate(integration, sfIntegrationService.getISFService(), iSchedulerSetting);
					}
				}catch(Exception ex){ 
					if(ex instanceof ApiFault) {
						if(((ApiFault) ex).getFaultCode().toString().contains("INVALID_FIELD")){
							LOGGER.warn("integrationInBound() >updateAgentLast_Next_Rundate >Warning: "+((ApiFault) ex).getFaultCode().toString());
						}
						else throw ex;
					}
					else throw ex;
				}
			
				saveLogs2SFAttachment(scheduledIntegrationId,sfIntegrationService.getISFService(), lastRunDateCache);
			}
			else {
				throw new Exception("integrationInBound() >Integration name: "+integration.getName()+", has no valid interfaces available. Make sure its Inbound interface(s) are Deployed/Activated.");
			}
		} 
		catch (Throwable e) {
			if(SFService.isErrorConnection(e)) {
				LOGGER.error(">GenericSFTask> integrationInBound> Error Connection!");
			}
			else
			// 05082010  manage log in batch
			ilogs.add(traceError2(sfIntegrationService.getISFService(), "ISERVICE", e));

		}
		finally {
			if(!stillProcessing && runPerIntegration) deleteCacheRunning(integration.getId());
				
			try {
				createLogs(ilogs, (SFService) sfIntegrationService.getISFService(), new MapIntegrationInfo(sfIntegrationService, integration, null, runTime));
			} 
			catch (Exception e) {
				LOGGER.error(">createLogs> Error: " + e.getMessage(),e);
			}
		
			LOGGER.trace(">integrationInBound() >IntegrationName: " + integration.getName() + " >Done");
			// release memory
			System.gc();
			System.runFinalization();
		}
	}

	protected void integrationAgent2SF(Integration__c integration, LastRunDateCache integrationOperationCache)throws Exception{
		
		new AgentRestService().checkLicenseKey(sfIntegrationService);
		
		if (sessionID != null) {
			sfIntegrationService.getISFService().setSessionId(sessionID);
		}
		
		List<Interfaces__c> lstInterfaces = new ArrayList<>();
		List<Interfaces__c> lstIntf = integration.getInterfaces();
		
		for(ISFIntegrationObject intf: lstIntf) {
			if(intf instanceof Interfaces__c) {
				// schedule per Integration
				if(StringUtils.isBlank(scheduledInterfaceId)) {
					if(((Interfaces__c) intf).getType__c().equalsIgnoreCase(DirectionTypeHelper.INBOUND)) {
						lstInterfaces.add((Interfaces__c) intf);
					}
				}
				// schedule per Interfaces
				else {
					if(((Interfaces__c) intf).getId().equalsIgnoreCase(scheduledInterfaceId)) {
						lstInterfaces.add((Interfaces__c) intf);
						break;
					}
				}
			}
		}
		if(iSchedulerSetting.getSchedulertype().contains(ISchedulerSetting.PROCESS_PER_INTERFACE_GROUP)) {
			if(integration.getInterface_groups().get(0).getType__c().equalsIgnoreCase("EO"))  {
				//process all interfaces at the same time when InterfaceGroup set type="EO"
				for (Interfaces__c intf : lstInterfaces)  {
					Thread thread = new Thread(new Runnable() {
						@Override
						public void run() {
							processOneInterface__Inbound(integration, intf, integrationOperationCache);
						}
					});
					thread.start();
				}
				// wait to finish all interfaces
				while(numberInterface__Processed!=lstInterfaces.size()) Thread.sleep(1000);
			}else {
				//"EOIO" process one by one and if one interface fail let's end integrate
				for (Interfaces__c intf : lstInterfaces)  {
					if(!processOneInterface__Inbound(integration, intf, integrationOperationCache)) break;
				}
			}
		}else {
			//process one by one (old) 
			for (Interfaces__c intf : lstInterfaces)  {
				processOneInterface__Inbound(integration, intf, integrationOperationCache);
			} 
		}
	}
	
	@SuppressWarnings("unused")
	private boolean processOneInterface__Inbound(Integration__c integration, Interfaces__c intf, LastRunDateCache integrationOperationCache) {
//		String interfaceName = "";
//		String interfaceId = "";
//		boolean initialization = false;
//		boolean isIntegrateSuccess = false;
//		//for one interface
//		RoutingLogFileName(integration.getName()+"_"+integration.getId());
//		try {
//			
//			// MapIntegrationInfo
//			MapIntegrationInfo mapIntgInfo = new MapIntegrationInfo(sfIntegrationService, integration, intf, getRunTime());
//			interfaceId = mapIntgInfo.getInterfaceId();
//			interfaceName = mapIntgInfo.getInterfaceName();
//			initialization = mapIntgInfo.isInitialization();
//			
//			if(intf.getStatus__c().equalsIgnoreCase("Development")) {
//				throw new GenericSFTaskException("Please change the status from Development to Deployed!");
//			}
//			
//			if(StringUtils.isEmpty(scheduledInterfaceId) && isInterfaceScheduled(integration.getName(),interfaceName)) {
//				LOGGER.trace(">integrationAgent2SF() >InterfaceName: "+interfaceName+" >has its own schedule.");
//				return false;
//			}
//
//			// prevent processing overlap, if interface still in processing skip the loop #2569
//			if(interfaceId!=null && !interfaceId.equals("") && FolderUtils.isFileExisting(TMP_FILE + "\\" + interfaceId)){
//				LOGGER.trace(">integrationAgent2SF() >InterfaceName: "+interfaceName+" >Still Processing.");
//				return false;
//			}
//			// create empty file with interfaceId as name, interface in processing
//			new File(TMP_FILE + "\\" +interfaceId).createNewFile();
//			
//			IDBConnection adapter = new CoreIntegration().getAdapter(mapIntgInfo.getAdapterProperties());
//			
//			LOGGER.trace(">integrationAgent2SF() >InterfaceName: " + interfaceName +" >Start");
//			LOGGER.trace(">integrationAgent2SF() >AdapterName: " + mapIntgInfo.getAdapterName());
//			LOGGER.trace(">integrationAgent2SF() >Initialization Mode: " + initialization);
//
//			// check folder 
//			if(adapter instanceof FileMapAdapter || adapter instanceof FTPAdapter) {
//				if(StringUtils.isBlank(adapter.getAdapter().getPropertyValue(PropertyNameHelper.FOLDER))) {
//					throw new Exception("InBound processing with adapter name: "+mapIntgInfo.getAdapterName()+" specify the adapter property 'Folder'.");
//				}	
//			}
//			
//			//add interfaceId for update interface last next run date
//			lstInterfaceId.add(interfaceId);
//
//			// construct the dynamic query
//			String query = mapIntgInfo.getQuery();
//			
//			// interface no query, insert log with JDBC adapter type
//			if (mapIntgInfo.getAdapterType() != null && mapIntgInfo.getAdapterType().contains(AdapterTypeHelper.JDBC)) {
//				if (query == null || query.trim().equals("")) {
//					String msg = "No query statement!";
//					throw new GenericSFTaskException(msg);
//				}
//			}
//			
//			//reset catch
//			if(adapter instanceof FileMapAdapter){
//				((FileMapAdapter)adapter).reset();
//			}
//			String lastRunDate = dateFormat.format(new Date());
//			
//			isIntegrateSuccess = adapter.doInBoundIntegration(query,mapIntgInfo);
//			
//			
//			// schedule based condition(the schedule will be not add to jopTask if it is integrated successfully for today)
//			if(isIntegrateSuccess & iSchedulerSetting.getSchedulebasedcondition().equals(ISchedulerSettingHelper.TRUE)) {
//				List<ISchedulerSetting> listISchedulerSetting = new ArrayList<ISchedulerSetting>();
//				listISchedulerSetting.add(iSchedulerSetting);
//				ISchedulerSettingDao.changeIschedulerSettingIsDataProcess(listISchedulerSetting,ISchedulerSettingHelper.TRUE,
//						ISchedulerSettingHelper.STOPPED,sfIntegrationService);
//			    LOGGER.trace(">Scheduler of Interface: "+iSchedulerSetting.getInterfacename()+": processed successfully> and ischedulesetting's status is changed to --stopped-- based on condition!");
//			}				
//			
//			//if no error save last runtime
//			if(interfaceId!=null) {
//				integrationOperationCache.getmIntfLastRunDate().put(interfaceId, lastRunDate);
//			}
//			
//			if(!isIntegrateSuccess) deleteCacheRunning(interfaceId);
//			
//		} catch (Throwable e1) {
//			isIntegrateSuccess = false;
//			if(e1 instanceof FileMapAdapter.WarningFileNotFoundException) {
//				LOGGER.warn(">integrationAgent2SF() >InterfaceName: " + interfaceName + ">Warning:\n" + e1.getMessage());
//			}
//			else if(e1 instanceof GenericSFTaskException) {
//				LOGGER.warn(">integrationAgent2SF() >InterfaceName: " + interfaceName + ">Warning:\n" + e1.getMessage());
//			}
//			else if(SFService.isErrorConnection(e1)) {
//				LOGGER.error(">integrationAgent2SF() >Error Connection!");
//			}
//			else {
//				LOGGER.error(">integrationAgent2SF() >InterfaceName: " + interfaceName + ">Error:\n" + e1.getMessage());
//				// 05082010 manage log in batch
//				ilogs.add(traceError2(sfIntegrationService.getISFService(), interfaceName, e1));
//			}
//			if(interfaceId!=null && !interfaceId.equals("")){
//				//delete interfaceId file: interface processing finished
//				if(FolderUtils.isFileExisting(TMP_FILE + "\\" + interfaceId)){
//					FolderUtils.deleteFile(TMP_FILE + "\\" + interfaceId);
//				}
//			}
//								
//		} 
//		//20111116 v1.14 initial interface query
//		finally {
//			if(interfaceId!=null && !interfaceId.equals("") && isIntegrateSuccess){
//				//delete interfaceId file: interface processing finished
//				if(FolderUtils.isFileExisting(TMP_FILE + "\\" + interfaceId)){
//					FolderUtils.deleteFile(TMP_FILE + "\\" + interfaceId);
//				}
//			}
//			if(initialization && interfaceId!=null && !interfaceId.equals("") && isIntegrateSuccess){
//				String pref = ">IntegrationAgent2SF() ";
//				try {
//					LOGGER.trace(pref + ">updateInitializationInterfaces >InterfaceIds: "+Arrays.asList(interfaceId));
//						updateInitializationInterfaces(Arrays.asList(interfaceId), sfIntegrationService.getISFService());
//						intf.setInitialization__c(false);
//						LOGGER.trace(pref + ">Update Initialization Mode >Done");
//				} catch (Exception e) {
//					LOGGER.error(pref + ">Update Initialization Mode >Error: " + e.getMessage());
//				}
//			}
//			LOGGER.trace(">integrationAgent2SF() >InterfaceName: " + interfaceName + (isIntegrateSuccess?">Done":">Fail")+"\n");
//			numberInterface__Processed ++;
//		}
//		return isIntegrateSuccess;
		return true;
	}
	
	private void deleteCacheRunning(String cacheName) {
		if(cacheName!=null && !cacheName.equals("")){
			//delete interfaceId file: interface processing finished
			if(FolderUtils.isFileExisting(TMP_FILE + "\\" + cacheName)){
				FolderUtils.deleteFile(TMP_FILE + "\\" + cacheName);
			}
		}
	}

	// 17-08-2017 create directory to store tmp files
	public static void createDirTMP_FILE() {
		deleteDirTMP_FILE();
		File dirtmpfile = new File(TMP_FILE);
		dirtmpfile.mkdir();
	}
	public static void deleteDirTMP_FILE() {
		File dirtmpfile = new File(TMP_FILE);
		if(dirtmpfile.exists()) {
			if(!dirtmpfile.delete()) {
				String[] entries = dirtmpfile.list();
				for(String entry : entries) {
					File currentFile = new File(dirtmpfile.getPath(),entry);
					currentFile.delete();
				}
				dirtmpfile.delete();
			}
		}
	}

	//20111116 v.14 Initial interfaces list to be updated
	/* Logic:
		 3.       Initial Query: When initial query is checked, it should only run once and then use the �normal� query. 
		 The process to change �initial� and delete the filter.tmp all the time is too time consuming 
		 and needs always involvement of the customer.
	 */
	public void updateInitializationInterfaces(List<String> interfaceIds, ISFService sfService) throws Exception {
		
		String nsPrefix = sfService.getQueryPackage(); 
		PartnerConnection sfPartner = sfService.getSFPartner();
		
		SObject[] sos = new SObject[interfaceIds.size()]; 
		for(int i=0;i<interfaceIds.size();i++){ 
			SObject so = new SObject();
			so.setType(nsPrefix + "Interfaces__c");
			so.setField("Id", interfaceIds.get(i));
			so.setField(nsPrefix + "Initialization__c", false);
			sos[i] = so; 
		} 
		
		UpsertResult[] ur = sfPartner.upsert("Id", sos);
		StringBuffer strErrors = new StringBuffer();
		if(ur != null) {		
			for(int i=0; i<ur.length; i++){
				if(!ur[i].isSuccess() && ur[i].getErrors()!=null){
					for(int j=0; j<ur[i].getErrors().length; j++){
						strErrors.append(ur[i].getId() + " : " + ur[i].getErrors()[j].getMessage() + "\n");
					}
				}
			}
		}
		if(strErrors.length()>0) {
			LOGGER.error(">updateInitialInterfaces> upsert:Error:"+strErrors);
		}
			
	}
	
	//20-04-2017 v.1.46 Agent Last/Next Run Date Interface
//	@SuppressWarnings("static-access")
//	private  void updateInterface_Last_Next_Rundate(String interfaceId, ISFService sfService, ISchedulerSetting iSchedulerSetting) throws Exception{
//		int timeRetry = 0;
//		int maxNumberOfRetry = sfService.MAX_NUMBER_OF_RETRY_DEFAULT;
//		int retryTimeInterval= sfService.RETRY_TIME_INTERVAL_DEFAULT;
//		
//		maxNumberOfRetry  = sfService.getMaxRetryNumber(sfIntegrationService.getIntegrationById(iSchedulerSetting.getIntegrationid()), sfIntegrationService.getInterfaceById(interfaceId)); 
//		retryTimeInterval = sfService.getRetryTimeInterval(sfIntegrationService.getIntegrationById(iSchedulerSetting.getIntegrationid()), sfIntegrationService.getInterfaceById(interfaceId));
//		
//		String nsPrefix = sfService.getQueryPackage();
//		PartnerConnection sfPartner = sfService.getSFPartner();
//		
//		SObject objIntf = new SObject( nsPrefix + "Interfaces__c");
//		objIntf.setId(interfaceId);
//		
//		objIntf.setField( nsPrefix + "LastRun__c", retrieveLast_RunDate());
//		objIntf.setField( nsPrefix + "Next_Run__c", retrieveNext_RunDate(iSchedulerSetting));
//		
//		SObject[] arrSObject=new SObject[1]; 
//		arrSObject[0] = objIntf;
//		
//		SaveResult[] srIntf = doUpdateSObject(arrSObject, sfPartner, timeRetry, maxNumberOfRetry, retryTimeInterval);
//		StringBuffer strErrors = new StringBuffer();
//		if(srIntf != null) {		
//			for(int i=0; i<srIntf.length; i++){
//				if(!srIntf[i].isSuccess() && srIntf[i].getErrors()!=null){
//					for(int j=0; j<srIntf[i].getErrors().length; j++){
//						strErrors.append(srIntf[i].getId() + " : " + srIntf[i].getErrors()[j].getMessage() + "\n");
//					}
//				}
//			}
//		}
//		if(strErrors.length()>0) {
//			LOGGER.error(">updateInitialInterfaces>upsert:Error:"+strErrors);
//		}
//		LOGGER.trace(">updateInterface_Last_Next_Rundate> interfaceId: "+interfaceId+" done.");
//	}
	
	// 02-12-2011 v.1.15 Agent Last/Next Run Date Integration
//		@SuppressWarnings("static-access")
//		private void updateIntegrationLast_Next_Rundate(String integrationId, ISFService sfService, ISchedulerSetting iSchedulerSetting) throws Exception{
//			int timeRetry = 0;
//			int maxNumberOfRetry = sfService.MAX_NUMBER_OF_RETRY_DEFAULT;
//			int retryTimeInterval= sfService.RETRY_TIME_INTERVAL_DEFAULT;
//			
//			maxNumberOfRetry  = sfService.getMaxRetryNumber(sfIntegrationService.getIntegrationById(iSchedulerSetting.getIntegrationid()), null); 
//			retryTimeInterval = sfService.getRetryTimeInterval(sfIntegrationService.getIntegrationById(iSchedulerSetting.getIntegrationid()), null);
//			
//			String nsPrefix = sfService.getQueryPackage();
//			PartnerConnection sfPartner = sfService.getSFPartner();
//			
//			SObject objIntg = new SObject( nsPrefix + "Integration__c");
//			objIntg.setId(integrationId);
//			Calendar lastRunDate = retrieveLast_RunDate();
//			Calendar nextRunDate = retrieveNext_RunDate(iSchedulerSetting);
//			objIntg.setField( nsPrefix + "Agent_Last_Run_Date__c", lastRunDate);
//			objIntg.setField( nsPrefix + "Agent_Next_Run_Date__c", nextRunDate);
//			
//			SObject[] arrIntgSObject=new SObject[1]; 
//			arrIntgSObject[0] = objIntg;
//			
//			SaveResult[] srIntg = doUpdateSObject(arrIntgSObject, sfPartner, timeRetry, maxNumberOfRetry, retryTimeInterval);
//			
//			StringBuffer strErrors = new StringBuffer();
//			if(srIntg != null) {		
//				for(int i=0; i<srIntg.length; i++){
//					if(!srIntg[i].isSuccess() && srIntg[i].getErrors()!=null){
//						for(int j=0; j<srIntg[i].getErrors().length; j++){
//							strErrors.append(srIntg[i].getId() + " : " + srIntg[i].getErrors()[j].getMessage() + "\n");
//						}
//					}
//				}
//			}
//			if(strErrors.length()>0) {
//				LOGGER.error(">updateInitialIntegration> upsert:Error: "+strErrors);
//			}
//			LOGGER.trace(">updateIntegrationLast_Next_Rundate> integrationId: "+integrationId+" done.");
//			
//			// update all interface schedule on integration		
//			for(String interfaceId : lstInterfaceId) {	
//				timeRetry = 0;
//				maxNumberOfRetry = sfService.MAX_NUMBER_OF_RETRY_DEFAULT;
//				retryTimeInterval= sfService.RETRY_TIME_INTERVAL_DEFAULT;
//				
//				maxNumberOfRetry  = sfService.getMaxRetryNumber(sfIntegrationService.getIntegrationById(iSchedulerSetting.getIntegrationid()), sfIntegrationService.getInterfaceById(interfaceId)); 
//				retryTimeInterval = sfService.getRetryTimeInterval(sfIntegrationService.getIntegrationById(iSchedulerSetting.getIntegrationid()), sfIntegrationService.getInterfaceById(interfaceId));
//				
//				SObject objIntf = new SObject( nsPrefix + "Interfaces__c");
//				objIntf.setId(interfaceId);
//				
//				objIntf.setField( nsPrefix + "LastRun__c", lastRunDate);
//				objIntf.setField( nsPrefix + "Next_Run__c", nextRunDate);
//				
//				SObject[] arrIntfSObject=new SObject[1]; 
//				arrIntfSObject[0] = objIntf;
//				
//				SaveResult[] srIntf = doUpdateSObject(arrIntgSObject, sfPartner, timeRetry, maxNumberOfRetry, retryTimeInterval);	
//				StringBuffer strErrorsIntf = new StringBuffer();		
//				if(srIntf != null) {				
//					for(int i=0; i<srIntf.length; i++){		
//						if(!srIntf[i].isSuccess() && srIntf[i].getErrors()!=null){		
//							for(int j=0; j<srIntf[i].getErrors().length; j++){		
//								strErrorsIntf.append(srIntf[i].getId() + " : " + srIntf[i].getErrors()[j].getMessage() + "\n");		
//							}		
//						}		
//					}		
//				}		
//				if(strErrorsIntf.length()>0) {		
//					LOGGER.error(">updateInitialInterfaces> upsert:Error: "+strErrorsIntf);
//				}
//				LOGGER.trace(">updateIntegrationLast_Next_Rundate> interfaceId: "+interfaceId+" done.");
//			}	
//		}
	
	// Task #7132 Change Sobject for update last/next run date for skyvva v2.45 and Agent v1.51
	private void updateInterface_Last_Next_Rundate(Integration__c intg, ISFService sfService, ISchedulerSetting iSchedulerSetting) throws Exception{
		String nsPrefix = sfService.getQueryPackage();
		PartnerConnection sfPartner = sfService.getSFPartner();
		Calendar nextRunDate = retrieveNext_RunDate(iSchedulerSetting);
		if(nextRunDate == null) return;
		Calendar lastRunDate = retrieveLast_RunDate();
		
		SObject[] arrControlRunTime = new SObject[1]; 
		arrControlRunTime[0] = generateSObject__ControlRunTime("Interface__c", iSchedulerSetting.getInterfaceid(), nsPrefix, lastRunDate, nextRunDate);
		
		StringBuffer strSuccess = new StringBuffer();
		
		int timeRetry = 0;
		int maxNumberOfRetry = sfService.getMaxRetryNumber(intg, null); 
		int retryTimeInterval= sfService.getRetryTimeInterval(intg, null);
		StringBuffer strErrors = new StringBuffer();
		try {
			// for new version
			UpsertResult[] upsertResult = doUpsertSObject(nsPrefix+"External_ID__c", arrControlRunTime, sfPartner, timeRetry, maxNumberOfRetry, retryTimeInterval);
			strSuccess.append("\n>updateInterface_Last_Next_Rundate> Interface_Id: "+iSchedulerSetting.getInterfaceid()+" done.");
			if(upsertResult != null) {		
				for(int i=0; i<upsertResult.length; i++){
					if(!upsertResult[i].isSuccess() && upsertResult[i].getErrors()!=null){
						for(int j=0; j<upsertResult[i].getErrors().length; j++){
							strErrors.append(upsertResult[i].getId() + " : " + upsertResult[i].getErrors()[j].getMessage() + "\n");
						}
					}
				}
			}
		}catch(Exception ex) {
			// for old version
			if(ex instanceof InvalidSObjectFault || ex instanceof InvalidFieldFault) {
				SObject objIntf = new SObject( nsPrefix + "Interfaces__c");
				objIntf.setId(iSchedulerSetting.getInterfaceid());
				objIntf.setField( nsPrefix + "LastRun__c", lastRunDate);
				objIntf.setField( nsPrefix + "Next_Run__c", nextRunDate);
				arrControlRunTime[0] = objIntf;
				SaveResult[] saveResult = doUpdateSObject(arrControlRunTime, sfPartner, timeRetry, maxNumberOfRetry, retryTimeInterval);
				strSuccess.append("\n>updateInterface_Last_Next_Rundate> Interface_Id: "+iSchedulerSetting.getInterfaceid()+" done.");
				if(saveResult != null) {		
					for(int i=0; i<saveResult.length; i++){
						if(!saveResult[i].isSuccess() && saveResult[i].getErrors()!=null){
							for(int j=0; j<saveResult[i].getErrors().length; j++){
								strErrors.append(saveResult[i].getId() + " : " + saveResult[i].getErrors()[j].getMessage() + "\n");
							}
						}
					}
				}
			}else {
				throw ex;
			}
		}
		
		if(strErrors.length()>0) {
			LOGGER.error(">Upsert Last/Next Run Date> Error: "+strErrors);
		}else {
			LOGGER.trace(">Upsert Last/Next Run Date> Success: "+strSuccess);
		}
	}
	
	private void updateIntegrationLast_Next_Rundate(Integration__c intg, ISFService sfService, ISchedulerSetting iSchedulerSetting) throws Exception{
		String nsPrefix = sfService.getQueryPackage();
		PartnerConnection sfPartner = sfService.getSFPartner();
		Calendar nextRunDate = retrieveNext_RunDate(iSchedulerSetting);
		if(nextRunDate == null) return;
		Calendar lastRunDate = retrieveLast_RunDate();
		
		//We will collect old and new way for updating last/next rundate the same time
		// old integration last/next rundate update
		SObject[] arrControlRunTimeOld = new SObject[1 + lstInterfaceId.size()]; 
		SObject objIntg = new SObject( nsPrefix + "Integration__c");
		objIntg.setId(intg.getId());
		objIntg.setField( nsPrefix + "Agent_Last_Run_Date__c", lastRunDate);
		objIntg.setField( nsPrefix + "Agent_Next_Run_Date__c", nextRunDate);
		arrControlRunTimeOld[0] = objIntg;
		// new integration last/next rundate update
		SObject[] arrControlRunTime = new SObject[1 + lstInterfaceId.size()]; 
		arrControlRunTime[0] = generateSObject__ControlRunTime("Integration__c", intg.getId(), nsPrefix, lastRunDate, nextRunDate);
		
		StringBuffer strSuccess = new StringBuffer();
		strSuccess.append("\n>updateIntegration_Last_Next_Rundate> IntegrationId: "+intg.getId()+" done.");
		
		// update all interfaces schedule on integration		
		int index=1;
		Map<String, Interfaces__c> mapId2Intf = getMapIdIntf(intg.getInterfaces());
		for(String intfProceeded : lstInterfaceId) {	
			Interfaces__c intf = mapId2Intf.get(intfProceeded);
			if(intf.getType__c().equalsIgnoreCase(iSchedulerSetting.getDirection())) {
				// new collection of interfaces last/next rundate update
				arrControlRunTime[index] = generateSObject__ControlRunTime("Interface__c", intf.getId(), nsPrefix, lastRunDate, nextRunDate);
				// old collection of interfaces last/next rundate update
				SObject objIntf = new SObject( nsPrefix + "Interfaces__c");
				objIntf.setId(intf.getId());
				objIntf.setField( nsPrefix + "LastRun__c", lastRunDate);
				objIntf.setField( nsPrefix + "Next_Run__c", nextRunDate);
				arrControlRunTimeOld[index] = objIntf;
				strSuccess.append("\n>updateInterface_Last_Next_Rundate> interfaceId: "+intf.getId()+" done.");
				index++;
			}
		}	
		
		int timeRetry = 0;
		int maxNumberOfRetry = sfService.getMaxRetryNumber(intg, null); 
		int retryTimeInterval= sfService.getRetryTimeInterval(intg, null);
		StringBuffer strErrors = new StringBuffer();
		try {
			// new way
			UpsertResult[] srIntg = doUpsertSObject(nsPrefix+"External_ID__c", arrControlRunTime, sfPartner, timeRetry, maxNumberOfRetry, retryTimeInterval);
			if(srIntg != null) {		
				for(int i=0; i<srIntg.length; i++){
					if(!srIntg[i].isSuccess() && srIntg[i].getErrors()!=null){
						for(int j=0; j<srIntg[i].getErrors().length; j++){
							strErrors.append(srIntg[i].getId() + " : " + srIntg[i].getErrors()[j].getMessage() + "\n");
						}
					}
				}
			}
		}catch(Exception ex) {
			// old way
			if(ex instanceof InvalidSObjectFault || ex instanceof InvalidFieldFault) {
				SaveResult[] saveResult = doUpdateSObject(arrControlRunTimeOld, sfPartner, timeRetry, maxNumberOfRetry, retryTimeInterval);
				if(saveResult != null) {		
					for(int i=0; i<saveResult.length; i++){
						if(!saveResult[i].isSuccess() && saveResult[i].getErrors()!=null){
							for(int j=0; j<saveResult[i].getErrors().length; j++){
								strErrors.append(saveResult[i].getId() + " : " + saveResult[i].getErrors()[j].getMessage() + "\n");
							}
						}
					}
				}
			}else {
				throw ex;
			}
		}
		
		if(strErrors.length()>0) {
			LOGGER.error(">Upsert Last/Next Run Date> Error: "+strErrors);
		}else {
			LOGGER.trace(">Upsert Last/Next Run Date> Success: "+strSuccess);
		}
	}
	
	private void updateInterfaceGroupLast_Next_Rundate(Integration__c intg, ISFService sfService, ISchedulerSetting iSchedulerSetting) throws Exception{
		String nsPrefix = sfService.getQueryPackage();
		PartnerConnection sfPartner = sfService.getSFPartner();
		Calendar nextRunDate = retrieveNext_RunDate(iSchedulerSetting);
		if(nextRunDate == null) return;
		Calendar lastRunDate = retrieveLast_RunDate();
		
		SObject[] arrControlRunTime = new SObject[intg.getInterface_groups().size() + lstInterfaceId.size()]; 
		arrControlRunTime[0] = generateSObject__ControlRunTime("Interface_Group__c", intg.getInterface_groups().get(0).getId(), nsPrefix, lastRunDate, nextRunDate);
		
		StringBuffer strSuccess = new StringBuffer();
		strSuccess.append("\n>updateInterface_GroupLast_Next_Rundate> Interface_GroupId: "+intg.getInterface_groups().get(0).getId()+" done.");
		
		// update all interfaces schedule on interfaceGroup		
		int index=1;
		Map<String, Interfaces__c> mapId2Intf = getMapIdIntf(intg.getInterfaces());
		for(String intfProceeded : lstInterfaceId) {
			Interfaces__c intf = mapId2Intf.get(intfProceeded);
			arrControlRunTime[index] = generateSObject__ControlRunTime("Interface__c", intf.getId(), nsPrefix, lastRunDate, nextRunDate);
			strSuccess.append("\n>updateInterface_Last_Next_Rundate> interfaceId: "+intf.getId()+" done.");
			index++;
		}	
		
		int timeRetry = 0;
		int maxNumberOfRetry = sfService.getMaxRetryNumber(intg, null); 
		int retryTimeInterval= sfService.getRetryTimeInterval(intg, null);
		
		UpsertResult[] srIntg = doUpsertSObject(nsPrefix+"External_ID__c", arrControlRunTime, sfPartner, timeRetry, maxNumberOfRetry, retryTimeInterval);
		StringBuffer strErrors = new StringBuffer();
		if(srIntg != null) {		
			for(int i=0; i<srIntg.length; i++){
				if(!srIntg[i].isSuccess() && srIntg[i].getErrors()!=null){
					for(int j=0; j<srIntg[i].getErrors().length; j++){
						strErrors.append(srIntg[i].getId() + " : " + srIntg[i].getErrors()[j].getMessage() + "\n");
					}
				}
			}
		}
		if(strErrors.length()>0) {
			LOGGER.error(">Upsert Last/Next Run Date> Error: "+strErrors);
		}else {
			LOGGER.trace(">Upsert Last/Next Run Date> Success: "+strSuccess);
		}
	}
	
	public Map<String, Interfaces__c> getMapIdIntf(List<Interfaces__c> intfs){
		Map<String, Interfaces__c> mapIntf = new HashMap<String, Interfaces__c>();
		MapUtils.populateMap(mapIntf, intfs, Interfaces__c::getId);
		if (intfs != null && intfs.size() > 0) {
			return mapIntf;
		}
		return new HashMap<String, Interfaces__c>();
	}
	
	private SObject generateSObject__ControlRunTime(String lookUpField, String lookUpId, String nsPrefix, Calendar lastRunDate, Calendar nextRunDate) {
		SObject controlRunTime = new SObject( nsPrefix + "InterfaceControlRuntime__c");
		controlRunTime.setField(nsPrefix + "LastRun__c", lastRunDate);
		controlRunTime.setField(nsPrefix + "NextRun__c", nextRunDate);
		controlRunTime.setField(nsPrefix + lookUpField, lookUpId);
		controlRunTime.setField(nsPrefix + "External_ID__c", lookUpId);
		controlRunTime.setField(nsPrefix + "RecordType__c", "Agent");
		return controlRunTime;
	}
	
	private UpsertResult[] doUpsertSObject(String extField, SObject[] arrSObject, PartnerConnection sfPartner, int timeRetry, int maxNumberOfRetry, int retryTimeInterval) throws Exception {
		//LOGGER.trace("GenericSFTask.doUpdateSObject> Time to Disconect!");
		//Thread.sleep(5000);
		try {
			return sfPartner.upsert(extField, arrSObject);
		} catch (Exception e) {
			if(SFService.isErrorConnection(e) && maxNumberOfRetry>0) {
				timeRetry++;
				maxNumberOfRetry--;
				LOGGER.trace(">GenericSFTask.doUpdateSObject> ERROR> Problem with Connection...!");
				LOGGER.trace(">GenericSFTask.doUpdateSObject> Retry Integration> ("+timeRetry+ (timeRetry<=1?" time":" times")+") Retry Time Interval="+retryTimeInterval+"ms!");
				Thread.sleep(retryTimeInterval);
				return doUpsertSObject(extField, arrSObject, sfPartner,timeRetry, maxNumberOfRetry, retryTimeInterval);
			}
			else
				throw e;
		}
	}
	
	private SaveResult[] doUpdateSObject(SObject[] arrSObject, PartnerConnection sfPartner, int timeRetry, int maxNumberOfRetry, int retryTimeInterval) throws Exception {
		//LOGGER.trace("GenericSFTask.doUpdateSObject> Time to Disconect!");
		//Thread.sleep(5000);
		try {
			return sfPartner.update(arrSObject);
		} catch (Exception e) {
			if(SFService.isErrorConnection(e) && maxNumberOfRetry>0) {
				timeRetry++;
				maxNumberOfRetry--;
				LOGGER.trace(">GenericSFTask.doUpdateSObject> ERROR> Problem with Connection...!");
				LOGGER.trace(">GenericSFTask.doUpdateSObject> Retry Integration> ("+timeRetry+ (timeRetry<=1?" time":" times")+") Retry Time Interval="+retryTimeInterval+"ms!");
				Thread.sleep(retryTimeInterval);
				return doUpdateSObject(arrSObject, sfPartner,timeRetry, maxNumberOfRetry, retryTimeInterval);
			}
			else
				throw e;
		}
	}
	
	// catch run date exactly
	private Calendar runTime = null;

	public void setRunTime(Calendar runTime) {
		this.runTime = runTime;
	}

	public Calendar getRunTime() {
		if(this.runTime==null) this.runTime = Calendar.getInstance();
		return this.runTime;
	}
	
	public Calendar retrieveLast_RunDate() throws Exception {
		Calendar calendarLastRun = new GregorianCalendar();
		calendarLastRun = getRunTime();
		LOGGER.trace(">updateAgentLast_Next_Rundate >lastRunDate: " + getRunTime().getTime());
		
		return calendarLastRun;
	}
	
	public Calendar retrieveNext_RunDate(ISchedulerSetting iSchedulerSetting) throws Exception{/*
		if(iSchedulerSetting.getCronvalue()==null) return null;
		CronExpressionBean cronBean = new CronExpressionBean(iSchedulerSetting.getCronvalue());
		Calendar calendarNextRun = new GregorianCalendar();
		
		Date nextRunDate = cronBean.getCronExpression().getNextValidTimeAfter(calendarNextRun.getTime());
		LOGGER.trace(">updateAgentLast_Next_Rundate >nextRunDate: " + nextRunDate);
		
		calendarNextRun.setTime(nextRunDate);
		return calendarNextRun;*/return null;
	}
	
	//05-12-2011 v.1.15 push logs files into SF Attachment
	public void saveLogs2SFAttachment(String scheduledIntegrationId,ISFService sfService,LastRunDateCache integrationOperationCache) throws  IOException,Exception{
		
		LOGGER.trace("saveLogs2SFAttachment>pushLogs2SF: "+mapSFConnInfo.getPushLogs2SF());
		if(Boolean.parseBoolean(mapSFConnInfo.getPushLogs2SF())){
			
			String path = Helper.getAgentHome() + Helper.getFileSeparator() + "logs";
			
			String INLogFileNames2BeDeleted = "";
			List<String> filenames = new ArrayList<String>();
				List<String> newFilenames = FolderUtils.getFiles(path, "log");
				for(String newFilename:newFilenames){
					if(newFilename.toLowerCase().startsWith(scheduledIntegrationId+".log")  || newFilename.indexOf(scheduledIntegrationId)>0){
						//18-01-2012
						File file=new File(path + Helper.getFileSeparator() + newFilename); 
						LOGGER.trace("saveLogs2SFAttachment>fileName:"+newFilename + "; last modified:"
								+ file.lastModified() + " cached one:" + integrationOperationCache.getmFilename_FileModifiedDate().get(newFilename));
						if((integrationOperationCache.getmFilename_FileModifiedDate().get(newFilename)==null || 
									(integrationOperationCache.getmFilename_FileModifiedDate().get(newFilename)!=null &&
										!integrationOperationCache.getmFilename_FileModifiedDate().get(newFilename)
											.equalsIgnoreCase(String.valueOf(file.lastModified()))))){
							
							filenames.add(newFilename);
							integrationOperationCache.getmFilename_FileModifiedDate().put(newFilename, String.valueOf(file.lastModified()));
							INLogFileNames2BeDeleted+="'" + "Agent_"+newFilename.replace("'", "\\'") + "',";
							
						}
					}
				}
			
			if(filenames.size()>0){
				
				PartnerConnection binding = sfService.getSFPartner();
				
				SObject[] sobjs = new SObject[filenames.size()];
				
				for(int i=0;i<filenames.size();i++){
					File f = new File(path+Helper.getFileSeparator()+filenames.get(i));
					InputStream is = new FileInputStream(f);
					byte[] body = new byte[(int)f.length()];
					is.read(body);
					is.close();
					
					SObject sobj = new SObject();
					sobj.setType("Attachment");
					
					sobj.setField("Name", "Agent_"+filenames.get(i));
					sobj.setField("ParentId", scheduledIntegrationId);
					sobj.setField("Body",body);
					
					sobjs[i]=sobj;
				}
				StringBuffer strErrors = null;
				
				
				//retrieve old log files from SF to delete after created new
				INLogFileNames2BeDeleted = "(" + INLogFileNames2BeDeleted.substring(0, INLogFileNames2BeDeleted.length()-1) + ")";//cut the last ,
				QueryResult qr = binding.query("Select Id From Attachment where ParentId='"+scheduledIntegrationId+"' and Name IN " + INLogFileNames2BeDeleted); //+"' and Name LIKE 'Agent_iservice.log%'");				
				
				//insert new Attachment into SF
				SaveResult[] sr = binding.create(sobjs);
				strErrors = new StringBuffer();
				if(sr != null) {		
					for(int i=0; i<sr.length; i++){
						if(!sr[i].isSuccess() && sr[i].getErrors()!=null){
							for(int j=0; j<sr[i].getErrors().length; j++){
								strErrors.append(sr[i].getId() + " : " + sr[i].getErrors()[j].getMessage() + "\n");
							}
						}
					}
				}
				if(strErrors.length()>0) {
					LOGGER.error(">saveLogs2SFAttachment>create:Error:"+strErrors);
				}
				
				//delete old attachments from SF
				if(qr.getSize()>0){
					String[] ids = new String[qr.getRecords().length];
					for(int i=0;i<qr.getRecords().length;i++){
						ids[i]=qr.getRecords()[i].getId();
					}
					//delete attachments
					
					DeleteResult[] dr = binding.delete(ids);
					strErrors = new StringBuffer();
					if(dr!=null){
						for(int i=0; i<dr.length; i++){
							if(!dr[i].isSuccess() && dr[i].getErrors()!=null){
								for(int j=0; j<dr[i].getErrors().length; j++){
									strErrors.append(dr[i].getId() + " : " + dr[i].getErrors()[j].getMessage() + "\n");
								}
							}
						}
					}
					if(strErrors.length()>0) {
						LOGGER.error(">saveLogs2SFAttachment>delete:Error:"+strErrors);
					}
					
				}
				LOGGER.trace(">saveLogs2SFAttachment>append integrationOperationCache.txt");
				FileHelper.saveLastRunDateCache(FileHelper.getFilename(GenericSFTask.LAST_INTEGRATION_DATETIME), integrationOperationCache);
				LOGGER.trace(">saveLogs2SFAttachment>delete old attachments>DONE");
			}
		}
	}
	
	public void saveSystemLogFile2SFAttachment(String scheduledIntegrationId,ISFService sfService,LastRunDateCache integrationOperationCache) throws  IOException,Exception{
			
			LOGGER.trace("saveSystemLogFile2SFAttachment>pushSystemLogFile2SF: "+mapSFConnInfo.getPushLogs2SF());
			if(Boolean.parseBoolean(mapSFConnInfo.getPushLogs2SF())){
				String path = Helper.getAgentHome() + Helper.getFileSeparator() + "logs";
					List<String> newFilenames = FolderUtils.getFiles(path, "log");
					for(String newFilename:newFilenames){
						if(newFilename.toLowerCase().startsWith("iservice"+".log")  || newFilename.indexOf("iservice")>0) saveIservice(newFilename,scheduledIntegrationId,sfService,integrationOperationCache,path);
						if(newFilename.toLowerCase().startsWith("jcrontab"+".log")  || newFilename.indexOf("jcrontab")>0) saveJcrontab(newFilename,scheduledIntegrationId,sfService,integrationOperationCache,path);
			}
		}
	}
	
	void saveIservice(String newFilename,String scheduledIntegrationId,ISFService sfService,LastRunDateCache integrationOperationCache,String path) throws  IOException,Exception{
		String INLogFileNames2BeDeleted = "";
		List<String> filenames = new ArrayList<String>();
		File file=new File(path + Helper.getFileSeparator() + newFilename); 
		LOGGER.trace("saveIservice2SFAttachment>fileName:"+newFilename + "; last modified:"
				+ file.lastModified() + " cached one:" + integrationOperationCache.getmFilename_FileModifiedDate().get(newFilename));
		if((integrationOperationCache.getmFilename_FileModifiedDate().get(newFilename)==null || 
					(integrationOperationCache.getmFilename_FileModifiedDate().get(newFilename)!=null &&
						!integrationOperationCache.getmFilename_FileModifiedDate().get(newFilename)
							.equalsIgnoreCase(String.valueOf(file.lastModified()))))){
			
			filenames.add(newFilename);
			integrationOperationCache.getmFilename_FileModifiedDate().put(newFilename, String.valueOf(file.lastModified()));
			INLogFileNames2BeDeleted+="'" + "System_Logs_"+newFilename.replace("'", "\\'") + "',";
		}
		
		if(filenames.size()>0){
			
			PartnerConnection binding = sfService.getSFPartner();
			
			SObject[] sobjs = new SObject[filenames.size()];
			
			for(int i=0;i<filenames.size();i++){
				File f = new File(path+Helper.getFileSeparator()+filenames.get(i));
				InputStream is = new FileInputStream(f);
				byte[] body = new byte[(int)f.length()];
				is.read(body);
				is.close();
				
				SObject sobj = new SObject();
				sobj.setType("Attachment");
				
				sobj.setField("Name", "System_Logs_"+filenames.get(i));
				sobj.setField("ParentId", scheduledIntegrationId);
				sobj.setField("Body",body);
				
				sobjs[i]=sobj;
			}
			StringBuffer strErrors = null;
			
			
			//retrieve old Iservice files from SF to delete after created new
			INLogFileNames2BeDeleted = "(" + INLogFileNames2BeDeleted.substring(0, INLogFileNames2BeDeleted.length()-1) + ")";//cut the last ,
			QueryResult qr = binding.query("Select Id From Attachment where ParentId='"+scheduledIntegrationId+"' and Name IN " + INLogFileNames2BeDeleted); //+"' and Name LIKE 'iservice.log%'");				
			
			//insert new Attachment into SF
			SaveResult[] sr = binding.create(sobjs);
			strErrors = new StringBuffer();
			if(sr != null) {		
				for(int i=0; i<sr.length; i++){
					if(!sr[i].isSuccess() && sr[i].getErrors()!=null){
						for(int j=0; j<sr[i].getErrors().length; j++){
							strErrors.append(sr[i].getId() + " : " + sr[i].getErrors()[j].getMessage() + "\n");
						}
					}
				}
			}
			if(strErrors.length()>0) {
				LOGGER.error(">saveIservice2SFAttachment>create:Error:"+strErrors);
			}
			
			//delete old attachments from SF
			if(qr.getSize()>0){
				String[] ids = new String[qr.getRecords().length];
				for(int i=0;i<qr.getRecords().length;i++){
					ids[i]=qr.getRecords()[i].getId();
				}
				//delete attachments
				
				DeleteResult[] dr = binding.delete(ids);
				strErrors = new StringBuffer();
				if(dr!=null){
					for(int i=0; i<dr.length; i++){
						if(!dr[i].isSuccess() && dr[i].getErrors()!=null){
							for(int j=0; j<dr[i].getErrors().length; j++){
								strErrors.append(dr[i].getId() + " : " + dr[i].getErrors()[j].getMessage() + "\n");
							}
						}
					}
				}
				if(strErrors.length()>0) {
					LOGGER.error(">saveIservice2SFAttachment>delete:Error:"+strErrors);
				}
				
			}
			LOGGER.trace(">saveIService2SFAttachment>append integrationOperationCache.txt");
			FileHelper.saveLastRunDateCache(FileHelper.getFilename(GenericSFTask.LAST_INTEGRATION_DATETIME), integrationOperationCache);
			LOGGER.trace(">saveIservice2SFAttachment>delete old attachments>DONE");
		}
	}
	
	void saveJcrontab(String newFilename,String scheduledIntegrationId,ISFService sfService,LastRunDateCache integrationOperationCache,String path) throws  IOException,Exception{
		String INLogFileNames2BeDeleted = "";
		List<String> filenames = new ArrayList<String>();
		File file=new File(path + Helper.getFileSeparator() + newFilename); 
		LOGGER.trace("saveJcrontab2SFAttachment>fileName:"+newFilename + "; last modified:"
				+ file.lastModified() + " cached one:" + integrationOperationCache.getmFilename_FileModifiedDate().get(newFilename));
		if((integrationOperationCache.getmFilename_FileModifiedDate().get(newFilename)==null || 
					(integrationOperationCache.getmFilename_FileModifiedDate().get(newFilename)!=null &&
						!integrationOperationCache.getmFilename_FileModifiedDate().get(newFilename)
							.equalsIgnoreCase(String.valueOf(file.lastModified()))))){
			
			filenames.add(newFilename);
			integrationOperationCache.getmFilename_FileModifiedDate().put(newFilename, String.valueOf(file.lastModified()));
			INLogFileNames2BeDeleted+="'" + "System_Logs_"+newFilename.replace("'", "\\'") + "',";
		}
		
		if(filenames.size()>0){
			
			PartnerConnection binding = sfService.getSFPartner();
			
			SObject[] sobjs = new SObject[filenames.size()];
			
			for(int i=0;i<filenames.size();i++){
				File f = new File(path+Helper.getFileSeparator()+filenames.get(i));
				InputStream is = new FileInputStream(f);
				byte[] body = new byte[(int)f.length()];
				is.read(body);
				is.close();
				
				SObject sobj = new SObject();
				sobj.setType("Attachment");
				
				sobj.setField("Name", "System_Logs_"+filenames.get(i));
				sobj.setField("ParentId", scheduledIntegrationId);
				sobj.setField("Body",body);
				
				sobjs[i]=sobj;
			}
			StringBuffer strErrors = null;
			
			
			//retrieve old Jcrontab files from SF to delete after created new
			INLogFileNames2BeDeleted = "(" + INLogFileNames2BeDeleted.substring(0, INLogFileNames2BeDeleted.length()-1) + ")";//cut the last ,
			QueryResult qr = binding.query("Select Id From Attachment where ParentId='"+scheduledIntegrationId+"' and Name IN " + INLogFileNames2BeDeleted); //+"' and Name LIKE 'iservice.log%'");				
			
			//insert new Attachment into SF
			SaveResult[] sr = binding.create(sobjs);
			strErrors = new StringBuffer();
			if(sr != null) {		
				for(int i=0; i<sr.length; i++){
					if(!sr[i].isSuccess() && sr[i].getErrors()!=null){
						for(int j=0; j<sr[i].getErrors().length; j++){
							strErrors.append(sr[i].getId() + " : " + sr[i].getErrors()[j].getMessage() + "\n");
						}
					}
				}
			}
			if(strErrors.length()>0) {
				LOGGER.error(">saveJcrontab2SFAttachment>create:Error:"+strErrors);
			}
			
			//delete old attachments from SF
			if(qr.getSize()>0){
				String[] ids = new String[qr.getRecords().length];
				for(int i=0;i<qr.getRecords().length;i++){
					ids[i]=qr.getRecords()[i].getId();
				}
				//delete attachments
				
				DeleteResult[] dr = binding.delete(ids);
				strErrors = new StringBuffer();
				if(dr!=null){
					for(int i=0; i<dr.length; i++){
						if(!dr[i].isSuccess() && dr[i].getErrors()!=null){
							for(int j=0; j<dr[i].getErrors().length; j++){
								strErrors.append(dr[i].getId() + " : " + dr[i].getErrors()[j].getMessage() + "\n");
							}
						}
					}
				}
				if(strErrors.length()>0) {
					LOGGER.error(">saveJcrontab2SFAttachment>delete:Error:"+strErrors);
				}
				
			}
			LOGGER.trace(">saveJcrontab2SFAttachment>append integrationOperationCache.txt");
			FileHelper.saveLastRunDateCache(FileHelper.getFilename(GenericSFTask.LAST_INTEGRATION_DATETIME), integrationOperationCache);
			LOGGER.trace(">saveJcrontab2SFAttachment>delete old attachments>DONE");
		}
	}

	void updateMapInterface(List<Map<String, String>>  mapInterface, SFService sfService) throws Exception {
		
		if(mapInterface.size()==0) return;
		
		boolean hasPacketInInterface = false;
		for(Map<String, String> i : mapInterface) {
			if(i.containsKey(ISFService.NUMBER_OF_RECORDS_PER_BATCH)) {
				hasPacketInInterface = true; break;
			}
		}
		if(!hasPacketInInterface) {
			String interfaceIds = " ";
			for(Map<String, String> i : mapInterface) {
				String iId = i.get(ISFService.INTERFACE_ID);
				if(iId!=null) interfaceIds+="'" +iId + "',";
			}
			interfaceIds = interfaceIds.substring(0,interfaceIds.length()-1);
			if(interfaceIds.length()>=21) {
				interfaceIds = "(" + interfaceIds + ")";				
				String nsPrefix = sfService.getQueryPackage();//SFService.PACKAGE.replace("/", "") + "__";				
				PartnerConnection binding = sfService.getSFPartner();				
				QueryResult qr = binding.query("Select id, "
					+ nsPrefix + "Name__c," + nsPrefix + "Number_of_Records_Per_Batch__c from "
					+ nsPrefix + "Interfaces__c where Id IN "+interfaceIds);
				
				if(qr.getSize()>0) {
					
					Map<String, String> mInterfNamePacket = new HashMap<String, String>();
					List<Interfaces__c> lstInterfaces = SFIntegrationObjectUtils.readSObjects(qr,Interfaces__c.class);
					for(int i =0;i<lstInterfaces.size();i++) {
						Interfaces__c so = lstInterfaces.get(i);
						mInterfNamePacket.put(so.getName__c(), so.getNumber_of_Records_Per_Batch__c());
					}
					for(Map<String, String> i : mapInterface) {
						if(!i.containsKey(ISFService.NUMBER_OF_RECORDS_PER_BATCH)) {
							String iName = i.get(ISFService.INTERFACEE);
							i.put(ISFService.NUMBER_OF_RECORDS_PER_BATCH, mInterfNamePacket.get(iName));
						}						
					}
				}
			}
		}		
	}

	/**
	 * if the value = false|true (type = String) return 0|1 <br>
	 * if the value is empty return default value <br>
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static int validateValue(String value, int defaultValue) {
		int result = defaultValue;
		if(value!=null && !value.equalsIgnoreCase("null")){
			try{
				result = (value.equalsIgnoreCase("false")|| value.equalsIgnoreCase("true"))?(Boolean.parseBoolean(value)?1:0):
								(int)Double.parseDouble(value);
			}catch(Exception ex){}
		}
		if(result<=0)result = defaultValue;
		
		return result;
	}
	

	/**
	 * #3272 Interface will not run in Integration Scheduled if it has its own schedule
	 * with cron file
	 */
	/*
	private boolean isInterfaceScheduled(String interfaceName) throws Exception {
		for(CronEntryBean cronEntry : FileHelper.getCronEntries()) {
			if(!StringUtils.isEmpty(cronEntry.getInterfaceName()) &&
					cronEntry.getInterfaceName().equals(interfaceName) &&
					cronEntry.isON() &&
					mapSFConnInfo.get("username").equals(cronEntry.getUsername())) 
				return true;
		}
		return false;
	}*/
	
	/**
	 * Interface will not run in Integration Scheduled if it has its own schedule
	 * @param interfaceName
	 * @return true, false
	 * @throws Exception
	 */
//	private boolean isInterfaceScheduled(String integrationName,String interfaceName) throws Exception {
//		List<ISchedulerSetting> lstISh = BaseDao.getDao(ISchedulerSettingDao.class).getStartedInterfaceScheduled(mapSFConnInfo.get("username"), integrationName, interfaceName);
//		if(lstISh!=null && !lstISh.isEmpty()) return true;
//		return false;
//	}

//	public static IDBConnection cacheConnection = null;
	
	public static IDBConnection createAdapter(String adapterType, List<Property> lstProperties) throws Exception {		
		Map <String, String> mapConInfo = new HashMap<String, String>();		
				
		for(Property p : lstProperties){		
					
			//for XML Adapter, cos XMLAdapter.login() doesn't call setConnectionInfo method		
			mapConInfo.put(p.getName(), p.getValue());		
		}		
				
		Adapter adapter = new Adapter();		
		adapter.setConnType(adapterType);		
		adapter.setProperty(lstProperties);		
				
		IDBConnection connection = ConnectionFactory.getInstance().createConnection(adapter);		
		mapConInfo.put(PropertyNameHelper.ADAPTER_TYPE, adapterType);		
		connection.setConnectionInfo(mapConInfo);		
		return connection;		
	}
	
	public static String getExceptionDetail(Throwable ex) {
		
		String msg = (ex.getMessage() != null && !ex.getMessage().isEmpty() ? ex.getMessage() : 
			ex.getLocalizedMessage()!=null && !ex.getLocalizedMessage().isEmpty() ? ex.getLocalizedMessage() : 
				ex.toString());
		
		if(msg!=null && msg.startsWith("java.lang.Exception: ")) {
			msg = msg.replaceFirst("java.lang.Exception: ", "");
		}
		return (msg!=null ? msg.trim() : msg);
	}

	// 05082010
	// create logs in batch
	public ILogs__c traceError2(ISFService sfService, String interfaceName, Throwable e1) {

//		ILogs__c ilog = null;
//		try {
//			
//			ilog = new ILogs__c(sfService.getQueryPackage());//SFService.PACKAGE.replace("/", "") + "__");
//			ilog.setIntegration__c(scheduledIntegrationId);
//			ilog.setSource__c(interfaceName);
//			
//			if(e1 instanceof FileMapAdapter.WarningFileNotFoundException){
//				ilog.setError_Level__c("Warning");
//			}
//			else{
//				ilog.setError_Level__c("Error");
//			}
//
//			StringWriter sw = new StringWriter();
//			e1.printStackTrace(new PrintWriter(sw));
//			String stacktrace = sw.toString()+"";
//			
//			if(stacktrace!=null && stacktrace.startsWith("java.lang.Exception: ")) {
//				stacktrace = stacktrace.replaceFirst("java.lang.Exception: ", "");
//			}
//			
//			LOGGER.error(stacktrace);
//			
//			// 20102010, handle case line break, bec log desc is Text Area(255)
//			String errMsg = stacktrace; //21012011 e1.getMessage();
//			errMsg = errMsg.replaceAll("\r", " ").replaceAll("\n", " ");
//			if (errMsg.length() > 255) {
//				ilog.setDescription__c(errMsg.substring(0, 255));
//			} else {
//				ilog.setDescription__c(errMsg);
//			}
//
//		} catch (Throwable e2) {
//			LOGGER.error(">traceError>interface:" + interfaceName + ">Error\n" + e2.getMessage());
//		}
//		return ilog;
		return null;
	}
	
	// 05082010
	// create logs in batch
	public void createLogs(List<ILogs__c> ilogs, SFService sfService, MapIntegrationInfo mapIngtInfo) throws Exception {

		try {
			
			if (ilogs != null && ilogs.size() > 0) {
				
				LOGGER.trace(">createLogs>>ilogs>" + ilogs);
				List<JsonObject> lsLog = new ArrayList<>();
				IBean[] filtererror = new IBean[5 * ilogs.size()];
				Integer nb = 0;
				IBean integrationBean = new IBean(ISFService.INTEGRATION_C, scheduledIntegrationId);
				for (int i = 0; i < ilogs.size(); i++) {
					IBean error = new IBean("LEVEL", ilogs.get(i).getError_Level__c());
					IBean source = new IBean("SOURCE", ilogs.get(i).getSource__c());
					IBean logBean = new IBean(ISFService.OPERATION_LOGS, ilogs.get(i).getDescription__c());
					IBean sourceType = new IBean("DataSourceType", "AGENT");
					filtererror[nb++] = integrationBean;
					filtererror[nb++] = error;
					filtererror[nb++] = logBean;
					filtererror[nb++] = source;
					filtererror[nb++] = sourceType;
					JsonObject json = new JsonObject();
					json.addProperty("ParentId", integrationBean.getValue());
					json.addProperty("level", ilogs.get(i).getError_Level__c());
					json.addProperty("source", ilogs.get(i).getSource__c());
					json.addProperty("sourceType", "AGENT");
					json.addProperty("description", ilogs.get(i).getDescription__c());
					lsLog.add(json);
				}
				try {
					Map<String, Object> criteria = new HashMap<String, Object>();
					criteria.put("ParentId", integrationBean.getValue());
					BaseDao.getDao(LogDao.class).deleteByCriteria(criteria);
					BaseDao.getDao(LogDao.class).bulkUpsert(lsLog);
				} catch (Exception e) {
					LOGGER.error(e.getMessage());
				}
				sfService.search(filtererror, mapIngtInfo);
				
				//20110725 release from memory
				filtererror = null;
				
				LOGGER.trace(">createLogs>Done");

				/**
				 * IBean[][] filters = new IBean[ilogs.size()][]; for(int
				 * i=0;i<ilogs.size();i++){
				 * 
				 * IBean[] filtererror = new IBean[4]; IBean error = new
				 * IBean("LEVEL",ilogs.get(i).getError_Level__c()); IBean source
				 * = new IBean("SOURCE",ilogs.get(i).getSource__c()); IBean
				 * logBean = new IBean(ISFService.OPERATION_LOGS,ilogs.get(i).
				 * getDescription__c()); filtererror[0] = integrationBean;
				 * filtererror[1] = error; filtererror[2] = logBean;
				 * filtererror[3] = source;
				 * 
				 * filters[i] = filtererror;
				 * 
				 * } sfService.createLogs(filters);
				 */

				/**
				 * SObject[] logs = new SObject[ilogs.size()]; for(int
				 * i=0;i<ilogs.size();i++){ SObject aLog =
				 * SFIntegrationObjectUtils.createSObject(ilogs.get(i)); logs[i]
				 * = aLog; } //try session, if no, log in
				 * sfService.getSessionId(); SoapBindingStub binding =
				 * sfService.getBinding();
				 * binding.setTimeout(SFService.TIMEOUT); SessionHeader sh = new
				 * SessionHeader(sfService.getSessionId());
				 * binding.setHeader(sfService
				 * .getSforceServiceLocator().getServiceName
				 * ().getNamespaceURI(), "SessionHeader", sh);
				 * binding._setProperty
				 * (SoapBindingStub.ENDPOINT_ADDRESS_PROPERTY,
				 * sfService.getServerURL());
				 * 
				 * SaveResult[] ur = binding.create(logs); for(int i=0;
				 * i<ur.length; i++){ if(!ur[i].isSuccess()){ StringBuffer
				 * strErrors = new StringBuffer(); for(int j=0;
				 * j<ur[i].getErrors().length; j++){
				 * strErrors.append(ur[i].getErrors(j).getMessage() + "\n"); }
				 * log.error(strErrors.toString()); } }
				 **/
			}
		} catch (Throwable e2) {
			LOGGER.error(">createLogs>>Error\n" + e2.getMessage());
		}
	}

	/**
	 * 04112010 Integration SF2Agent
	 * 
	 */
	private static String getLastInterfaceDateTime(String intfId) {
//		java.sql.Timestamp tm=null;
//		try {
//			tm = CoreJDBCAdapter.getLastIntegrationDateTime(intfId);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
//		return dateFormatter.format(tm);
		return null;
	}

	//23022011 PE
	public static IBean[] createFilterOPERATION_SF_BUILD_QUERY_INTERFACE(String integrationId, String interfaceId) {
		IBean[] filter = new IBean[4];
		IBean setupBean = new IBean(ISFService.OPERATION_SF_BUILD_QUERY_INTERFACE, ISFService.OPERATION_SF_BUILD_QUERY_INTERFACE);
		IBean interfaceBean = new IBean(ISFService.INTERFACEE, interfaceId);
		IBean integBean = new IBean(ISFService.INTEGRATION_C, integrationId);
		IBean lastInterfaceDatetime = new IBean(ISFService.LAST_INTERFACE_DATETIME,getLastInterfaceDateTime(interfaceId));
		filter[0] = setupBean;
		filter[1] = interfaceBean;
		filter[2] = integBean;
		filter[3] = lastInterfaceDatetime;
		return filter;
	}
	//23022011 PE
	public static IBean[] createFilterOPERATION_SF_ALL_OBJECT_IDS(String integrationId, String interfaceId) throws Exception {
		IBean[] filter = new IBean[4];
		IBean setupBean = new IBean(ISFService.OPERATION_SF_ALL_OBJECT_IDS, ISFService.OPERATION_SF_ALL_OBJECT_IDS);
		IBean interfaceBean = new IBean(ISFService.INTERFACEE, interfaceId);
		IBean integBean = new IBean(ISFService.INTEGRATION_C, integrationId);
		IBean lastInterfaceDatetime = new IBean(ISFService.LAST_INTERFACE_DATETIME,getLastInterfaceDateTime(interfaceId));
		filter[0] = setupBean;
		filter[1] = interfaceBean;
		filter[2] = integBean;
		filter[3] = lastInterfaceDatetime;
		return filter;
	}

	// @PARAM ids: string of Ids (comma separated Ids)
	public static IBean[] createFilterOPERATION_SF_DATA(String integrationId, String interfaceId, String ids, String DATE_TIME_FORMAT) {
		IBean[] filter = new IBean[6];//add DATE_TIME_FORMAT
		IBean setupBean = new IBean(ISFService.OPERATION_SF_DATA, ISFService.OPERATION_SF_DATA);
		IBean integrationBean  =new IBean(ISFService.INTEGRATION_C,integrationId);
		IBean interfaceBean = new IBean(ISFService.INTERFACEE, interfaceId);
		IBean idsBean = new IBean(ISFService.OBJECT_IDS, ids);
		IBean DATE_TIME_FORMATBean = new IBean(ISFService.SF_DATE_TIME_FORMAT, DATE_TIME_FORMAT);
		IBean lastInterfaceDatetime = new IBean(ISFService.LAST_INTERFACE_DATETIME,getLastInterfaceDateTime(interfaceId));
		filter[0] = setupBean;
		filter[1] = integrationBean;
		filter[2] = interfaceBean;
		filter[3] = idsBean;
		filter[4] = DATE_TIME_FORMATBean;
		filter[5] = lastInterfaceDatetime;
		return filter;
	}
	/**
	 * 22/09/2016
	 * @param integrationId
	 * @param inerfaceId
	 * @param ids String of Ids (separated by comma)
	 * @return
	 */
	public static IBean[] createFilterOPERATION_SF_DATA(String integrationId, String interfaceId, String ids){
		IBean[] filter = new IBean[5];//add DATE_TIME_FORMAT

		IBean setupBean = new IBean(ISFService.OPERATION_SF_DATA, ISFService.OPERATION_SF_DATA);
		IBean integrationBean  =new IBean(ISFService.INTEGRATION_C,integrationId);
		IBean interfaceBean = new IBean(ISFService.INTERFACEE, interfaceId);
		IBean idsBean = new IBean(ISFService.OBJECT_IDS, ids);

		filter[0] = setupBean;
		filter[1] = integrationBean;
		filter[2] = interfaceBean;
		filter[3] = idsBean;
		
		return filter;
	}
	
	public static IBean[] createFilterOPERATION_SF2AGENT_REPROCESS_ALL(String integrationId, String interfaceId) {
		IBean[] filter = new IBean[3];
		IBean setupBean = new IBean( ISFService.OPERATION_SF2AGENT_REPROCESS_ALL, ISFService.OPERATION_SF2AGENT_REPROCESS_ALL);
		IBean integrationBean  =new IBean(ISFService.INTEGRATION_C,integrationId);
		IBean interfaceBean = new IBean(ISFService.INTERFACEE, interfaceId);
		filter[0] = setupBean;
		filter[1] = integrationBean;
		filter[2] = interfaceBean;
		return filter;
	}

	// @PARAM ids: string of Ids (comma separated Ids)
	public static IBean[] createFilterOPERATION_SF2AGENT_REPROCESS_IDS(String integrationId, String interfaceId, String ids) {
		IBean[] filter = new IBean[4];
		IBean setupBean = new IBean( ISFService.OPERATION_SF2AGENT_REPROCESS_IDS, ISFService.OPERATION_SF2AGENT_REPROCESS_IDS);
		IBean integrationBean  =new IBean(ISFService.INTEGRATION_C,integrationId);
		IBean interfaceBean = new IBean(ISFService.INTERFACEE, interfaceId);
		IBean idsBean = new IBean(ISFService.MESSAGE_IDS, ids);
		filter[0] = setupBean;
		filter[1] = integrationBean;
		filter[2] = interfaceBean;
		filter[3] = idsBean;
		return filter;
	}

	/*
	 * Schedule SF-Agent 
	 * Either direct or from reprocess
	 */
	public void integrationOutBound(Integration__c integration, ISchedulerSetting iSchedulerSetting) {
		
		//20111116 v.14 Initial ilogs list
		if(ilogs==null) {
			ilogs = new ArrayList<ILogs__c>();
		}
		else {
			ilogs.clear();
		}
		
		//20012011 manage interfaces last data
		mapInterfaceLastData.clear();
		
		//clear interfaceId for update interface last next run date
		lstInterfaceId.clear();
		
		boolean isReprocessed = StringUtils.equalsIgnoreCase(iSchedulerSetting.getReprocess(),ScheduleModeHelper.REPROCESSING);
		String pref = isReprocessed ? ">integrationOutBound() >Reprocess " : ">integrationOutBound() >Process ";
		
		try {
			
			LOGGER.trace(pref+">IntegrationName: " + integration.getName() + " >Start");
			
			//has interface(s)
			if(integration!=null && integration.getInterfaces().size()>0) {
				
				//16-11-2016
				String lastruntimeFileName = FileHelper.getFilename(GenericSFTask.LAST_INTEGRATION_DATETIME);
				LastRunDateCache lastRunDateCache = FileHelper.readLastRunDateCache(lastruntimeFileName);
				
				if(lastRunDateCache == null){//in case do new integration (File not found)
					lastRunDateCache = new LastRunDateCache();
				}
				
				integrationSF2Agent(sfIntegrationService, integration, isReprocessed, lastRunDateCache);
				
				//add to cache file
				FileHelper.saveLastRunDateCache(lastruntimeFileName, lastRunDateCache);
				
				try{
					if(iSchedulerSetting.getSchedulertype().contains(ISchedulerSetting.PROCESS_PER_INTERFACE_GROUP)){
						updateInterfaceGroupLast_Next_Rundate(integration, sfIntegrationService.getISFService(), iSchedulerSetting);
					}
					else if(StringUtils.isBlank(getScheduledInterfaceId())) {
						updateIntegrationLast_Next_Rundate(integration, sfIntegrationService.getISFService(), iSchedulerSetting);
					}
					else {
						updateInterface_Last_Next_Rundate(integration, sfIntegrationService.getISFService(), iSchedulerSetting);
					}
				}catch(Exception e){
					if(e instanceof ApiFault) {
						if(((ApiFault) e).getFaultCode().toString().contains("INVALID_FIELD")){
							LOGGER.warn(pref+">updateAgentLast_Next_Rundate> Warning: "+((ApiFault) e).getFaultCode().toString());
						}
						else throw e;
					}
					else throw e;
				}
				saveLogs2SFAttachment(scheduledIntegrationId, sfIntegrationService.getISFService(), lastRunDateCache);
			}
			else {
				throw new Exception(pref+">Integration name: "+integration.getName()+", has no valid interfaces available. Make sure its OutBound interface(s) are Deployed/Activated.");
			}
		} catch (Throwable e) {
			if(SFService.isErrorConnection(e)) {
				LOGGER.error(">GenericSFTask> integrationInBound> Error Connection!");
			}
			else {
				LOGGER.error(pref+">Error: " + e.getMessage(), e);
				ilogs.add(traceError2(sfIntegrationService.getISFService(), "ISERVICE", e));
			}
		}

		finally {
			try {
				createLogs(ilogs, (SFService) sfIntegrationService.getISFService(), new MapIntegrationInfo(sfIntegrationService, integration, null, runTime));
			} catch (Exception e) {
				LOGGER.error(pref+">createLogs:Error" + e,e);
			}
			LOGGER.trace(pref+">IntegrationName: " + integration.getName() + " >Done");
			// release memory
			System.gc();
			System.runFinalization();
		}
	}
	
	protected void integrationSF2Agent(SFIntegrationService sfIntegrationService, Integration__c integration, Boolean isReprocessed, LastRunDateCache integrationOperationCache)throws Exception {
		
		new AgentRestService().checkLicenseKey(sfIntegrationService);
	
		if (sessionID != null) {
			sfIntegrationService.getISFService().setSessionId(sessionID);
		}
		
		List<Interfaces__c> lstInterfaces = new ArrayList<>();
		List<Interfaces__c> lstIntf = integration.getInterfaces();
		
		for(ISFIntegrationObject intf: lstIntf) {
			if(intf instanceof Interfaces__c) {
				// schedule per Integration
				if(scheduledInterfaceId == null || scheduledInterfaceId.isEmpty() || scheduledInterfaceId.trim().equals("")) {
					if(((Interfaces__c) intf).getType__c().equalsIgnoreCase(DirectionTypeHelper.OUTBOUND)) {
						lstInterfaces.add((Interfaces__c) intf);
					}
				}
				// schedule per Interfaces
				else {
					if(((Interfaces__c) intf).getId().equalsIgnoreCase(scheduledInterfaceId)) {
						lstInterfaces.add((Interfaces__c) intf);
						break;
					}
				}
			}
		}
		
		if(iSchedulerSetting.getSchedulertype().contains(ISchedulerSetting.PROCESS_PER_INTERFACE_GROUP)) {
			if(integration.getInterface_groups().get(0).getType__c().equalsIgnoreCase("EO"))  {
				//process all interfaces at the same time when InterfaceGroup set type="EO"
				for (Interfaces__c intf : lstInterfaces)  {
					Thread thread = new Thread(new Runnable() {
						@Override
						public void run() {
							processOneInterface__Outbound(integration, intf, integrationOperationCache, isReprocessed);
						}
					});
					thread.start();
				}
				// wait to finish all interfaces
				while(numberInterface__Processed!=lstInterfaces.size()) Thread.sleep(1000);
			}else{
				//EOIO process one by one and if one interface fail let's end integrate
				for (Interfaces__c intf : lstInterfaces)  {
					if(processOneInterface__Outbound(integration, intf, integrationOperationCache, isReprocessed)) break;
				} 
			}
		}else {
			//process one by one
			for (Interfaces__c intf : lstInterfaces)  {
				processOneInterface__Outbound(integration, intf, integrationOperationCache, isReprocessed);
			} 
		}
	}
	private boolean processOneInterface__Outbound(Integration__c integration, Interfaces__c intf, LastRunDateCache integrationOperationCache, Boolean isReprocessed ) {
	
//		String interfaceName = "";
//		String interfaceId = "";
//		boolean initialization = false;
//		boolean isError = false;
//	
//		RoutingLogFileName(integration.getName()+"_"+integration.getId());
//		try {
//			
//			MapIntegrationInfo mapIntgInfo = new MapIntegrationInfo(sfIntegrationService, integration, intf, getRunTime());
//			interfaceId = mapIntgInfo.getInterfaceId();
//			interfaceName = mapIntgInfo.getInterfaceName();
//			initialization = mapIntgInfo.isInitialization();
//			
//			if(intf.getStatus__c().equalsIgnoreCase("Development")) {
//				throw new Exception("Interface: "+ intf.getName__c() +"\nPlease change the status from Development to Deployed.");
//			}
//			
//			if(StringUtils.isEmpty(scheduledInterfaceId) && isInterfaceScheduled(integration.getName(),interfaceName)) {
//				LOGGER.trace(">integrationSF2Agent() >InterfaceName: "+interfaceName+" > has its own schedule...");
//				return false;
//			}
//			
//			// prevent interface processing overlap #2569
//			if(interfaceId!=null && !interfaceId.equals("") && FolderUtils.isFileExisting(TMP_FILE + "\\" + interfaceId)){
//				LOGGER.trace(">integrationSF2Agent() >InterfaceName: "+interfaceName+"> Still Processing...");
//				return false;
//			}
//			new File(TMP_FILE + "\\" + interfaceId).createNewFile();
//			
//			// get adapter
//			IDBConnection adapter = new CoreIntegration().getAdapter(mapIntgInfo.getAdapterProperties());
//			
//			LOGGER.trace(">integrationSF2Agent() >InterfaceName: " + interfaceName +" >Start");
//			LOGGER.trace(">integrationSF2Agent() >AdapterName: " + mapIntgInfo.getAdapterName());
//			LOGGER.trace(">integrationSF2Agent() >Initialization: " + initialization);
//		
//			// check folder 
//			if(adapter instanceof FileMapAdapter || adapter instanceof FTPAdapter) {
//				if(StringUtils.isBlank(adapter.getAdapter().getPropertyValue(PropertyNameHelper.FOLDER))) {
//					throw new Exception("OutBound processing with adapter name: "+mapIntgInfo.getAdapterName()+" specify the adapter property 'Folder'.");
//				}	
//			}
//			
//			// add interfaceId for update last next run date		
//			lstInterfaceId.add(interfaceId);
//			
//			// construct the dynamic query
//			String query = mapIntgInfo.getQuery();
//			
//			// if integrate and interface no sf query, insert log
//			if (!isReprocessed && (query == null || query.trim().equals(""))) {
//				String msg =">integrationSF2Agent() >InterfaceName: " + interfaceName + " has no query statement. The query statement is required to query Salesforce records";
//				LOGGER.warn(msg);
//				throw new GenericSFTaskException(msg);
//			}
//			
//			String tableName = mapIntgInfo.getSourceName();
//			String externalFieldName = "";
//			String externalIdFieldType = "";
//			
//			for(ISFIntegrationObject mapping: sfIntegrationService.getIMapping__c(interfaceId)) {
//				IMapping__c m = (IMapping__c) mapping;
//				if(m.isExternalId()) {
//					externalFieldName = m.getTarget__c();
//					externalIdFieldType = m.getTarget_Type__c();
//					break;// support only one external id
//				}
//			}
//
//			//20121003 in case FILE integration, no need to check external ID mapping
//			//in case Update Source System, it is necessary to know the External Filed in the related table
//			if( (externalFieldName ==null || externalFieldName.trim().equals("")) && !(adapter instanceof FileMapAdapter) ){
//				String msg =">integrationSF2Agent() >InterfaceName: " + interfaceName +" has no external Id field defined in its mappings. It is required for SF-Agent integration except SF-File.";
//				
//				LOGGER.warn(msg);
//				
//				throw new GenericSFTaskException(msg);
//			}
//			
//			if(adapter.getBatchExecutor()!=null) {
//				adapter.getBatchExecutor().setExternalIdField(externalFieldName);
//				adapter.getBatchExecutor().setExternalIdFieldType(externalIdFieldType);
//				adapter.getBatchExecutor().setTableName(tableName);
//			}
//			
//			//As soon as it queried for the interface, set the run date at once
//			//retrieve current time to add to new cache
//			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//			formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
//			String lastRunDate = formatter.format(new Date());
//			
//			adapter.setExternalIdField(externalFieldName);
//			adapter.setTableName(tableName);
//
//			IntegrationSF2AgentThread th1 = new IntegrationSF2AgentThread(mapIntgInfo, adapter, isReprocessed);
//
//			th1.setRecordPacket(mapIntgInfo.getPackageSize());
//			
//			th1.run();
//			
//			//update the cache at the map at the interfaceId with the new last run date
//			//if the th1 not isError
//			if(interfaceId!=null && !th1.isError) {
//				integrationOperationCache.getmIntfLastRunDate().put(interfaceId, lastRunDate);
//			}
//			isError = th1.hasFailRecord;
//			
//			if(isError) deleteCacheRunning(interfaceId);
//
//		} catch (Throwable e1) {
//			isError = true;
//			if (SFService.isErrorConnection(e1)) {
//				LOGGER.error(">GenericSFTask> integrationSF2Agent> Error Connection!");
//			}
//			else {
//				LOGGER.error(">integrationSF2Agent() >InterfaceName: " + interfaceName+ ">Error: " + e1,e1);
//				ilogs.add(traceError2(sfIntegrationService.getISFService(), interfaceName, e1));
//			}
//			if(interfaceId!=null && !interfaceId.equals("")){
//				//delete interfaceId file: interface processing finished
//				if(FolderUtils.isFileExisting(TMP_FILE + "\\" + interfaceId)){
//					FolderUtils.deleteFile(TMP_FILE + "\\" + interfaceId);
//				}
//			}
//		}
//		
//		//20111116 v1.14 initial interface query
//		finally {
//			if(interfaceId!=null && !interfaceId.equals("") && !isError){
//				//delete interfaceId file: interface processing finished
//				if(FolderUtils.isFileExisting(TMP_FILE + "\\" + interfaceId)){
//					FolderUtils.deleteFile(TMP_FILE + "\\" + interfaceId);
//				}
//			}
//			if(!isReprocessed && initialization && interfaceId!=null && !interfaceId.equals("") && !isError) {
//				try {
//					updateInitializationInterfaces(Arrays.asList(interfaceId), sfIntegrationService.getISFService());
//					intf.setInitialization__c(false);
//				} catch (Exception e) {
//					LOGGER.error(">updateInitializationInterfaces> Error: " + e.getMessage(), e);
//				}
//			}
//			LOGGER.trace(">integrationSF2Agent() >InterfaceName: " + interfaceName +(isError?" >Fail": " >Done")+"\n");
//			numberInterface__Processed++;
//		} //end one interface
//		return isError;
		return false;
	}

	public static DtoLastData updateMessages(List<IBean> bs, ISFService sfService, MapIntegrationInfo mapIntgInfor) throws Exception {
		
		Integer failed = 0, pending = 0, completed = 0;
		IBean bOPERATION_UPDATE_MESSAGES=new IBean(ISFService.OPERATION_UPDATE_MESSAGES,ISFService.OPERATION_UPDATE_MESSAGES);
		bs.add(bOPERATION_UPDATE_MESSAGES);
		
		IBean[] bsArr=new IBean[bs.size()];
		for(int i=0;i<bs.size();i++){
			bsArr[i]=bs.get(i);
			
			//IBean b=new IBean("IM#"+msgIndex++, i.getMessaegId()+":" +i.getStatus()+":"+ i.getMessage());
			if(!bs.get(i).getName().equals(ISFService.OPERATION_UPDATE_MESSAGES)) {
				String[] oneMessageValueList = bs.get(i).getValue().split(":", 3);
				String status = oneMessageValueList.length>2 ? oneMessageValueList[1] : "";
				if(status.equalsIgnoreCase("Failed")) {
					failed++;
				}
				else if(status.equalsIgnoreCase("Completed")) {
					completed++;
				}
				else {
					pending++;
				}
			}
		}
		sfService.search(bsArr, mapIntgInfor);
		
		return new DtoLastData(null, null, null, null, failed, pending, completed, null);
	}

	//20112011
	public static void upsertInterfacesLastData(SFIntegrationService intService, Map<Interfaces__c, String> mapInterfaceLastData) throws LimitExceededException, Exception {
		
		ISFService sfService = intService.getISFService();
		if(mapInterfaceLastData!=null && mapInterfaceLastData.size()>0 && sfService!=null) {
		
			String nsPrefix = sfService.getQueryPackage();
			nsPrefix = nsPrefix.equals("__")?"":nsPrefix;
			
			PartnerConnection binding = sfService.getSFPartner();
			
			List<Interfaces__c> lstInterfaces = new ArrayList<>();
			for(Interfaces__c intf : mapInterfaceLastData.keySet()) {
				lstInterfaces.add(intf);
			}
				
			List<SObject> sos = new ArrayList<SObject>();
			for(int i =0;i<lstInterfaces.size();i++) {
				
				Interfaces__c so = lstInterfaces.get(i);
				
				if(mapInterfaceLastData.containsKey(so)) {
					
					String thisLastData = mapInterfaceLastData.get(so);
					
					if(thisLastData!=null) {
						
						if(so.getLast_Data__c()!=null && !so.getLast_Data__c().equalsIgnoreCase("")){
							Map<String, String> mapPreviousLastData = new HashMap<String, String>();
							String strLDs[] = so.getLast_Data__c().split("\n");
							
							String system="", status="";
							for(String oneData: strLDs){
								String tmp[] = oneData.split(",");
								if(tmp.length>1){
									system = tmp[0].trim();
									status = tmp[7].trim();
									mapPreviousLastData.put(system + status, oneData);
								}
							}
							
							String tmp[] = thisLastData.split(",");
							String thisSystem = tmp[0].trim();
							String thisStatus = tmp[7].trim();
							mapPreviousLastData.put(thisSystem + thisStatus, thisLastData);
							
							String newLastData = "";
							for(String key: mapPreviousLastData.keySet()){
								newLastData += mapPreviousLastData.get(key) + "\n";
							}
							
							so.setLast_Data__c(newLastData);
							
						}else{
							so.setLast_Data__c(thisLastData);
						}
						
						sos.add(SFIntegrationObjectUtils.createSObject(so));
					}
				}
			}
			SObject[] sos1 = new SObject[sos.size()];
			for(int i=0;i<sos.size();i++) {
				sos1[i] = sos.get(i);
			}
			SFIntegrationService.upsert(binding, sos1);
			
			LOGGER.trace(">upsertInterfacesLastData> done.");
		}
	}
	
//	class IntegrationSF2AgentThread {
//		
//		String source = "";
//		public boolean isError;
//		MapIntegrationInfo mapIntgInfo = null;
//		IDBConnection adapter = null;
//		public boolean hasFailRecord = false;
//			
//		//to enable interface-level packet in addition to integration-level packet
//		Integer recordPacket;
//		public void setRecordPacket(Integer r) {
//			if(r==null || r<=0) r = PACKET; //default
//			this.recordPacket = r;
//		}
//		
//		Boolean isReprocessed;
//				
//		//16112010 dateTimeFormat
//		String DATE_TIME_FORMAT = "dd-MM-yyyy HH:mm:ss";
//		
//		Integer bloc = 1000; //nb of message statuses to be updated to SF one time
//		Integer msgIndex = 1;
//		
//		List<IBean> messagesStatusTrack=new ArrayList<IBean>();
//		Set<String> setMessageIds = new HashSet<String>(); //fine tune last data total
//
//		// either Object Id or Message Id (case reprocess)
//		List<String> allObjectIds = new ArrayList<String>();
//		
//		//20012011 Last Data
//		DtoLastData dtoLastData = null;
//		
//		ResultsetMapper mapper = new ResultsetMapper();
//
//		public DtoLastData getDtoLastData() {
//			if(dtoLastData==null){
//				dtoLastData = new DtoLastData(SF_AGENT, new Date(), 0, 0, 0, 0, 0, "In Progress");
//			}
//			return dtoLastData;
//		}
//
//		public IntegrationSF2AgentThread(MapIntegrationInfo mapIntgInfo, IDBConnection adapter, Boolean isReprocessed) throws Exception {
//			
//			this.mapIntgInfo = mapIntgInfo;
//			this.adapter = adapter;
//			this.isReprocessed = isReprocessed;
//
//			source = isReprocessed ? ">IntegrationSF2AgentThread> Reprocess"
//					: ">IntegrationSF2AgentThread> Process";
//			
//			if(adapter.getBatchExecutor() != null){
//				DATE_TIME_FORMAT = adapter.getBatchExecutor().getSFDateTimeFormat();
//			}
//		}
//		
//		//update message on SF
//		private void updateMessages(List<IBean> bs, MapIntegrationInfo mapIntgInfor){
//			
//			if(bs!=null && bs.size()>0) {
//				Integer nb = bs.size();
//				DtoLastData dto = null;
//				try {
//					//21012011 Last Data
//					getDtoLastData().setProcessed(getDtoLastData().getProcessed()+nb);
//					
//					dto = GenericSFTask.updateMessages(bs, mapIntgInfo.getISFService(), mapIntgInfor);
//					
//					//updateMessages success
//					//21012011 Last Data
//					if(dto!=null) {
//						getDtoLastData().setFailed(getDtoLastData().getFailed()+dto.failed);
//						getDtoLastData().setPending(getDtoLastData().getPending()+dto.pending);
//						getDtoLastData().setCompleted(getDtoLastData().getCompleted()+dto.completed);
//					}
//				}
//				catch(Exception ex) {
//				
//					LOGGER.error(source + " >interface: " + mapIntgInfo.getInterfaceName() + " >updateAllMessages> updateMessages bloc: " + bs.size() + " >ERROR: "+ex.getMessage(), ex);
//					ilogs.add(traceError2(mapIntgInfo.getISFService(), "ISERVICE Thread",ex));
//					
//					//updateMessages exception
//					//210012011 Last Data
//					getDtoLastData().setPending(getDtoLastData().getPending()+nb);
//				}
//			}
//		}
//		
//		//20012011 with last data function
//		public void updateAllMessages(MapIntegrationInfo mapIntgInfo) {
//
//			if(messagesStatusTrack!=null && messagesStatusTrack.size()>0) {
//				
//				List<IBean> bsL=new ArrayList<IBean>();
//				
//				for(int i=0;i<messagesStatusTrack.size();i++){
//					
//					bsL.add(messagesStatusTrack.get(i));
//					
//					if(bsL.size()==bloc){
//						
//						updateMessages(bsL, mapIntgInfo);
//						bsL = new ArrayList<IBean>();
//						
//					}
//				}
//				if(bsL.size()>0){
//					//update message on SF
//					updateMessages(bsL, mapIntgInfo);
//				}
//				//end update all messages loop
//				
//				//prepare this current last data info
//				String status = getDtoLastData().getTotal().intValue() == getDtoLastData().getProcessed().intValue() ? "Completed" : "In Progress";
//				String thisLastData = 	getDtoLastData().getSys() 
//										+ "," + getDtoLastData().getRunTime().getTime()
//										+ "," + getDtoLastData().getTotal()
//										+ "," + getDtoLastData().getProcessed() 
//										+ "," + getDtoLastData().getFailed() 
//										+ "," + getDtoLastData().getPending() 
//										+ "," + getDtoLastData().getCompleted() 
//										+ "," + status;
//				mapInterfaceLastData.put(mapIntgInfo.getInterfaces(), thisLastData);
//			}
//		}
//		
//		public void integrateSF2Agent(String ids) throws Exception {
//			
//			if(ids==null || ids.trim().length()<15) {
//				LOGGER.warn(source + " >interface: " + mapIntgInfo.getInterfaceName()  + ">integrateSF2Agent: No ids to be processed.");
//				return;
//			}
//
//			IBean[] filter=null; 
//			JSONArray payloadRecords = null;  //hierarchical data
//			if(mapIntgInfo.getInterfaces().getMessageType__c()==null) {
//				if (isReprocessed) {
//					filter = createFilterOPERATION_SF2AGENT_REPROCESS_IDS(mapIntgInfo.getIntegrationId(), mapIntgInfo.getInterfaceId(), ids);
//				}
//	
//				else {
//					filter = createFilterOPERATION_SF_DATA(mapIntgInfo.getIntegrationId(), mapIntgInfo.getInterfaceId(), ids, DATE_TIME_FORMAT); 
//				}
//			}
//			else {
//				NormalIntegrationV3Event integrationV3Service = new NormalIntegrationV3Event(new MapIntegrationInfo(mapIntgInfo.getSFIntegrationService(), null, mapIntgInfo.getInterfaces(), getRunTime()));
//				payloadRecords =  integrationV3Service.search(ids);
//				LOGGER.trace("Search with ids: " + ids.split(",").length + " and records: " + (payloadRecords!=null?payloadRecords.size():"null"));
//			}
//			//get Message and data from SF
//			//prvent cpu limit exceed in sf
//			IBean[][] records = null; //flat data
//			try{
//				if(filter!=null) {
//					//LOGGER.trace(">GenericSFTask.integrate> Time to Disconect!=="+mapIntgInfo.getInterfaces().getMax_Number_Of_API_Retry__c());
//					//Thread.sleep(5000);
//					records = sfIntegrationService.getISFService().search(filter, mapIntgInfo);
//					LOGGER.trace("Search with ids: " + ids.split(",").length + " and records: " + (records!=null?records.length:"null"));
//				}
//			}catch(Exception ex){
//				if(ex.getMessage().contains("System.LimitException") || ex.getMessage().contains("UNABLE_TO_LOCK_ROW")){
//					String[] tmpIds = ids.split(",");
//					LOGGER.trace("LimitException with tmpIds: " + tmpIds.length);
//					Integer size = tmpIds.length/2;
//					int subPacket = ((size*2 != tmpIds.length)?++size:size);
//					if(subPacket < 1)throw ex;
//					
//					String strIds = "";
//					for(int i=0;i<tmpIds.length;i++){
//						strIds += tmpIds[i]+",";
//						if(i == (subPacket-1)){
//							LOGGER.trace("Split tmpIds by 2: " + size + ", try again with subPacket: " + subPacket + " and strIds: " + strIds.split(",").length);
//							integrateSF2Agent(strIds.substring(0,strIds.length()-1));
//							strIds = "";
//						}
//					}
//					if(strIds.length() > 0){
//						integrateSF2Agent(strIds.substring(0,strIds.length()-1));
//					}
//					return;
//				}
//				else if (SFService.isErrorConnection(ex)) {
//					throw ex;
//				}
//				else{
//					LOGGER.error("GenericSFTask>integrateSF2Agent>sfService.search(filter)>Error: " + ex.getMessage(), ex);
//					throw ex;
//				}
//			}
//			
//			//20120320 v1.16
//			//invoked interface to SFDC back
//			//??List<List<IBean>> responseList = null;
//			
//			if(records!=null && records.length>0 || (payloadRecords!=null && !payloadRecords.isEmpty())){
//				
//				ResponseInterfaceHandler responseInterfaceHandler = null;
//				List<List<IBean>> lsResponseRecords = new ArrayList<List<IBean>>(); 
//				//20012011 manage Last Data
//				try {
//					
//					//20121003 better last data total records
//					Integer nb = ids.split(",").length; //records.length
//					getDtoLastData().setTotal(getDtoLastData().getTotal()+nb);
//					
//					List<IChained_Interfaces__c> Parent_IChained_Interfaces = null;
//					
//					//has child interface
//					List<BatchExecutorResponseItem> response = null;
//					if(records!=null && records.length>0) {
//						Parent_IChained_Interfaces = sfIntegrationService.getIChained_Interfaces__c(mapIntgInfo.getInterfaceId());
//						if(Parent_IChained_Interfaces!=null && Parent_IChained_Interfaces.size()>0){
//							response = adapter.updateChainData(sfIntegrationService, mapIntgInfo.getInterfaces(), records);
//						}else{
//							if(adapter instanceof com.iservice.adapter.XmlAdapter) {
//								response = adapter.update2(sfIntegrationService, mapIntgInfo.getInterfaces(), records);
//							}else {
//								response = adapter.update(records, mapIntgInfo.getInterfaces().getOperationType__c(), sfIntegrationService, mapIntgInfo.getInterfaces());
//							}
//						}	
//					}else {
//						response = adapter.updateV3(sfIntegrationService, payloadRecords, mapIntgInfo.getInterfaces());
//						
//					}
//						
//					// add messages status to update on SF
//					for(BatchExecutorResponseItem i : response) {
//						if(i.getMessaegId()!=null && !setMessageIds.contains(i.getMessaegId())) {
//							IBean b=new IBean("IM#"+msgIndex++, i.getMessaegId()+":" +i.getStatus()+":"+ i.getMessage());
//							messagesStatusTrack.add(b);
//							setMessageIds.add(i.getMessaegId());
//							if(i.getResponseRecords().size()>0 && mapIntgInfo.getInterfaces().getResponseInterface__c()!=null) {
//								lsResponseRecords.addAll(i.getResponseRecords());
//							}
//							// if there are messageError we will set this process to fail to stop processing InterfaceGroup for type EOIO
//							if(i.getStatus().equalsIgnoreCase("Failed")) this.hasFailRecord=true;
//						}
//					}
//					if(lsResponseRecords.size()>0 && mapIntgInfo.getInterfaces().getResponseInterface__c()!=null) {
//						responseInterfaceHandler = ResponseInterfaceFactory(mapIntgInfo.getInterfaces().getResponseInterface__c(), sfIntegrationService, lsResponseRecords);
//						if(isHandleResponseInterface(mapIntgInfo.getInterfaces(), responseInterfaceHandler)) responseInterfaceHandler.process();
//					}
//				}
//				catch(Exception e) {
//					if(SFService.isErrorConnection(e)) {
//						throw e;
//					}
//					else {
//						LOGGER.error(source +" >interface: " + mapIntgInfo.getInterfaceName() + ">Error: "+e,e);
//						// in case push data 2 DBMS failed
//						getDtoLastData().setProcessed(getDtoLastData().getProcessed()+records.length);
//						getDtoLastData().setPending(getDtoLastData().getPending()+records.length);
//						throw e;
//					}
//				}
//			}
//			else {
//				LOGGER.warn(source + " >interface: " + mapIntgInfo.getInterfaceName() + ">integrateSF2Agent: No objects with the given ids to be processed.");
//				return;
//			}
//
//		}
//		//23022011 PE
//		private IBean[][] searchAllObjectIds(String integrationId, String interfaceId, String originalSQL) throws Exception {
//			MapIntegrationInfo mapIntgInfo = new MapIntegrationInfo(sfIntegrationService, sfIntegrationService.getIntegrationById(integrationId), 
//					sfIntegrationService.getInterfaceById(interfaceId), getRunTime());
//			//Build query
//			IBean[] filter = createFilterOPERATION_SF_BUILD_QUERY_INTERFACE(integrationId, interfaceId);
//			IBean[][] queryResult = mapIntgInfo.getISFService().search(filter, mapIntgInfo);
//			
//			String sql = "";
//			if(queryResult==null || queryResult.length==0) {
//				sql = originalSQL;
//			}
//			else {
//				IBean[] q = queryResult[0];
//				sql = q[0].getValue().trim();
//			}
//			//check SQL with space and enter (line breaks)
//			sql = sql.replaceAll("\r", " ").replaceAll("\n", " ");
//			//check SQL with ID selected
//			Integer idxFrom = sql.toLowerCase().indexOf(" from ");
//			if(idxFrom>0) {
//				//26112012 since only ID taken
//				sql = "SELECT Id " + sql.substring(idxFrom+1);
//			}
//			
//			IBean[][] res = null;
//			List<IBean[]> lstArrBeans = new ArrayList<IBean[]>();
//			
//			//Create Binding Stub
//			PartnerConnection binding = mapIntgInfo.getISFService().getSFPartner();
//			
//			//06-07-2017 $LAST_INTEGRATION_DATETIME() for outbound interface
//			sql= CoreJDBCAdapter.replaceFunctionExpressions(sql, interfaceId);
//			
//			//Query sql
//			QueryResult qr = binding.query(sql);
//			boolean done = false;
//			if (qr.getSize() > 0){
//				
//				while (!done) {
//					SObject[] sos = qr.getRecords();
//					IBean[] beans = new IBean[sos.length];
//					for (int i=0;i<sos.length;i++) {
//						beans[i] = new IBean("ID", sos[i].getId().substring(0, 15));
//					}
//					lstArrBeans.add(beans);
//						
//					if (qr.isDone()) {
//						done = true;
//					} else {
//						qr = binding.queryMore(qr.getQueryLocator());
//					}
//				}
//			}
//			res = new IBean[lstArrBeans.size()][];
//			for(int i=0; i<lstArrBeans.size(); i++) {
//				res[i] = lstArrBeans.get(i);
//			}
//			return res;
//		}
//
//		// @Override
//		public void run() throws Exception {
//
//			boolean isError = false;
//			
//			try {
//				//20012011 SF2Agent >10000 records
//				IBean[][] objectIds = null;
//				if (isReprocessed) {
//					IBean[] filter = null;
//					filter = createFilterOPERATION_SF2AGENT_REPROCESS_ALL(mapIntgInfo.getIntegrationId(), mapIntgInfo.getInterfaceId());
//					objectIds = mapIntgInfo.getISFService().search(filter, mapIntgInfo);
//				} 
//				//NOT FROM REPROCESS; MEANS SF2AGENT DIRECT
//				else {
//					//Check originalSQL: ALL ROWS ? -> Yes: 
//					String strAllRow="all rows";
//					if(mapIntgInfo.getOriginalQuery().toLowerCase().contains(strAllRow)){
//						//Case: SOQL with ALL ROWS (max query rows = SFDC query row limit -> webservice - apex)
//						IBean[] filter = null;
//						filter = createFilterOPERATION_SF_ALL_OBJECT_IDS(mapIntgInfo.getIntegrationId(), mapIntgInfo.getInterfaceId());
//						objectIds = mapIntgInfo.getISFService().search(filter, mapIntgInfo);
//					}
//					else{
//						//Case: SOQL withOUT ALL ROWS (max rows: n - queryMore, but ALL ROWS -> ERROR)
//						objectIds = searchAllObjectIds(mapIntgInfo.getIntegrationId(), mapIntgInfo.getInterfaceId(), mapIntgInfo.getOriginalQuery());
//					}
//				}				
//				
//				if(objectIds==null || objectIds.length==0) {
//					if(isReprocessed) {
//						LOGGER.warn(source + ">InterfaceName: " + mapIntgInfo.getInterfaceName()
//								+ " >No Messages to be reprocessed due to either no pending/failed messages or the related integration is not scheduled.");						
//					}
//					else {
//						LOGGER.warn(source + " >InterfaceName: " + mapIntgInfo.getInterfaceName()  + " > has no records for SF-Agent integration.");
//					}
//					return;
//				}
//				
//				for (IBean[] l : objectIds) {
//					for (IBean b : l) {
//						allObjectIds.add(b.getValue());
//					}
//				}
//				
//				if (!isReprocessed) {
//					LOGGER.trace(source+ " >InterfaceName: "+ mapIntgInfo.getInterfaceName() + " has "
//									+ allObjectIds.size()+ " records from SF to be integrated into Agent data source.");
//				} else {
//					LOGGER.trace(source + " >interface:" + mapIntgInfo.getInterfaceName()  + " has "
//							+ allObjectIds.size()+ " messages records from SF to be reprocessed.");
//				}
//
//				// max record processed is PACKET
//				if (allObjectIds != null && allObjectIds.size() > this.recordPacket) {
//					// split
//					int recLength = allObjectIds.size();
//					int remainRec = allObjectIds.size();
//					int counter = 0;
//					String ids = "";
//					int numProcessing = 0;
//					for (int i = 0; i < recLength; i++) {
//						if (counter >= this.recordPacket) {
//							// 13102010
//							// add try catch inner for loop
//							//LOGGER.trace("numProcessing,"+numProcessing+"counter,"+counter+"recLength,"+recLength);
//							try {
//								numProcessing = numProcessing + counter;
//								integrateSF2Agent(ids);
//								LOGGER.trace(source+ ">InterfaceName: " + mapIntgInfo.getInterfaceName() + " > " + numProcessing + " records have been integrated.");
//							}
//							catch (Exception ex) {
//								isError = true;
//								if (SFService.isErrorConnection(ex)) {
//									throw ex;
//								}
//								else {
//									LOGGER.error(source + ">interface:" + mapIntgInfo.getInterfaceName() + ">ERROR at index:" + i + ">" + ex.getMessage(),ex);
//									ilogs.add(traceError2(sfIntegrationService.getISFService(), "ISERVICE Thread",ex));
//								}
//							}
//							finally {
//								counter = 0;
//								ids = "";
//							}
//						}
//						
//						ids = ids + allObjectIds.get(i) + ",";
//						counter++;
//						updateAllMessages(mapIntgInfo);
//						messagesStatusTrack.clear();
//					}
//					
//					// in case remains
//					if (ids != null && ids.length() > 0) {
//						remainRec = remainRec - numProcessing;
//						numProcessing = numProcessing + remainRec;
//						integrateSF2Agent(ids);
//						LOGGER.trace(source+ ">InterfaceName: " + mapIntgInfo.getInterfaceName() + " > " + numProcessing + " records have been integrated.");
//						ids = null;
//					}
//
//				} else {
//
//					String allIds = "";
//					for (String id : allObjectIds) {
//						allIds += id + ",";
//					}
//					integrateSF2Agent(allIds);
//				}
//				
//				updateAllMessages(mapIntgInfo);
//				
//				//LOGGER.trace(source + " >interface: " + mapIntgInfo.getInterfaceName() + " >Done.");
//
//			} catch (Throwable e) {
//				isError = true;
//				if (SFService.isErrorConnection(e)) {
//					throw e;
//				}
//				else {
//					LOGGER.error(source + " >interface: " + mapIntgInfo.getInterfaceName() + ">Error>Step:"+ e, e);
//	
//					try {
//						ilogs.add(traceError2(sfIntegrationService.getISFService(), "ISERVICE Thread", e));
//					} catch (Exception ex) {
//						ilogs.add(traceError2(sfIntegrationService.getISFService(), "ISERVICE Thread", ex));
//					}
//				}
//			} finally {
//				
//				if(!isError) {
//					try {
//						// terminate
//						adapter.terminate();
//					} catch (Exception e) {
//						LOGGER.error(source + " >interface: " + mapIntgInfo.getInterfaceName() + ">Error>Step:adapter.terminate\n"+ e,e);
//					}
//				}
//				
//				//20012011 manage Last data
//				try {
//					new AgentRestService().upsertInterfaceLastDataOnTable(sfIntegrationService, mapInterfaceLastData);
//				} catch (Exception e) {
//					LOGGER.error(source + " >upsertInterfacesLastData:Error" + e,e);
//				}
//
//				if (isError) {
//					try {
//						// unlock xml files if any errors
//						adapter.onError();
//						
//					} catch (Exception ex) {
//						
//						ilogs.add(traceError2(sfIntegrationService.getISFService(), "ISERVICE Thread", ex));
//					}
//				}
//			}
//		}
//	}
	
//	public Map<Interfaces__c, String> integrateSF2AgentManual(IDBConnection adapter, SFIntegrationService integrationService,String integrationId, Interfaces__c intf, String ids) throws Exception {

//		Map<Interfaces__c, String> mapInterfaceLastData = new HashMap<Interfaces__c, String>();
//		MapIntegrationInfo mapIntgInfo = new MapIntegrationInfo(integrationService, integrationService.getIntegrationById(integrationId) , intf, getRunTime());
//		String DATE_TIME_FORMAT = "dd-MM-yyyy HH:mm:ss";
//		if(adapter.getBatchExecutor()!=null){
//			DATE_TIME_FORMAT = adapter.getBatchExecutor().getSFDateTimeFormat();
//		}
//		
//		IBean[] filter=null;
//		JSONArray payloadRecords = null;
//		if(intf.getMessageType__c()==null) {
//			filter = GenericSFTask.createFilterOPERATION_SF_DATA(integrationId, intf.getId(), ids, DATE_TIME_FORMAT);
//		}else{
//			NormalIntegrationV3Event integrationV3Service = new NormalIntegrationV3Event(mapIntgInfo);
//			payloadRecords =  integrationV3Service.search(ids); //query for each sobj by specify its id
//		}
//		//prevent CPU limit exceed in sf
//		IBean[][] records = null;
//		try{
//			if(!isShouldProcess())return null;//stop integration
//			if(filter!=null) records = integrationService.getISFService().search(filter, mapIntgInfo);
//			LOGGER.trace("Search with ids: " + ids.split(",").length + " and records: " + (records!=null?records.length:"null"));
//		}catch(Exception ex){
//			//LOGGER.warn("GenericSFTask> integrateSF2AgentManual> sfService.search(filter)> Error: " + ex.getLocalizedMessage(), ex);
//			if(ex.getMessage().contains("System.LimitException") || ex.getMessage().contains("UNABLE_TO_LOCK_ROW")){
//				String[] tmpIds = ids.split(",");
//				Integer size = tmpIds.length/2;
//				//System.out.println("LimitException with tmpIds: " + tmpIds.length);
//				LOGGER.trace("LimitException with tmpIds: " + tmpIds.length);
//				int subPacket = ((size*2 != tmpIds.length)?++size:size);
//				if(subPacket < 1)throw ex;
//				
//				String strIds = "";
//				for(int i=0;i<tmpIds.length;i++){
//					strIds += tmpIds[i]+",";
//					if(i == (subPacket-1)){
//						if(!isShouldProcess())return null;//stop integration
//						LOGGER.trace("Split tmpIds by 2: " + size + ", try again with subPacket: " + subPacket + " and strIds: " + strIds.split(",").length);
//						mapInterfaceLastData = integrateSF2AgentManual(adapter,integrationService,integrationId,intf,strIds.substring(0,strIds.length()-1));
//						strIds = "";
//					}
//				}
//				if(strIds.length() > 0){
//					if(!isShouldProcess())return null;//stop integration
//					mapInterfaceLastData = integrateSF2AgentManual(adapter,integrationService,integrationId,intf,strIds.substring(0,strIds.length()-1));
//				}
//				return mapInterfaceLastData;
//			}else{
//				throw ex;
//			}
//		}
//		if( (records!=null && records.length>0) || (payloadRecords!=null && !payloadRecords.isEmpty()) ){
//			
//			ResponseInterfaceHandler responseInterfaceHandler = null;
//			List<List<IBean>> lsResponseRecords = new ArrayList<List<IBean>>(); 
//			
//			if(!isShouldProcess())return null;//stop integration
//			List<IChained_Interfaces__c> Parent_IChained_Interfaces = null; 
//			//has child interface
//			List<BatchExecutorResponseItem> response = null;
//			//for flat data format
//			if(records!=null && records.length>0) {
//				Parent_IChained_Interfaces = integrationService.getIChained_Interfaces__c(intf.getId());
//				if(Parent_IChained_Interfaces!=null && Parent_IChained_Interfaces.size()>0){
//					response = adapter.updateChainData(integrationService, intf, records);
//				}else{
//					if(adapter instanceof com.iservice.adapter.XmlAdapter) {
//						response = adapter.update2(integrationService, intf, records);
//					}else {
//						response = adapter.update(records, intf.getOperationType__c(), integrationService, intf);	
//					}
//				}
//			}
//			//for hierarchical data
//			else {
//				response =  adapter.updateV3(integrationService , payloadRecords, intf);
//			}
//			
//			if(!isShouldProcess())return null;//stop integration
//			// add messages status to update on SF
//			List<IBean> messagesStatusTrack=new ArrayList<IBean>();
//			for(int c=0;c<response.size();c++) {
//				BatchExecutorResponseItem i = response.get(c);
//				if(i.getMessaegId()!=null){
//					IBean b=new IBean("IM#"+c, i.getMessaegId()+":" +i.getStatus()+":"+ i.getMessage());
//					messagesStatusTrack.add(b);
//					if(i.getResponseRecords().size()>0 && intf.getResponseInterface__c()!=null) {
//						lsResponseRecords.addAll(i.getResponseRecords());
//					}
//				}
//			}
//			
//			if(messagesStatusTrack!=null && messagesStatusTrack.size()>0) {
//				List<IBean> bsL=new ArrayList<IBean>();
//				for(int i=0;i<messagesStatusTrack.size();i++){
//					bsL.add(messagesStatusTrack.get(i));
//					if(bsL.size()==1000){
//						updateMsgOnSF(messagesStatusTrack, integrationService, mapIntgInfo);
//						bsL = new ArrayList<IBean>();
//					}
//				}
//				if(bsL.size()>0){
//					//update message on SF
//					updateMsgOnSF(messagesStatusTrack, integrationService, mapIntgInfo);
//				}
//			}
//		
//			if(!isShouldProcess())return null;//stop integration
//			//prepare this current last data info
//			String status = (getDtoLastData().getTotal().intValue() == getDtoLastData().getProcessed().intValue() ? "Completed" : "In Progress");
//			String thisLastData = 	getDtoLastData().getSys() 
//									+ "," + getDtoLastData().getRunTime().getTime()
//									+ "," + getDtoLastData().getTotal()
//									+ "," + getDtoLastData().getProcessed() 
//									+ "," + getDtoLastData().getFailed() 
//									+ "," + getDtoLastData().getPending() 
//									+ "," + getDtoLastData().getCompleted() 
//									+ "," + status;
//			mapInterfaceLastData.put(intf, thisLastData);
//			
//			if(lsResponseRecords.size()>0 && intf.getResponseInterface__c()!=null) {
//				responseInterfaceHandler = ResponseInterfaceFactory(intf.getResponseInterface__c(), integrationService, lsResponseRecords);
//				if(isHandleResponseInterface(intf, responseInterfaceHandler))  responseInterfaceHandler.process();
//			}
//		}
//		return mapInterfaceLastData;
//		return null;
//	}
	
	protected ResponseInterfaceHandler ResponseInterfaceFactory(String reponseIntfId, SFIntegrationService integrationService, List<List<IBean>> lsResponseRecords) throws Exception {
		Interfaces__c responseIntf = integrationService.getInterfaceById(reponseIntfId);
		Integration__c responseIntg = integrationService.getIntegrationById(responseIntf.getIntegration__c());
		MapIntegrationInfo mapResponseIntgInfo = new MapIntegrationInfo(integrationService, responseIntg , responseIntf, getRunTime());
		if(responseIntf.getMessageType__c()==null && lsResponseRecords.size()>0) {
			return new ResponseInterfaceHandlerV2(mapResponseIntgInfo, lsResponseRecords);
		}else {
			List<XmlNode> lsResponseRecV3 = new ArrayList<XmlNode>();
			for(List<IBean> oneRec : lsResponseRecords) {
				XmlNode oneNode = new XmlNode(false);
				for(IBean oneField : oneRec) {
					XmlField oneNodeField = new XmlField();
					oneNodeField.setName(oneField.getName());
					oneNodeField.setValue(oneField.getValue());
					oneNode.addField(oneNodeField);
				}
				oneNode.setName(responseIntf.getSource_Name__c());
				lsResponseRecV3.add(oneNode);
			}
			return new ResponseInterfaceHandlerV3(mapResponseIntgInfo, lsResponseRecV3);
		}
	}
	
	protected boolean isHandleResponseInterface(Interfaces__c intf, ResponseInterfaceHandler responseIntfHandler) {
		if( intf.getInterface_Mode__c().equalsIgnoreCase("Synchronous") && 
			intf.getResponseInterface__c() != null &&
			responseIntfHandler != null && responseIntfHandler.hasRecordData() ) {
			LOGGER.trace(">Handle a Response-Interface >True");
			return true;
		}
		else {
			LOGGER.trace(">Handle a Response-Interface >False");
			return false;
		}
	}
	
	public static DtoLastData dtoLastData = null;
	
	public static DtoLastData getDtoLastData() {
		
		if(dtoLastData==null){
			dtoLastData = new DtoLastData(SF_AGENT, new Date(), 0, 0, 0, 0, 0, "In Progress");
		}
		return dtoLastData;
	}
	
	public static void updateMsgOnSF(List<IBean> messagesStatusTrack, SFIntegrationService integrationService, MapIntegrationInfo mapIntgInfo) {
		//end update all messages loop
		if(messagesStatusTrack!=null && messagesStatusTrack.size()>0) {
			Integer nb = messagesStatusTrack.size();
			DtoLastData dto = null;
			try {
				//16-05-2017 Last Data
				getDtoLastData().setProcessed(getDtoLastData().getProcessed()+nb);
				
				dto = GenericSFTask.updateMessages(messagesStatusTrack, integrationService.getISFService(), mapIntgInfo);
				
				//updateMessages success
				//16-05-2017 Last Data
				if(dto!=null) {
					getDtoLastData().setFailed(getDtoLastData().getFailed()+dto.failed);
					getDtoLastData().setPending(getDtoLastData().getPending()+dto.pending);
					getDtoLastData().setCompleted(getDtoLastData().getCompleted()+dto.completed);
				}
				
			}
			catch(Exception ex) {
				//updateMessages exception
				//210012011 Last Data
				getDtoLastData().setPending(getDtoLastData().getPending()+nb);
			}
		}
	}

	public static class DtoLastData {
		
		private String sys;
		private Integer failed = 0, pending = 0, completed = 0, total = 0, processed = 0;
		private String status;
		private Date runTime;
		
		/**
		 * @param runTime
		 * @param total
		 * @param processed
		 * @param failed
		 * @param pending
		 * @param completed
		 * @param status
		 */
		public DtoLastData(String sys, Date runTime, Integer total, Integer processed,
				Integer failed, Integer pending, Integer completed,
				String status) {
			super();
			this.sys = sys;
			this.runTime = runTime;
			this.total = total;
			this.processed = processed;
			this.failed = failed;
			this.pending = pending;
			this.completed = completed;
			this.status = status;
		}


		public Integer getFailed() {
			return failed;
		}

		public void setFailed(Integer failed) {
			this.failed = failed;
		}

		public Integer getPending() {
			return pending;
		}

		public void setPending(Integer pending) {
			this.pending = pending;
		}

		public Integer getCompleted() {
			return completed;
		}

		public void setCompleted(Integer completed) {
			this.completed = completed;
		}


		public String getSys() {
			return sys;
		}


		public void setSys(String sys) {
			this.sys = sys;
		}


		public Integer getTotal() {
			return total;
		}


		public void setTotal(Integer total) {
			this.total = total;
		}


		public Integer getProcessed() {
			return processed;
		}


		public void setProcessed(Integer processed) {
			this.processed = processed;
		}


		public String getStatus() {
			return status;
		}


		public void setStatus(String status) {
			this.status = status;
		}


		public Date getRunTime() {
			return runTime;
		}


		public void setRunTime(Date runTime) {
			this.runTime = runTime;
		}
	}
	
	public static List<String> convertToListArray(String[] sobjs){
		List<String> arrs=new ArrayList<String>();
		for(int i=0;i<sobjs.length;i++){
			arrs.add(sobjs[i]);
		}
		return arrs;
	}
	
	public String getScheduledPropertyFile() {
		return scheduledPropertyFile;
	}

	public void setScheduledPropertyFile(String scheduledPropertyFile) {
		this.scheduledPropertyFile = scheduledPropertyFile;
	}

	public String getScheduledInterfaceId() {
		return scheduledInterfaceId;
	}

	public void setScheduledInterfaceId(String scheduledInterfaceId) {
		this.scheduledInterfaceId = scheduledInterfaceId;
	}

	public String getScheduledIntegrationId() {
		return scheduledIntegrationId;
	}

	public void setScheduledIntegrationId(String scheduledIntegrationId) {
		this.scheduledIntegrationId = scheduledIntegrationId;
	}
	
	public static List<String> getPropertyFileName(){
		List<String> lsPfs = new ArrayList<String>();
		try{
			File folder = new File(Helper.getAgentHome() + Helper.getFileSeparator());
			
			FilenameFilter textFilter = new FilenameFilter() {
		        @Override
				public boolean accept(File dir, String name) {
		            return name.toLowerCase().endsWith(".properties");
		        }
		    };
		    
		    File[] files = folder.listFiles(textFilter);
		    for (File file : files) {
		    	lsPfs.add(file.getName());
		    }
		    LOGGER.trace("GenericSFTask>getPerpertyFileName()>lsPfs: " + lsPfs);
		}catch(Exception ex){
			LOGGER.trace("GenericSFTask>getPerpertyFileName(): " + ex.getLocalizedMessage());
		}
		return lsPfs;
	}
	
	/**
	 * This method check if the id (either integration_id or interface_id) passed from iScheduleSetting file is in the property file
	 * @return Integration__c
	 * @throws Exception
	 */
	public Integration__c getIntegrationToSchedule() throws Exception{
		sfIntegrationService = new SFIntegrationService(mapSFConnInfo);
		sfIntegrationService.login();
		Integration__c integration = null;
		if(iSchedulerSetting.getSchedulertype().contains(ISchedulerSetting.PROCESS_PER_INTERFACE_GROUP)) {
			// in this case we borrow interfaceId field to store interface group id 
			integration = sfIntegrationService.getIntegration__BasedInterfaceGroup(iSchedulerSetting.getIntegrationid(), iSchedulerSetting.getInterfaceid());
		}else {
			integration = sfIntegrationService.getIntegrationById(iSchedulerSetting.getIntegrationid());
		}
		if(integration != null) {
			if(!iSchedulerSetting.getSchedulertype().contains(ISchedulerSetting.PROCESS_PER_INTERFACE_GROUP) && StringUtils.isNotBlank(iSchedulerSetting.getInterfaceid())) {
				Interfaces__c interfacee = sfIntegrationService.getInterfaceById(iSchedulerSetting.getInterfaceid());
				if(interfacee == null) {
					throw new GenericSFTaskException("Invalid Interface: [ Id= "+iSchedulerSetting.getInterfaceid()+" , Name= "+ iSchedulerSetting.getInterfacename() + 
							" ] in Integration: [ Id= "+iSchedulerSetting.getIntegrationid()+" , Name= "+ iSchedulerSetting.getIntegrationname() + " ] with Property File Name: " + scheduledPropertyFile);
				}
			}else if(integration.getInterface_groups().size()==0 && StringUtils.isNotBlank(iSchedulerSetting.getInterfaceid())){
				throw new GenericSFTaskException("Invalid InterfaceGroup: [ Id= "+iSchedulerSetting.getInterfaceid()+" , Name= "+ iSchedulerSetting.getInterfacename() + 
						" ] in Integration: [ Id= "+iSchedulerSetting.getIntegrationid()+" , Name= "+ iSchedulerSetting.getIntegrationname() + " ] with Property File Name: " + scheduledPropertyFile);
			}
		}else {
			throw new GenericSFTaskException("Invalid Integration: [ Id= "+iSchedulerSetting.getIntegrationid()+" , Name= "+ iSchedulerSetting.getIntegrationname() + " ] with Property File Name: " + scheduledPropertyFile);
		}
		return integration;
	}
	
	/**
	 * Sokdet #3005
	 * @return header of csv, excel, xml? in case Data no header
	 */
	public static List<? extends ISFIntegrationObject> getListHeader(Interfaces__c myInterface, List<Property> properties, SFIntegrationService intService) throws Exception{
		List<? extends ISFIntegrationObject> listHeader = null;
		for(Property p:properties){
			if(p.getName().equals(PropertyNameHelper.HAS_FILE_HEADER) && p.getValue().equals("false")){
				//get IStructure or MessageType
				if(intService!=null){
					//MessageType!=null 
					if(myInterface.getMessageType__c()!=null){
						listHeader = intService.getMsgTypeFieldEntry__c(myInterface.getMessageType__c());
					}
					//IStructure==null
					else if(myInterface.getIStructures()!=null) {
						listHeader = myInterface.getIStructures();
						
					}else{
						throw new Exception("Interface has no MessageType or IStructure.");
					}
				}
				break;
			}
		}
		
		return listHeader;
	}
	public static void createMsgToSF(SFIntegrationService intService,Integration__c integration, Interfaces__c intf, 
			String status, String id, String message, String fileName) throws Exception{
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.ssssss");
		String datetime = (formatter.format(Calendar.getInstance().getTime())).trim();
		boolean isPushOnlyFailMessage = intf.isIsNotPersistMessage__c()||integration.getClear_After_Processing__c();
		
		IMessage__c imsg = new IMessage__c(DEMO32?"":"skyvvasolutions__");
		imsg.setIntegration__c(integration.getId());
		imsg.setInterface__c(intf.getId());
		imsg.setType__c(DirectionTypeHelper.INBOUND);
		imsg.setStatus__c(status);
		imsg.setRelated_To__c(id);
		imsg.setComment__c(message);
		imsg.setName("IM#"+datetime);
		imsg.setExtValue__c(fileName);

		SObject so =  SFIntegrationObjectUtils.createSObject(imsg);
		try{
			if(intf.isBy_Passing_Message__c()) {
				// do nth
			}else if(isPushOnlyFailMessage && status.equalsIgnoreCase("Failed")) {
				intService.upsert(new SObject[]{so});
			}else if(!intf.isBy_Passing_Message__c() && !isPushOnlyFailMessage){
				intService.upsert(new SObject[]{so});
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	public static void closeQuietly(final Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (final IOException ioe) {
            // ignore
        }
    }
	
	public static String replaceLast(String string, String toReplace, String replacement) {
		
		StringBuilder builder = new StringBuilder();
		int start = string.lastIndexOf(toReplace);
		builder.append(string.substring(0, start))
			.append(replacement)
			.append(string.substring(start + toReplace.length()));
		
		return builder.toString();
	}
	
	public static Map<String, String> convertListBeanToMap(List<IBean> list) {
		Map<String, String> result = new HashMap<>();
		
		for(IBean b: list) {
			result.put(b.getName(), b.getValue());
		}
		
		return result;
	}	
	
	// use in IntegrationDetailsController
	private static boolean shouldProcess = true;
	public void setShouldProcess(boolean shouldProcess) {
		GenericSFTask.shouldProcess = shouldProcess;
	}
	public static boolean isShouldProcess() {
		return shouldProcess;
	}
	// end use in IntegrationDetailsController
	
	public static void main(String[] args) throws Exception{
		
		SQLite.getInstance();
		PropertySettingDao dao = PropertySettingDao.getInstance();
		MapSFConnInfo conn = dao.getAllValueByFileName("test.properties");
		
		SFIntegrationService service = new SFIntegrationService(conn);
		service.login();
		
//		ISFService sfService = service.getISFService();
//		
//		String nsPrefix = sfService.getQueryPackage();
//		PartnerConnection sfPartner = sfService.getSFPartner();
		
//		ISchedulerSetting iSchedulerSetting = BaseDao.getDao(ISchedulerSettingDao.class).getISchedulerSettingBySchedulerType("AgentProcessPerInterface#a0a3E000006inuK");
//		
//		GenericSFTask taske = new GenericSFTask(iSchedulerSetting);
//		taske.doIntegrationService();
		
//		Thread thread = new Thread(new Runnable() {
//			@Override
//			public void run() {
//				taske.doIntegrationService();
//			}
//		});
//		thread.start();
		
//		SObject objIntf = new SObject( nsPrefix + "Interfaces__c");
//		objIntf.setId("a0V6F00000kmSJu");
//		
//		objIntf.setField( nsPrefix + "LastRun__c", new GenericSFTask().retrieveLast_RunDate());
//		objIntf.setField( nsPrefix + "Next_Run__c", new GenericSFTask().retrieveNext_RunDate(iSchedulerSetting));
//		
//		SObject[] arrSObject=new SObject[1]; 
//		arrSObject[0] = objIntf;
//		
//		SaveResult[] srIntf = sfPartner.update(arrSObject);
//		StringBuffer strErrors = new StringBuffer();
//		if(srIntf != null) {		
//			for(int i=0; i<srIntf.length; i++){
//				if(!srIntf[i].isSuccess() && srIntf[i].getErrors()!=null){
//					for(int j=0; j<srIntf[i].getErrors().length; j++){
//						strErrors.append(srIntf[i].getId() + " : " + srIntf[i].getErrors()[j].getMessage() + "\n");
//					}
//				}
//			}
//		}
	}
	
	public static void systemLog() {

		try {

			LOGGER.trace("==================== Java Information ====================" +
					"\nJava VM Version: " + System.getProperty("java.vm.version") +  //eg: 1.6.0_01-b06
					"\nJava VM Name: " + System.getProperty("java.vm.name") + //Java HotSpot(TM) Client VM
					"\nJava Version: " + System.getProperty("java.version") + //1.6.0_01
					"\nJava Vendor: " + System.getProperty("java.vendor") + //Sun Microsystems Inc.	   
					
					"\nJava Specification Version: " + System.getProperty("java.specification.version") + //1.6
					"\nJava Specification Name: " + System.getProperty("java.specification.name")+ //Java Platform API Specification
					
					"\nJava Runtime Version: " + System.getProperty("java.runtime.version") + //1.6.0_01-b06
					"\nJava Runtime Name: " + System.getProperty("java.runtime.name")+ //Java(TM) SE Runtime Environment

					"\nJava Home: " + System.getProperty("java.home")+ //D:\apps\JavaDev2\jdk1.6.0_01\jre
					"\nJava Library Path: " + System.getProperty("java.library.path")+ //D:\apps\JavaDev2\jdk1.6.0_01\bin;.;C:\WINDOWS\Sun\Java\bin;C:\WINDOWS\system32;C:\WINDOWS;D:/apps/JavaDev2/jdk1.6.0_01/bin/../jre/bin/client;c:\oraclexe\app\oracle\product\10.2.0\server\bin;...
					"\nSun Boot Class Path: " + System.getProperty("sun.boot.class.path") + //D:\apps\JavaDev2\jdk1.6.0_01\jre
					"\nSun Boot Library Path: " + System.getProperty("sun.boot.library.path") + //D:\apps\JavaDev2\jdk1.6.0_01\jre\bin
					"\nSun Java Launcher: " + System.getProperty("sun.java.launcher")+ //SUN_STANDARD
					"\nSun Arch Data Model: " + System.getProperty("sun.arch.data.model")+ //32 | 64

					"\n==================== OS and CPU Information ====================" +
					"\nOS Name: " + System.getProperty("os.name") + //Windows XP
					"\nOS Arch: " + System.getProperty("os.arch") + //x86
					"\nOS Version: " + System.getProperty("os.version") + //5.1
					"\nAvailable Processors: " + Runtime.getRuntime().availableProcessors() +	   

					"\n==================== Memory Information ====================" +
					"\nMemory: " + getMemoryMXBean().getHeapMemoryUsage()+
					"\nThe amount of free memory in the Java Virtual Machine: " + Runtime.getRuntime().freeMemory()+
					"\nThe amount of total memory in the Java Virtual Machine: " + Runtime.getRuntime().totalMemory()+
					"\nThe amount of max memory in the Java Virtual Machine: " + Runtime.getRuntime().maxMemory());

			/**
		   key : sun.boot.library.path; value: D:\apps\JavaDev2\jdk1.6.0_01\jre\bin
		   key : java.vm.version; value: 1.6.0_01-b06
		   key : java.vm.vendor; value: Sun Microsystems Inc.
		   key : java.vendor.url; value: http://java.sun.com/
		   key : path.separator; value: ;
		   key : java.vm.name; value: Java HotSpot(TM) Client VM
		   key : file.encoding.pkg; value: sun.io
		   key : sun.java.launcher; value: SUN_STANDARD
		   key : user.country; value: US
		   key : sun.os.patch.level; value: Service Pack 3
		   key : java.vm.specification.name; value: Java Virtual Machine Specification
		   key : user.dir; value: D:\apps\JavaDev2\EclipseWorkspace\iodservice
		   key : java.runtime.version; value: 1.6.0_01-b06
		   key : java.awt.graphicsenv; value: sun.awt.Win32GraphicsEnvironment
		   key : java.endorsed.dirs; value: D:\apps\JavaDev2\jdk1.6.0_01\jre\lib\endorsed
		   key : os.arch; value: x86
		   key : java.io.tmpdir; value: C:\DOCUME~1\home\LOCALS~1\Temp\
		   key : line.separator; value: 

		   key : java.vm.specification.vendor; value: Sun Microsystems Inc.
		   key : user.variant; value: 
		   key : os.name; value: Windows XP
		   key : sun.jnu.encoding; value: Cp1258
		   key : java.library.path; value: D:\apps\JavaDev2\jdk1.6.0_01\bin;.;C:\WINDOWS\Sun\Java\bin;C:\WINDOWS\system32;C:\WINDOWS;D:/apps/JavaDev2/jdk1.6.0_01/bin/../jre/bin/client;D:/apps/JavaDev2/jdk1.6.0_01/bin/../jre/bin;c:\oraclexe\app\oracle\product\10.2.0\server\bin;C:\WINDOWS\system32;C:\WINDOWS;C:\WINDOWS\system32\wbem;c:\program files\microsoft sql server\80\tools\binn;c:\program files\microsoft sql server\90\tools\binn\;c:\program files\mysql\mysql server 5.5\bin;c:\program files\openldap\kfw\binary;c:\program files\mit\kerberos\bin;c:\program files\ibm\gsk8\lib;C:\PROGRA~1\IBM\TDSV6~1.3DB\BIN;C:\PROGRA~1\IBM\TDSV6~1.3DB\FUNCTION;C:\PROGRA~1\IBM\TDSV6~1.3DB\SAMPLES\REPL
		   key : java.specification.name; value: Java Platform API Specification
		   key : java.class.version; value: 50.0
		   key : sun.management.compiler; value: HotSpot Client Compiler
		   key : os.version; value: 5.1
		   key : user.home; value: C:\Documents and Settings\home
		   key : user.timezone; value: 
		   key : java.awt.printerjob; value: sun.awt.windows.WPrinterJob
		   key : file.encoding; value: UTF-8
		   key : java.specification.version; value: 1.6
		   key : java.class.path; value: D:\apps\JavaDev2\EclipseWorkspace\iodservice\bin;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\activation.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\avalon-framework-4.1.4.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\axis.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\commons-beanutils-1.7.0.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\commons-codec-1.2.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\commons-collections-3.2.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\commons-configuration-20030620.073343.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\commons-discovery-0.2.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\commons-email-1.0.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\commons-httpclient-contrib.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\commons-httpclient.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\commons-lang-2.1.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\commons-logging-1.0.4.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\commons-logging-1.1.1.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\DJNativeSwing-SWT.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\DJNativeSwing.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\edtftpj.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\ejb-api.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\jaxrpc.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\log4j-1.2.15.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\mail.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\ojdbc14_g.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\ostermillerutils_1_07_00.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\poi-2.5.1-final-20040804.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\poi-contrib-2.5.1-final-20040804.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\poi-scratchpad-2.5.1-final-20040804.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\saaj.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\salesforce-api-1.0.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\servlet-api-2.4.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\servlet.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\sqljdbc4.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\swt-3.6-win32-win32-x86.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\turbine-20030610.165428.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\wsdl4j-1.5.1.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\xml-apis-2.3.0.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\xmlrpc-2.0.jar;D:\apps\JavaDev2\eclipse\plugins\org.junit_3.8.2.v20080602-1318\junit.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\xstream-1.3.1.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\jettison-1.0.1.jar;D:\apps\JavaDev2\EclipseWorkspace\iodservice\lib\mysql-connector-java-5.1.15-bin.jar
		   key : user.name; value: home
		   key : java.vm.specification.version; value: 1.0
		   key : java.home; value: D:\apps\JavaDev2\jdk1.6.0_01\jre
		   key : sun.arch.data.model; value: 32
		   key : user.language; value: en
		   key : java.specification.vendor; value: Sun Microsystems Inc.
		   key : awt.toolkit; value: sun.awt.windows.WToolkit
		   key : java.vm.info; value: mixed mode
		   key : java.version; value: 1.6.0_01
		   key : java.ext.dirs; value: D:\apps\JavaDev2\jdk1.6.0_01\jre\lib\ext;C:\WINDOWS\Sun\Java\lib\ext
		   key : sun.boot.class.path; value: D:\apps\JavaDev2\jdk1.6.0_01\jre\lib\resources.jar;D:\apps\JavaDev2\jdk1.6.0_01\jre\lib\rt.jar;D:\apps\JavaDev2\jdk1.6.0_01\jre\lib\sunrsasign.jar;D:\apps\JavaDev2\jdk1.6.0_01\jre\lib\jsse.jar;D:\apps\JavaDev2\jdk1.6.0_01\jre\lib\jce.jar;D:\apps\JavaDev2\jdk1.6.0_01\jre\lib\charsets.jar;D:\apps\JavaDev2\jdk1.6.0_01\jre\classes
		   key : java.vendor; value: Sun Microsystems Inc.
		   key : file.separator; value: \
		   key : java.vendor.url.bug; value: http://java.sun.com/cgi-bin/bugreport.cgi
		   key : sun.io.unicode.encoding; value: UnicodeLittle
		   key : sun.cpu.endian; value: little
		   key : sun.desktop; value: windows
		   key : sun.cpu.isalist; value: pentium_pro+mmx pentium_pro pentium+mmx pentium i486 i386 i86
			 */

		}
		catch(Exception e) {
			LOGGER.error("systemLog(): ERROR: " + e);
		}
	}

}
