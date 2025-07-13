package com.iservice.adapter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.iservice.adapter.batch.BatchExecutorResponseItem;
import com.iservice.adapter.batch.IBatchExecutor;
import com.iservice.adapter.reader.IRecordReader;
import com.iservice.adapter.reader.XmlFileReader;
import com.iservice.adapter.reader.XmlFileReaderV3;
import com.iservice.gui.data.IChained_Interfaces__c;
import com.iservice.gui.data.IMapping__c;
import com.iservice.gui.data.ISFIntegrationObject;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.gui.helper.Helper;
import com.iservice.gui.helper.PropertyNameHelper;
import com.iservice.sforce.SFIntegrationService;
import com.iservice.task.BulkIntegrationV3Event;
import com.iservice.task.MapIntegrationInfo;
import com.iservice.xmlparser.DynamicXmlReader;
import com.sforce.soap.schemas._class.IServices.IBean;

public class XmlAdapter extends FileMapAdapter {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(XmlAdapter.class);
	
	private String filter=EMPTY;
	
	public static final String soapNS = "http://schemas.xmlsoap.org/soap/envelope/"; 
	public static final String xsi = "http://www.w3.org/2001/XMLSchema-instance"; 
	public static final String xsd = "http://www.w3.org/2001/XMLSchema";            
	public static final String ns0 = "http://soap.sforce.com/schemas/class/skyvvasolutions/IServices";

	public static final String DEFAULT_PREFIX = "ns0";
	public static final String XML_PREFIX = "xml_prefix";
	public static final String XML_NAMESPACE = "xml_namespace";
	public static final String SUPPRESS_NULL_VALUE = "SuppressNullVaule";
	public static final String CDATA = "cdata";
	public static final String SecondLevelXMLTag = "SecondLevelXMLTag";
	
	public XmlAdapter() throws Exception {
		super();                
	}
	
	@Override
	void validateProperties() throws Exception { 		
	}

	
	public List<List<String>> getColumns(String fileName, List<? extends ISFIntegrationObject> listHeader) throws Exception {
		throw new UnsupportedOperationException("Not Support.");
	}

	public List<List<String>> getRelationship(String objectName) throws Exception {
		return null;
	}

	
	public List<List<IBean>> query(String value, List<ISFIntegrationObject> listHeader) throws Exception {
		throw new UnsupportedOperationException("Not Support.");
	}
	
	public List<List<IBean>> queryInputStream(InputStream is, List<ISFIntegrationObject> listHeader) throws Exception{
		throw new UnsupportedOperationException("Not Support.");
	}
	
	// update Xml
	public List<BatchExecutorResponseItem> update(IBean[][] beans, String interfacetype, SFIntegrationService integrationService, Interfaces__c intff) throws Exception {
		
		if (beans == null || beans.length < 0)
			throw new Exception("XMLAdapter> update> No records available for an update or create process in the source system.");
		
		if(folder==null || folder.equals("")  || targetFileName==null || targetFileName.trim().equals("") ) {
			setConnectionInfo(map());
		}
		
		List<BatchExecutorResponseItem> lstMsg = new ArrayList<BatchExecutorResponseItem>();
		String msgStatus = "Completed", msg = "Creation of a new record.";
		
		if (!folder.trim().equals("") && !targetFileName.equals("")) {
			try {
				
				updateToFTPFile(beans, null, folder, targetFileName, null, null, null);
				
				msgStatus = "Completed";
				msg = "Creation of a new record.";
			} 
			catch (IOException ex) {
				msgStatus = "Failed";
				msg = ex.getLocalizedMessage();
			}
		}
		
		for (int i = 0; i < beans.length; i++) {
			BatchExecutorResponseItem bMsg = new BatchExecutorResponseItem(i, beans[i][0].getValue(), msgStatus, msg);
			if(msgStatus.equals("Completed") && intff!=null && intff.getResponseInterface__c()!=null) bMsg.setResponseRecords(beans[i]);
			lstMsg.add(bMsg);
		}
		
		return lstMsg;
	}
	
