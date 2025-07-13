package com.iservice.helper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.lang3.StringUtils;

import com.iservice.gui.data.Adapter__c;
import com.iservice.gui.data.IChained_Interfaces__c;
import com.iservice.gui.data.ILogs__c;
import com.iservice.gui.data.IMapping__c;
import com.iservice.gui.data.IMessage__c;
import com.iservice.gui.data.ISFIntegrationObject;
import com.iservice.gui.data.IStructure__c;
import com.iservice.gui.data.Integration__c;
import com.iservice.gui.data.InterfaceGroup__c;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.gui.data.MessageType__c;
import com.iservice.gui.data.MsgTypeFieldEntry__c;
import com.iservice.gui.data.Property__c;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.bind.XmlObject;

public class SFIntegrationObjectUtils {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(SFIntegrationObjectUtils.class);
	
	public static ISFIntegrationObject createSFIntegrationObject(String type){
		
		ISFIntegrationObject isfObject=null;
		
		if(type!=null) {
			
			if(type.contains("Adapter__c")) {
				isfObject= new Adapter__c(type.replace("Adapter__c", "")); //package
			}
			else if(type.contains("Property__c")){
				isfObject = new Property__c(type.replace("Property__c", ""));
			}
			else if(type.contains("Integration__c")){
				isfObject = new Integration__c(type.replace("Integration__c", ""));
			}
			else if(type.contains("Interfaces__c") && !type.contains("IChained_Interfaces__c")){
				isfObject = new Interfaces__c(type.replace("Interfaces__c", ""));
			
			}else if(type.contains("IStructure__c")){
				isfObject = new IStructure__c(type.replace("IStructure__c", ""));
				
			}else if(type.contains("ILogs__c")){
				isfObject = new ILogs__c(type.replace("ILogs__c", ""));
				
			}else if(type.contains("IMessage__c")){
				isfObject = new IMessage__c(type.replace("IMessage__c", ""));
				
			}else if(type.contains("IMapping__c")){
				isfObject = new IMapping__c(type.replace("IMapping__c", ""));
				
			}else if(type.contains("IChained_Interfaces__c")){
				isfObject = new IChained_Interfaces__c(type.replace("IChained_Interfaces__c", ""));
			}
			else if(type.contains("MessageType__c")){
				isfObject = new MessageType__c(type.replace("MessageType__c", ""));
				
			}else if(type.contains("MsgTypeFieldEntry__c")){
				isfObject = new MsgTypeFieldEntry__c(type.replace("MsgTypeFieldEntry__c", ""));
	    
			}else if(type.contains("InterfaceGroup__c")){
				isfObject = new InterfaceGroup__c(type.replace("InterfaceGroup__c", ""));
	    
			}
			//else if other objects to be added later
		}

		return isfObject;
		
	}
	
	/**
	 * Create SObject from SFIntegrationObject
	 * @param obj
	 * @return
	 * @throws Exception
	 */
	public static SObject createSObject(ISFIntegrationObject obj) throws Exception {
		SObject sObject=null;
		if(obj!=null) {
			sObject = new SObject(obj.getType());
			List<String> nullFileds = new ArrayList<String>();
			for(Field f : obj.getClass().getDeclaredFields()){
				if(f.getName().equalsIgnoreCase("clsPath") || f.getName().equalsIgnoreCase("objectType") ||
					f.getName().equalsIgnoreCase("packagee") || f.getName().equalsIgnoreCase("serialVersionUID") || 
					// children and lookup
					ISFIntegrationObject.class.isAssignableFrom(f.getType()) || f.getType().getName().equalsIgnoreCase("java.util.List")) 
				{
					continue;
				}
				if(obj.isSalesForceField(f.getName())){
					String fName = f.getName();
					Object val = obj.get(f.getName());
					if(val!=null) {
						if(f.getName().equalsIgnoreCase("id") && StringUtils.isBlank((String)val))continue;
						if(fName.contains("__c")){
							fName = obj.getPackage()+f.getName();
						}
						//setFieldsToNull
						if(val.equals("")) {
							nullFileds.add(fName);
						}
						else {
							sObject.setField(fName, val);
						}
					}
				}
			}	
			String[] nullFieldsArr= new String[nullFileds.size()];
			for(int i=0;i<nullFileds.size();i++) {
				nullFieldsArr[i]=nullFileds.get(i);
			}			
			sObject.setFieldsToNull(nullFieldsArr);
		}
		return sObject;
	}
	
