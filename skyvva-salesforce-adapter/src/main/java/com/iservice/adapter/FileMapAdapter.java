package com.iservice.adapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.lang3.StringUtils;

import com.iservice.gui.data.ISFIntegrationObject;
import com.iservice.gui.helper.DirectionTypeHelper;
import com.iservice.gui.helper.Helper;
import com.iservice.gui.helper.PropertyNameHelper;
//import com.iservice.gui.panel.AdapterPropertyPanel;
import com.iservice.helper.FileHelper;
import com.iservice.helper.FolderUtils;

public abstract class FileMapAdapter extends MapAdapter {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(FileMapAdapter.class);
    
	private final static String MSG_FILENOTFOUND = " (The system could not find the path specified).\n Please check the adapter property 'Folder' and/or 'FileName' to ensure they are valid.";
	/* Enable support of upload File as Attachment instead of its content */	
	public final static String ATTACHMENT_FILE_NAME = "FILE_NAME";
	public final static String ATTACHMENT_FILE_TYPE = "FILE_TYPE";
	public final static String ATTACHMENT_FILE_EXTENTION = "FILE_EXTENTION";
	public final static String ATTACHMENT_FILE_PARENTID = "PARENTID";
	public final static String ATTACHMENT_FILE_BODY = "FILE_BODY";
	public final static String ATTACHMENT_FILE_LENGTH = "FILE_LENGTH";
	public static final String EMPTY = "";
	public static final String UTF_8_CHAR_SET = "UTF-8";
	
	protected Map<String, String> mapProps=new HashMap<String, String>();
	protected List<String> files = new ArrayList<String>();
	public List<String> attachments = new ArrayList<String>();
	
	protected String filetype;
	protected String folder = EMPTY;
	protected String binaryfolder = EMPTY;
	protected String filename = EMPTY;
	protected String mergeFile = EMPTY;
	protected String fileSize = EMPTY;
	protected Boolean isRename = false;
	protected String currentTime = EMPTY;
	
	//lock file after data reading
	//default: lock (real integration), in case from UI, no lock
	protected boolean isLockFile = false;  
	
	//delete file after data processing
	protected boolean isDeleteFile = false;
	
	protected String separator = EMPTY; //CSV only
	protected String sheetname = EMPTY; //Excel only
	
	protected String targetFileName = EMPTY; //SF2Agent[Files]
	
	protected boolean isBackupFile = false; //v1.16
	protected String backupFolder;
	
	/* Enable support of upload File as Attachment instead of its content */
	protected String file_upload_mode = ""; //CONTENT | FILE
	protected String fileExtentions; //eg ".csv|.xls|.doc|.pdf|.png|.tif"
	
	// 01-09-2017 v1.47
	protected boolean isKeepFile = false;
	
	// 08-11-2017
	protected String afterProcessing = EMPTY;
	
	protected boolean hasFileHeader = true;
	protected boolean hasQuotes = true;
	
	//24-05-2019
	protected static String replace_header_except = EMPTY;
	protected boolean isAddPrefix = false;
	
	@SuppressWarnings("serial")
	public static class WarningFileNotFoundException extends Exception {
		public WarningFileNotFoundException(String msg) {
			super( msg);
		}
		/*public WarningFileNotFoundException(Throwable msg) {
			super( msg);
		}*/
	}
	
	//check file in folder
	public static Throwable fileNotFound(Throwable ex, FileMapAdapter ad) {
		
		if(ex instanceof FileNotFoundException && ad.getFilename()!=null && ad.getFilename().trim().length()>0
				&& (ad.isDeleteFile || ad.isLockFile)){
			
			return new WarningFileNotFoundException(ex.getMessage()); 
		}	
		return ex;
		
	}
	
	//MUST OVERRIDE
	abstract void validateProperties() throws Exception;
	
	@Override
	public boolean login() throws Exception {
		
		setConnectionInfo(map());
		File f = null;
		if(FileHelper.hasWildCardCharacter(filename)) {
			if(FolderUtils.getFiles(folder, filename).isEmpty()) throw new FileNotFoundException(filename + MSG_FILENOTFOUND);
		}else {
			String tmpFilename = filename;
			if(interfaceType!=null && interfaceType.equals(DirectionTypeHelper.OUTBOUND)) tmpFilename = "";
			String path = folder + Helper.getFileSeparator() + tmpFilename;
			f = new File(path); 
			if(!f.exists()) throw new FileNotFoundException(f.getPath() + MSG_FILENOTFOUND);
		}
		//v1.16
		//check isBackupFile true
		if(isBackupFile) {
			// and no backupFolder
			if(backupFolder==null || backupFolder.trim().isEmpty()) {
				throw new Exception("Missing backup folder: 'Backup File' option is active. Please specify the adapter property 'Backup Folder'.");
			}
			
			//check backup folder valid ?
			else {
				f = new File(backupFolder);
				if(!f.exists()) throw new FileNotFoundException("Path Not Found: the adapter property 'Backup Folder': [" + f.getPath() + "] could not be found.");
			}	
		}
		
		checkFileExtenstion(filename, filetype);
		
		return true;
	}
		
