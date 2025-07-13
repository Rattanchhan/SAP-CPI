package com.iservice.gui.data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("serial")
public abstract class SFIntegrationObject implements Serializable, ISFIntegrationObject {
	
	//protected String clsPath;
	protected String objectType;
	protected String packagee;
	protected Set<String> fieldGetFromSF=new HashSet<String>();
	
	//public abstract void initCls();
	public SFIntegrationObject(String pkage){
		setPackage(pkage);
		setType(pkage+this.getClass().getSimpleName()); //ex. Skyvva__Adapter__c
	}
	
	protected Object getH2Field(Object fieldVal) {
		if(fieldVal==null) return null;
		else return fieldVal;
	}
	
	public SFIntegrationObject() {
		
	}
	@Override
	public String getType() {
		return objectType;
	}
	@Override
	public void setType(String objectType) {
		this.objectType=objectType;
	}
	@Override
	public String getPackage() {
		return this.packagee;
	}
	@Override
	public void setPackage(String packagee) {
		this.packagee=packagee;
	}
	
	//Custom Method
	@Override
	public Object get(String fieldName) throws Exception {
		
		//Class<?> cls = null;
		try {
//			cls =getClass();// Class.forName(clsPath);
//			String field = toLUpperCase(fieldName);
//			
//			return cls.getMethod("get"+field, (Class<?>[])null).invoke(this, (Object[])null);
			String field =ensueFieldName(fieldName);
			Object val = BeanUtilsBean.getInstance().getPropertyUtils().getProperty(this, field);
			if(val==null){
				//try with upper case when the value is null
				field = toLUpperCase(fieldName);
				try{
					val = BeanUtilsBean.getInstance().getPropertyUtils().getProperty(this, field);
				}catch(NoSuchMethodException e){
					//nothing todo
				}
			}
			return val;
			
		}
//		catch(NoSuchMethodException ex ) {
//			
//			throw new IServices.IServiceException("No such field:" + fieldName  + " for object:" + cls.getSimpleName());
//		}
		catch(Exception ex) {
			throw ex;
		}
	}
	//Custom Method
	@Override
	public void put(String fieldName, Object value) throws Exception {
		try {
			String field = ensueFieldName(fieldName);
			fieldGetFromSF.add(StringUtils.lowerCase(field));
			BeanUtils.setProperty(this, field, value);
		}catch(Exception ex) {
			throw ex;
		}
	}
	
	
	@Override
	public boolean isSalesForceField(String field){
		//empty mean that all field are validate
		return fieldGetFromSF.isEmpty()||fieldGetFromSF.contains(StringUtils.lowerCase(field));
	}
	public Set<String> getAllfields() {
		return new HashSet<String>(fieldGetFromSF);
	}
	protected String ensueFieldName(String fieldName){
		return toLowerCase(fieldName);
	}
	
	//Custom Method
	//method convert first character of string to UPPERCASE
	//Ex: name -> Name
	public static String toLUpperCase(String st){		
		if(st==null) {
			return null;
		}		
		return st.substring(0, 1).toUpperCase()+ st.substring(1);		
	}
	protected  String toLowerCase(String st){		
		if(st==null) {
			return null;
		}		
		return st.substring(0, 1).toLowerCase()+ st.substring(1);		
	}
	
	

}
