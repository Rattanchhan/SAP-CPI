package com.iservice.gui.helper;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;

//import com.iservice.gui.frame.HelpWindow;
//import com.iservice.gui.frame.InterfaceSourceFrame;
import com.iservice.model.ProcessingJobInfo;
import com.sforce.soap.partner.fault.ApiFault;
import com.sforce.soap.schemas._class.IServices.IBean;

public class Helper {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(Helper.class);
	
//	private static HelpWindow helpWin;
	private static String fileSeparator;
	private static String agentHome;
	
	public static final String LIBS_DIRECTORY = "libs";
	public static final String CRONTAB_FILENAME = "crontab";
	public static final String SKYVVAAGENT_FILENAME = "skyvvaagent.bat";
	public static final String ARGUMENT_DELIMITER = "¤";
	public static final String DEPLOY_DIRECTORY = "deploy";
	/* URL Characters reference: 
		http://en.wikipedia.org/wiki/Basic_Multilingual_Plane#Basic_Multilingual_Plane
		http://blog.mark-mclaren.info/2007/02/invalid-xml-characters-when-valid-utf8_5873.html
		http://www.salesforce.com/us/developer/docs/api210/Content/implementation_considerations.htm
	*/
	// escape ghost chars
	public static String INVALID_CHARS = "[\\uFEFF\\uFFFE\\uFFFF\\u200B\\u2060\\u007F\\x7F\\u0008\\u000B\\u000C\\u0000-\\u0008\\u000E-\\u001F\\uD800-\\uDFFF]";
	
	public static String SKYVVA_AGENT_HOME = "SKYVVA_PROPERTIES"; 
	
	public static enum TMessageType{
		ERR("Error:",Color.red),WARN("Warning:",Color.darkGray),INFO("Info:",Color.blue);
		private String prefix;
		private Color textCol;
		private TMessageType(String prefix,Color textCol){
			this.prefix = prefix;
			this.textCol = textCol;
		}
		public String getPrefix() {
			return prefix;
		}
		public Color getTextCol() {
			return textCol;
		}
	}
	
	public static String handleApiFault(Exception ex) {
		if(ex instanceof ApiFault) {
			return ((ApiFault)ex).getExceptionMessage();
		}else {
			return ex.getLocalizedMessage();
		}
	}
	
	public static void showErrorMessage(JTextComponent errorDisplay, String errorMessage){
		showMessage(errorDisplay, errorMessage,TMessageType.ERR);
	} 
	
	public static void showWarningMessage(JTextArea errorDisplay,  String warningMessage){
		showMessage(errorDisplay, warningMessage,TMessageType.WARN);
	}
	
	public static void showInfoMessage(JFrame frame, JTextArea errorDisplay, int rowPosition, String infoMessage){
		showMessage(errorDisplay,infoMessage,TMessageType.INFO);
	}
	
	public static void showMessage(JTextComponent display,String message,TMessageType type){
		display.setVisible(true);
		display.setText(type.getPrefix()+" " + message);
		display.setForeground(type.getTextCol());
	}
	
	public static void refreshFrame(JFrame frame){
		frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		frame.repaint();
//		if(!(frame instanceof InterfaceSourceFrame)) {
//			frame.pack();
//		}
		frame.validate();
	}
	