	/** 
	 * 
	 * @param qr
	 * @return lo : must be not null
	 * @throws Exception
	 */
	public static <C extends ISFIntegrationObject> List<C> readSObjects(QueryResult qr,Class<C> cls) throws Exception {
		
		List<C> lo = new ArrayList<C>();
		// qr.isDone() is false when 500 record.
		if(qr!=null &&  qr.getSize()>0) {
			
			SObject[] sos=qr.getRecords();
			for(SObject so: sos) {
				
				C o=readSObject(so,cls);
				if(o!=null) {
					
					Iterator<XmlObject> fields = so.getChildren();
					while(fields.hasNext()){
						XmlObject xo = fields.next();
						
						if(xo instanceof SObject){
							String lookupRelName =xo.getName().getLocalPart();
							ISFIntegrationObject lookupObject=readSObject((SObject)xo,null);
							o.put(lookupRelName, lookupObject);
						}else if(xo.getXmlType()!=null && "QueryResult".equals(xo.getXmlType().getLocalPart())) {
							List<ISFIntegrationObject> children = new ArrayList<ISFIntegrationObject>();
							Iterator<XmlObject> iter = xo.getChildren("records");
							while(iter.hasNext()){
								XmlObject rec = iter.next();
								if(rec instanceof SObject){
									children.add(readSObject((SObject)rec,null));
								}
							}
							String childrenRelName = xo.getName().getLocalPart();
							o.put(StringUtils.replace(childrenRelName,o.getPackage(),""), children);
						}
					}
					
					lo.add(o);
				}
			}
		}
		return lo;
	}
	
	/**
	 * 
	 * 
	 * @param so
	 * @return
	 * @throws Exception
	 */
	public static <C extends ISFIntegrationObject> C readSObject(SObject so,Class<C> cls) throws Exception {
				
		C o=null;
		
		if(so!=null) {
			if(cls==null){
				o =(C) SFIntegrationObjectUtils.createSFIntegrationObject(so.getType());
			}else{
				Constructor<C> cons=	cls.getConstructor(String.class);
				o = cons.newInstance(StringUtils.replace(so.getType(),cls.getSimpleName(), ""));
			}
			o.setId(so.getId());
			
			Iterator<XmlObject> sfields=so.getChildren();
			while(sfields.hasNext()){
				XmlObject me = sfields.next();
				if(me.getValue()!=null){
					try {
						String fieldname =StringUtils.replace( me.getName().getLocalPart(), o.getPackage(), "");
						o.put(fieldname, me.getValue());
					}catch(Exception ex) {
						LOGGER.warn("SFIntegrationObjectUtils.readSObject() at element: " + me.getName() + "> " + ex);
					}
				}
			}
		}
		return o;
		
	}
	
	public static void main(String[] args) {
		
		try {
			ISFIntegrationObject adapter = new IMapping__c("skyvvasolutions__");
			adapter.put("name", "AdapterTest1");
			adapter.put("nype__c", "ODBC");
			adapter.put("EXT_ID__c", "test");
			SObject sadp = SFIntegrationObjectUtils.createSObject(adapter);
			System.out.println(">>>>>> "+sadp);
			ISFIntegrationObject readapter=SFIntegrationObjectUtils.readSObject(sadp,IMapping__c.class);
			System.out.println(">>>>>> "+readapter);
			System.out.println(adapter.getClass().isAssignableFrom(ISFIntegrationObject.class));
			System.out.println(ISFIntegrationObject.class.isAssignableFrom(adapter.getClass()));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
