package com.iservice.gui.helper;

import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;

import com.iservice.gui.data.Adapter__c;
import com.iservice.gui.data.Interfaces__c;

public class ComboBoxItemsHelper {

	public static String CREATE_NEW = "Create New";
	public static String ALL = "-All-";

	public static Interfaces__c allInterface(String skyvvaPackage) {
		Interfaces__c createNew = new Interfaces__c(skyvvaPackage);
		createNew.setName__c(ALL);
		return createNew;
	}

	public static Adapter__c createNewAdapter(String skyvvaPackage) {
		Adapter__c createNew = new Adapter__c(skyvvaPackage);
		createNew.setName(CREATE_NEW);
		return createNew;
	}
	
	public static void addIntegrationMode(JComboBox<String> cboIntegrationMode){
		cboIntegrationMode.removeAllItems();
		
		for(String key : IntegrationModeScheduleHelper.getScheduleModeMap().keySet()){
			cboIntegrationMode.addItem(key);
		}
		
		cboIntegrationMode.setSelectedIndex(0);
	}
	
	public static void addAdapterTypes(JComboBox<String> cboAdapterType, List<String> adapterTypes) {
		cboAdapterType.removeAllItems();
		
		for(String str : adapterTypes){
			cboAdapterType.addItem(str);
		}
		
		cboAdapterType.setSelectedIndex(0);
	}

	public static void addAdapterNames(JComboBox<Adapter__c> cboAdapterName,
			Map<String, Adapter__c> mapNames) {
		if (mapNames == null)
			return;
		for (Object key : mapNames.keySet()) {
			cboAdapterName.addItem(mapNames.get(key));
		}
	}

	public static void addTableNames(JComboBox<String> cboSObjects, List<String> Names) {
		cboSObjects.removeAllItems();
		if (Names == null)
			return;
		for (String str : Names) {
			cboSObjects.addItem(str);
		}
	}

	public static void addOperationType(JComboBox<String> cboOperationType, String interfaceType,String adapterType) {
		cboOperationType.removeAllItems();
		cboOperationType.addItem(PropertyNameHelper.UPSERT);
		cboOperationType.addItem(PropertyNameHelper.INSERT);
		cboOperationType.addItem(PropertyNameHelper.UPDATE);
		
		//20120711 - add operation type "delete" in case INBOUND (Agent 2 SF)
		if(DirectionTypeHelper.INBOUND.equalsIgnoreCase(interfaceType)) {
			cboOperationType.addItem(PropertyNameHelper.DELETE);
		}
		//18102016 - add operation type "delete" in case INBOUND (SF 2 Agent)
		if(DirectionTypeHelper.OUTBOUND.equalsIgnoreCase(interfaceType) && adapterType.equalsIgnoreCase(AdapterTypeHelper.JDBC)){
			cboOperationType.addItem(PropertyNameHelper.DELETE);
			cboOperationType.addItem(PropertyNameHelper.STORED_PROCEDURE);
		}
		
	}

	public static void addSeparator(JComboBox<String> cboSeparator) {
//		cboSeparator.removeAllItems();
//		for(String s:CSVAdapter.separaters){
//			cboSeparator.addItem(s);
//		}
	}

	public static void addInterfaceStatus(JComboBox<String> cboStatus) {
		cboStatus.removeAllItems();
		cboStatus.addItem("Development");
		cboStatus.addItem("Deployed");
		cboStatus.setSelectedIndex(0);
	}

	public static void addServerEnvironment(JComboBox<String> cboEnvironment) {
		cboEnvironment.removeAllItems();
		cboEnvironment.addItem(ServerHelper.SANDBOX);
		cboEnvironment.addItem(ServerHelper.PRODUCTION_DEVELOPER);
		cboEnvironment.setSelectedIndex(1);
	}
	
	//30-07-2012
	public static void addProxyServerType(JComboBox<String> cboProxyServerType) {
		cboProxyServerType.removeAllItems();
		cboProxyServerType.addItem("HTTPS");
		cboProxyServerType.addItem("HTTP");
		cboProxyServerType.addItem("SOCK");
		cboProxyServerType.setSelectedIndex(0);
	}
	
	public static void addInterfaceType(JComboBox<String> cboInterfaceType){
		cboInterfaceType.removeAllItems();

		cboInterfaceType.addItem(DirectionTypeHelper.INBOUND);
		cboInterfaceType.addItem(DirectionTypeHelper.OUTBOUND);
		
	}
	
	public static void addFunctions(JComboBox<String> cboFunctions, String intfType){
//		cboFunctions.removeAllItems();
//		
//		if(DirectionTypeHelper.INBOUND.equalsIgnoreCase(intfType)) {
//			cboFunctions.addItem(CoreJDBCAdapter.$PARENTLOOKUP);
//			cboFunctions.addItem(CoreJDBCAdapter.$TODAY);
//			//07-07-2017
//			cboFunctions.addItem(CoreJDBCAdapter.$LAST_INTERFACE_DATETIME);
//			//11-01-2012
//			cboFunctions.addItem(CoreJDBCAdapter.$LAST_INTEGRATION_DATETIME);
//		}else {
//			//07-07-2017
//			cboFunctions.addItem(CoreJDBCAdapter.$LAST_INTERFACE_DATETIME);
//			//20-10-2019
//			cboFunctions.addItem(CoreJDBCAdapter.$QUERY_FILTER_SOBJECT);
//		}
		
	}
	
	// 08-09-2017
	public static void addScheduleMode(JComboBox<String> cboScheduleMode) {
		cboScheduleMode.removeAllItems();
		cboScheduleMode.addItem(ScheduleModeHelper.PROCESSING);
		cboScheduleMode.addItem(ScheduleModeHelper.REPROCESSING);
		cboScheduleMode.setSelectedIndex(0);
	}

}