	public static void repaintComponents(Component[] comps){
		for(Component com: comps){
			if(com instanceof JRootPane){
				repaintComponents(((JRootPane)com).getComponents());
				
			}else if(com instanceof JPanel){
				JPanel p = (JPanel)com;
				repaintComponents(p.getComponents());
				
			}else if(com instanceof JLabel)
				com.setForeground(null);
		}
	}

//	public static HelpWindow getHelpWindow() {
//		if(helpWin == null)
//			helpWin = new HelpWindow("Help", "index.html");
//		helpWin.setVisible(true);
//		return helpWin;
//	}
	

	
	public static <K,V extends Comparable<V>> Map<K,V> sortByValue(Map<K,V> map) {
		if(map == null) return null;
		
	     List<Entry<K, V>> list = new LinkedList<>(map.entrySet());
	     Collections.sort(list, new Comparator<Entry<K, V>>() {
	          @Override
			public int compare(Entry<K, V> o1, Entry<K, V> o2) {
	               return ObjectUtils.compare(o1.getValue(), o2.getValue());
	          }
	     });
		Map<K,V> result = new LinkedHashMap<K,V>();
		for (Iterator<Entry<K, V>> it = list.iterator(); it.hasNext();) {
		     Map.Entry<K,V> entry = it.next();
		     result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
	
	public static String removeWizardTitle(JFrame frame){
		String newTitle = frame.getTitle();
		if(newTitle.contains("")){
			newTitle = newTitle.replace("Integration Setup Wizard - ", "");
		}
		return newTitle;
	}
	
	public static Image getImage(Class<?> myClass, String fileName) throws IOException{
		BufferedImage image = null;
		if(myClass.getClassLoader().getResource(fileName) == null){
			image = ImageIO.read(Helper.class.getResource("/images/"+fileName));

		}else{
			image = ImageIO.read(myClass.getClassLoader().getResourceAsStream(fileName));
		
		}
		
		return image;
	}
	
	public static String getCrontabFile() throws IOException {
		return Helper.getAgentHome() + Helper.getFileSeparator() + Helper.LIBS_DIRECTORY + Helper.getFileSeparator() + Helper.CRONTAB_FILENAME;
	}
	
	public static String getDirFileJKS() throws IOException {
		return Helper.getAgentHome() + Helper.getFileSeparator() + Helper.LIBS_DIRECTORY + Helper.getFileSeparator() + Helper.DEPLOY_DIRECTORY;
	}
	
	public static String getFileJKS(String filename) throws IOException {
		return Helper.getAgentHome() + Helper.getFileSeparator() + Helper.LIBS_DIRECTORY + Helper.getFileSeparator() + Helper.DEPLOY_DIRECTORY + Helper.getFileSeparator() + filename;
	}
	
	public static String getFileSeparator(){
		if(fileSeparator == null){
			fileSeparator = System.getProperty("file.separator").trim();
		}
		return fileSeparator;
	}
	
	public static String getAgentHome() throws IOException{
//		if(agentHome == null){
//			agentHome = System.getenv(SKYVVA_AGENT_HOME);
//		}
		return (agentHome == null || agentHome.equals(""))? new File(".").getCanonicalPath() : agentHome; 
	}
	
	public static String replaceInvalidXmlCharacter(String s){
		if(s == null) return null;
		return s.replaceAll(INVALID_CHARS, "");
	}
	
	public static void refineIIntegration(com.sforce.soap.schemas._class.IServices.IIntegration integration){
		
		try{
			if(integration != null && integration.getRecords()!=null){
				IBean[][] beans = integration.getRecords();
				for(int i=0; i<beans.length; i++){
					
					IBean[] aRecord = beans[i];			
					
					if(aRecord != null){
						for (int j=0; j<aRecord.length; j++){
							IBean bean =aRecord[j];
							if(bean!=null){
								bean.setValue(replaceInvalidXmlCharacter(bean.getValue()));
							}
						}
						
					}
				}
			}
			
		}catch(Exception ex){
			LOGGER.error("Helper>refineIIntegration>Error: " + ex.getMessage());
		}
	}

	/**
	 * clear batch data from harddisk
	 * @param jobId
	 * @throws IOException
	 */
	public static void deleteJobFolder(ProcessingJobInfo job) throws IOException{
		File rootFolder = getJobFolder(job);
		if(rootFolder.exists()){
			//delete if exist
            LOGGER.trace("Job {}: deleting directory {}", job.getJobid(), rootFolder);
			FileUtils.deleteDirectory(rootFolder);
		}
	}
	/**
	 * get job folder
	 * @param jobId
	 * @return jobfolder is folder exist else return null
	 * @throws IOException
	 */
	public static File getJobFolder(ProcessingJobInfo job) throws IOException{
		String jobPath = Helper.getAgentHome()+Helper.getFileSeparator()+job.getJobFolder();
        LOGGER.trace("Job {}: resolved path: {}", job.getJobid(), jobPath);
		File rootFolder = new File(jobPath);
		if (!rootFolder.exists()){
			//if not exist then create
			//rootFolder.mkdir();
		}
		return rootFolder;
	}
	/**
	 * get message folder
	 * @param jobId
	 * @return jobfolder is folder exist else return null
	 * @throws IOException
	 */
	public static File getJobMessageFolder(ProcessingJobInfo job) throws IOException{
		
		return createFolderUnderJobId(job,"messages"); 
	}
	/**
	 * get message folder
	 * @param jobId
	 * @return jobfolder is folder exist else return null
	 * @throws IOException
	 */
	public static File getJobDataFolder(ProcessingJobInfo job) throws IOException{
		
		return createFolderUnderJobId(job,"datas");
	}
	
	public static File getReprocessFolder(ProcessingJobInfo job) throws IOException{
		return createFolderUnderJobId(job,"reprocess");
	}

	private static File createFolderUnderJobId(ProcessingJobInfo job,String foldername)
			throws IOException {
		File jobfolder = Helper.getJobFolder(job);
		File data = new File(jobfolder,foldername);
		if(!data.exists()){
			data.mkdir();
		}
		
		return data;
	}
}
