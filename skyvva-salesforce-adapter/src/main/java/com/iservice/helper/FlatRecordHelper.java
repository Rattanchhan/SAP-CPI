package com.iservice.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.iservice.database.PropertySettingDao;
import com.iservice.gui.data.IChained_Interfaces__c;
import com.iservice.gui.data.IMapping__c;
import com.iservice.gui.data.ISFIntegrationObject;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.sforce.SFIntegrationService;
import com.sforce.soap.schemas._class.IServices.IBean;

public class FlatRecordHelper {
	
	
	private static String delimiter = "[.]"; //default
	
	private List<String> lstOrderTable = new ArrayList<String>(); //[tbAccount, tbContact]
	private Map<String,String> mapExtId = new HashMap<String, String>(); //{tbContact=tbContact.contactId, tbAccount=tblAccount.AccountId}
	private Map<String,IBean[][]> mapB = new HashMap<String, IBean[][]>(); //{tbAccount=IBean[][], tbContact=IBean[][]}
	private Map<String, String> mapOrderTableWithOperationType = new HashMap<>();
	
	private boolean isMultipleObjects = false; //?
		
	private void reinit() {
		lstOrderTable.clear();
		mapExtId.clear();
		mapB.clear();
		isMultipleObjects = false;
	}

	// #2796 use for ichain data 
	private Map<String, List<IMapping__c>> cacheMapping = new HashMap<>();
	private Map<String, List<IChained_Interfaces__c>> cacheIChainInterfaces = new HashMap<>();
	
	public FlatRecordHelper(SFIntegrationService integrationService, Interfaces__c intff, IBean[][] records) throws Exception {
		reinit();
		cacheMapping.clear();
		cacheIChainInterfaces.clear();
		
		List<IChained_Interfaces__c> icis = new ArrayList<>();
		List<IChained_Interfaces__c> sfobjects = cacheIChainInterfaces.get(intff.getId());
		if(sfobjects==null){
			sfobjects = integrationService.getIChained_Interfaces__c(intff.getId());
			if(sfobjects==null)sfobjects = new ArrayList<>();
			cacheIChainInterfaces.put(intff.getId(), sfobjects);
		}
		for(ISFIntegrationObject sfobject: sfobjects){
			icis.add((IChained_Interfaces__c)sfobject);
		}
		icis.sort((o1,o2)-> {return o1.getSequence__c().compareToIgnoreCase(o2.getSequence__c());});
		
		List<IMapping__c> mpps = cacheMapping.get(intff.getId());
		if(mpps==null){
			mpps=integrationService.getIMapping__c(intff.getId());
			cacheMapping.put(intff.getId(), mpps);
		}
		
		if(!lstOrderTable.contains(intff.getSource_Name__c())){
			lstOrderTable.add(intff.getSource_Name__c());
			mapOrderTableWithOperationType.put(intff.getSource_Name__c(), intff.getOperationType__c());
		}
		Map<String, List<List<IBean>>> mapBeans = new HashMap<>();
		mapBeans.put(intff.getSource_Name__c(), new ArrayList<>());

		List<String> extIdFieldValueList = new ArrayList<>();
		List<IBean> lstBeans = null;
		for(IBean[] oneRecord: records){

			lstBeans = new ArrayList<>(); //fields
			lstBeans.add(new IBean(oneRecord[0].getName(), oneRecord[0].getValue()));
			IBean bean = null;
			IBean parentField = null;
			String extIdField = null;
			boolean allFieldHasNoValue = true;
			for(IMapping__c mapping : mpps){
				//IMapping__c mapping = (IMapping__c)mpp;
				boolean isExtId =mapping.isExternalId();// StringUtils.isEmpty(mapping.getEXT_ID__c())?false:Boolean.parseBoolean(mapping.getEXT_ID__c());
				for(IBean field: oneRecord){
					String[] valueSplit = field.getName().split("\\.", 2);
					String fieldName = "", fieldType = "";
					if(valueSplit.length>1){
						fieldType = valueSplit[0];
						fieldName = valueSplit[1];
					}else{
						fieldName = valueSplit[0];
					}
					if(fieldName.equals("SKYVVA__PARENTID") || fieldName.equals("SKYVVA__SALESFORCEID"))continue;
					else {
						bean = (!StringUtils.isEmpty(fieldType) && intff.getSource_Name__c().equals(fieldType))?
							(fieldName.equals(mapping.getTarget__c())?new IBean(fieldName, field.getValue()):null):null;
						if(bean!=null){
							if(lstBeans == null) lstBeans = new ArrayList<IBean>();
							lstBeans.add(bean);
							if(!StringUtils.isEmpty(field.getValue())){allFieldHasNoValue=false;}
							parentField = field;
							if(isExtId) {
								extIdField = field.getValue();
								if(mapExtId.get(intff.getSource_Name__c())==null){
									mapExtId.put(intff.getSource_Name__c(), field.getName());
								}
								lstBeans.add(new IBean("EXTERNAL_FIELD_NAME", field.getName().split("\\.", 2)[1]+":text"));
							}
							break;
						}
					}
				}
			}
			if(!extIdFieldValueList.contains(extIdField) || extIdField==null){ 
				if(extIdField!=null)extIdFieldValueList.add(extIdField);
				if(!allFieldHasNoValue){
					mapBeans.get(intff.getSource_Name__c()).add(reorderBean(lstBeans));
				}
			}
		}
		
		for(IChained_Interfaces__c ici : icis){
			addChildRecord(records, integrationService, ici.getChildInterfaceId__c(), mapBeans);
		}
		
		if(mapBeans.size()>0){
			for(int l=0 ; l<lstOrderTable.size() ; l++){
				List<List<IBean>> lst = mapBeans.get(lstOrderTable.get(l));
				if(lst.size()>0){
					IBean[][] b = new IBean[lst.size()][];
					for(int i=0 ; i<lst.size() ; i++){
						b[i]=new IBean[lst.get(i).size()];
						for(int j=0 ; j<lst.get(i).size();j++){
							
							b[i][j]=lst.get(i).get(j);
						}
					}
					mapB.put(lstOrderTable.get(l), b);
				}
			}
		}
		isMultipleObjects = true;
	}

