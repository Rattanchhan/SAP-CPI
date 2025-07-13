package com.iservice.adapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.iservice.adapter.batch.BatchExecutorResponseItem;
import com.iservice.adapter.reader.ContentFileReader;
import com.iservice.adapter.reader.IRecordReader;
import com.iservice.adapter.reader.QueryResult;
import com.iservice.adapter.reader.RawFileReader;
import com.iservice.gui.data.IChained_Interfaces__c;
import com.iservice.gui.data.IMapping__c;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.gui.helper.AdapterTypeHelper;
import com.iservice.gui.helper.Helper;
import com.iservice.gui.helper.PropertyNameHelper;
import com.iservice.helper.FolderUtils;
import com.iservice.sforce.SFIntegrationService;
import com.iservice.task.AbstractBulkDirectObjectEvent.TBulkVersion;
import com.iservice.task.*;
import com.model.iservice.Adapter;
import com.sforce.soap.schemas._class.IServices.IBean;

import static java.lang.String.format;

public abstract class MapAdapter implements IDBConnection {
    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(MapAdapter.class);

	@SuppressWarnings("serial")
	public static final Map<String, String> mapFileType2FileExts = new HashMap<String, String>() {
		{
			put(FileTypeHelper.JSON, ".json");
			put(FileTypeHelper.CSV, ".csv");
			put(FileTypeHelper.XML, ".xml");
			put(FileTypeHelper.EXCEL, ".xls|.xlsx");

		}
	};

	protected IIntegrationProcessListener processLisenter;
	protected Adapter adapter;

	protected MapAdapter parentAdapter;

	protected Map<String, String> map() {
		return CoreIntegration.map(getAdapter());
	}

	@Override
	public Adapter getAdapter() {
		return adapter;
	}

	@Override
	public void setAdapter(Adapter adapter) {
		this.adapter = adapter;
	}

	public boolean checkPrecondition(Map<String, String> mapAdapter) {
		return true;
	}

	@Override
	public List<File> updateToFTPFile(IBean[][] beans, List<List<IBean>> lstRecords, String folder,
			String targetFileName, SFIntegrationService integrationService, List<BatchExecutorResponseItem> lsMsg, Interfaces__c intf) throws Exception {
		return null;
	}

	// 27.09.10
	protected List<String> getAllFiles(Map<String, String> mapAdapter,String adapterType) throws FileNotFoundException {
		adapterType = (adapterType == null ? "" : adapterType.trim());
		String typeExtensions = mapFileType2FileExts.get(adapterType);
		return getAllFiles(mapAdapter, adapterType, typeExtensions);

	}

	/*
	 * typeExtensions: eg ".csv"; ".xml"; ".xls|.xlsx";
	 * ".jpg|.bmp|.png|.gif|.tiff|.tif"
	 */
	protected List<String> getAllFiles(Map<String, String> mapAdapter,String adapterTypeOrFilename, String typeExtensions) throws FileNotFoundException {
		List<String> lstFilename = new ArrayList<String>();
		String folderPath = null;
		if (mapAdapter != null)  folderPath = mapAdapter.get(PropertyNameHelper.FOLDER);
		if (folderPath == null) folderPath = "";
		String path = folderPath + Helper.getFileSeparator();
		File f = new File(path);
		if (!f.exists()) throw new FileNotFoundException(f.getAbsolutePath()+ " (The system cannot find the path specified). Please check the adapter property 'Folder' to ensure it is valid.");
		// 20110727
		if (typeExtensions == null && (adapterTypeOrFilename.equalsIgnoreCase(AdapterTypeHelper.JDBC) == true ||
			adapterTypeOrFilename.equalsIgnoreCase(AdapterTypeHelper.FILE) == true ||
			adapterTypeOrFilename.equalsIgnoreCase(AdapterTypeHelper.FTP) == true))
			throw new RuntimeException("The adapter type [" + adapterTypeOrFilename + "] is not supported for File-based integration.");
		
		lstFilename = FolderUtils.loadFiles(folderPath, adapterTypeOrFilename, typeExtensions, mapAdapter.get(PropertyNameHelper.SORT_BY), mapAdapter.get(PropertyNameHelper.ORDER_BY));
		return lstFilename;
	}

