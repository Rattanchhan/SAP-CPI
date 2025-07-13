package com.iservice.task;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.lang3.StringUtils;

import com.iservice.gui.data.ISFIntegrationObject;
import com.iservice.gui.data.Integration__c;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.gui.data.Property__c;
import com.iservice.gui.helper.DirectionTypeHelper;
import com.iservice.gui.helper.PropertyNameHelper;
import com.iservice.sforce.ISFService;
import com.iservice.sforce.SFIntegrationService;
import com.iservice.task.GenericSFTask.GenericSFTaskException;
import com.model.iservice.Property;
import com.sforce.soap.SFConnectorConfig;

public class MapIntegrationInfo extends HashMap<String, String> {
	private static final long serialVersionUID = -9003962316281152056L;
	
	private static final int DEFAULT_PACKET_SIZE = 20;
    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(MapIntegrationInfo.class);
	
	protected Calendar runTime;
	protected SFIntegrationService sfIntegrationService;
	protected Integration__c integration;
	protected Interfaces__c interfaces;
	protected Map<String, String> adapterProperties = new HashMap<>();
	
	InputStream inputData;
	Object responseObject;
	
	protected List<? extends ISFIntegrationObject> headers;
	
	public MapIntegrationInfo(SFIntegrationService sfIntegrationService, Integration__c integration, 
			Interfaces__c interfaces, Map<String,String> adapterProperties, Calendar runTime, InputStream inputData) throws GenericSFTaskException {
		this.runTime = runTime;
		this.sfIntegrationService = sfIntegrationService;
		this.integration = integration;
		this.interfaces = interfaces;
		this.adapterProperties = adapterProperties;
		this.inputData = inputData;
	}
	
	/**
	 * for doTestQuery
	 * @param sfIntegrationService
	 * @param integration
	 * @param interfaces
	 * @param adapterProperties
	 * @throws GenericSFTaskException
	 */
	public MapIntegrationInfo(SFIntegrationService sfIntegrationService, Integration__c integration, 
			Interfaces__c interfaces, Map<String,String> adapterProperties, Calendar runTime) throws GenericSFTaskException {
		this.runTime = runTime;
		this.sfIntegrationService = sfIntegrationService;
		this.integration = integration;
		this.interfaces = interfaces;
		this.adapterProperties = adapterProperties;
	}

	public MapIntegrationInfo(SFIntegrationService sfIntegrationService, Integration__c integration, Interfaces__c interfaces, Calendar runTime) throws Exception {
		this.runTime = runTime;
		this.sfIntegrationService = sfIntegrationService;
		this.integration = (integration!=null? integration : new Integration__c());
		this.interfaces = (interfaces!=null? sfIntegrationService.getInterfaceById(interfaces.getId()) : new Interfaces__c());
//		initAdapter();
	}
	
	@SuppressWarnings("unused")
	private void initAdapter() throws Exception{
		List<Property__c> properties = new ArrayList<>();
		String adapterName = "";
		String adapterType = "";
		String adapterId = (interfaces.getType__c()!=null? (interfaces.getType__c().equalsIgnoreCase(DirectionTypeHelper.INBOUND))?interfaces.getSource_Adapter__c():interfaces.getAdapterId__c() : "");
		if(StringUtils.isBlank(adapterId) && integration.getAdapter()==null) return;
		if(StringUtils.isBlank(adapterId)) {
			LOGGER.warn("InterfaceName: "+ interfaces.getName__c() +" has no Adapter. Interface will use Adapter on Integration.");
			if(integration.getAdapter()==null) {
				throw new GenericSFTaskException("IntegrationName: "+ integration.getName() +" and InterfaceName: "+ interfaces.getName__c() +"  has no Adapter. Please set Adapter to Interface.");
			}
			adapterId = integration.getAdapter().getId();
		}
		adapterName = sfIntegrationService.getAdapterById(adapterId).getName__c();
		adapterType = sfIntegrationService.getAdapterById(adapterId).getType__c();
		properties = sfIntegrationService.getAdapter(adapterType, adapterId).getProperties();
		
		if (properties != null) {
			this.adapterProperties = new HashMap<String, String>();
			this.adapterProperties.put("AdapterName", adapterName);
			this.adapterProperties.put(PropertyNameHelper.ADAPTER_TYPE, adapterType);
			properties.forEach(property ->{
				Property__c p = property;
				this.adapterProperties.put(p.getName__c(), p.getValue2__c());
			});
		}
	}
	public SFConnectorConfig getSalesforceConfiguration() throws Exception{
		return sfIntegrationService.getSalesforceConfiguration();
	}
	// when from gui we need to get it from object interface
	public String getNumberOfRecordPerBatch() {
		return interfaces.getNumber_of_Records_Per_Batch__c();
	}

	public String getInterfaceId() {
		return interfaces.getId();
	}

	public String getInterfaceName() {
		return interfaces.getName__c();
	}

