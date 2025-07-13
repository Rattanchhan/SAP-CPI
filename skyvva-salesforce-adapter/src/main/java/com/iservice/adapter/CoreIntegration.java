package com.iservice.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iservice.adapter.reader.QueryResult;
import com.iservice.gui.helper.PropertyNameHelper;
import com.model.iservice.Adapter;
import com.model.iservice.Property;
import com.sforce.soap.schemas._class.IServices.IBean;

public class CoreIntegration {
   
   public static final Map<String,String> TYPE_TO_ADAPTER = new HashMap<String,String>();
   
   public IDBConnection adapter=null;
  
   static {
      TYPE_TO_ADAPTER.put("JDBC","com.iservice.adapter.JDBCAdapter");
      TYPE_TO_ADAPTER.put("CSV","com.iservice.adapter.CSVAdapter");
      TYPE_TO_ADAPTER.put("JSON","com.iservice.adapter.JsonAdapter");
      TYPE_TO_ADAPTER.put("EXCEL","com.iservice.adapter.ExcelAdapter");
      TYPE_TO_ADAPTER.put("XML","com.iservice.adapter.XmlAdapter");
      TYPE_TO_ADAPTER.put("FTP","com.iservice.adapter.FTPAdapter");
      TYPE_TO_ADAPTER.put("FILE","com.iservice.adapter.FileAdapter");
      TYPE_TO_ADAPTER.put("OTHER","com.iservice.adapter.FileAdapter");
   }
   
   public IDBConnection getAdapter(Map<String, String> mapProperties) throws Exception {
	   if(adapter==null){
		   String adapterType = "";
		   if(mapProperties!=null && mapProperties.get(PropertyNameHelper.FILE_TYPE)!=null) {
			   adapterType = mapProperties.get(PropertyNameHelper.FILE_TYPE).toUpperCase();
		   }else {
			   //20110727
			   adapterType = (mapProperties!=null && mapProperties.get(PropertyNameHelper.ADAPTER_TYPE)!=null ?  mapProperties.get(PropertyNameHelper.ADAPTER_TYPE).toUpperCase() : null);
		   }
		   if(adapterType==null) throw new RuntimeException("Cannot get adapter implementation. No adapter type was specified!");
		   String adapterClass = TYPE_TO_ADAPTER.get(adapterType.toUpperCase());
		   if(adapterClass==null) throw new RuntimeException("No implementation for this adapter type " + adapterType);
		   adapter = ConnectionFactory.getInstance().createConnection(adapterClass);
		   adapter.setConnectionInfo(mapProperties);
	   }
	   return adapter;
   }

   public static Adapter createDTOAdapterFromMap(Map<String, String> mapProps) {
	   
	   Adapter ad = new Adapter();
	   if(mapProps!=null){
		   ad.setConnType(mapProps.get(PropertyNameHelper.ADAPTER_TYPE));
		   List<Property> lstProps = new ArrayList<Property>();
		   for(String key: mapProps.keySet()){
			   Property p = new Property(key, mapProps.get(key));
			   lstProps.add(p);
		   }
		   ad.setProperty(lstProps); 
		   
	   }
	   return ad;
   }

   public static Map<String, String> map(Adapter ad) {
      Map<String, String> mapProps = new HashMap<String, String>();
      //20110727
      if(ad!=null && ad.getProperty()!=null) {
	      Iterator<Property> props = ad.getProperty().iterator();
	      while (props.hasNext()) {
	         Property pr = (Property) props.next();
	         if(pr!=null && pr.getName()!=null) {
	        	 mapProps.put(pr.getName().toLowerCase(), pr.getValue());
	         }
	      }// get a map of name to value (connect info)
      }

      return mapProps;
   }

   /**
    * this method for File Adapter only
    * @param mapAdapter
    * @param query
    * @return
    * @throws Exception
    */
   public List<List<IBean>> readSampleDataStructure(Map<String, String> mapAdapter, String query) throws Exception {
  
      if (mapAdapter == null) { // || StringUtils.isEmpty(query)
         return null;
      }   
     QueryResult result = getAdapter(mapAdapter).doTestQuery(query, null);
     if(result.isDifferentCol() && result.getResult()!=null){
    	 //re-structure when result has different column
    	 List<List<IBean>> samples = new ArrayList<List<IBean>>();
    	 List<IBean> oneRec = new ArrayList<IBean>();
    	 samples.add(oneRec);//header
    	 samples.add(oneRec);//sample data
    	 Set<String> existAdded = new HashSet<String>();
    	 for(List<IBean> rec:result.getResult()){
    		 for(IBean bean:rec){
    			 if(!existAdded.contains(bean.getName())){
    				 existAdded.add(bean.getName());
    				 oneRec.add(bean);
    			 }
    		 }
    	 }
    	 return samples;
     }
      // query from Source DB
      return result.getResult();
     
   }
   
}