	// 27.09.10
	protected List<String> getFilesByFilter(List<String> allFiles, String filter) throws Exception {
		List<String> lst = new ArrayList<String>();
		if (allFiles == null) return lst;
		// 20110726 if no filter, then return all files
		if (filter == null || filter.trim().equals("")) return allFiles;
		for (int i = 0; i < allFiles.size(); i++) {
			String fName = allFiles.get(i);
			if (fName != null) {
				// 20110726 case insens
				if (fName.toLowerCase().contains(filter.toLowerCase())) {
					// manage the file's date
					lst.add(fName);
				}
			}
		}
		return lst;
	}

	@Override
	public List<List<String>> getColumnsOutBound(String objectName) throws Exception {
		return null;
	}
	
	public IRecordReader doCreateRecordReader(MapIntegrationInfo integrationInfo) throws Exception{
		if(getAdapter().isModFile() && this instanceof FileMapAdapter){
			return new ContentFileReader((FileMapAdapter)this,integrationInfo);
		}else if(getAdapter().isMode_RawContent() && this instanceof FileMapAdapter){
			return new RawFileReader((FileMapAdapter)this, integrationInfo);
		}
		return createRecordReader(integrationInfo);
	}
	
	protected abstract IRecordReader createRecordReader(MapIntegrationInfo integrationInfo) throws Exception;

	public AbstractIntegrationEvent createIntegrationEvent(long totalRec, MapIntegrationInfo integrationInfo, IRecordReader reader) throws Exception {
		if (integrationInfo.isAutoSwitchMode()) {
			if (totalRec > integrationInfo.getIntegrateBatchSize()) {// bulk mode
				LOGGER.trace(">doInBoundIntegrationV3() > with Bulk Mode with total: "+totalRec+" records from Agent data source to be integrated into SF.");
				return new BulkIntegrationV3Event(totalRec,integrationInfo);
			}else if (totalRec > integrationInfo.getIntegrateMaxSize()) {
				integrationInfo.getInterfaces().setBatch_Mode__c(true);
				LOGGER.trace(">doInBoundIntegrationV3() > with total: "+totalRec+" records from Agent data source to be integrated into SF.");
				return new IntegrationV3Event(totalRec,integrationInfo);
			}
		}else if(integrationInfo.isBulkMode()) {
			LOGGER.trace(">doInBoundIntegrationV3() > with Bulk Mode with total: "+totalRec+" records from Agent data source to be integrated into SF.");
			return createBulkEvent(totalRec,integrationInfo);
		}
		LOGGER.trace(">doInBoundIntegrationV3() > with total: "+totalRec+" records from Agent data source to be integrated into SF.");
		return new IntegrationV3Event(totalRec,integrationInfo);		
	}
	
	protected AbstractIntegrationEvent createBulkEvent(long totalRec,MapIntegrationInfo integrationInfo) {
		String bkV = integrationInfo.getInterfaces().getBulk_Version__c();
		if(StringUtils.isBlank(bkV)){
			Map<String,String> adapterProps = integrationInfo. getAdapterProperties();
			//this is mean that skyvva version is older than 2.41, then try to check bulkd mode from adapter
			if(adapterProps!=null ){
				//get bulk version from adapter properties
				bkV = StringUtils.trimToEmpty(adapterProps.get("bulk_version"));
			}
		}
		TBulkVersion tbkv = TBulkVersion.getByApiName(bkV);
		switch (tbkv) {
			case SFV1:
				return new BulkDirectObjectIntegrationEvent(totalRec, integrationInfo);
			case SFV2:
				return new BulkDirectObjectV2Integration(totalRec, integrationInfo);
			case SKYVVA:
				return new BulkIntegrationV3Event(totalRec,integrationInfo);
			default:
				return new BulkIntegrationV3Event(totalRec,integrationInfo);
		}
	}
	