	public List<File> updateToFTPFile(IBean[][] beans, List<List<IBean>> lstRecords, String folder, String targetFileName, 
			SFIntegrationService integrationService, List<BatchExecutorResponseItem> lsMsg, Interfaces__c intf) throws Exception {
		List<File> lsFile = new ArrayList<File>();
		// write file attachment if has.
		lsFile = new FileAdapter().updateToFTPFile(beans, null, folder, targetFileName, null, null, null);
		if(lsMsg!=null){
			if(getMapProps().get(PropertyNameHelper.XML_TYPE).equalsIgnoreCase(PropertyNameHelper.TYPE_ATTRIBUTE)) {
				for(File file : lsFile) {
					if (file != null && file.exists()
							&& mergeFile.equalsIgnoreCase("false")) {
						file.delete();
					}
	 			}
				throw new Exception("XMLAdapter> updateChainData> Not support with Xml type attribute.");
			}
			createIChainMessage(integrationService, lsMsg, intf, beans, lsFile);
		}
		else {
			List<List<IBean>> newRecords = convertArrayToList2(beans);
			
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
			currentTime = formatter.format(Calendar.getInstance().getTime());
			String newFilename = (folder + Helper.getFileSeparator() + targetFileName + currentTime + ".xml").trim();
			boolean isAppendFile = "true".equals(mergeFile);
			String currentFilename = "";
			if(isAppendFile) currentFilename = (folder + Helper.getFileSeparator() + filename + (filename.endsWith(".xml")?"":".xml")).trim();
			else currentFilename = (folder + Helper.getFileSeparator() + targetFileName + ".csv").trim();
			File file = new File(currentFilename);
			Long currentFileSize = file.exists() == false ? 0 :file.length()/1048576;
			boolean isExists = file.exists();
			//return name current file if merger file was selected and current file size is more than defined File Size
			if(isAppendFile && currentFileSize >= Long.valueOf(fileSize)){
				file.renameTo(new File(newFilename));
				isRename = true; //this variable is used with FTP file
			}
			if(isAppendFile && isExists) newRecords = appendXmlFile(newRecords, currentFilename);

			Map<String, String> adapterProperties = this.getMapProps();
			String suppressNullValue = adapterProperties.get(XmlAdapter.SUPPRESS_NULL_VALUE);
			String xml_namespace = adapterProperties.get(XmlAdapter.XML_NAMESPACE);
			String xml_prefix = StringUtils.isEmpty(adapterProperties.get(XmlAdapter.XML_PREFIX))?XmlAdapter.DEFAULT_PREFIX:adapterProperties.get(XmlAdapter.XML_PREFIX);		
			String SecondLevelXMLTag = adapterProperties.get(XmlAdapter.SecondLevelXMLTag);
			
			String cdata = adapterProperties.get(XmlAdapter.CDATA);
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			
			List<BatchExecutorResponseItem> lstMsg = new ArrayList<BatchExecutorResponseItem>();
			String msgStatus = "Completed", msg = "Creation of a new record.";
			Element root = null;
			if(adapterProperties.get(PropertyNameHelper.XML_TYPE).equalsIgnoreCase(PropertyNameHelper.TYPE_ELEMENT)) {
				List<IMapping__c> mpps = integrationService.getIMapping__c(intf.getId());
				IMapping__c fmap = mpps.get(0);
				String mappingTarget = fmap.getTarget__c();//(String) mpps.get(0).get("Target__c");
				String mappingTargetPath =fmap.getTarget_Path__c();// (String) mpps.get(0).get("Target_Path__c");
				if(mappingTargetPath== null)throw new Exception("XMLAdapter> update2>mapping:"+fmap.getId()+"> mappingTargetPath is empty");
				mappingTargetPath = mappingTargetPath.replace("(text)", "");
				String mappingTargetObject = fmap.getTarget_Object__c();//(String) mpps.get(0).get("Target_Object__c");
				String[] arrayMappingTargetPath = mappingTargetPath.split("/");
				
				if(StringUtils.isEmpty(xml_namespace)) {
					xml_prefix = "";
					root = doc.createElement(intf.getName__c().replaceAll("\\s+", "_"));
				}else {
					root = doc.createElement(xml_prefix+ ":schema");
					root.setAttribute("xmlns:"+xml_prefix, xml_namespace);
					xml_prefix = xml_prefix+":";
				}
				// interfaceName/DEBMAS06/IDOC/E1KNA1M/KUNNR(text)
				Element childElement = appendChild(arrayMappingTargetPath, 1, mappingTargetObject, mappingTarget, doc, xml_prefix);
				if(childElement!=null) root.appendChild(childElement);
				
				// createXmlElement(records, targetFileName);
				for (int i = 0; i < newRecords.size(); i++) {
					
					Element recordElement = null;
					if(StringUtils.isEmpty(SecondLevelXMLTag)) {
						recordElement = doc.createElement(xml_prefix+mappingTargetObject);	
					}else {
						recordElement = doc.createElement(xml_prefix+SecondLevelXMLTag);	
					}
					
					for (int j = 0; j < newRecords.get(i).size(); j++) {
						if(!(!StringUtils.isEmpty(suppressNullValue) && suppressNullValue.equalsIgnoreCase("true") && StringUtils.isEmpty(newRecords.get(i).get(j).getValue()))) {
							Element fieldElement = doc.createElement(xml_prefix+newRecords.get(i).get(j).getName());

							String value = (newRecords.get(i).get(j).getValue()==null?"":new String(newRecords.get(i).get(j).getValue().getBytes("UTF-8"),"UTF-8"));
							
							value = (!StringUtils.isEmpty(cdata) && cdata.equalsIgnoreCase("true"))?("<![CDATA["+value+"]]>"):value;
							
							fieldElement.appendChild(doc.createTextNode(value));
							recordElement.appendChild(fieldElement);
						}
						
					}
					if(cacheYoungestElement!=null) {
						cacheYoungestElement.appendChild(recordElement);
					}else {
						root.appendChild(recordElement);
					}
				}
			}
			else {
				root = doc.createElement("result");
				// put the queried data from SF to the file
				for (int i = 0; i < newRecords.size(); i++) {
					Element recordElement = doc.createElement("row");
					for (int j = 0; j < newRecords.get(i).size(); j++) {
						
						if(!(!StringUtils.isEmpty(suppressNullValue) && suppressNullValue.equalsIgnoreCase("true") && StringUtils.isEmpty(newRecords.get(i).get(j).getValue()))) {
							Element fieldElement = doc.createElement("field");
	
							String value = (newRecords.get(i).get(j).getValue()==null?"":new String(newRecords.get(i).get(j).getValue().getBytes("UTF-8"),"UTF-8"));
							
							value = (!StringUtils.isEmpty(cdata) && cdata.equalsIgnoreCase("true"))?("<![CDATA["+value+"]]>"):value;
							
							fieldElement.setAttribute("name", newRecords.get(i).get(j).getName());
							fieldElement.appendChild(doc.createTextNode(value));
							recordElement.appendChild(fieldElement);
						}
					}
					root.appendChild(recordElement);
				}
			}
			doc.appendChild(root);
			msgStatus = "Completed";
			msg = "Creation of a new record.";

			file = new File(isAppendFile==true?currentFilename:newFilename);
			TransformerFactory tfactory = TransformerFactory.newInstance();
			Transformer t = tfactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(file);
			t.transform(source, result);
			lsFile.add(file);
		}
		return lsFile;
		/*
		//20111004 v1.12 new SF-XML (FTP) - Veasna
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		
		String filename = (targetFileName + formatter.format(Calendar.getInstance().getTime()) + ".xml").trim();
		File file = new File(folder + Helper.getFileSeparator() + filename);
		
		OutputStream out1 = new FileOutputStream(file);
		OutputStreamWriter out = new OutputStreamWriter(out1,"UTF-8");
		
		//23012017 check adapter properties xml type = attribute or element
		List<Property> properties = getAdapter().getProperty();
		
		//Create Xml content
		String content="";
		for(Property p:properties){
			if(p.getName().equals(PropertyNameHelper.XML_TYPE)){
				// SOKDET
				content = (p.getValue().equals(PropertyNameHelper.TYPE_ELEMENT))?createXmlElement(beans, targetFileName):createXmlAttribute(beans);
				break;
			}
		}
		//write content
		out.write(content);
		out.close();
		*/
	}
	
