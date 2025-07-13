package com.iservice.adapter.batch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.Map;

import com.iservice.adapter.IDBConnection;
import com.sforce.soap.schemas._class.IServices.IBean;

public interface IBatchExecutor {
	
	
	List<BatchExecutorResponseItem> executeBatch(IDBConnection adapterConn, IBean[][] data) throws Exception;

	//update and insert
	List<BatchExecutorResponseItem> executeBatch(IDBConnection adapterConn, String sqlScripts, List<String> lstMsgKeysUpdated, List<String> lstMsgKeysInserted,
			List<Map.Entry<String, String>> lstMsgKeysInsertExist,List<Map.Entry<String, String>> lstMsgKeysUpdateNotExist) throws Exception;
	
	//delete
	List<BatchExecutorResponseItem> executeBatch(IDBConnection adapterConn, String sqlScripts, List<Map.Entry<String, String>> lstMsgKeysDeleted) throws Exception;
	
	String getUpdateSQL(Map<Integer, Integer> mapColTypes, Map<Integer, String> mapColTypeNames, IBean[] bean, String externalIdField, String externalIdValue) throws Exception;
	
	String getInsertSQL(Map<Integer, Integer> mapColTypes, Map<Integer, String> mapColTypeNames, IBean[] bean) throws Exception;
	
	//get delete
	String getDeleteSQL(IBean[] bean, String externalIdField, String externalIdValue) throws Exception;
	
	// procedure
	String getProcedureSQL(Map<Integer, Integer> mapColTypes, Map<Integer, String> mapColTypeNames, IBean[] bean, String externalIdField, String externalIdValue) throws Exception;
	List<BatchExecutorResponseItem> executeBatchProcedure(IDBConnection adapterConn, String sqlScripts, List<Map.Entry<String, String>> lstMsgKeysProcedured, IBean[][] beans) throws Exception;
	
	String terminateScript(String sql);
	
	void setDatabaseProductionVersion(String version);
	
	String getSFDateTimeFormat();
	
	//String getDBMSDateTimeFormat(); //20110829 v1.12
	
	String getTableName();
	void setTableName(String tblName);
	
	String getExternalIdField();
	void setExternalIdField(String externalIdField);
	String getExternalIdFieldType();
	void setExternalIdFieldType(String externalIdFieldType) ;
	
	//13012011
	Integer getExternalIdFieldTypeInt();
	void setExternalIdFieldTypeInt(Integer externalIdFieldTypeInt);
	String getExternalIdFieldTypeName();
	void setExternalIdFieldTypeName(String externalIdFieldTypeName);

	void setConnection(Connection connection);

	Connection getConnection();
	
	void setResultSetMetaData(ResultSetMetaData rsmd);
	
	ResultSetMetaData getResultSetMetaData();
	
	//20110825 v1.12
	PreparedStatement getData(IBean[][] beans) throws Exception;
	Integer getIndexExternalIdInIRecord();
	
	void setNoClientTool(boolean noClientTool);
	
	String getDatabaseType();

	void setDatabaseType(String databaseType);
	
	//surround a field name or table name with a specified character (EX: "xxx" -> "`xxx`" for MYSQL) 
	String surroundName(String name);

}
