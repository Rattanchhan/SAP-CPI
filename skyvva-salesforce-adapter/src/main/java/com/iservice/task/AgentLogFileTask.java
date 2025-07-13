package com.iservice.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.iservice.gui.helper.Helper;
import com.iservice.sforce.MapSFConnInfo;
import com.iservice.sforce.SFIntegrationService;
import com.iservice.sforce.SFService;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.sobject.SObject;

public class AgentLogFileTask {
//	public Logger log = LogManager.getLogger(AgentLogFileTask.class);
	private MapSFConnInfo mapProps = null;
	private Map<String, String> existedFiles = null;
	public AgentLogFileTask(Map<String, Object> objMap){
		if(objMap != null) {
			mapProps = new MapSFConnInfo();
			mapProps.put("username",objMap.get("username")==null?"":objMap.get("username").toString());
			mapProps.put("password",objMap.get("password")==null?"":objMap.get("password").toString());
			mapProps.put("token",objMap.get("token")==null?"":objMap.get("token").toString());
			mapProps.put("urlserver",objMap.get("urlserver")==null?"":objMap.get("urlserver").toString());
			existedFiles = (Map<String, String>)objMap.get("ExistedFiles");
		}
		
	}
	public boolean createLogToSF() {
		try {
			//ILogs__c ilog = new ILogs__c(SFService.PACKAGE.replace("/", "") + "__"); 
			List<File> listFiles = readLogFile(existedFiles);
			String errorTitle = extractErrorTitle(listFiles);
			if(StringUtils.isNotBlank(errorTitle)) {
				SObject[] atts = createAttments(listFiles);
				SFIntegrationService service = new SFIntegrationService(mapProps);
				service.login();
				// 2 connect to SF and retrieve integration information
				SFService sfService = (SFService)service.getISFService(); //new SFService(mapProps);
				PartnerConnection binding = sfService.getSFPartner();
				binding.create(atts);
				return true;
			}
		} catch (Exception e) {
//			log.error(e);
		}
		
		return false;
	}
	private SObject[] createAttments(List<File> listFiles) throws Exception {
		if(listFiles.size() >0) {
			SObject[] sobjs = new SObject[listFiles.size()];
			
			for(int i=0;i<listFiles.size();i++){
				File f = listFiles.get(i);
				InputStream is = new FileInputStream(f);
				byte[] body = new byte[(int)f.length()];
				is.read(body);
				is.close();
				
				SObject sobj = new SObject("Attachment");
				sobj.setField("Name", "agent_"+f.getName());
				sobj.setField("ParentId", "");
				sobj.setField("Body", body);
				
				sobjs[i]=sobj;
			}
			
			return sobjs;
		}
		return null;
	}
	private List<File> readLogFile(Map<String,String> existFiles) {
		
		Map<String, String> env = System.getenv();
		String path=env.get("SKYVVA_AGENT_HOME")+Helper.getFileSeparator()+"logs";
		List<File> newListFiles = new ArrayList<>();
		File folder=new File(path);
		File[] listFiles = folder.listFiles();
		if(listFiles != null && listFiles.length>0) {
			for(int i=listFiles.length - 1;i>=0;i--) {
				File file = listFiles[i];
//				System.out.println (file.getName() + " >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + file.lastModified());
				if(file.getName().toLowerCase().startsWith("iservice")){
					if(existFiles != null && existFiles.get(file.getName()) != null) {
						continue;
					}
					newListFiles.add(file);
				}
			}
		}
		
		return newListFiles;
	
	}
	private String extractErrorTitle(List<File> listFiles) {
		
		if(listFiles != null && listFiles.size()>0) {
			boolean isError = false;
			for(int i=0;i<listFiles.size();i++) {
				File file = listFiles.get(i);
				if(file.getName().toLowerCase().startsWith("iservice")){
					
					try {
						
						FileInputStream fstream = new FileInputStream(file);
						BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
						String strLine;
						 /* read log line by line */
						String logLine = "";
						while ((strLine = br.readLine()) != null)   {
						     /* parse strLine to obtain what you want */
							    if(strLine != null && strLine.toLowerCase().indexOf("error")>0 && isError == false) {
							    	logLine = strLine;
							    	isError = true;
							    	continue;
							    }
							    if(isError && !StringUtils.isBlank(strLine)) {
							    	if(strLine.indexOf("INFO") > 0 || strLine.indexOf("com.") > 0 || strLine.indexOf("org.") > 0) {
							    		break;
							    	}
							    	logLine = logLine + "\n" + strLine;
							    	System.out.println(logLine);
							    	break;
								}
						}
					    fstream.close();
					  //read only current error at the latest file
						if(isError) {
							return logLine;
						}
						 
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		return "";
	}
	 private static Logger log ;
	 static {
		 System.setProperty("filename", "iservice");
		 log = LogManager.getLogger(AgentLogFileTask.class);;
	 }
	public void testLog() {
//		List<String> ls = new ArrayList<>(Arrays.asList("log1","log2","log3"));
//		for(String fname:ls) {
//			System.setProperty("filename", fname);
//			LoggerContext ctx =  (LoggerContext)LogManager.getContext(false);
//			ctx.getConfiguration().getAppenders();
//			ctx.reconfigure();
////			Logger log = LogManager.getLogger(AgentLogFileTask.class);
//			log.trace(">>>>>>>>>>>>>>>>>>>>>>>>>>>> " + fname);
//		}
	}
	public static void main(String[] args) {
		new AgentLogFileTask(null).testLog();
//		Map<String, Object> map = new HashMap<>();
//		map.put("username", "vimean1982@gmail.com");
//		map.put("password", "Mean030282");
//		map.put("token", "4HNJMCBP1g1UJsqJv7HReAF41");
//		map.put("urlserver", "https://takeo-dev-ed--c.eu11.visual.force.com");
//		new AgentLogFileTask(map).createLogToSF();
	}
}
