package com.iservice.adapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.Ostermiller.util.ExcelCSVParser;
import com.iservice.adapter.batch.BatchExecutorResponseItem;
import com.iservice.adapter.batch.IBatchExecutor;
import com.iservice.adapter.reader.CsvFileReader;
import com.iservice.adapter.reader.IRecordReader;
import com.iservice.gui.data.IChained_Interfaces__c;
import com.iservice.gui.data.IMapping__c;
import com.iservice.gui.data.ISFIntegrationObject;
import com.iservice.gui.data.IStructure__c;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.gui.data.MsgTypeFieldEntry__c;
import com.iservice.gui.helper.Helper;
import com.iservice.gui.helper.PropertyNameHelper;
import com.iservice.sforce.SFIntegrationService;
import com.iservice.task.MapIntegrationInfo;
import com.sforce.soap.schemas._class.IServices.IBean;


public class CSVAdapter extends FileMapAdapter {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(CSVAdapter.class);

	//31082010
	//make it support other separator besides ,
	private static final String _DEF_SEP = ",";
	private static final String _SEMI_SEP = ";";
	private static final String _TAB_SEP = "\t";
	private static final String _PIPE_SEP = "|";//07-03-12

	private static List<String> validSeparators = new ArrayList<String>();

	private ExcelCSVParser csvParser;

	static {
		validSeparators.add(_DEF_SEP);
		validSeparators.add(_SEMI_SEP);
		validSeparators.add(_TAB_SEP);
		validSeparators.add(_PIPE_SEP);//07-03-12
	}

	public static List<String> separaters = new ArrayList<String>(){
		/**
		 * 
		 */
		private static final long serialVersionUID = 9051243125112051093L;
		{add(_DEF_SEP);}
		{add(_SEMI_SEP);}
		{add("Tab");}
		{add(_PIPE_SEP);}
	};

	public CSVAdapter() {}

	@Override
	void validateProperties() throws Exception {

		if(!validSeparators.contains(separator)) {
			String v = "";
			for(String s : validSeparators){
				v+= "[" +s + "] ";
			}
			throw new Exception("The separator '" + separator + "' is not supported. Invalid separator execption: only " + v + " are supported.");
		}
	}

	//23-12-2016 if file no header-> prepareHeader
	public String[][] prepareCSVHeader(String[][] strRecord,List<ISFIntegrationObject> listHeader) throws Exception{

		Vector<String[]> preRecord = new Vector<String[]>();

		if(strRecord != null && strRecord.length>0) {

			//sort mapListHeaders by Sequence
			listHeader.sort((o1,o2)-> {
				if(o1 instanceof IStructure__c && o2 instanceof IStructure__c){
					IStructure__c s1=(IStructure__c) o1,s2=(IStructure__c) o2;
					if(s1.getSequence__c()==null || s2.getSequence__c()==null) return 0;
					return s1.getSequence__c().compareToIgnoreCase(s2.getSequence__c());
				}
				else{
					MsgTypeFieldEntry__c s1=(MsgTypeFieldEntry__c) o1,s2=(MsgTypeFieldEntry__c) o2;
					if(s1.getSequence__c()==null || s2.getSequence__c()==null) return 0;
					return s1.getSequence__c().compareToIgnoreCase(s2.getSequence__c());
				}
			});
			//define length for create header from iStructures
			//create header
			Vector<String> preheader = new Vector<String>();
			//IStructure
			if(listHeader.get(0) instanceof IStructure__c){
				for(ISFIntegrationObject iStructure : listHeader){
					preheader.add(iStructure.get("Name").toString());
				}
			}else{//MessageType
				for(ISFIntegrationObject messageType : listHeader){
					preheader.add(((IStructure__c)messageType.get("IStructure__c")).getName().toString());
				}
			}

			if (preheader.size() == 0)return null;

			String[] resultHeader = new String[preheader.size()];
			preRecord.add(preheader.toArray(resultHeader));

			//define length for create header new from data
			int headerLength =0;
			for(String[] tmp:strRecord){
				if(headerLength<=tmp.length){
					headerLength=tmp.length;
				}
			}
			//add data
			for(String[] str : strRecord){
				preRecord.add(str);
			}
			if (preRecord.size() == 0){
				return null;
			}
			String[][] resultRecord = new String[preRecord.size()][];
			return (preRecord.toArray(resultRecord));
		}
		return null;
	}