	public boolean isForceStop(){
		if (this.getProcessLisenter()!=null && this.getProcessLisenter().isStop()){
			return true;
		}
		return false;
	}
	
	@Override
	public boolean doInBoundIntegration(String criteria, MapIntegrationInfo integrationInfo) throws Exception {
		try {
			IRecordReader reader = doCreateRecordReader(integrationInfo);
			boolean status = reader.doIntegration(criteria);
			try {
				if (isForceStop()) {
					LOGGER.trace("> doInBoundIntegration> isForceStop: " + isForceStop());
					this.onError();		
				} else {
					this.terminate();
				}
            } catch (Exception e){
				LOGGER.warn(e.getMessage(),e);
			}
			return status;
		} catch (Throwable ex) {
            LOGGER.error(format("Error occurred during execution of integrate for interface %s: %s",
                integrationInfo.getInterfaceName(),
                ExceptionUtils.getMessage(ex)
            ), ex);

			if (ex instanceof UnknownHostException || ex instanceof NoRouteToHostException || ex instanceof HttpHostConnectException) {
				this.terminate();
				throw ex;
			}

			try {
				this.onError();		
			} catch (Exception e1) {
				LOGGER.warn(ex.getMessage(),ex);
			}

			throw ex;
		}
	}

	@Override
	public IIntegrationProcessListener getProcessLisenter() {
		return processLisenter;
	}

	@Override
	public void setProcessLisenter(IIntegrationProcessListener processLisenter) {
		this.processLisenter = processLisenter;
	}
	
	@Override
	public boolean doTestInBoundIntegration(String criteria,MapIntegrationInfo integrationInfo)throws Exception{
		try{
			
			IRecordReader reader = doCreateRecordReader(integrationInfo);
			boolean status = reader.doTestIntegration(criteria);
			return status;
			
		}catch(Throwable e){
			LOGGER.error(e.getMessage(),e);
			throw e;
		}finally{
			System.gc();
			System.runFinalization();
		}
		
	}
	
	public MapAdapter getParentAdapter() {
		return parentAdapter;
	}

	public void setParentAdapter(MapAdapter parentAdapter) {
		this.parentAdapter = parentAdapter;
	}

	@Override
	public QueryResult doTestQuery(String criteria, MapIntegrationInfo integrationInfo)throws Exception{
	    IRecordReader reader = doCreateRecordReader(integrationInfo);
	    return reader.doTestQuery(criteria, true);
	}
	