	public void createIChainMessage(SFIntegrationService integrationService, List<BatchExecutorResponseItem> lsMsg, Interfaces__c intf, IBean[][] beans,
			List<File> lsFile) throws Exception {
		try {
			List<IChained_Interfaces__c> icis = new ArrayList<>();
			List<IChained_Interfaces__c> sfobjects = cacheIChainInterfaces.get(intf.getId());
			if(sfobjects==null){
				sfobjects = integrationService.getIChained_Interfaces__c(intf.getId());
				if(sfobjects==null)sfobjects = new ArrayList<>();
				cacheIChainInterfaces.put(intf.getId(), sfobjects);
			}
			for(ISFIntegrationObject sfobject: sfobjects){
				icis.add((IChained_Interfaces__c)sfobject);
			}
			icis.sort((o1,o2)-> {return o1.getSequence__c().compareToIgnoreCase(o2.getSequence__c());});
			
			List<IMapping__c> mpps = cacheMapping.get(intf.getId());
			if(mpps==null){
				mpps=integrationService.getIMapping__c(intf.getId());
				cacheMapping.put(intf.getId(), mpps);
			}
			boolean mappingHasNoExtID = true;
			
			for(ISFIntegrationObject mpp : mpps){
				IMapping__c mapping = (IMapping__c)mpp;
				if(mapping.isExternalId()){//StringUtils.isEmpty(mapping.getEXT_ID__c())?false:Boolean.parseBoolean(mapping.getEXT_ID__c())) {
					mappingHasNoExtID = false;
					break;
				}
			}
			IMapping__c fmap = mpps.get(0);
			String mappingTarget =fmap.getTarget__c(); //(String) mpps.get(0).get("Target__c");
			String mappingTargetPath = fmap.getTarget_Path__c();//(String) mpps.get(0).get("Target_Path__c");
			if(mappingTargetPath== null)throw new Exception("XMLAdapter> updateChainData>mapping:"+fmap.getId()+"> mappingTargetPath is empty");
			mappingTargetPath = mappingTargetPath.replace("(text)", "");
			String mappingTargetObject =fmap.getTarget_Object__c();// (String) mpps.get(0).get("Target_Object__c");
			String[] arrayMappingTargetPath = mappingTargetPath.split("/");
			
			Map<String, String> adapterProperties = this.getMapProps();
			String xml_namespace = adapterProperties.get(XmlAdapter.XML_NAMESPACE);
			String suppressNullValue = adapterProperties.get(XmlAdapter.SUPPRESS_NULL_VALUE);
			String xml_prefix = StringUtils.isEmpty(adapterProperties.get(XmlAdapter.XML_PREFIX))?XmlAdapter.DEFAULT_PREFIX:adapterProperties.get(XmlAdapter.XML_PREFIX);		
			String interfaceSourceName = intf.getSource_Name__c();
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			Element root = null;
			if(StringUtils.isEmpty(xml_namespace)) {
				xml_prefix = "";
				root = doc.createElement(intf.getName__c().replaceAll("\\s+", "_"));
			}else {
				root = doc.createElement(xml_prefix+ ":schema");
				root.setAttribute("xmlns:"+xml_prefix, xml_namespace);
				xml_prefix = xml_prefix+":";
			}
			doc.appendChild(root);
			//     interfaceName/DEBMAS06/IDOC/E1KNA1M/KUNNR(text)
			Element childElement = appendChild(arrayMappingTargetPath, 1, mappingTargetObject, mappingTarget, doc, xml_prefix);
			if(childElement!=null)root.appendChild(childElement);
			
			
			List<String> extIdFieldValueList = new ArrayList<>();
			
			List<List<IBean>> recordList = convertArrayToList(beans);
			for(List<IBean> oneRecord: recordList){
				Element recordElement = null;
				IBean parentField = null;
				String extIdField = null;
				for(ISFIntegrationObject mpp : mpps){
					IMapping__c mapping = (IMapping__c)mpp;
					boolean ext_id = mapping.isExternalId();//StringUtils.isEmpty(mapping.getEXT_ID__c())?false:Boolean.parseBoolean(mapping.getEXT_ID__c());
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
						
						Element fieldElement = (!StringUtils.isEmpty(fieldType) && interfaceSourceName.equals(fieldType))?
														(fieldName.equals(mapping.getTarget__c())?doc.createElement(xml_prefix+fieldName):null):null;
						if(fieldElement!=null){
							if(recordElement==null)recordElement = doc.createElement(xml_prefix+mappingTargetObject);
							
							if(!(!StringUtils.isEmpty(suppressNullValue) && suppressNullValue.equalsIgnoreCase("true") && StringUtils.isEmpty(field.getValue()))) {

								fieldElement.appendChild(doc.createTextNode(field.getValue()));
								recordElement.appendChild(fieldElement);
							}
							
							if(ext_id) {
								extIdField = field.getValue(); // this is external id
								parentField = field;
							}
							if(mappingHasNoExtID)parentField = field;
							break;
						}
					}
				}
				if(mappingHasNoExtID || (!extIdFieldValueList.contains(extIdField) || extIdField==null)){ //if external id, create recordElement only once
					if(extIdField!=null)extIdFieldValueList.add(extIdField);
					if(recordElement!=null){
						String msgStatus = "Completed", msg = "";
						for(IChained_Interfaces__c ici : icis){
							XMLChildNode n = addChildNode(recordElement, recordList, ici.getChildInterfaceId__c(), doc, integrationService, parentField);
							recordElement = n.getNode();
							String tmpMsgSplit[] = n.getMessage().split(" ");
							String msgSplit[] = msg.split(" ");	
							if((tmpMsgSplit.length>2 && msgSplit.length>2) && msg.contains(tmpMsgSplit[tmpMsgSplit.length-1])){ //msg = Creation of 2 Contact. Creation of 1 Case.    
									         																	//n.message = Creation of 2 Case.
								try{
									msgSplit[msgSplit.length-2] = ""+ (Integer.parseInt(tmpMsgSplit[tmpMsgSplit.length-2]) + Integer.parseInt(msgSplit[msgSplit.length-2])); // msg -> 1 case //n.message -> 2 case
									msg="";
									for(String m: msgSplit)msg+= m+" ";
								}catch(Exception e){
									msg += n.getMessage();
									e.printStackTrace();
								}
								
							}else{
								msg += n.getMessage();
							}
						}
						msg =  "Creation of an " + interfaceSourceName + ". " + msg;
						lsMsg.add(new BatchExecutorResponseItem(lsMsg.size(), oneRecord.get(0).getValue(), msgStatus, msg));
						if(cacheYoungestElement!=null) {
							cacheYoungestElement.appendChild(recordElement);
						}else {
							root.appendChild(recordElement);
						}
					}
				}
			}
			if(lsFile!=null) {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
				String filename = (targetFileName + formatter.format(Calendar.getInstance().getTime()) + ".xml").trim();
				File file = new File(folder + Helper.getFileSeparator() + filename);
				
				TransformerFactory tfactory = TransformerFactory.newInstance();
				Transformer t = tfactory.newTransformer();
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(file);
				t.transform(source, result);
				lsFile.add(file);
			}
		} 
		catch (IOException ex) {
			LOGGER.error("> XML adapter: updateChainData()> ERROR> " + ex);
		}
	}
	
	//Create Xml content with attribute
	private String createXmlAttribute(IBean[][] beans) throws Exception{
		
		String content="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+"<result>\n";
		// put the queried data from SF to the file
		for (int i = 0; i < beans.length; i++) {
			content+="	<row>\n";
			String childnode="";
			for (int j = 2; j < beans[i].length; j++) {
				childnode+="		<field name=\""+beans[i][j].getName()+"\"><![CDATA["
								+(beans[i][j].getValue()==null?"":new String(beans[i][j].getValue().getBytes("UTF-8"),"UTF-8"))
								+"]]></field>\n";
			}
		content+=childnode;
		content+="	</row>\n";
		}
	
		content+="</result>";
		return content;
	}
	
	//Create Xml content with element
	private String createXmlElement(IBean[][] beans, String targetFileName) throws Exception{
		
		String content="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+"<result>\n";
		// put the queried data from SF to the file
		for (int i = 0; i < beans.length; i++) {
			content+="	<"+targetFileName+">\n";
			String childnode="";
			for (int j = 2; j < beans[i].length; j++) {
				childnode+="		<"+beans[i][j].getName()+"><![CDATA["
								+(beans[i][j].getValue()==null?"":new String(beans[i][j].getValue().getBytes("UTF-8"),"UTF-8"))
								+"]]></"+beans[i][j].getName()+">\n";
			}
		content+=childnode;
		content+="	</"+targetFileName+">\n";
		}
	
		content+="</result>";
		return content;
	}
	
	// 28-09-2017 append xml file
	private List<List<IBean>> appendXmlFile(List<List<IBean>> newRecords, String currentFilename) throws Exception{
		List<List<IBean>> oldRecords = new ArrayList<>();
		InputStream input = null;
		try {
			input = new FileInputStream(currentFilename);
			oldRecords = new DynamicXmlReader(isAddPrefix).read(input);
			//FolderUtils.deleteFile(currentFilename);
		}catch(Throwable ex) {
			ex = FileMapAdapter.fileNotFound(ex, this);
			throw new Exception(ex);
		}finally {
			if(input!=null) {
				input.close();
				input=null;
			}
		}
		oldRecords.addAll(newRecords);
		return oldRecords;
	}
	
	@Override
	public List<BatchExecutorResponseItem> update2(SFIntegrationService integrationService, Interfaces__c intff, IBean[][] records) throws Exception {

		if (records == null || records.length < 0)
			throw new Exception("XMLAdapter> update2> No records available for an update or create process in the source system.");
		
		if(folder==null || folder.equals("")  || targetFileName==null || targetFileName.trim().equals("") ) {
			setConnectionInfo(map());
		}
		new FileAdapter().updateToFTPFile(records, null, folder, targetFileName, null, null, null);
		List<List<IBean>> newRecords = convertArrayToList2(records);
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		currentTime = formatter.format(Calendar.getInstance().getTime());
		String newFilename = (folder + Helper.getFileSeparator() + targetFileName + currentTime + ".xml").trim();
		boolean isAppendFile = "true".equals(mergeFile);
		String currentFilename = "";
		if(isAppendFile) currentFilename = (folder + Helper.getFileSeparator() + filename + (filename.endsWith(".xml")?"":".xml")).trim();
		else currentFilename = (folder + Helper.getFileSeparator() + targetFileName + ".csv").trim();
		File file = new File(currentFilename);
		Long currentFileSize = file.exists() == false ? 0 :file.length()/1048576;
		boolean isExists = file.exists();
		//return name current file if merger file was selected and current file size is more than defined File Size
		if(isAppendFile && currentFileSize >= Long.valueOf(fileSize)){
			file.renameTo(new File(newFilename));
			isRename = true; //this variable is used with FTP file
		}
		if(isAppendFile && isExists) newRecords = appendXmlFile(newRecords, currentFilename);
		
		Map<String, String> adapterProperties = this.getMapProps();
		String xml_namespace = adapterProperties.get(XmlAdapter.XML_NAMESPACE);
		String suppressNullValue = adapterProperties.get(XmlAdapter.SUPPRESS_NULL_VALUE);
		String xml_prefix = StringUtils.isEmpty(adapterProperties.get(XmlAdapter.XML_PREFIX))?XmlAdapter.DEFAULT_PREFIX:adapterProperties.get(XmlAdapter.XML_PREFIX);		
		String SecondLevelXMLTag = adapterProperties.get(XmlAdapter.SecondLevelXMLTag);
		
		String cdata = adapterProperties.get(XmlAdapter.CDATA);
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.newDocument();
		
		List<BatchExecutorResponseItem> lstMsg = new ArrayList<BatchExecutorResponseItem>();
		String msgStatus = "Completed", msg = "Creation of a new record.";
		Element root = null;
		if (!folder.trim().equals("") && !targetFileName.equals("")) {
			try {
				if(adapterProperties.get(PropertyNameHelper.XML_TYPE).equalsIgnoreCase(PropertyNameHelper.TYPE_ELEMENT)) {
					
					List<IMapping__c> mpps = integrationService.getIMapping__c(intff.getId());
					IMapping__c fmap = mpps.get(0);
					String mappingTarget = fmap.getTarget__c();//(String) mpps.get(0).get("Target__c");
					String mappingTargetPath =fmap.getTarget_Path__c();// (String) mpps.get(0).get("Target_Path__c");
					if(mappingTargetPath== null)throw new Exception("XMLAdapter> update2>mapping:"+fmap.getId()+"> mappingTargetPath is empty");
					mappingTargetPath = mappingTargetPath.replace("(text)", "");
					String mappingTargetObject = fmap.getTarget_Object__c();//(String) mpps.get(0).get("Target_Object__c");
					String[] arrayMappingTargetPath = mappingTargetPath.split("/");
					
					if(StringUtils.isEmpty(xml_namespace)) {
						xml_prefix = "";
						root = doc.createElement(intff.getName__c().replaceAll("\\s+", "_"));
					}else {
						root = doc.createElement(xml_prefix+ ":schema");
						root.setAttribute("xmlns:"+xml_prefix, xml_namespace);
						xml_prefix = xml_prefix+":";
					}
					// interfaceName/DEBMAS06/IDOC/E1KNA1M/KUNNR(text)
					Element childElement = appendChild(arrayMappingTargetPath, 1, mappingTargetObject, mappingTarget, doc, xml_prefix);
					if(childElement!=null) root.appendChild(childElement);
					
					// createXmlElement(records, targetFileName);
					for (int i = 0; i < newRecords.size(); i++) {
						
						Element recordElement = null;
						if(StringUtils.isEmpty(SecondLevelXMLTag)) {
							recordElement = doc.createElement(xml_prefix+mappingTargetObject);	
						}else {
							recordElement = doc.createElement(xml_prefix+SecondLevelXMLTag);	
						}
						
						for (int j = 0; j < newRecords.get(i).size(); j++) {
							if(!(!StringUtils.isEmpty(suppressNullValue) && suppressNullValue.equalsIgnoreCase("true") && StringUtils.isEmpty(newRecords.get(i).get(j).getValue()))) {
								Element fieldElement = doc.createElement(xml_prefix+newRecords.get(i).get(j).getName());

								String value = (newRecords.get(i).get(j).getValue()==null?"":new String(newRecords.get(i).get(j).getValue().getBytes("UTF-8"),"UTF-8"));
								
								value = (!StringUtils.isEmpty(cdata) && cdata.equalsIgnoreCase("true"))?("<![CDATA["+value+"]]>"):value;
								
								fieldElement.appendChild(doc.createTextNode(value));
								recordElement.appendChild(fieldElement);
							}
							
						}
						if(cacheYoungestElement!=null) {
							cacheYoungestElement.appendChild(recordElement);
						}else {
							root.appendChild(recordElement);
						}
					}
				}else {
					root = doc.createElement("result");
					// put the queried data from SF to the file
					for (int i = 0; i < newRecords.size(); i++) {
						Element recordElement = doc.createElement("row");
						for (int j = 0; j < newRecords.get(i).size(); j++) {
							
							if(!(!StringUtils.isEmpty(suppressNullValue) && suppressNullValue.equalsIgnoreCase("true") && StringUtils.isEmpty(newRecords.get(i).get(j).getValue()))) {
								Element fieldElement = doc.createElement("field");

								String value = (newRecords.get(i).get(j).getValue()==null?"":new String(newRecords.get(i).get(j).getValue().getBytes("UTF-8"),"UTF-8"));
								
								value = (!StringUtils.isEmpty(cdata) && cdata.equalsIgnoreCase("true"))?("<![CDATA["+value+"]]>"):value;
								
								fieldElement.setAttribute("name", newRecords.get(i).get(j).getName());
								fieldElement.appendChild(doc.createTextNode(value));
								recordElement.appendChild(fieldElement);
							}
						}
						root.appendChild(recordElement);
					}
				}
				doc.appendChild(root);
				msgStatus = "Completed";
				msg = "Creation of a new record.";
	
				file = new File(isAppendFile==true?currentFilename:newFilename);
				TransformerFactory tfactory = TransformerFactory.newInstance();
				Transformer t = tfactory.newTransformer();
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(file);
				t.transform(source, result);
			} 
			catch (IOException ex) {
				LOGGER.error("> XML adapter: update2()> ERROR> " + ex);
				msgStatus = "Failed";
				msg = ex.getLocalizedMessage();
			}
		}
		
		for (int i = 0; i < records.length; i++) {
			BatchExecutorResponseItem bMsg = new BatchExecutorResponseItem(i, records[i][0].getValue(), msgStatus, msg);
			if(msgStatus.equals("Completed") && intff!=null && intff.getResponseInterface__c()!=null) bMsg.setResponseRecords(records[i]);
			lstMsg.add(bMsg);
		}
		
		return lstMsg;
	}
	
	public int update(String expression) throws Exception {
		return 0;
	}	

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public void setExternalIdField(String extIdField) {}
	public IBatchExecutor getBatchExecutor() throws Exception {
		return null;
	}
	
	//--Phanith--8/27/2019--Ticket#5897//
	@SuppressWarnings("unchecked")
	public List<BatchExecutorResponseItem> updateV3(SFIntegrationService integrationService, JSONArray records, Interfaces__c intff) throws Exception {
		if (records == null || records.isEmpty())
			throw new Exception("XMLAdapter> updateHierarchicalData> No records available for an update or create process in the source system.");
		
		if(getMapProps().get(PropertyNameHelper.XML_TYPE).equalsIgnoreCase(PropertyNameHelper.TYPE_ATTRIBUTE))
			throw new Exception("XMLAdapter> updateHierarchicalData> Not support with Xml type attribute.");
		
		if(folder==null || folder.equals("")  || targetFileName==null || targetFileName.trim().equals("") ) {
			setConnectionInfo(map());
		}
		List<BatchExecutorResponseItem> lstMsg = new ArrayList<BatchExecutorResponseItem>();
		if (!folder.trim().equals("") && !targetFileName.equals("")) {
			Map<String, String> adapterProperties = this.getMapProps();
			String xml_namespace = adapterProperties.get(XmlAdapter.XML_NAMESPACE);
			String xml_prefix = StringUtils.isEmpty(adapterProperties.get(XmlAdapter.XML_PREFIX))?XmlAdapter.DEFAULT_PREFIX:adapterProperties.get(XmlAdapter.XML_PREFIX);				
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			Element root = null;
			if(StringUtils.isEmpty(xml_namespace)) {
				xml_prefix = "";
				root = doc.createElement(intff.getName__c().replaceAll("\\s+", "_"));
			}else {
				root = doc.createElement(xml_prefix+ ":schema");
				root.setAttribute("xmlns:"+xml_prefix, xml_namespace);
				xml_prefix = xml_prefix+":";
			}
			doc.appendChild(root);
			Element rootElement = root;
			//here we read each object of hierarchical data and looking for record and message to write the xml file and create message
			records.forEach(row -> {
				try {
					readJsonObject((JSONObject) row, doc, rootElement, null, lstMsg); //here we put "doc" as parameter cuz every node must be in the same document.
				} catch (Exception e) {
					e.printStackTrace();
				}
			} );
			try {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
				String filename = (targetFileName + formatter.format(Calendar.getInstance().getTime()) + ".xml").trim();
				File file = new File(folder + Helper.getFileSeparator() + filename);
				
				TransformerFactory tfactory = TransformerFactory.newInstance();
				Transformer t = tfactory.newTransformer();
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(file);
				t.transform(source, result);
			}catch(Exception e) {
				for(int i=0; i<lstMsg.size(); i++) {
					lstMsg.get(i).setStatus("Failed");
					lstMsg.get(i).setMessage(e.getLocalizedMessage());
				}
			}
		}
		return lstMsg;
	}
	
	@SuppressWarnings({ "unchecked" })
	private void readJsonObject(JSONObject oneRecord, Document doc, Element parentElement, Element childElement, 
			List<BatchExecutorResponseItem> lstMsg ) throws Exception {
		//check for ObjectType (Ex: Account, Contact) it is message Type. and append it with it's parent
		if(oneRecord.get("objectType")!=null) {
			childElement = doc.createElement(oneRecord.get("objectType").toString());
			parentElement.appendChild(childElement);
		}
		//check for records
		if(oneRecord.get("record")!=null && oneRecord.get("record") instanceof JSONObject) {
			JSONObject record = (JSONObject) oneRecord.get("record");
			List<String> allFields = jSonKeyToStringList(record.keySet());
			for(String field: allFields) {
				Element oneFieldElement = null;
				//check for message Id and create message for upsert to SF using the id
				if(field.equalsIgnoreCase("MESSAGE_ID"))
					lstMsg.add(new BatchExecutorResponseItem(null, record.get(field)!=null?record.get(field).toString():null, "Completed", "Creation of 1 "+oneRecord.get("objectType").toString()));
				else {
					//add each field to record(we don't need Message Id to put in the record)
					oneFieldElement = doc.createElement(field);
					oneFieldElement.appendChild(doc.createTextNode(record.get(field).toString()));
					childElement.appendChild(oneFieldElement);
				}
			}
		}
		//check for child 
		if(oneRecord.get("children") instanceof JSONArray) {
			JSONArray childOfOneRecord = (JSONArray) oneRecord.get("children");
			if(!childOfOneRecord.isEmpty()) {
				//read for each object in child and append it with its parent(use recursive approach)
				Element parentEle = childElement; //now child become parent
				childOfOneRecord.forEach(row -> {
					try {
						readJsonObject((JSONObject) row, doc, parentEle, null, lstMsg);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
		}
	}
	//--Phanith--8/27/2019--Ticket#5897//
	
	//use for update chain data only
	private Map<String, List<IMapping__c>> cacheMapping = new HashMap<>();
	private Map<String, List<IChained_Interfaces__c>> cacheIChainInterfaces = new HashMap<>();
	public List<BatchExecutorResponseItem> updateChainData(SFIntegrationService integrationService, Interfaces__c intff, IBean[][] records) throws Exception{
		cacheMapping.clear();
		cacheIChainInterfaces.clear();
		if (records == null || records.length < 0)
			throw new Exception("XMLAdapter> updateChainData> No records available for an update or create process in the source system.");
		
		if(getMapProps().get(PropertyNameHelper.XML_TYPE).equalsIgnoreCase(PropertyNameHelper.TYPE_ATTRIBUTE))
			throw new Exception("XMLAdapter> updateChainData> Not support with Xml type attribute.");
		
		if(folder==null || folder.equals("")  || targetFileName==null || targetFileName.trim().equals("") ) {
			setConnectionInfo(map());
		}
		
		List<BatchExecutorResponseItem> lstMsg = new ArrayList<BatchExecutorResponseItem>();
		
		if (!folder.trim().equals("") && !targetFileName.equals("")) {
			updateToFTPFile(records, null, folder, targetFileName, 
					integrationService, lstMsg, intff);
		}
		
		return lstMsg;
    }
	
	private Element cacheYoungestElement = null;
	private Element appendChild(String[] arrayMappingTargetPath, int index, String mappingTargetObject, String mappingTarget, Document doc, String xmlPrefix) {
		if(index >= arrayMappingTargetPath.length) {
			return null;
		}
		if(arrayMappingTargetPath[index].equalsIgnoreCase(mappingTargetObject) || arrayMappingTargetPath[index].equalsIgnoreCase(mappingTarget)) {
			return null;
		}
		
		Element e = doc.createElement(xmlPrefix+arrayMappingTargetPath[index]);
		cacheYoungestElement = e;
		index = index + 1;

		Element e1 = appendChild(arrayMappingTargetPath, index, mappingTargetObject, mappingTarget, doc, xmlPrefix);
		
		if(e1!=null) {
			e.appendChild(e1);
			cacheYoungestElement = e1;
		}
		
		return e;
	}
	
	private XMLChildNode addChildNode(Element parentRecordElement, List<List<IBean>> records, String interfaceId, Document doc, SFIntegrationService integrationService, IBean parentField) throws Exception{
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
		boolean mappingHasNoExtID = true;
		for(ISFIntegrationObject mpp : mpps){
			IMapping__c mapping = (IMapping__c)mpp;
			if(mapping.isExternalId()){//if(StringUtils.isEmpty(mapping.getEXT_ID__c())?false:Boolean.parseBoolean(mapping.getEXT_ID__c())) {
				mappingHasNoExtID = false;
				break;
			}
		}
		IMapping__c fmap = mpps.get(0);
		//String mappingTarget =fmap.getTarget__c();// (String) mpps.get(0).get("Target__c");
		String mappingTargetPath =fmap.getTarget_Object__c();// (String) mpps.get(0).get("Target_Path__c");
		if(mappingTargetPath== null)throw new Exception("XMLAdapter> update2>mapping:"+fmap.getId()+"> mappingTargetPath is empty");
		mappingTargetPath = mappingTargetPath.replace("(text)", "");
		String mappingTargetObject =fmap.getTarget_Object__c();// (String) mpps.get(0).get("Target_Object__c");
		
		Map<String, String> adapterProperties = this.getMapProps();
		String xml_namespace = adapterProperties.get(XmlAdapter.XML_NAMESPACE);
		String suppressNullValue = adapterProperties.get(XmlAdapter.SUPPRESS_NULL_VALUE);
		String xml_prefix = StringUtils.isEmpty(adapterProperties.get(XmlAdapter.XML_PREFIX))?XmlAdapter.DEFAULT_PREFIX:adapterProperties.get(XmlAdapter.XML_PREFIX);		
		if(StringUtils.isEmpty(xml_namespace)){
			xml_prefix = "";
		}else {
			xml_prefix = xml_prefix+":";
		}
		
		String msg = "";
		int numberOfRecord = 0;
		boolean validRecord = false;

		List<String> extIdFieldValueList = new ArrayList<>();
		Interfaces__c intff = integrationService.getInterfaceById(interfaceId);
		String interfaceSourceName = intff.getSource_Name__c();
		//Element subRecordElement = null;
		for(List<IBean> oneRecord: records){
			String extIdField = null;
			if(!oneRecord.contains(parentField))continue;
			Element recordElement = null;
			boolean allFieldHasNoValue = true;
			IBean parentFieldRecurse = null;
			for(ISFIntegrationObject mpp : mpps){
				IMapping__c mapping = (IMapping__c)mpp;
				boolean ext_id = mapping.isExternalId();//StringUtils.isEmpty(mapping.getEXT_ID__c())?false:Boolean.parseBoolean(mapping.getEXT_ID__c());
				for(IBean field: oneRecord){
					String[] tmp = field.getName().split("\\.", 2); //Eg. Account.Id, MESSAGE
					String fieldName = "", fieldType = "";
					if(tmp.length>1){
						fieldType = tmp[0];
						fieldName = tmp[1];
					}else{
						fieldName = tmp[0];
					}
					if(fieldName.equals("SKYVVA__PARENTID") || fieldName.equals("SKYVVA__SALESFORCEID"))continue;
					
					Element fieldElement = (!StringUtils.isEmpty(fieldType) && interfaceSourceName.equals(fieldType))?
													(fieldName.equals(mapping.getTarget__c())?doc.createElement(xml_prefix+fieldName):null):null;
					if(fieldElement!=null){
						if(recordElement==null)recordElement = doc.createElement(xml_prefix+mappingTargetObject);
						String fieldValue = field.getValue();
						if(!StringUtils.isEmpty(fieldValue)){allFieldHasNoValue=false;}
						
						if(!(!StringUtils.isEmpty(suppressNullValue) && suppressNullValue.equalsIgnoreCase("true") && StringUtils.isEmpty(field.getValue()))) {
							fieldElement.appendChild(doc.createTextNode(field.getValue()));
							recordElement.appendChild(fieldElement);
						}
						
						if(ext_id) {
							extIdField = field.getValue(); // this is external id
							parentFieldRecurse = field;
						}
						
						if(mappingHasNoExtID)parentFieldRecurse = field;
						break;
					}
				}
			}
			if(mappingHasNoExtID || (!extIdFieldValueList.contains(extIdField) || extIdField==null)){ //if external id, create recordElement only once
				if(extIdField!=null)extIdFieldValueList.add(extIdField);
				if(recordElement!=null && !allFieldHasNoValue){
					validRecord = true;
					//if(subRecordElement==null){
					//	subRecordElement = doc.createElement(target_object+"s");
					//}
					for(IChained_Interfaces__c ici : icis){
						XMLChildNode n = addChildNode(recordElement, records, ici.getChildInterfaceId__c(), doc, integrationService, parentFieldRecurse);
						recordElement = n.getNode();
						String tmpMsgSplit[] = StringUtils.split(n.getMessage()," ");
						String msgSplit[] = msg.split(" ");	
						if((tmpMsgSplit.length>2 && msgSplit.length>2) && msg.contains(tmpMsgSplit[tmpMsgSplit.length-1])){ //msg = Creation of 2 Contact. Creation of 1 Case.    
																													//n.message = Creation of 2 Case.
							try{
								msgSplit[msgSplit.length-2] = ""+ (Integer.parseInt(tmpMsgSplit[tmpMsgSplit.length-2]) + Integer.parseInt(msgSplit[msgSplit.length-2])); // msg -> 1 case //n.message -> 2 case
								msg="";
								for(String m: msgSplit)msg+= m+" ";
							}catch(Exception e){
								msg += n.getMessage();
								e.printStackTrace();
							}
							
						}else{
							msg += n.getMessage();
						}
					}
					parentRecordElement.appendChild(recordElement);
					//subRecordElement.appendChild(recordElement); use parent record append child directly, no need "targetObject+s"
					numberOfRecord++;
				}
			}
		}
		//if(subRecordElement!=null)parentRecordElement.appendChild(subRecordElement);
		msg = ((numberOfRecord>0 && validRecord)?"Creation of "+numberOfRecord+" "+interfaceSourceName+". ":"") + msg;
		return new XMLChildNode(msg, parentRecordElement);
	}
	
	private static List<List<IBean>> convertArrayToList(IBean[][] records){
		List<List<IBean>> listBean = new ArrayList<>();
		for(IBean[] record: records){
			List<IBean> lb = new ArrayList<>();
			for(IBean b: record){
				lb.add(b);
			}
			listBean.add(lb);
		}
		return listBean;
	}
	
	// read data from index 2
	private static List<List<IBean>> convertArrayToList2(IBean[][] records){
		List<List<IBean>> listBean = new ArrayList<>();
		for (int i = 0; i < records.length; i++) {
			List<IBean> oneRecord = new ArrayList<IBean>();
			for (int j = 2; j < records[i].length; j++) {
				oneRecord.add(new IBean(records[i][j].getName(), records[i][j].getValue()));
			}
			listBean.add(oneRecord);
		}
		return listBean;
	}

	@SuppressWarnings("unused")
	public static void main(String[] args){
		try{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			ByteArrayInputStream inputStream = new ByteArrayInputStream(stringBuilder.toString().getBytes("UTF-8"));
			Document document = builder.parse(inputStream);
			
			
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
	}

	@Override
	public List<List<String>> getParameters(String objectName) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getExtenFilter() {
		
		return FileMapAdapter.getFileExtensions(FileTypeHelper.XML);
	}

	@Override
	protected IRecordReader createRecordReader(MapIntegrationInfo integrationInfo) throws Exception {		
		if(integrationInfo.isBulkMode()) {
			if(!(createBulkEvent(0,integrationInfo) instanceof BulkIntegrationV3Event)) {
				return new XmlFileReader(this, integrationInfo);
			}	
		}
		return new XmlFileReaderV3(this, integrationInfo);
	}

}