	@Override
	public void setConnectionInfo(Map<String, String> mapProp) throws Exception {
		this.mapProps = mapProp;
		
		if(mapProp!=null) {
			folder = mapProp.get(PropertyNameHelper.FOLDER) == null ? "" : mapProp.get(PropertyNameHelper.FOLDER).trim();
			binaryfolder = mapProp.get(PropertyNameHelper.BINARY_FOLDER) == null ? "" : mapProp.get(PropertyNameHelper.BINARY_FOLDER).trim();
			filename = mapProp.get(PropertyNameHelper.FILE_NAME) == null ? "" : mapProp.get(PropertyNameHelper.FILE_NAME).trim();
			mergeFile = mapProp.get(PropertyNameHelper.MERGE_FILE) == null ? ( mapProp.get("Merger File") == null ? "false" : mapProp.get("Merger File").trim() ) : mapProp.get(PropertyNameHelper.MERGE_FILE).trim();
			
			//default file size = 1 MB
			fileSize = (mapProp.get(PropertyNameHelper.FILE_SIZE) == null || "".equals(mapProp.get(PropertyNameHelper.FILE_SIZE))) ? ( ( mapProp.get("File Size") == null || "".equals(mapProp.get("File Size")) ? "1" : mapProp.get("File Size").trim() )) : mapProp.get(PropertyNameHelper.FILE_SIZE).trim();

			// v1.16
			backupFolder = mapProp.get(PropertyNameHelper.BACKUP_FOLDER) == null ? "" : mapProp.get(PropertyNameHelper.BACKUP_FOLDER).trim();
			
			//CSV
			separator = mapProp.get(PropertyNameHelper.SEPARATOR) == null ? "" : mapProp.get(PropertyNameHelper.SEPARATOR);
			if(separator.equalsIgnoreCase("tab")) separator = "\t";
			
			hasFileHeader = mapProp.get(PropertyNameHelper.HAS_FILE_HEADER) == null ? true : mapProp.get(PropertyNameHelper.HAS_FILE_HEADER).equalsIgnoreCase("true");
			replace_header_except = mapProp.get(PropertyNameHelper.REPLACE_HEADER_EXCEPT) == null ? "--None--" : mapProp.get(PropertyNameHelper.REPLACE_HEADER_EXCEPT).trim();
			
			hasQuotes = mapProp.get(PropertyNameHelper.HAS_QUOTES) == null ? true : mapProp.get(PropertyNameHelper.HAS_QUOTES).equalsIgnoreCase("true");
			
			//Excel
			sheetname = mapProp.get(PropertyNameHelper.SHEET_NAME) == null ? EMPTY : mapProp.get(PropertyNameHelper.SHEET_NAME).trim();
			
			//FTP
			filetype = mapProp.get(PropertyNameHelper.FTP_FILE_TYPE) == null ? EMPTY : mapProp.get(PropertyNameHelper.FTP_FILE_TYPE).trim();
			if(filetype.equals("")){
				filetype = mapProp.get(PropertyNameHelper.FILE_TYPE) == null ? EMPTY : mapProp.get(PropertyNameHelper.FILE_TYPE).trim();
			}
			
			fileExtentions = mapProp.get(PropertyNameHelper.FILE_EXTENTIONS) == null ? null : mapProp.get(PropertyNameHelper.FILE_EXTENTIONS).trim();
			file_upload_mode = mapProp.get(PropertyNameHelper.FILE_UPLOAD_MODE) == null ? null : mapProp.get(PropertyNameHelper.FILE_UPLOAD_MODE).trim();
			
			// v1.47
			afterProcessing = mapProp.get(PropertyNameHelper.AFTER_PROCESSING) == null ? "" : mapProp.get(PropertyNameHelper.AFTER_PROCESSING).trim();
			if(afterProcessing == EMPTY) {
				for(String key : mapProp.keySet()) {
					if(key.equalsIgnoreCase(PropertyNameHelper.KEEP_FILE_AFTER_PROCESSING) && mapProp.get(key) != null && mapProp.get(key).equalsIgnoreCase("true"))
						isKeepFile = true;
					
					if(key.equalsIgnoreCase(PropertyNameHelper.LOCK_FILE_AFTER_PROCESSING) && mapProp.get(key) != null && mapProp.get(key).equalsIgnoreCase("true"))
						isLockFile = true;
					
					if(key.equalsIgnoreCase(PropertyNameHelper.DELETE_FILE_AFTER_PROCESSING) && mapProp.get(key) != null && mapProp.get(key).trim().equalsIgnoreCase("true"))
						isDeleteFile = true;
					
					if(key.equalsIgnoreCase(PropertyNameHelper.IS_BACKUP_FILE) && mapProp.get(key) != null && mapProp.get(key).trim().equalsIgnoreCase("true"))
						isBackupFile = true;
				}
				// get flag priority
//				String flagPriority = AdapterPropertyPanel.getFlagPriority(isLockFile, isDeleteFile, isBackupFile);
//				setFlagPriorityFileMapAdapter(flagPriority);
			}else {
				// set flag priority
				setFlagPriorityFileMapAdapter(afterProcessing);
			}
			isAddPrefix = mapProp.get(PropertyNameHelper.ADD_PREFIX) == null ? false : mapProp.get(PropertyNameHelper.ADD_PREFIX).equalsIgnoreCase("true");
		}
		
		setAdapter(CoreIntegration.createDTOAdapterFromMap(mapProp));
		
		//MUST OVERRIDE
		validateProperties();
	}
	
