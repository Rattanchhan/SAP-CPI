package com.iservice.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.lang3.StringUtils;

//import com.iservice.database.ISchedulerSettingDao;
import com.iservice.database.PropertySettingDao;
import com.iservice.gui.helper.Helper;
import com.iservice.sforce.MapSFConnInfo;
import com.iservice.sforce.SFIntegrationService;
import com.iservice.task.LastRunDateCache;

public class FileHelper {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(FileHelper.class);

	public static boolean hasWildCardCharacter(String filename) {
		return filename!=null && (filename.contains("*") || filename.contains("?"));
	}
	//17-11-2011 return filename with full path
	public static String getFilename(String filename) throws IOException{
		String fileSeperator = Helper.getFileSeparator();
		String path = Helper.getAgentHome();
		path = FilePathHelper.removeExtraPath(path, fileSeperator + Helper.LIBS_DIRECTORY);
		return path + fileSeperator + filename;
	}

	public synchronized static void saveLastRunDateCache(String operationCacheFilename, LastRunDateCache  lastRunDateCache) throws Exception {
		if(lastRunDateCache!=null && StringUtils.isNotBlank(operationCacheFilename)) {
            ObjectOutput out = new ObjectOutputStream(new FileOutputStream(operationCacheFilename));
            out.writeObject(lastRunDateCache);
            out.close();
		}
		else throw new Exception("FileHelper.saveObject: Warning! No filename is available or specified AND/OR No object data to be saved!");
	}
	
	public synchronized static LastRunDateCache readLastRunDateCache(String operationCacheFilename) throws Exception {
		if(StringUtils.isNotBlank(operationCacheFilename)) {
            File file = new File(operationCacheFilename);
            if(!file.exists() || file.length()==0)return null;
            try {
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
                LastRunDateCache operationCache = (LastRunDateCache) inputStream.readObject();
                inputStream.close();
                return operationCache;
            } catch (ClassNotFoundException | IOException e) {
                return null;
            }
		}
		return null;
	}