	private void addChildRecord(IBean[][] records, SFIntegrationService integrationService, String interfaceId, Map<String, List<List<IBean>>> mapBeans) throws Exception {
		
		List<IChained_Interfaces__c> icis = new ArrayList<>();
		List<IChained_Interfaces__c> sfobjects = cacheIChainInterfaces.get(interfaceId);
		if(sfobjects==null){
			sfobjects = integrationService.getIChained_Interfaces__c(interfaceId);
			if(sfobjects==null)sfobjects = new ArrayList<>();
			cacheIChainInterfaces.put(interfaceId, sfobjects);
		}
		for(ISFIntegrationObject sfobject: sfobjects){
			icis.add((IChained_Interfaces__c)sfobject);
		}
		icis.sort((o1,o2)-> {return o1.getSequence__c().compareToIgnoreCase(o2.getSequence__c());});
		List<IMapping__c> mpps =  cacheMapping.get(interfaceId);
		if(mpps==null){
			mpps=integrationService.getIMapping__c(interfaceId);
			cacheMapping.put(interfaceId, mpps);
		}
		Interfaces__c intff = integrationService.getInterfaceById(interfaceId);
		if(!lstOrderTable.contains(intff.getSource_Name__c())){
			lstOrderTable.add(intff.getSource_Name__c());
			mapOrderTableWithOperationType.put(intff.getSource_Name__c(), intff.getOperationType__c());
		}
		
		if(mapBeans.get(intff.getSource_Name__c())!=null) {
			return;
		}
		mapBeans.put(intff.getSource_Name__c(), new ArrayList<>());
		
		List<String> extIdFieldValueList = new ArrayList<>();
		List<IBean> lstBeans = null;
		for(IBean[] oneRecord: records){

			lstBeans = new ArrayList<>(); //fields
			lstBeans.add(new IBean(oneRecord[0].getName(), oneRecord[0].getValue()));
			IBean bean = null;
			IBean parentField = null;
			String extIdField = null;
			boolean allFieldHasNoValue = true;
			for(ISFIntegrationObject mpp : mpps){
				IMapping__c mapping = (IMapping__c)mpp;
				boolean isExtId =mapping.isExternalId();// StringUtils.isEmpty(mapping.getEXT_ID__c())?false:Boolean.parseBoolean(mapping.getEXT_ID__c());
				for(IBean field: oneRecord){
					String[] valueSplit = field.getName().split("\\.", 2);
					String fieldName = "", fieldType = "";
					if(valueSplit.length>1){
						fieldType = valueSplit[0];
						fieldName = valueSplit[1];
					}else{
						fieldName = valueSplit[0];
					}
					if(fieldName.equals("SKYVVA__PARENTID") || fieldName.equals("SKYVVA__SALESFORCEID"))continue;
					else {
						bean = (!StringUtils.isEmpty(fieldType) && intff.getSource_Name__c().equals(fieldType))?
							(fieldName.equals(mapping.getTarget__c())?new IBean(fieldName, field.getValue()):null):null;
						if(bean!=null){
							if(lstBeans == null) lstBeans = new ArrayList<IBean>();
							parentField = field;
							if(isExtId) {
								extIdField = field.getValue();
								if(mapExtId.get(intff.getSource_Name__c())==null){
									mapExtId.put(intff.getSource_Name__c(), field.getName());
								}
								lstBeans.add(new IBean("EXTERNAL_FIELD_NAME", field.getName().split("\\.", 2)[1]+":text"));
							}
							lstBeans.add(bean);
							if(!StringUtils.isEmpty(field.getValue())){allFieldHasNoValue=false;}
							break;
						}
					}
				}
			}
			if(!extIdFieldValueList.contains(extIdField) || extIdField==null){ 
				if(extIdField!=null)extIdFieldValueList.add(extIdField);
				if(!allFieldHasNoValue){
					mapBeans.get(intff.getSource_Name__c()).add(reorderBean(lstBeans));
				}
			}
		}
		
		for(IChained_Interfaces__c ici : icis){
			addChildRecord(records, integrationService, ici.getChildInterfaceId__c(), mapBeans);
		}
		
	}