	private void setFlagPriorityFileMapAdapter(String str) {
		if(str.equalsIgnoreCase(PropertyNameHelper.LOCK_FILE)){
			isLockFile=true;isDeleteFile=false;isBackupFile=false;isKeepFile=false;
		}else if(str.equalsIgnoreCase(PropertyNameHelper.DELETE_FILE)){
			isLockFile=false;isDeleteFile=true;isBackupFile=false;isKeepFile=false;
		}else if(str.equalsIgnoreCase(PropertyNameHelper.BACKUP_FILE)){
			isLockFile=false;isDeleteFile=false;isBackupFile=true;isKeepFile=false;
		}else {
			isLockFile=false;isDeleteFile=false;isBackupFile=false;isKeepFile=true;
		}
	}
	
	public static String getFileExtensions(String filetype) {
		if (FileTypeHelper.CSV.equalsIgnoreCase(filetype)) {
			return ".csv";
		} else if (FileTypeHelper.EXCEL.equalsIgnoreCase(filetype)) {
			return ".xls|.xlsx";
		} else if (FileTypeHelper.XML.equalsIgnoreCase(filetype)) {
			return ".xml";			 
		} else if (FileTypeHelper.JSON.equalsIgnoreCase(filetype)) {
			return ".json";
		}else if (FileTypeHelper.OTHER.equalsIgnoreCase(filetype)) {
			return "";
		}
		return "";
		
	}
	
	public void reset(){
		files.clear();
	}
	
	@Override
	public final void unlockFiles() throws Exception {
		if(getParentAdapter()!=null) {
			getParentAdapter().unlockFiles();
		}else {
			unlockFiles(files);
		}
	}
	
	protected void unlockFiles(List<String> files) throws FileNotFoundException {
		if(files==null || files.isEmpty()) {
			LOGGER.trace(">FileMapAdapter> unlockFiles(): done with no files");
			return;
		}
		for(int i=0;i<files.size();i++){
			String oldfilename = folder + Helper.getFileSeparator() + files.get(i);
			String lockedFileName= oldfilename + PropertyNameHelper.LOCK_EXTENSION;
			
			if(!new File(lockedFileName).exists()) {
				oldfilename = binaryfolder + Helper.getFileSeparator() + files.get(i);
				lockedFileName = oldfilename + PropertyNameHelper.LOCK_EXTENSION;
				if(!new File(lockedFileName).exists()) {
					continue;
				}
			}
			
			FolderUtils.renameFile(lockedFileName, oldfilename);
		}
	}
	
