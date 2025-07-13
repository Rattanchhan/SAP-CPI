package com.iservice.sforce;

import java.util.List;

import com.iservice.gui.data.Integration__c;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.model.IMessageTree;
import com.iservice.task.MapIntegrationInfo;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.schemas._class.IServices.IBean;
import com.sforce.ws.ConnectionException;

public interface ISFService {

	public static String OPERATION_SETUP="SETUP";
	public static String OPERATION_LOGS="LOGS";

	public static String OPERATION_SETUP_OUTBOUND="SETUP_OUTBOUND";//search integration for outbound interfaces 
	public static String OPERATION_SF_DATA="SALESFORCE_DATA";//search data records at SF for an outbound interface
	public static String OPERATION_SF_ALL_OBJECT_IDS="SALESFORCE_DATA_ALL_IDS";//search all object ids at SF for an outbound interface

	//20012011 Build Query for an interface (filter objects concerned)
	public static final String OPERATION_SF_BUILD_QUERY_INTERFACE = "SF_BUILD_QUERY_INTERFACE";

	//in case reprocess SF2Agent
	public static String OPERATION_SF2AGENT_REPROCESS_ALL="SALESFORCE_AGENT_REPROCESS_ALL";
	public static String OPERATION_SF2AGENT_REPROCESS_IDS="SALESFORCE_AGENT_REPROCESS_IDS";

	//19-07-2017 v1.47
	public static final String LAST_INTERFACE_DATETIME="LAST_INTERFACE_DATETIME";

	public static String OPERATION_UPDATE_MESSAGES="UPDATE MESSAGES";

	public static String INTEGRATION_C="INTEGRATION";
	public static String INTERFACEE="INTERFACE";
	public static String OBJECT_IDS="OBJECT_IDS";
	public static String MESSAGE_IDS="MESSAGE_IDS";

	//16112010 dateTimeFormat
	public static String SF_DATE_TIME_FORMAT="SF_DATE_TIME_FORMAT";

	//17112010 test Query
	public static String SALESFORCE_AGENT_TEST_QUERY="SALESFORCE_AGENT_TEST_QUERY";
	public static String MAX_LIMIT_TEST_QUERY="MAX_LIMIT_TEST_QUERY";

	//05112010
	public static String OPERATION_SEARCH_ADAPTER="SEARCH_ADAPTER";

	//CHAININTERFACE
	public static String CHAININTERFACE="CHAININTERFACE";
	public static String CHAIN_CHILD_NAME="CHILDNAME"; //child interface name
	public static String CHAIN_PARENT_NAME="PARENTNAME"; //parent interface name
	public static String CHAIN_CHILD_SOURCE_NAME="CHILDSOURCENAME"; //child interface source name
	public static String CHAIN_PARENT_SOURCE_NAME="PARENTSOURCENAME"; //parent interface source name
	public static String CHAIN_SEQUENCE="SEQUENCE"; //chain sequence
	public static String CHAIN_INITOPERATION="INITOPERATION"; //chain interface's init operation
	public static String CHAIN_RELATIONSHIPNAME="RELATIONSHIPNAME"; //chain interface's parent relationship name

	//23022011 use for Professional Edition
	public static String ATTACHMENT_NAME = "ATTACHMENT_NAME";

	//20110805 v1.12
	public static final String CLEAR_MESSAGES_AFTER_PROCESSING = "CLEAR_MESSAGES_AFTER_PROCESSING";

	//20111116 v1.14
	public static final String INTERFACE_ID = "INTERFACE_ID";
	public static final String INITIALIZATION = "INITIALIZATION";

	//20120320 v1.16
	public static final String INVOKED_INTERFACE = "INVOKED_INTERFACE";
	public static final String INVOKED_INTERFACE_ID = "INVOKED_INTERFACE_ID";

	//TODO ??? NOT YET USED
	public static final String OPERATION_TYPE = "OPERATION_TYPE";

	//Packet on interface-level (interface field Number_of_Records_Per_Batch__c)
	public static final String NUMBER_OF_RECORDS_PER_BATCH = "NUMBER_OF_RECORDS_PER_BATCH";

	public static final Integer INTEGRATE_MAX_SIZE = 200;
	public static final Integer INTEGRATE_BATCH_MAX_SIZE = 5000;

	public static final Integer BULK_PACKAGE_SIZE = 1000;

	public static final int RETRY_TIME_INTERVAL_DEFAULT = 60000;
	public static final int MAX_NUMBER_OF_RETRY_DEFAULT = 5; 

	public void integrate(com.sforce.soap.schemas._class.IServices.IIntegration integration) throws java.rmi.RemoteException, Exception;

	public com.sforce.soap.schemas._class.IServices.IBean[][] search(com.sforce.soap.schemas._class.IServices.IBean[] filter) throws java.rmi.RemoteException, Exception;

	public boolean login() throws ConnectionException;

	public void setSessionId(String sessionID);

	public String getSessionId() throws ConnectionException;

	public String getServiceUrl();
	public String getIntegrationUrl() ;
	
	public PartnerConnection getSFPartner() throws Exception;
	
	public String getPackage();
	public String getQueryPackage();
	
	public int getMaxRetryNumber(Integration__c intg, Interfaces__c intf);
	public int getRetryTimeInterval(Integration__c intg, Interfaces__c intf);

	public IBean[][] search(IBean[] filter, MapIntegrationInfo mapIntgInfor) throws Exception;
	public void integrateV3Inbound(List<IMessageTree> iMessage, int numberOfTree, String interfaceMode)throws Exception;
	public void setMapIntgInfo(MapIntegrationInfo mapIntgInfo);
	
}
