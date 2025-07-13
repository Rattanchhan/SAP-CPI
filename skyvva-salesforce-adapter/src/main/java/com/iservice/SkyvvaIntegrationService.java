package com.iservice;

import static java.lang.String.format;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.iservice.adapter.CoreIntegration;
import com.iservice.adapter.IDBConnection;
import com.iservice.database.CacheSqlite;
import com.iservice.gui.data.Integration__c;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.gui.helper.PropertyNameHelper;
import com.iservice.model.Prefs;
import com.iservice.model.SalesforceCredential;
import com.iservice.sforce.MapSFConnInfo;
import com.iservice.sforce.SFIntegrationService;
import com.iservice.task.MapIntegrationInfo;

import static java.lang.String.format;

public class SkyvvaIntegrationService {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(SkyvvaIntegrationService.class);
	
	SFIntegrationService sfService;
	String integrationName;
	String interfaceName;
	
	public enum Mode {
		SKYVVA_BATCH,
		SKYVVA_BULK,
		SALESFORCE_BULK,
		AUTO_SWITCH,
		NORMAL
	}
	
	public enum PayloadFormat{
		XML,
		JSON
	}
	
	public SkyvvaIntegrationService(SalesforceCredential salesforceCredential, String sfApiVersion) {
		// set SF credential
    	Map<String, String> mapProps = new HashMap<>();
		mapProps.put("username", salesforceCredential.getUsername());
		mapProps.put("password", salesforceCredential.getPassword());
		mapProps.put("token", salesforceCredential.getToken());
		mapProps.put("package", "skyvvasolutions");
        String loginUrl = salesforceCredential.getLoginUrl();
        if (!loginUrl.contains("/services/Soap/u/")) {
            loginUrl+= format("/services/Soap/u/%s", sfApiVersion);
        }
        mapProps.put("loginUrl", loginUrl);
		MapSFConnInfo mapSFConnInfo = new MapSFConnInfo(mapProps);
        try {
            LOGGER.trace("Trying to login to SF...");
		    sfService = new SFIntegrationService(mapSFConnInfo);
			sfService.login();
			
			// cache SF user. we need it to create Message for SF bulk mode
			List<Prefs> prefs = new ArrayList<Prefs>();
			for(String key : mapProps.keySet()) {
				Prefs pref = new Prefs();
				pref.setKey(key);
				pref.setValue(mapProps.get(key));
				pref.setUsername(salesforceCredential.getUsername());
				prefs.add(pref);
			}
			CacheSqlite.getInstance().savePropertySetting(prefs, salesforceCredential.getUsername());
            LOGGER.trace("Successfully logged in to SF");
		} catch (Exception ex) {
            LOGGER.trace(ExceptionUtils.getMessage(ex) + "\n" + ExceptionUtils.getStackTrace(ex));
			throw new RuntimeException("Couldn't login to SF: " + ex.getMessage(), ex);
		}
	}
	
	public Object integrateInbound(String integrationName, String interfaceName, PayloadFormat payloadFormat, Mode mode, InputStream businessPayload) {
		try {
			Integration__c selectedIntegration = sfService.getIntegrationByName(integrationName);
			if (selectedIntegration == null) {
			    throw new IllegalArgumentException("Couldn't find integration " + integrationName);
            }
			Interfaces__c selectedInterfaces = sfService.getInterface(selectedIntegration, interfaceName);
            if (selectedInterfaces == null) {
                throw new IllegalArgumentException("Couldn't find interface " + interfaceName);
            }

			selectInboundIntegrationMode(selectedInterfaces, mode);
			
			// simulate file adapter
			Map<String, String> adapterProperties = new HashMap<String, String>();
			adapterProperties.put(PropertyNameHelper.FILE_TYPE, payloadFormat == PayloadFormat.XML? "xml": "json");
	    	adapterProperties.put(PropertyNameHelper.ADAPTER_TYPE, "File");
	    	adapterProperties.put(PropertyNameHelper.ADAPTER_NAME, "SAP CPI");
	    	adapterProperties.put(PropertyNameHelper.FILE_UPLOAD_MODE, "CONTENT");
	    	
			MapIntegrationInfo mapIntgInfo = new MapIntegrationInfo(
					sfService,
					selectedIntegration,
					selectedInterfaces,
					adapterProperties,
					java.util.Calendar.getInstance(),
					businessPayload);	
	
			IDBConnection adapter = new CoreIntegration().getAdapter(mapIntgInfo.getAdapterProperties());
			
			adapter.doInBoundIntegration("", mapIntgInfo);
			
			return mapIntgInfo.getResponseObject();
		} catch (Exception ex) {
            Throwable rootCause = ExceptionUtils.getRootCause(ex);
            String errorMsg = format("Error occurred during execution of inbound integration. Integration: %s, " +
                    "interface: %s, payload format: %s, mode: %s. Error: %s%s",
                integrationName,
                interfaceName,
                payloadFormat,
                mode,
                ex,
                rootCause == ex ? "" : ", root cause: " + rootCause
            );
            LOGGER.error(errorMsg, ex);
			throw new RuntimeException(errorMsg, ex);
		}
	}

    void selectInboundIntegrationMode(Interfaces__c selectedInterfaces, Mode mode) {
        switch(mode) {
            case SKYVVA_BATCH:
                selectedInterfaces.setBatch_Mode__c(true);
                selectedInterfaces.setUse_Auto_Switch_Mode__c(false);
                selectedInterfaces.setIsBULKAPI__c(false);
                break;
            case SKYVVA_BULK:
                selectedInterfaces.setBatch_Mode__c(false);
                selectedInterfaces.setUse_Auto_Switch_Mode__c(false);
                selectedInterfaces.setIsBULKAPI__c(true);
                selectedInterfaces.setBulk_Version__c("SKYVVA Bulk 1.0");
                break;
            case SALESFORCE_BULK:
                selectedInterfaces.setBatch_Mode__c(false);
                selectedInterfaces.setUse_Auto_Switch_Mode__c(false);
                selectedInterfaces.setIsBULKAPI__c(true);
                selectedInterfaces.setBulk_Version__c("SFDC Bulk API 1.0");
                break;
            case AUTO_SWITCH:
                selectedInterfaces.setBatch_Mode__c(false);
                selectedInterfaces.setUse_Auto_Switch_Mode__c(true);
                selectedInterfaces.setIsBULKAPI__c(false);
                break;
            case NORMAL:
                selectedInterfaces.setBatch_Mode__c(false);
                selectedInterfaces.setUse_Auto_Switch_Mode__c(false);
                selectedInterfaces.setIsBULKAPI__c(false);
                break;
            default:
                break;
        }
    }
}