	//v1.16
	protected void backupFiles(List<String> files, String backupFolder) throws FileNotFoundException {
		if(isBackupFile) {
			for(int i=0;i<files.size();i++){
				// file integrate with error we need to keep file
				if(keepFiles.size()>0 && keepFiles.contains(files.get(i))) continue;
				String temp = folder;
				boolean isExisting = FolderUtils.isFileExisting(folder + Helper.getFileSeparator() + files.get(i) + PropertyNameHelper.LOCK_EXTENSION);
				if(!isExisting) {
					isExisting = FolderUtils.isFileExisting(binaryfolder + Helper.getFileSeparator() + files.get(i) + PropertyNameHelper.LOCK_EXTENSION);
					temp = binaryfolder;
				}
				if(isExisting){
					String fileSpec = temp + Helper.getFileSeparator() + files.get(i) + PropertyNameHelper.LOCK_EXTENSION;

					Date dt = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd hhmmss");
					
					Integer mid = files.get(i).lastIndexOf(".");
					String fName = files.get(i).substring(0, mid) +"_"+ sdf.format(dt);
					fName = fName + files.get(i).substring(mid, files.get(i).length());
					
					FolderUtils.renameFile(fileSpec, (backupFolder + Helper.getFileSeparator() + fName));
				}
			}
			if(keepFiles.size()>0) {
				unlockFiles(keepFiles);
				keepFiles.clear();
			}
		}
	}
	
	@Override
	public void onError() throws Exception {
		//in case call from integrateAttachmentAndContentVersion.
		folder = mapProps.get(PropertyNameHelper.FOLDER) == null ? "" : this.mapProps.get(PropertyNameHelper.FOLDER).trim();
		
		if(files==null) {
			LOGGER.trace(">FileMapAdapter> onError(): done with no files");
			return;
		}
		LOGGER.trace(">FileMapAdapter> onError(): start");
		//20110726 In case isLocked, then when error, try unlock
		unlockFiles(files);
	}
	  
	public void setFilesForBackup(List<String> filesName) {
		if(getParentAdapter()!=null){
			FileMapAdapter fAdapter =(FileMapAdapter)getParentAdapter();
			fAdapter.setFilesForBackup(filesName);
		}else{
			files = filesName;
		}
	}
	@Override
	public void terminate() throws Exception{	
		//in case call from integrateAttachmentAndContentVersion.
		folder = mapProps.get(PropertyNameHelper.FOLDER) == null ? "" : this.mapProps.get(PropertyNameHelper.FOLDER).trim();
		
		if(files==null) {
			LOGGER.trace(">FileMapAdapter> terminate(): done with no files");
			return;
		}
		LOGGER.trace(">FileMapAdapter> terminate(): start");
		
		//v1.16
		LOGGER.trace(">FileMapAdapter> isBackupFiles:" + isBackupFile);
		backupFiles(files, backupFolder);
		
		//20110726 In case isToBeDeleted, then when integrate DONE, try delete the files
		LOGGER.trace(">FileMapAdapter> isDeleteFile:" + isDeleteFile);
		if(isDeleteFile) {
			for(int i=0;i<files.size();i++){
				// file integrate with error we need to keep file
				if(keepFiles.size()>0 && keepFiles.contains(files.get(i))) continue;
				
				String oldfilename = folder + Helper.getFileSeparator() + files.get(i);
				String lockedFileName= oldfilename + PropertyNameHelper.LOCK_EXTENSION;
				
				if(!new File(lockedFileName).exists()) {
					oldfilename = binaryfolder + Helper.getFileSeparator() + files.get(i);
					lockedFileName = oldfilename + PropertyNameHelper.LOCK_EXTENSION;
					if(!new File(lockedFileName).exists()) {
						LOGGER.trace(">FileMapAdapter> deleteFile(): with filename: "+oldfilename+" > not found!");
					}
				}
				
				boolean d=FolderUtils.deleteFile(lockedFileName);
				if(!d) {
					LOGGER.trace(">FileMapAdapter> deleteFile: " + lockedFileName + " > deleted: " + d);
				}
			}
			if(keepFiles.size()>0) {
				unlockFiles(keepFiles);
				keepFiles.clear();
			}
		}
		
		//31-08-2017  1.47
		//In case isUnlock
		LOGGER.trace(">FileMapAdapter> isKeepFile: "+isKeepFile);
		if(isKeepFile) {
			unlockFiles(files);
			if(keepFiles.size()>0) keepFiles.clear();
		}
		
		//In case isLocked
		LOGGER.trace(">FileMapAdapter> isLocked: "+isLockFile);
		if(isLockFile && keepFiles.size()>0) {
			unlockFiles(keepFiles);
			keepFiles.clear();
		}
	}
	
	public boolean isLocked() {
		return isLockFile;
	}
	public void setLocked(boolean isLocked) {
		this.isLockFile = isLocked;
	}
	

	public String lockFile(String fn) throws Exception {
		return lockFile(fn, folder);
	}
	