	// Receive records and write them to a new CSV file
	//folder and filename are required fields
	public List<BatchExecutorResponseItem> update(IBean[][] beans,String interfacetype, SFIntegrationService integrationService, Interfaces__c intff) throws Exception {
		// beans[0][0] is used for Message_Key
		// beans[0][1] is used for ExternalIdField
		if (beans == null || beans.length < 0)
			throw new Exception("CSVAdapter> update> No records available for an update or create process in the source system.");

		if(folder==null || folder.equals("")  || targetFileName==null || targetFileName.trim().equals("") || separator==null || separator.equals("")) {
			setConnectionInfo(map());
		}

		List<BatchExecutorResponseItem> lstMsg = new ArrayList<BatchExecutorResponseItem>();
		String msgStatus = "Completed", msg = "Creation of a new record.";

		try{
			if (!folder.trim().equals("") && !targetFileName.equals("")) {
				try {
					updateToFTPFile(beans,null, folder, targetFileName, null, null, null);
					msgStatus = "Completed";
					msg = "Creation of a new record.";
				} 
				catch (IOException ex) {
					LOGGER.error("> CSV adapter: update()> ERROR> " + ex, ex);
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
		finally{
			if(csvParser !=null){
				csvParser.close();
				csvParser = null;
			}
		}
	}

	public List<File> updateToFTPFile(IBean[][] beans, List<List<IBean>> lstRecords, String folder, String targetFileName, 
			SFIntegrationService integrationService, List<BatchExecutorResponseItem> lsMsg, Interfaces__c intf) throws Exception{
		List<File> lsFile = new ArrayList<File>();
		// create a new unique CSV file
		// filename = "Account20101206012459PM.csv"
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		
		//store current time
		currentTime = formatter.format(Calendar.getInstance().getTime());
		//new file name and it is used with csv file and FTP file for renaming file purpose
		String newFilename = (folder + Helper.getFileSeparator() + targetFileName + currentTime+".csv").trim();

		//check if this file should be appended or not
		Boolean isAppendFile = "true".equals(mergeFile);
		//get current file
		String currentFilename = "";
		if(isAppendFile) currentFilename = (folder + Helper.getFileSeparator() + filename + (filename.endsWith(".csv")?"":".csv")).trim();
		else currentFilename = (folder + Helper.getFileSeparator() + targetFileName + ".csv").trim();
		File file = new File(currentFilename);
		//store current file size
		Long currentFileSize = file.exists() == false ? 0 :file.length()/1048576;


		//return name current file if merger file was selected and current file size is more than defined File Size
		if(isAppendFile && currentFileSize >= Long.valueOf(fileSize)){
			file.renameTo(new File(newFilename));
			//this variable is used with FTP file
			isRename = true;
		}
		//check current file if exist or not
		file = new File(isAppendFile==true?currentFilename:newFilename);
		//OutputStream out1 = new FileOutputStream(file);
		//OutputStreamWriter out = new OutputStreamWriter(out1,"UTF-8");

		// write file attachment if has.
		FileAdapter fileAdapter = new FileAdapter();
		lsFile = fileAdapter.updateToFTPFile(beans, null, folder, targetFileName, null, null, null);
		OutputStreamWriter out = null;

		if(!file.exists()){
			out = new OutputStreamWriter(new FileOutputStream(isAppendFile==true?currentFilename:newFilename,isAppendFile),"UTF-8");
			if(hasFileHeader) {
				if(beans!=null) {
					// Create column headers for the file from array
					for (int j = 2; j < beans[0].length; j++) {
						String header = beans[0][j].getName();
						header = header.replace("\"", "\"\"");
						if(hasQuotes) {
							out.write("\"" + header + "\"");
						}else {
							out.write(header);
						}
						if (j < beans[0].length - 1) {
							out.write(separator);
						}
					}
				}
				// Create column headers from list
				else if(lstRecords!=null && lstRecords.size()>0) {
					for (int j = 0; j < lstRecords.get(0).size(); j++) {
						String header = lstRecords.get(0).get(j).getName();
						header = header.replace("\"", "\"\"");
						if(hasQuotes) {
							out.write("\"" + header + "\"");
						}else {
							out.write(header);
						}
						if (j < lstRecords.get(0).size() - 1) {
							out.write(separator);
						}
					}
				}
				out.write("\n");
			}
		}else{
			out = new OutputStreamWriter(new FileOutputStream(isAppendFile==true?currentFilename:newFilename,isAppendFile),"UTF-8");
		}
		if(beans!=null) {
			// put the queried data from SF to the file
			for (int i = 0; i < beans.length; i++) {
				for (int j = 2; j < beans[i].length; j++) {
					String value = beans[i][j].getValue();
					if(value == null) value = "";
					value = value.replace("\"", "\"\"");
					if(hasQuotes) {
						out.write("\"" + value+ "\"");
					}else {
						out.write(value);
					}
					if (j < beans[i].length - 1) {
						out.write(separator);
					}
				}
				out.write('\n');
			}
		}
		else if(lstRecords!=null && lstRecords.size()>0) {
			for (int i = 0; i < lstRecords.size(); i++) {
				for (int j = 0; j < lstRecords.get(0).size(); j++) {
					String value = lstRecords.get(i).get(j).getValue();
					if(value == null) value = "";
					value = value.replace("\"", "\"\"");
					if(hasQuotes) {
						out.write("\"" + value+ "\"");
					}else {
						out.write(value);
					}
					if (j < lstRecords.get(0).size() - 1) {
						out.write(separator);
					}
				}
				out.write('\n');
			}
		}
		
		out.close();
		lsFile.add(file);
		//create Chain Message
		if(lsMsg!=null) {
			new XmlAdapter().createIChainMessage(integrationService, lsMsg, intf, beans, null);
		}

		return lsFile;
	}

	//use for update chain data only
	private Map<String, List<IMapping__c>> cacheMapping = new HashMap<>();
	private Map<String, List<IChained_Interfaces__c>> cacheIChainInterfaces = new HashMap<>();
	public List<BatchExecutorResponseItem> updateChainData(SFIntegrationService integrationService, Interfaces__c intff, IBean[][] records) throws Exception{
		cacheMapping.clear();
		cacheIChainInterfaces.clear();
		if (records == null || records.length < 0) throw new Exception("CSVAdapter() >updateChainData> No records available for an update or create process in the source system.");
		if(folder==null || folder.equals("")  || targetFileName==null || targetFileName.trim().equals("") ) {
			setConnectionInfo(map());
		}
		List<BatchExecutorResponseItem> lstMsg = new ArrayList<BatchExecutorResponseItem>();
		if (!folder.trim().equals("") && !targetFileName.equals("")) {
			try {
				// update
				updateToFTPFile(records,null, folder, targetFileName, integrationService, lstMsg, intff);
			}catch (IOException ex) {
				LOGGER.error(">CSVAdapter()>updateChainData()> ERROR: " + ex);
				String msgStatus = "Failed", msg = ex.getLocalizedMessage();
				lstMsg = new ArrayList<BatchExecutorResponseItem>();
				for (int i = 0; i < records.length; i++) {
					lstMsg.add(new BatchExecutorResponseItem(i, records[i][0].getValue(), msgStatus, msg));
				}
			}
			
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


	// get the first record (Column Header) from a CSV file
	public List<List<String>> getColumns(String objectName, List<? extends ISFIntegrationObject> listHeader) throws Exception {

		//Each element is List<STring> holds Name, Label, ....
		List<List<String>> listCols = new ArrayList<List<String>>(0);
		try{
			setConnectionInfo(map());
			List<List<IBean>> records = this.doTestQuery("", null).getResult();
			if (records!=null && records.size() > 0) {
				List<IBean> colHeader = records.get(0); // is Column Header for the file

				if(colHeader!=null) {
					for (int j = 0; j < colHeader.size(); j++) {
						//31082010 replace hidden char (for the first column)				
						if(j==0 && colHeader.get(j)!=null) {
							colHeader.get(j).setName(colHeader.get(j).getName().replaceAll(Helper.INVALID_CHARS, ""));
						}
						if(colHeader.get(j)!=null) {
							List<String> lstCol = new ArrayList<String>(0);
							lstCol.add(colHeader.get(j).getName());
							listCols.add(lstCol);
						}
					}
				}
			}
			return listCols;

		}
		finally{
			if(csvParser !=null){
				csvParser.close();
				csvParser = null;
			}
		}
	}
	
		//--Phanith--8/27/2019--Ticket #5897 //
		@SuppressWarnings("unchecked")
		public List<BatchExecutorResponseItem> updateV3(SFIntegrationService integrationService, JSONArray records, Interfaces__c intff) throws Exception {
			if (records == null || records.isEmpty())
				throw new Exception("CSVAdapter() >updateChainData> No records available for an update or create process in the source system.");
			
			if(folder==null || folder.equals("")  || targetFileName==null || targetFileName.trim().equals("") ) {
				setConnectionInfo(map());
			}
			List<BatchExecutorResponseItem> lstMsg = new ArrayList<BatchExecutorResponseItem>();
			if (!folder.trim().equals("") && !targetFileName.equals("")) {
				//get all map parent and it's child in oder to make csv header
				List<String> lstMappingFields = new ArrayList<String>();
				getAllRelatedMap(integrationService, intff.getId(), lstMappingFields);
				//here we read each object of hierarchical data and wrap list of message and the element to write the xml file and create message
				List<List<IBean>> lstRecords = new ArrayList<List<IBean>>();
				records.forEach(row -> {
					try {
						readJsonObjectToFlat((JSONObject) row, lstMappingFields, null, lstRecords, null, lstMsg);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} );
				try {
					updateToFTPFile(null, lstRecords , folder, targetFileName, null, null, null);
				}catch(Exception e) {
					for(int i=0; i<lstMsg.size(); i++) {
						lstMsg.get(i).setStatus("Failed");
						lstMsg.get(i).setMessage(e.getLocalizedMessage());
					}
				}
			}
			return lstMsg;
		}
		//--Phanith--8/27/2019--Ticket#5897//

	public List<List<String>> getRelationship(String objectName)
			throws Exception {
		return null;
	}

	public int update(String expression) throws Exception {
		return 0;
	}  

	public void setExternalIdField(String extIdField) {

	}

	public IBatchExecutor getBatchExecutor() throws Exception {
		return null;
	}

	public static void main(String[] args) {

		CSVAdapter adapter = new CSVAdapter();
		Map<String, String> mapProp = new HashMap<String, String>();

		mapProp.put(PropertyNameHelper.FOLDER, "D:\\Customer Folder\\IOD Cloudbroker\\Cloud Agent\\Error\\Logs PRODUCTA2S");
		mapProp.put(PropertyNameHelper.FILE_NAME, "PRODUCTA2S - Updated.CSV");
		mapProp.put(PropertyNameHelper.SEPARATOR, _DEF_SEP);


		try {
			adapter.setConnectionInfo(mapProp);
			List<List<IBean>>records = adapter.doTestQuery("", null).getResult();
			for(int i=0; i<records.size(); i++){
				for(int j=0; j<records.get(i).size(); j++){
					System.out.println(records.get(i).get(j).getName() + " - " + records.get(i).get(j).getValue());
				}
				System.out.println();
			}

			adapter.unlockFiles(adapter.files);

			List<List<String>> lstCols = adapter.getColumns("", null);
			System.out.println(lstCols.size());
			for(int i=0; i<lstCols.size(); i++){
				for(int j=0; j<lstCols.get(i).size(); j++){
					System.out.println(lstCols.get(i).get(j));
				}
			}

			adapter.unlockFiles(adapter.files);

		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	@Override
	public List<List<String>> getParameters(String objectName) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BatchExecutorResponseItem> update2(SFIntegrationService integrationService, Interfaces__c intff,
			IBean[][] records) throws Exception {
		return update(records, intff.getOperationType__c(), null, null);
	}

	@Override
	protected String getExtenFilter() {
		return FileMapAdapter.getFileExtensions(FileTypeHelper.CSV);
	}

	@Override
	protected IRecordReader createRecordReader(
			MapIntegrationInfo integrationInfo) {

		return new CsvFileReader(separator.charAt(0), this, integrationInfo);
	}

}
