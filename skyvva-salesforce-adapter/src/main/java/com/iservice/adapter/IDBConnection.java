package com.iservice.adapter;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;

import com.iservice.adapter.batch.BatchExecutorResponseItem;
import com.iservice.adapter.batch.IBatchExecutor;
import com.iservice.adapter.reader.QueryResult;
import com.iservice.gui.data.ISFIntegrationObject;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.sforce.SFIntegrationService;
import com.iservice.task.MapIntegrationInfo;
import com.model.iservice.Adapter;
import com.sforce.soap.schemas._class.IServices.IBean;

public interface IDBConnection {

	public static final String COLUMN_NAME = "COLUMN_NAME";
	public static final String COLUMN_LABEL = "COLUMN_LABEL";
	public static final String COLUMN_SIZE = "COLUMN_SIZE";
	public static final String DATA_TYPE = "DATA_TYPE";
	public static final String SOAP_TYPE = "SOAP_TYPE";

	boolean login() throws Exception;

	public Adapter getAdapter();

	public void setAdapter(Adapter adapter);

	public void setConnectionInfo(Map<String, String> mapProp) throws Exception;
	
	public List<File> updateToFTPFile(IBean[][] beans, List<List<IBean>> lstRecords, String folder, String targetFileName, 
									  SFIntegrationService integrationService, List<BatchExecutorResponseItem> lsMsg, Interfaces__c intf) throws Exception;
	
	/**
	 * return a map of <MessageId, update status> 
	 */
	List<BatchExecutorResponseItem> updateChainData(SFIntegrationService integrationService, Interfaces__c intff, IBean[][] records) throws Exception;
	
	List<BatchExecutorResponseItem> update2(SFIntegrationService integrationService, Interfaces__c intff, IBean[][] records) throws Exception;
	
	List<BatchExecutorResponseItem> update(IBean[][] beans, String interfaceType, SFIntegrationService integrationService, Interfaces__c intff) throws Exception;
	
	List<BatchExecutorResponseItem> updateV3(SFIntegrationService integrationService, JSONArray payloadRecord, Interfaces__c intf ) throws Exception;

	int update(String expression) throws Exception;

	List<String> getAllObjects(String objectType) throws Exception;

	List<List<String>> getColumns(String objectName, List<? extends ISFIntegrationObject> listHeader) throws Exception;
	List<List<String>> getColumnsOutBound(String objectName) throws Exception;//retrieve table field for Interface IStructure OutBound(no field with autoincrement type)

	List<List<String>> getRelationship(String objectName) throws Exception;

	void terminate() throws Exception;
	
	void onError() throws Exception;
   
	void setTableName(String tblName);
   
	void setExternalIdField(String extIdField);
	
	void setInterfaceType(String interfaceType);
   
	IBatchExecutor getBatchExecutor() throws Exception;
	
	// get parameter from stored procedure
	List<List<String>> getParameters(String objectName) throws Exception;
	
	void unlockFiles() throws Exception;
	
	public boolean doInBoundIntegration(String criteria,MapIntegrationInfo interaceInfo) throws Exception;
	
	public boolean doTestInBoundIntegration(String criteria,MapIntegrationInfo integrationInfo) throws Exception;
	
	public QueryResult doTestQuery(String criteria,MapIntegrationInfo integrationInfo) throws Exception;
	
	public IIntegrationProcessListener getProcessLisenter() ;

	public void setProcessLisenter(IIntegrationProcessListener processLisenter);
	
}