	public String getSourceName() {
		return interfaces.getSource_Name__c();
	}

	public String getQuery() {
		if(isInitialization()) {
			return interfaces.getInitialization_Query__c();
		}
		return interfaces.getQuery__c();
	}
	
	public String getOriginalQuery() {
		return interfaces.getQuery__c();
	}

	public boolean isInitialization() {
		return interfaces.getInitialization__c();
	}

	public boolean isBulkMode() {
		return interfaces.getIsBULKAPI__c();
	}

	public boolean isBatchMode() {
		return interfaces.getBatch_Mode__c();
	}

	public boolean isAutoSwitchMode() {
		return interfaces.getUse_Auto_Switch_Mode__c();
	}

	public int getIntegrateMaxSize() {
		return GenericSFTask.validateValue(interfaces.getIntegrate_Max_Size__c(), ISFService.INTEGRATE_MAX_SIZE);
	}

	public int getIntegrateBatchSize() {
		return GenericSFTask.validateValue(interfaces.getIntegrate_Batch_Max_Size__c(), ISFService.INTEGRATE_BATCH_MAX_SIZE);
	}

	public int getBulkPackageSize() {
		return GenericSFTask.validateValue(interfaces.getBulk_Package_Size__c(), ISFService.BULK_PACKAGE_SIZE);
	}
	
	private int packageSize = 0;
	public int getPackageSize() {
		if(packageSize == 0) {
			String nbrpb = ""+DEFAULT_PACKET_SIZE;
			if(StringUtils.isBlank(interfaces.getPackage_Size__c())) {
				if(StringUtils.isBlank(interfaces.getNumber_of_Records_Per_Batch__c())) {
					if(integration.getInterface_groups().size()>0 && !StringUtils.isBlank(integration.getInterface_groups().get(0).getPackageSize__c())) {
						nbrpb = integration.getInterface_groups().get(0).getPackageSize__c();
					}else {
						if(StringUtils.isNotBlank((integration.getPacket__c()))) {
							nbrpb = integration.getPacket__c();
						}
					}
				}else {
					nbrpb = interfaces.getNumber_of_Records_Per_Batch__c();
				}
			}else {
				nbrpb = interfaces.getPackage_Size__c();
			}
			packageSize = (int) Double.parseDouble(StringUtils.trim(nbrpb));
		}
		return packageSize;
	}
	
	public String getAdapterName() {
		return adapterProperties.get("AdapterName");
	}

	public String getAdapterType() {
		return adapterProperties.get(PropertyNameHelper.ADAPTER_TYPE);
	}

	public Map<String, String> getAdapterProperties() {
		return adapterProperties;
	}

	public String getIntegrationId() {
		return integration.getId();
	}

	public Integration__c getIntegration() throws Exception {
		if (integration == null) {
			String integrationId = getIntegrationId();
			if (StringUtils.isNotBlank(integrationId)) {
				integrationId = getInterfaces().getIntegration__c();
			}
			integration = sfIntegrationService.getIntegrationById(integrationId);
		}
		return integration;
	}

	public void setIntegration(Integration__c integration) {
		this.integration = integration;
	}

	public Interfaces__c getInterfaces() {
		return this.interfaces;
	}

	public void setInterfaces(Interfaces__c interfaces) {
		this.interfaces = interfaces;
	}

	public SFIntegrationService getSFIntegrationService() {
		return sfIntegrationService;
	}
	
	public ISFService getISFService() {
		return sfIntegrationService.getISFService();
	}

	public List<? extends ISFIntegrationObject> getListHeader(
			com.model.iservice.Adapter adapter) throws Exception {
		return getListHeader(adapter.getProperty());
	}

	public List<? extends ISFIntegrationObject> getListHeader(List<Property> properties)
			throws Exception {
		if (headers == null) {

			Interfaces__c myInterface = getInterfaces();
			for (Property p : properties) {
				if (p.getName().equals(PropertyNameHelper.HAS_FILE_HEADER)
						&& p.getValue().equals("false")) {
					// get IStructure or MessageType
					if (getSFIntegrationService() != null) {
						// MessageType!=null
						if (myInterface.getMessageType__c() != null) {
							headers = getSFIntegrationService().getMsgTypeFieldEntry__c(
									myInterface.getMessageType__c());
						}
						// IStructure==null
						else if (myInterface.getIStructures() != null) {
							headers = myInterface.getIStructures();

						} else {
							throw new Exception(
									"Interface has no MessageType or IStructure.");
						}
					}
					break;
				}
			}
		}

		return headers;
	}

	public Calendar getRunTime() {
		return runTime;
	}

	public void setRunTime(Calendar runTime) {
		this.runTime = runTime;
	}
	
	public InputStream getInputData() {
		return this.inputData;
	}
	
	public Object getResponseObject() {
		return responseObject;
	}
	
	public void setResponseObject(Object responseObject) {
		this.responseObject = responseObject;
	}
}