	public String lockFile(String fn,String folder) throws Exception {

		//12.10.10 SPR lock the file by renaming it to .lock
		String fnLocked = fn;
		String lockedFilePath = folder + Helper.getFileSeparator() + fn;
		lockedFilePath += PropertyNameHelper.LOCK_EXTENSION;
		fnLocked += PropertyNameHelper.LOCK_EXTENSION;
		boolean success = FolderUtils.renameFile(folder + Helper.getFileSeparator() + fn, lockedFilePath);
		if(!success) {
			throw new Exception("Cannot rename from file: "+fn+" to file: "+fnLocked+", Please check file permission.");
		}
		return fnLocked;
	}
	public File getFile(String filename){
		String path = folder + Helper.getFileSeparator() + filename;
		return new File(path);
	}
	
	public Map<String, String> getMapProps() {
		return mapProps;
	}

	public String getFilename() {
		return filename;
	}
	
	public boolean isToBeDeleted() {
		return isDeleteFile;
	}
	
	//07-03-2014
	private static Pattern getPtnSPChar() { 
		//24-05-2019 Match all punctuation except dot . using [\\p{Punct}&&[^.]]
		if(StringUtils.equalsIgnoreCase(replace_header_except, PropertyNameHelper.EXCEPT_DOT)) {
			return Pattern.compile("[\\p{Punct}&&[^.]]|[\\p{Space}|\\p{Cntrl}|(äÄéöÖüÜß«»„“”€°§£àôµ²³)]");
		}
		return Pattern.compile("[\\p{Punct}|\\p{Space}|\\p{Cntrl}|(äÄéöÖüÜß«»„“”€°§£àôµ²³)]");
	}
	public static String manageSPChar(String d){ 
		if(d!=null){
			return getPtnSPChar().matcher(d).replaceAll("_");
	    }
	    return d;
	}
	
	protected abstract String getExtenFilter();
	
	public List<String> getAllFiles(String filter) throws FileNotFoundException, Exception {
		List<String> fileNames = new ArrayList<String>();
		//One file
		if(!filename.equals("") && !FileHelper.hasWildCardCharacter(filename)){ 
			fileNames.add(filename);
		}
		//Many files based on filter (value)
		else {
			String fileExts = getExtenFilter();
			if( getAdapter().isModFile() && fileExtentions!=null && fileExtentions.trim().length()>0) fileExts = fileExtentions;
			List<String> lstFiles = getAllFiles(mapProps, filename, fileExts);
			fileNames = getFilesByFilter(lstFiles,filter);
		}
		return fileNames;
	}
	@Override
	public final List<String> getAllObjects(String objectType) throws Exception {
		
		return null;

	}
	@Override
	public void setTableName(String tblName) {
		targetFileName = tblName;
	}
	public static void main(String args[]){
		
		String s="0`A~B!C@D#E$F%G^I&J*K(L)M-N=O+";
        s=manageSPChar(s);
        System.out.println(">>>"+s);
        System.out.println("0_A_B_C_D_E_F_G_I_J_K_L_M_N_O_=>"+s);
        
        s="0[A{B]C}D\\E|F";
        s=manageSPChar(s);
        System.out.println(">>>"+s);
        System.out.println("0_A_B_C_D_E_F=>"+s);
        
        s="0;A:B'C\"D";
        s=manageSPChar(s);
        System.out.println(">>>"+s);
        System.out.println("0_A_B_C_D=>"+s);
        
        s="0,A<B.C>D/E?F ";
        s=manageSPChar(s);
        System.out.println(">>>"+s);
        System.out.println("0_A_B_C_D_E_F_=>"+s);
        
        s="0Ã¤AÃ„BÃ©CÃ¶DÃ–EÃ¼FÃœGÃŸHÂ«IÂ»Jâ€žKâ€œLâ€�Mâ‚¬NÂ°OÂ§PÂ£Q";
        s=manageSPChar(s);
        System.out.println(">>>"+s);
        System.out.println("0_A_B_C_D_E_F_G_H_I_J_K_L_M_N_O_P_Q=>"+s);
        
        s = "123abc!\"#$%&'()*+,-./:123abc;<=>?@[\\]^_`{|}~321cba";
        s=manageSPChar(s);
        System.out.println(">>>"+s);
        System.out.println("123abc________________123abc________________321cba=>"+ s);
	}
	
	public String getFolder() {
		return folder;
	}
	
	@Override
	public List<List<String>> getColumns(String objectName,
			List<? extends ISFIntegrationObject> listHeader) throws Exception {
		return null;
	}
	
	// 1.49
	protected String interfaceType;
	@Override
	public void setInterfaceType(String interfaceType) {
		this.interfaceType = interfaceType;
	}
	
	public boolean getIsAddPrefix() {
		return this.isAddPrefix;
	}
}