	public static Object readObject(InputStream is) throws Exception {

		if(is==null) throw new Exception("FileHelper.readObject: Warning! No inputstream is available!");

		// Deserialize from a file
		ObjectInputStream in = new ObjectInputStream(is);
		// Deserialize the object
		Object obj = in.readObject();
		in.close();

		return obj;

	}
	public synchronized static String readCronTabFile(InputStream in, String lineInfileAsCondition) throws IOException {
		String cronValue="";
		String strLine;
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));	
		while ((strLine = reader.readLine()) != null) {
			strLine = strLine.replace((char)65279, ' ').trim();
			if(!(strLine.startsWith("#") || strLine.startsWith("!")) && !strLine.trim().equals("")){
				if(strLine.substring(strLine.indexOf("com.iservice.")).trim().equals(lineInfileAsCondition)){					
					cronValue=strLine;
					cronValue=cronValue.substring(0,cronValue.indexOf("com.iservice.")).trim();
					break;
				}
			}
		}
		reader.close();
		return cronValue;
	}
	/**
	 * @author Ponnreay
	 * 
	 * return all entries schedule in crontab file
	 * @return List&#60;CronEntryBean&#62;
	 * @throws Exception
	 */
	public static List<CronEntryBean> getCronEntries() throws Exception{
		List<CronEntryBean> list = new ArrayList<>();
		InputStream in = new FileInputStream(Helper.getCrontabFile());
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));	

		String strLine = "";
		while ((strLine = reader.readLine()) != null) {
			strLine = strLine.replace((char)65279, ' ').trim();//clean up ghost character
			if(!(strLine.startsWith("#") || strLine.startsWith("!")) && !strLine.trim().equals("")){		
				list.add(new CronEntryBean(strLine));
			}
		}
		if(reader!=null)reader.close();
		return list;
	}

	// if user change username
	public static void isChangeUsername(MapSFConnInfo mapConInfo, SFIntegrationService intService) throws Exception {
		// read property file
		MapSFConnInfo mapInfo = PropertySettingDao.getInstance().getSelectedProperties();
		String oldUsername = String.valueOf(mapInfo.getUsername());
		String newUsername = mapConInfo.get(MapSFConnInfo.USERNAME_P);
		if(StringUtils.isNotBlank(oldUsername) && !oldUsername.equalsIgnoreCase(newUsername)) {
			LOGGER.trace("isChangeUsername(): true, old Username: "+oldUsername+" to new Username: "+ newUsername);
//			replaceUserNameISchedulerSettings(oldUsername, newUsername);
		}
		mapConInfo.setAgentUsername(mapInfo.getAgentUsername());
		mapConInfo.setAgentPassword(mapInfo.getAgentPassword());
		mapConInfo.setHostName(mapInfo.getHostName());
		mapConInfo.setPortForward(mapInfo.getPortForward());
		PropertySettingDao.getInstance().savePropertiesToDatabase(mapConInfo);
		intService.createSettingToSF();
	}
	/*
	public static void replaceUserNameISchedulerSettings(String oldUsername, String newUsername) throws Exception{
		List<ISchedulerSetting> listEntries = BaseDao.getDao(ISchedulerSettingDao.class).getAllSchedulerByUsername(oldUsername);
		for(ISchedulerSetting iSch : listEntries) {
			iSch.setUsername(newUsername);
			iSch.setCreatedby(newUsername);
			iSch.setCreateddate(iSch.getCreateddate());
			iSch.setModifiedby(newUsername);
			iSch.setModifieddate(iSch.getModifieddate());
			iSch.setSyncstatus(ISchedulerSettingHelper.UPD_CRON);
			// push to database
			BaseDao.getDao(ISchedulerSettingDao.class).bulkUpsert(new Gson().toJsonTree(iSch).getAsJsonObject());
		}
	}
	*/
	public static List<String> readCrontabFileListEntriesON() throws Exception{
		List<String> list = new ArrayList<>();
		List<String> entries = readCrontabFileListEntries();
		for(String entry: entries){
			String[] switchModes = entry.split(" ");
			if(entry.contains("com.iservice.integration.IntegrationSF2AgentService")){ //OutBound
				if(switchModes.length == 8){ //re processing
					if("true".equals(switchModes[6])) {
						list.add(entry);
					}
				}else{ //processing
					if("true".equals(switchModes[7])) {
						list.add(entry);
					}
				}
			}else{ //InBound
				if("true".equals(switchModes[6])) {
					list.add(entry);
				}
			}
		}
		return list;
	}
	
	public static List<String> readCrontabFileListEntries(String crontabFileName) throws Exception{
		List<String> list = new ArrayList<>();
		InputStream in = new FileInputStream(crontabFileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));	
		String strLine = "";
		while ((strLine = reader.readLine()) != null) {
			strLine = strLine.replace((char)65279, ' ').trim();//clean up ghost character
			if(!(strLine.startsWith("#") || strLine.startsWith("!")) && !strLine.trim().equals("")){		
				list.add(strLine);
			}
		}
		if(reader!=null)reader.close();		
		return list;
	}
	
	public static List<String> readCrontabFileListEntries() throws Exception{
		List<String> list = new ArrayList<>();
		InputStream in = new FileInputStream(Helper.getCrontabFile());
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));	

		String strLine = "";
		while ((strLine = reader.readLine()) != null) {
			strLine = strLine.replace((char)65279, ' ').trim();//clean up ghost character
			if(!(strLine.startsWith("#") || strLine.startsWith("!")) && !strLine.trim().equals("")){		
				list.add(strLine);
			}
		}
		if(reader!=null)reader.close();
		return list;
	}
	/*
	public static void writeCrontabFileListEntries(String dir) throws IOException{
		StringBuffer strData = new StringBuffer();
		strData.append("# Tasks configuration file.\r\n");
		strData.append("# IMPORTANT: All the index begin in 0, except day of month and\r\n\r\n");
		strData.append("#   Seconds	 			0-59	 			, - * /\r\n");
		strData.append("#	Minutes	 			0-59	 			, - * /\r\n");
		strData.append("#	Hours	 			0-23	 			, - * /\r\n");
		strData.append("#	Day-of-month	 	1-31	 			, - * ? / L W\r\n");
		strData.append("#	Month	 			1-12 or JAN-DEC	 	, - * /\r\n");
		strData.append("#	Day-of-Week	 		1-7 or SUN-SAT	 	, - * ? / L #\r\n");
		strData.append("#	Year (Optional)	 	empty, 1970-2199	, - * /\r\n\r\n");
		strData.append("# IMPORTANT: The first day of the week is Sunday\r\n\r\n");
				
		List<ISchedulerSetting> iSchedulerSettings = BaseDao.getDao(ISchedulerSettingDao.class).getAllScheduler();
		List<String> newCronExpList = new ArrayList<>();
		
		for(ISchedulerSetting iSchedulerSetting : iSchedulerSettings) {
			newCronExpList.add(iSchedulerSetting.getCrontabEntry()); 
		}
			
		for(String newCronExp: newCronExpList){
			if(newCronExp.trim().equals(""))continue;
			if(newCronExp.contains(ISchedulerSetting.OUTBOUND)){
				if(newCronExp.split(" ").length==9){
					strData.append("#SF-Agent Processing\r\n");
					strData.append(newCronExp+ "\r\n\r\n");
				}else{
					if(newCronExp.split(" ").length==8){
						strData.append("#SF-Agent Reprocessing\r\n");
						strData.append(newCronExp+ "\r\n\r\n");
					}
				}
			}else{
				strData.append("#Agent-SF\r\n");
				strData.append(newCronExp+ "\r\n\r\n");
			}
		}
		strData.trimToSize();
		
		//write cron data to the file
		File file = new File(dir, Helper.CRONTAB_FILENAME);
		OutputStream out = new FileOutputStream(file);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
		writer.write(strData.toString());
		writer.close();
		
	}
	*/
	public static void main(String[] args) throws Exception{
		
		List<String> listEntries = new ArrayList<>();
		listEntries.add("18 11 * * * com.iservice.integration.IntegrationOnDemandService false ponnreay@test.com¤https://ap2.salesforce.com/services/Soap/class/skyvvasolutions/IServices¤Test%20AAA¤XX");
		listEntries.add("18 11 * * * com.iservice.integration.IntegrationOnDemandService false rayok@test.com¤https://ap2.salesforce.com/services/Soap/class/skyvvasolutions/IServices¤Test%20AAA¤XX");
		List<String> newListEntries = new ArrayList<>();
		for(int i=0; i<listEntries.size(); i++) {
			newListEntries.add(listEntries.get(i).replace("ponnreay@test.com", "sokdet@test.com"));
		}
		
		for(String str : newListEntries) {
			System.out.println(str);
		}
		
		//	   LastRunDateCache lastRunDate = new LastRunDateCache();
		//	   DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		//	   Date date = new Date();
		//	   
		//	   //lastRunDate.getmIntfLastRunDate().put("a0D90000001LYWY", dateFormat.format(date));
		//	   //lastRunDate.getmIntfLastRunDate().put("a0D90000001LWDG", dateFormat.format(date));
		//
		//	   String fileSeperator = Helper.getFileSeparator();
		//	   String path="";
		//		path = Helper.getAgentPath();
		//	   path = FilePathHelper.removeExtraPath(path, fileSeperator + Helper.LIBS_DIRECTORY);
		//	   String fileName=path + fileSeperator + GenericSFTask.LAST_INTEGRATION_DATETIME;
		//	   //saveObject(fileName,lastRunDate);
		//	   lastRunDate = (LastRunDateCache) readObject("C:\\Program Files\\SKYVVA Integration Agent\\"+GenericSFTask.LAST_INTEGRATION_DATETIME);
		//	   //lastRunDate = (LastRunDateCache) readObject(fileName);
	}
	
}