	private List<IBean> reorderBean(List<IBean> lstBeans) {
		List<IBean> res = new ArrayList<>();
		Map<String, IBean> m = new HashMap<>();
		lstBeans.forEach(bean->{
			m.put(bean.getName(), bean);
		});
		
		res.add(m.get("MESSAGE"));
		res.add(m.get("EXTERNAL_FIELD_NAME"));
		m.forEach((key, value)->{
			if(!(key.equals("MESSAGE") || key.equals("EXTERNAL_FIELD_NAME"))) {
				res.add(value);
			}
		});
		return res;
	}

	//#1681 Master-Detail data from Salesforce to Database
	public FlatRecordHelper(IBean[][] beans) {
		
		try{
			String dbColDelimeter = PropertySettingDao.getInstance().getValue("dbTableFieldDelimiter");
			if(StringUtils.isNotBlank(dbColDelimeter)) {
				delimiter = dbColDelimeter;
			}
		}catch(Exception e){}
						
		
		Map<String,List<List<IBean>>> mapBeans = new HashMap<String,List<List<IBean>>>();
		Map<String,Set<String>> mapCheckExtDup = new HashMap<String, Set<String>>();
		
		Map<String,List<IBean>> mapBean;
		for(int i=0 ; i<beans.length ; i++){
			
			mapBean=new HashMap<String,List<IBean>>();
			
			for(int j=2 ; j<beans[i].length ; j++){ // loop field 
				
				List<IBean> lstBeans = new ArrayList<IBean>();
				String[] valueSplit = beans[i][j].getName().split(delimiter, 2);
				
				if(valueSplit.length>=2){
					
					if(valueSplit[1].equals("SKYVVA__KEY") || valueSplit[1].equals("SKYVVA__PARENTID")){
						if(mapExtId.get(valueSplit[0])==null){
							mapExtId.put(valueSplit[0], beans[i][j].getValue());
						}
						continue;
					}
					
					if(lstOrderTable!=null){
						if(!lstOrderTable.contains(valueSplit[0])){
							lstOrderTable.add(valueSplit[0]);
						}
					}
					
					beans[i][j].setName(valueSplit[1]);
					lstBeans.add(beans[i][j]);
					
					if(mapBean.size()>0){
						List<IBean> lstBean = mapBean.get(valueSplit[0]);
						if(lstBean!=null){
							lstBean.add(beans[i][j]);
						}
						else{
							mapBean.put(valueSplit[0], lstBeans);
						}
					}
					else{
						mapBean.put(valueSplit[0], lstBeans);
					}
				}
			}
			
			List<List<IBean>> lst;
			if(mapBean.size()>0){
				for(String key: mapBean.keySet()){
					
					List<IBean> lst1 = mapBean.get(key);
					
					String ext = mapExtId.get(key).split(delimiter)[1];
					
					boolean isFound=false;
					for(int t=0 ; t<lst1.size() ; t++){
						if(lst1.get(t).getName().equals(ext)){
							if(mapCheckExtDup.size()>0 && mapCheckExtDup!=null){
								Set<String> sExt= mapCheckExtDup.get(key);
								if(sExt!=null){
									if(sExt.contains(lst1.get(t).getValue())){
										isFound=true;
										break;
									}
									else{
										Set<String> s = new HashSet<String>();
										s.add(lst1.get(t).getValue());
										mapCheckExtDup.put(key, s);
									}
								}
								else{
									Set<String> s = new HashSet<String>();
									s.add(lst1.get(t).getValue());
									mapCheckExtDup.put(key, s);
								}
							}
							else{
								Set<String> s = new HashSet<String>();
								s.add(lst1.get(t).getValue());
								mapCheckExtDup.put(key, s);
							}
						}
					}
					
					if(isFound){
						continue;
					}
					//For Message Id
					lst1.add(0, beans[i][0]);
					
					//For External Id Field
					IBean b = new IBean();
					b.setName("EXTERNAL_FIELD_NAME");
					b.setValue(mapExtId.get(key).split(delimiter)[1] + ":text");
					lst1.add(1, b);
					
					if(mapBeans.size()>0){
						List<List<IBean>> lsts = mapBeans.get(key);
						if(lsts!=null){
							lsts.add(lst1);
						}
						else{
							lst = new ArrayList<List<IBean>>();
							lst.add(lst1);
							mapBeans.put(key, lst);
						}
						
					}
					else{
						lst = new ArrayList<List<IBean>>();
						lst.add(lst1);
						mapBeans.put(key, lst);
					}
				}
			}
		}
		
		//Prepare bean for each database
		if(mapBeans.size()>0){
			for(int l=0 ; l<lstOrderTable.size() ; l++){
				List<List<IBean>> lst = mapBeans.get(lstOrderTable.get(l));
				if(lst.size()>0){
					IBean[][] b = new IBean[lst.size()][];
					for(int i=0 ; i<lst.size() ; i++){
						b[i]=new IBean[lst.get(i).size()];
						for(int j=0 ; j<lst.get(i).size();j++){
							
							b[i][j]=lst.get(i).get(j);
						}
					}
					mapB.put(lstOrderTable.get(l), b);
				}
			}
		}
		
		isMultipleObjects = lstOrderTable.size()>1;
		
	}
	

	public Map<String, String> getMapOrderTableWithOperationType() {
		return mapOrderTableWithOperationType;
	}
	
	public void setMapOrderTableWithOperationType(Map<String, String> mapOrderTableWithOperationType) {
		this.mapOrderTableWithOperationType = mapOrderTableWithOperationType;
	}
	
	public boolean isMultipleObjects() {
		return isMultipleObjects;
	}


	public void setMultipleObjects(boolean isMultipleObjects) {
		this.isMultipleObjects = isMultipleObjects;
	}


	public List<String> getLstOrderTable() {
		return lstOrderTable;
	}


	public void setLstOrderTable(List<String> lstOrderTable) {
		this.lstOrderTable = lstOrderTable;
	}


	public Map<String, String> getMapExtId() {
		return mapExtId;
	}


	public void setMapExtId(Map<String, String> mapExtId) {
		this.mapExtId = mapExtId;
	}


	public Map<String, IBean[][]> getMapB() {
		return mapB;
	}


	public void setMapB(Map<String, IBean[][]> mapB) {
		this.mapB = mapB;
	}

	
	
	
}