	@SuppressWarnings("unchecked")
	protected void readJsonObjectToFlat(JSONObject oneObject, List<String> lstMappingFields, List<IBean> oneRecord, 
			List<List<IBean>> records, List<IBean> parentRecord, List<BatchExecutorResponseItem> lstMsg ) throws Exception {
		//Firstly, initial mapped fields, run oonly once time per oneObject
		if(oneRecord==null) {
			oneRecord = new ArrayList<IBean>();
			for(String oneMapField : lstMappingFields) {
				IBean oneField = new IBean();
				oneField.setName(oneMapField);
				oneRecord.add(oneField);
			}
		}
		//copy parent to new oneRecord cuz parentRecord is reference. we don't want it to change any data of it.
		if(parentRecord!=null) {
			for(IBean field : parentRecord) {
				oneRecord.add(new IBean(field.getName(),field.getValue())); 
			}
		}
		
		//check for records
		if(oneObject.get("record")!=null && oneObject.get("record") instanceof JSONObject) {
			JSONObject record = (JSONObject) oneObject.get("record");
			List<String> allFields = jSonKeyToStringList(record.keySet());
			int mappingFieldAdded = 0;
			boolean isMessageAdded = false;
			for(int i=0; i<oneRecord.size(); i++) {
				for(String field: allFields) {
					if(!isMessageAdded) {
						if(field.equalsIgnoreCase("MESSAGE_ID")) {
							lstMsg.add(new BatchExecutorResponseItem(null, record.get(field)!=null?record.get(field).toString():null, "Completed", "Creation of 1 "+oneObject.get("objectType").toString()));
							isMessageAdded = true;
							mappingFieldAdded++;
						}
					}
					if(oneRecord.get(i).getName().contains(oneObject.get("objectType").toString()+"."+field)){
						oneRecord.get(i).setValue(record.get(field).toString());
						mappingFieldAdded ++;
						break;
					}
				}
				if(record.size()==mappingFieldAdded) //if all fields added we just break it to increase speed
					break;
			}
		}
		//check for child 
		if(oneObject.get("children") instanceof JSONArray) {
			JSONArray childOfOneRecord = (JSONArray) oneObject.get("children");
			if(!childOfOneRecord.isEmpty()) {
				List<IBean> parentRecords = new ArrayList<IBean>();
				//now the current record has child, so it will become a parent and let's continue find it's child.
				//we need to copy from oneRecord to a new List<IBean> object cuz it is reference. if we pass it as param it will effect all record
				//so we need to make new object to make it to be repeated parent record
				for(IBean tmpField : oneRecord) {
					parentRecords.add(new IBean(tmpField.getName(), tmpField.getValue()));
				}
				childOfOneRecord.forEach(row -> {
					try {
						readJsonObjectToFlat((JSONObject) row, null, new ArrayList<IBean>(), records, parentRecords, lstMsg);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
			else {
		        //add each records to a list if don't have no child
				records.add(oneRecord);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void readJsonObjectToFlat2(JSONObject oneObject, List<String> lstMappingFields, List<IBean> oneRecord, 
			List<List<IBean>> records, List<IBean> parentRecord, List<BatchExecutorResponseItem> lstMsg ) throws Exception {
		//Firstly, initial mapped fields, run oonly once time per oneObject
		if(oneRecord==null) {
			oneRecord = new ArrayList<IBean>();
			oneRecord.add(new IBean("MESSAGE",""));
			oneRecord.add(new IBean("EXTERNAL_FIELD_NAME",""));
			for(String oneMapField : lstMappingFields) {
				if(oneMapField.contains("EXTERNAL_FIELD_NAME")) {
					oneRecord.get(1).setValue(oneMapField.substring(oneMapField.indexOf(".")+1, oneMapField.length()));
				}else {
					IBean oneField = new IBean();
					oneField.setName(oneMapField);
					oneRecord.add(oneField);
				}
			}
		}
		//copy parent to new oneRecord cuz parentRecord is reference. we don't want it to change any data of it.
		if(parentRecord!=null) {
			for(IBean field : parentRecord) {
				oneRecord.add(new IBean(field.getName(),field.getValue())); 
			}
		}
		
		//check for records
		if(oneObject.get("record")!=null && oneObject.get("record") instanceof JSONObject) {
			JSONObject record = (JSONObject) oneObject.get("record");
			List<String> allFields = jSonKeyToStringList(record.keySet());
			int mappingFieldAdded = 0;
			boolean isMessageAdded = false;
			for(int i=0; i<oneRecord.size(); i++) {
				for(String field: allFields) {
					if(!isMessageAdded) {
						if(field.equalsIgnoreCase("MESSAGE_ID")) {
							lstMsg.add(new BatchExecutorResponseItem(null, record.get(field)!=null?record.get(field).toString():null, "Completed", "Creation of 1 "+oneObject.get("objectType").toString()));
							oneRecord.get(0).setValue(record.get(field).toString());
							isMessageAdded = true;
							mappingFieldAdded++;
						}
					}
					if(oneRecord.get(i).getName().contains(oneObject.get("objectType").toString()+"."+field)){
						oneRecord.get(i).setName(field);
						oneRecord.get(i).setValue(record.get(field).toString());
						mappingFieldAdded ++;
						break;
					}
				}
				if(record.size()==mappingFieldAdded) //if all fields added we just break it to increase speed
					break;
			}
		}
		//check for child 
		if(oneObject.get("children") instanceof JSONArray) {
			JSONArray childOfOneRecord = (JSONArray) oneObject.get("children");
			if(!childOfOneRecord.isEmpty()) {
				List<IBean> parentRecords = new ArrayList<IBean>();
				//now the current record has child, so it will become a parent and let's continue find it's child.
				//we need to copy from oneRecord to a new List<IBean> object cuz it is reference. if we pass it as param it will effect all record
				//so we need to make new object to make it to be repeated parent record
				for(IBean tmpField : oneRecord) {
					parentRecords.add(new IBean(tmpField.getName(), tmpField.getValue()));
				}
				childOfOneRecord.forEach(row -> {
					try {
						readJsonObjectToFlat2((JSONObject) row, null, new ArrayList<IBean>(), records, parentRecords, lstMsg);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
			else {
		        //add each records to a list if don't have no child
				records.add(oneRecord);
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes" })
	protected List<String> jSonKeyToStringList(Set key) {       
		List<String> keys = new ArrayList<String>();
		for(Object oneKey : key) {
			keys.add(oneKey.toString());
        }
        return keys;      
	}
	
	protected void getMappingFields(SFIntegrationService integrationService, String intfId, List<String> lstMappingField) throws Exception {
		List<IMapping__c> mpps = integrationService.getIMapping__c(intfId);
		for(IMapping__c tmpMap : mpps) {
			if( !(tmpMap.getTarget__c().equals("SKYVVA__PARENTID") || tmpMap.getTarget__c().equals("#")) )
				lstMappingField.add(tmpMap.getTarget_Object__c()+"."+tmpMap.getTarget__c()); //no need: SKYVVA__PARENTID, #
			if(tmpMap.getEXT_ID__c()) lstMappingField.add("EXTERNAL_FIELD_NAME."+tmpMap.getTarget__c());
		}
	}
	
	protected void getAllRelatedMap(SFIntegrationService integrationService, String intfId, List<String> lstMappingField) throws Exception {
		List<IMapping__c> mpps = integrationService.getIMapping__c(intfId);
		for(IMapping__c tmpMap : mpps) {
			if( !(tmpMap.getTarget__c().equals("SKYVVA__PARENTID") || tmpMap.getTarget__c().equals("#")) )
				lstMappingField.add(tmpMap.getTarget_Object__c()+"."+tmpMap.getTarget__c()); //no need: SKYVVA__PARENTID, #
		}
		List<IChained_Interfaces__c> sfobjects = integrationService.getIChained_Interfaces__c(intfId);
		// return true if hasChain
		if(sfobjects!=null) {
			getAllRelatedMap(integrationService, sfobjects.get(0).getChildInterfaceId__c(), lstMappingField);
		}
	}
	
	public List<String> keepFiles = new ArrayList<String>();
	
	public List<String> getKeepFiles() {
		return keepFiles;
	}
	
	public void setKeepFiles(List<String> keepFiles) {
		this.keepFiles = keepFiles;
	}
	
	protected void checkFileExtenstion(String filename, String filetype) throws Exception {
		// check if file type and file chosen if they have same extension 
		if(!filename.isEmpty() && !filetype.isEmpty() && !filetype.equalsIgnoreCase(FileTypeHelper.OTHER)) {
			List<String> extensionTypes = new ArrayList<>();
			String fileTypeChosen = filename.substring(filename.lastIndexOf(".")+1).toUpperCase();
			// due to some fileType has many extension type so we need to add it as list for example excel has xlsx,xls
			if(filetype.equalsIgnoreCase(FileTypeHelper.EXCEL)) {
				extensionTypes.add("xlsx".toUpperCase());
				extensionTypes.add("xls".toUpperCase());
			}else {
				extensionTypes.add(filetype.toUpperCase());
			}
		
			if(!extensionTypes.contains(fileTypeChosen)) throw new Exception("File type '"+filetype+"' and the file chosen '"+filename+"' have different extension.");
		}
	}
	
	public List<BatchExecutorResponseItem> createFailedMessage(IBean[][] beans, String msg) {
		List<BatchExecutorResponseItem> response = new ArrayList<BatchExecutorResponseItem>();
		for (int i = 0; i < beans.length; i++) {
			BatchExecutorResponseItem bMsg = new BatchExecutorResponseItem(i, beans[i][0].getValue(), "Failed", msg);
			response.add(bMsg);
		}
		return response;
	}
}
