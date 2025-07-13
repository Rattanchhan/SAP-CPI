package com.iservice.adapter.reader;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.util.json.DeserializationException;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;
import org.apache.commons.lang3.StringUtils;

import com.iservice.gui.data.IChained_Interfaces__c;
import com.iservice.gui.data.IMapping__c;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.model.IMessageTree;
import com.iservice.task.MapIntegrationInfo;
import com.iservice.xmlparser.XmlField;
import com.iservice.xmlparser.XmlNode;
import com.sforce.soap.schemas._class.IServices.IBean;


public abstract class IMsgGeneratorV3 {
	final static String STRUCTURE_TYPE = "Structure";
	public final static String DOT_REPLACING = "!$!";
	public final static String SEP = ".";

	MapIntegrationInfo mapIntegrationInfo = null;
	String myPackage = null;
	DTOInterfaceStructure dtoInterfaceStructure;
	IMessageTree rootMsg = null;
	Map<String, IMessageTree> mMsg = new HashMap<String, IMessageTree>();
	int indexMsg = 0;
	String transferId;
	
	private static final char ALPHA_CHAR_CODES[] = { 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90 };
	private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private static final DecimalFormat nf = new DecimalFormat("000");
	
	public abstract List<XmlNode> getAllRootNode() throws Exception;
	public abstract XmlNode getAllRootNode(List<IBean> lsIBean) throws Exception;
	
	public IMsgGeneratorV3() {}
	
	public IMsgGeneratorV3(MapIntegrationInfo mapIntegrationInfo) throws Exception {
		this.mapIntegrationInfo = mapIntegrationInfo;
		getDTOInterfaceStructure(mapIntegrationInfo, null, null);
		myPackage = mapIntegrationInfo.getSFIntegrationService().getPackage();
		this.transferId = generateMessageExtId();
	}
	
	protected List<XmlNode> getMasterNodeMapped(XmlNode rootNode) throws Exception{
		List<XmlNode> masterNodeMapped = new ArrayList<XmlNode>();
		if(rootNode.getName().equalsIgnoreCase(dtoInterfaceStructure.getRootSourceObject()) && 
				rootNode.getCacheAllTreeNodes().get(dtoInterfaceStructure.getSourcePath())!=null ) {
			masterNodeMapped = rootNode.getCacheAllTreeNodes().get(dtoInterfaceStructure.getSourcePath());
		}
		
		if(masterNodeMapped.size()==0) {
			throw new Exception("No record found! Please check your data source and the mapping. Make sure it is matching the structure.");
		}
		
		return masterNodeMapped;
	}
	
	private void getDTOInterfaceStructure(MapIntegrationInfo mapIntegrationInfo, List<DTOInterfaceStructure> lstDtoInterfaceStructureChild, IChained_Interfaces__c childIntf ) throws Exception {
		Interfaces__c intf;
		List<IMapping__c> lsMapping;
		List<IChained_Interfaces__c> lsIChained_Interfaces__c;
		DTOInterfaceStructure oneDtoInterfaceStructure;
		Map<String, String> mSourceInfo;
		
		if(lstDtoInterfaceStructureChild==null) {  //root interface
			intf = mapIntegrationInfo.getInterfaces();
			lsMapping = mapIntegrationInfo.getSFIntegrationService().getIMapping__c(intf.getId());
			mSourceInfo = getSourceObjectInfo(lsMapping);
			oneDtoInterfaceStructure = new DTOInterfaceStructure(intf, intf.getId(), lsMapping , mSourceInfo.get("sourceObject"), null, mSourceInfo.get("rootSourceObject"),
					mSourceInfo.get("sourcePath"));
			this.dtoInterfaceStructure = oneDtoInterfaceStructure;
		}else { //child interface
			String[] subIntfId = {childIntf.getChildInterfaceId__c()};
			intf = mapIntegrationInfo.getSFIntegrationService().getInterfaceByIds(subIntfId, true).get(0);
			lsMapping = mapIntegrationInfo.getSFIntegrationService().getIMapping__c(intf.getId());
			mSourceInfo = getSourceObjectInfo(lsMapping);
			oneDtoInterfaceStructure = new DTOInterfaceStructure(intf, intf.getId(), lsMapping , mSourceInfo.get("sourceObject"), childIntf.getParentInterfaceId__c(), 
					mSourceInfo.get("rootSourceObject"), mSourceInfo.get("sourcePath"));
			lstDtoInterfaceStructureChild.add(oneDtoInterfaceStructure);
		}
		
		lsIChained_Interfaces__c = mapIntegrationInfo.getSFIntegrationService().getIChained_Interfaces__c(intf.getId());
		if(lsIChained_Interfaces__c!=null && lsIChained_Interfaces__c.size()>0) {
			for(IChained_Interfaces__c tmp : lsIChained_Interfaces__c){
				getDTOInterfaceStructure(mapIntegrationInfo, oneDtoInterfaceStructure.getChild(), tmp); 
			}
			
		}
	}
	
	private Map<String, String> getSourceObjectInfo(List<IMapping__c> lsMapping) {
		Map<String, String> mSourceInfo = new HashMap<String, String>();
		if(lsMapping !=null && lsMapping.size()>0) {
			for(IMapping__c oneMapping : lsMapping) {
				if(oneMapping.getType__c().equals(STRUCTURE_TYPE) && oneMapping.getFull_Source_Path__c() !=null) {
					String[] sourceInfo = oneMapping.getFull_Source_Path__c().split("/");
					String sourcePath = oneMapping.getFull_Source_Path__c().substring(oneMapping.getFull_Source_Path__c().indexOf("/")+1).replace("/", ".").toUpperCase();
					mSourceInfo.put("rootSourceObject", sourceInfo[1]);
					mSourceInfo.put("sourceObject", sourceInfo[sourceInfo.length-1]);
					mSourceInfo.put("sourcePath", sourcePath);
					return mSourceInfo;
				}
			}
		}
		return mSourceInfo;
	}
	
	public List<IMessageTree> generateIMessage(List<XmlNode> lsChosenNode) throws Exception {
		List<IMessageTree> lstImsg = new ArrayList<IMessageTree>();
		this.indexMsg = 0;
		rootMsg = null;
		mMsg = new HashMap<String, IMessageTree>();
		for(int i=0; i<lsChosenNode.size(); i++) {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
			String transactionId =formatter.format(Calendar.getInstance().getTime())+i;
			generateIMsgOneNode(lsChosenNode.get(i), dtoInterfaceStructure, lstImsg, 0 , true, transactionId, 1);
		}
		return lstImsg;
	}
	
	protected JsonObject getFilterFromField(IMapping__c mapping, String field) throws DeserializationException {
		if (StringUtils.isNotBlank(mapping.getFilters__c())) {
			JsonObject filter = (JsonObject)  (JsonObject) Jsoner.deserialize(mapping.getFilters__c());
			if(StringUtils.isNotBlank(field)) {
				return (JsonObject)filter.get(field);
			}else {
				return filter;
			}
		}
		return null;
	}
	
	private void generateIMsgOneNode(XmlNode node, DTOInterfaceStructure dtoInterfaceStructure, List<IMessageTree> lstImsg, int index, boolean isRoot, String transactionId,
			int level) throws Exception{
		boolean isDoMasterNode = false;
		Map<String, String> oneDataRecord = new HashMap<String, String>();
		// master mapping
		if(dtoInterfaceStructure.getLsMapping().size()==0) throw new Exception("No mapping defined for the interface: "+dtoInterfaceStructure.getInterfaces__c().getName__c());
		for(IMapping__c oneMapping : dtoInterfaceStructure.getLsMapping()) {
			if(oneMapping.getType__c().equals(STRUCTURE_TYPE) || oneMapping.getSource__c().indexOf(".")==-1 && !isDoMasterNode) {
				isDoMasterNode = true;
				oneDataRecord.putAll(node.getMapData(dtoInterfaceStructure.getLsMapping(), true));
			}
			// none master mapping
			else if(oneMapping.getSource__c().indexOf(".")>-1 && !oneMapping.getType().equals(STRUCTURE_TYPE) &&
					oneMapping.getReference_Field_Parent_And_Above__c()!=null) {
				String[] reference_Fields = oneMapping.getReference_Field_Parent_And_Above__c().split(",");
				for(String tmpStr : reference_Fields) {
					String[] arr = tmpStr.split("=");
					String relatedPath = arr[2].substring(0, arr[2].lastIndexOf(".")).toUpperCase();
					String relatedField = arr[0].substring(arr[0].lastIndexOf(".")+1, arr[0].lastIndexOf("_"));
					JsonObject filterCondition = getFilterFromField(oneMapping, arr[0]);
					Map<String, String> retatedRecord = null;
					XmlNode relatedNode = null;
					
					// parent level
					if(arr[1].toUpperCase().equals("parent_level".toUpperCase()) ) {
						relatedNode = getRelatedParentNode(node.getParent(), relatedPath);
					}
					// ghost level
					if(arr[1].toUpperCase().equals("parent_level_ghost".toUpperCase())) {
						relatedNode = getRelatedGhostNode(node, relatedPath);
					}
					// context level
					if(arr[1].toUpperCase().equals("parent_level_context".toUpperCase())) {
						relatedNode = getRelatedContextNode(node, relatedPath, filterCondition, false);
//						if(relatedNode==null) relatedNode = getRelatedGhostNode(node, retatedPath);
					}
					
					if(relatedNode!=null) retatedRecord = relatedNode.getMapData(dtoInterfaceStructure.getLsMapping(), false);
					
					if(retatedRecord!=null) {
						String fieldValue = retatedRecord.get(relatedField.toLowerCase());
						oneDataRecord.put(arr[0].toLowerCase(), fieldValue);
					}
				}
			}
			// uncle field used in mapping
			if(oneMapping.getReference_Field_Uncle__c()!=null) {
				String[] uncleFields = oneMapping.getReference_Field_Uncle__c().split(",");
				for(String tmpStr: uncleFields) {
					String[] arr = tmpStr.split("=");
					String unclePath = arr[1].substring(0, arr[1].lastIndexOf(".")).toUpperCase();
					String uncleField = arr[1].substring(arr[1].lastIndexOf(".")+1);
					Map<String, String> retatedRecord = null;
					// sibling and uncle node using the same mapping field "Reference Field Uncle"
					// find for sibling node
					XmlNode relatedNode = getRelatedUncleNode(node, unclePath);
					// find for uncle node
					if(relatedNode==null) relatedNode = getRelatedUncleNode(node.getParent(), unclePath);
					// when parent context with grand parent so other grand parent become uncle
					if(relatedNode==null && node.getParent()!=null) relatedNode = getRelatedUncleNode(node.getParent().getParent(), unclePath);
					if(relatedNode!=null) retatedRecord = relatedNode.getMapData(dtoInterfaceStructure.getLsMapping(), false);
					if(retatedRecord!=null) {
						String fieldValue = retatedRecord.get(uncleField.toLowerCase());
						oneDataRecord.put(arr[0].toLowerCase(), fieldValue);
					}
				}
			}
			// Context Parent 
			if(oneMapping.getContext_Parent_Filter__c()!=null) {
				String[] reference_Fields = oneMapping.getContext_Parent_Filter__c().split(",");
				for(String tmpStr : reference_Fields) {
					String[] arr = tmpStr.split("=");
					String relatedPath = arr[1].substring(0, arr[1].lastIndexOf(".")).toUpperCase();
					String relatedField = arr[1].substring(arr[1].lastIndexOf(".")+1);
					Map<String, String> retatedRecord = null;
					
					JsonObject filterCondition = getFilterFromField(oneMapping, null);
					if(filterCondition!=null) {
											
						XmlNode relatedNode = getRelatedContextNode(node, relatedPath, filterCondition, true);
						
						if(relatedNode!=null) retatedRecord = relatedNode.getMapData(dtoInterfaceStructure.getLsMapping(), false);
						
						if(retatedRecord!=null) {
							String fieldValue = retatedRecord.get(relatedField);
							oneDataRecord.put(arr[0].toLowerCase(), fieldValue);
						}
					}
				}
			}
		}
		
		IMessageTree parentMsg = null;
		String rootId = null;
		String parentId = null;
		
		if(!isRoot) {
			rootMsg.getMessage().put(myPackage+"HasChild__c", true);
			rootId = rootMsg.getMessage().get(myPackage+"External_Id2__c").toString();
			parentMsg = mMsg.get(dtoInterfaceStructure.getParentIntfId());
			parentMsg.getMessage().put(myPackage+"HasChild__c", true);
			parentId = parentMsg.getMessage().get(myPackage+"External_Id2__c").toString();
		}
		IMessageTree msg = createIMessage(rootId, parentId, transactionId, false, level, dtoInterfaceStructure.getSourcePath(), dtoInterfaceStructure.getInterfaces__c(), oneDataRecord);
		lstImsg.add(msg);
		mMsg.put(dtoInterfaceStructure.getInterfaceId(), msg);
		if(isRoot) {
			rootMsg = msg;
		}
		//child
		level++;
		for(DTOInterfaceStructure childDtoIntf : dtoInterfaceStructure.getChild()) {
			List<XmlNode> childNode = new ArrayList<XmlNode>();
			retrieveNodeForIChained(node, childDtoIntf.getSourcePath(), childNode);
			for(int i=0; i<childNode.size(); i++) {
				generateIMsgOneNode(childNode.get(i), childDtoIntf, lstImsg, i, false, transactionId, level);
			}
		}
	}
	
	List<JsonObject> nodeToListJson(XmlNode node){
		if(node==null) return null;
		List<JsonObject> lsJson = new ArrayList<JsonObject>();
		for(XmlField tmp : node.getFields()) {
			JsonObject data = new JsonObject();
			data.put(tmp.getName(), tmp.getValue());
			lsJson.add(data);
		}
		return lsJson;
	}
	
	private XmlNode getRelatedContextNode(XmlNode node, String retatedNodePath, JsonObject filterCondition, boolean isFilterByParent) {
		if(node!=null && retatedNodePath!=null) {
			if(node.getCacheTreeNode().get(retatedNodePath)!=null){
				return node;
			}else {
				if(node.getChildren()!=null && node.getChildren().size()>0) 
					for(XmlNode oneChild : node.getChildren()) { 
						XmlNode relatedContextNode = getRelatedContextNode(oneChild, retatedNodePath, filterCondition, isFilterByParent);
						// check filter condition
						if(relatedContextNode!=null && filterCondition!=null) {
							
							List<JsonObject> filterNode = null;
							// find parent node to filter
							if(isFilterByParent) {
								String filterParentPath = filterCondition.keySet().iterator().next();
								XmlNode filterParentNode = getRelatedParentNode(relatedContextNode, filterParentPath);
								filterNode = nodeToListJson(filterParentNode);
								if(FilterContextNode.getRecordByFilter((JsonObject) filterCondition.get(filterParentPath), filterNode)!=null) {
									return relatedContextNode;
								}
							}else { // filter with current node
								filterNode = nodeToListJson(relatedContextNode);
								if(FilterContextNode.getRecordByFilter(filterCondition, filterNode)!=null) {
									return relatedContextNode;
								}
							}						
						}else if(relatedContextNode!=null){
							return relatedContextNode;
						}
					}
				else return null;
			}
		}
		return null;
	}
	
	private XmlNode getRelatedParentNode(XmlNode node, String retatedNodePath) {
		if(node!=null && retatedNodePath!=null) {
			retatedNodePath = StringUtils.toRootUpperCase(retatedNodePath);
			if(node.getCacheTreeNode().get(retatedNodePath)!=null){
				return node;
			}else {
				return getRelatedParentNode(node.getParent(), retatedNodePath);
			}
		}
		return null;
	}
	
	private XmlNode getRelatedGhostNode(XmlNode node, String retatedNodePath) {
		if(node!=null && retatedNodePath!=null && node.getParent()!=null && node.getParent().getChildren()!=null) {
			for(XmlNode oneNode : node.getParent().getChildren()) {
				if(oneNode.getCacheTreeNode().get(retatedNodePath)!=null){
					return oneNode;
				}
			}
			// if the node may has context level and not child of root
			if(!node.getParent().isRoot()) {
				for(XmlNode oneNode : node.getParent().getChildren()) {
					XmlNode relatedContextNode = getRelatedContextNode(oneNode, retatedNodePath, null, false);
					if(relatedContextNode!=null) return relatedContextNode;
				}
				return getRelatedGhostNode(node.getParent(), retatedNodePath);
			}else {
				return (node.getParent().getCacheAllTreeNodes().get(retatedNodePath)!=null? node.getParent().getCacheAllTreeNodes().get(retatedNodePath).get(0): null);
			}
		}
		return null;
	}
	
	private XmlNode getRelatedUncleNode(XmlNode node, String retatedNodePath) {
		if(node!=null && retatedNodePath!=null && node.getParent()!=null && node.getParent().getChildren()!=null) {
			for(XmlNode oneNode : node.getParent().getChildren()) {
				if(oneNode.getCacheTreeNode().get(retatedNodePath)!=null){
					return oneNode;
				}
			}
			// if the node may has context level
			for(XmlNode oneNode : node.getParent().getChildren()) {
				XmlNode relatedContextNode = getRelatedContextNode(oneNode, retatedNodePath, null, false);
				if(relatedContextNode!=null) return relatedContextNode;
			}
		}
		return null;
	}
	
	private void retrieveNodeForIChained(XmlNode parentNode, String sourcePath, List<XmlNode> lsNode) {
		boolean isFound=false;
		for(XmlNode oneNode : parentNode.getChildren()) {
			if( oneNode.getCacheTreeNode().get(sourcePath)!=null ) {
				isFound=true;
				lsNode.add(oneNode);	
			}
		}
		
		if(!isFound) {
			for(XmlNode oneNode : parentNode.getChildren()) {
				retrieveNodeForIChained(oneNode, sourcePath, lsNode);
			}
		}
	}
	
	private IMessageTree createIMessage(String rootId, String parentId, String transactionId, boolean hasChild, Integer level, String hPath, Interfaces__c intf, Map<String,String> mapData) {
		indexMsg ++ ;
		String msgName = generateMessageName(indexMsg);
		JsonObject imessage = new JsonObject();
		String externalId = generateMessageExtId();
		imessage.put("Name", msgName);
		imessage.put(myPackage + "Type__c", "InBound");
		imessage.put(myPackage + "Status__c", "New");
		imessage.put(myPackage + "TransactionId__c", transactionId);
		imessage.put(myPackage + "HasChild__c", hasChild);
		imessage.put(myPackage + "HLevel__c", level);
		imessage.put(myPackage + "HPath__c", hPath);
		imessage.put(myPackage + "isRoot__c", StringUtils.isBlank(rootId));
		imessage.put(myPackage + "TransferId__c", transferId);
		imessage.put(myPackage + "Message_Type__c", "sObject Message");
		imessage.put(myPackage + "ProcessMode__c", "Hierarchical");
		imessage.put(myPackage + "Data__c", Jsoner.serialize(mapData));
		imessage.put(myPackage + "External_Id2__c", externalId);
		imessage.put(myPackage + "Integration__c", intf.getIntegration__c());
		imessage.put(myPackage + "Interface__c", intf.getId());
		return new IMessageTree(imessage, rootId, parentId);
	}
	
	public String generateMessageExtId(){
		char uid[] = new char[13];
		int index = 0;
		for (int i = 0; i < 4; i++) {
			uid[index++] = ALPHA_CHAR_CODES[(int) (Math.random() * Character.MAX_RADIX)];
		}
		uid[index++] = 45; // charCode for "-"
		long time = System.currentTimeMillis();
		String timeString = ("0000000" + Long.toString(time, Character.MAX_RADIX).toUpperCase());
		timeString = timeString.substring(timeString.length() - 8);
		for (int i = 0; i < timeString.length(); i++) {
			uid[index++] = timeString.charAt(i);
		}
		//SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		//formatter.format(Calendar.getInstance().getTime());
		return new String(uid);
	}
	
	private String generateMessageName(int index){
		return "IM#"+df.format(new Date(System.currentTimeMillis()))+nf.format(index);
	}
	
	protected class DTOInterfaceStructure {
		Interfaces__c interfaces__c;
		String interfaceId;
		String parentIntfId;
		List<IMapping__c> lsMapping = new ArrayList<IMapping__c>();
		String sourceObject;
		List<DTOInterfaceStructure> child;
		String rootSourceObject;
		String sourcePath;
		
		public DTOInterfaceStructure(Interfaces__c interfaces__c, String interfaceId,
				List<IMapping__c> lsMapping, String sourceObject, String parentIntfId, String rootSourceObject, String sourcePath) {
			super();
			this.child = new ArrayList<DTOInterfaceStructure>();
			this.interfaces__c = interfaces__c;
			this.interfaceId = interfaceId;
			this.lsMapping = lsMapping;
			this.sourceObject = sourceObject;
			this.parentIntfId = parentIntfId;
			this.rootSourceObject = rootSourceObject;
			this.sourcePath = sourcePath;
		}
		
		public List<DTOInterfaceStructure> getChild() {
			return child;
		}
		public void setChild(List<DTOInterfaceStructure> child) {
			this.child = child;
		}
		public Interfaces__c getInterfaces__c() {
			return interfaces__c;
		}
		public void setInterfaces__c(Interfaces__c interfaces__c) {
			this.interfaces__c = interfaces__c;
		}
		public String getInterfaceId() {
			return interfaceId;
		}
		public void setInterfaceId(String interfaceId) {
			this.interfaceId = interfaceId;
		}
		public List<IMapping__c> getLsMapping() {
			return lsMapping;
		}
		public void setLsMapping(List<IMapping__c> lsMapping) {
			this.lsMapping = lsMapping;
		}
		public String getSourceObject() {
			return sourceObject;
		}
		public void setSourceObject(String sourceObject) {
			this.sourceObject = sourceObject;
		}
		public String getParentIntfId() {
			return parentIntfId;
		}

		public void setParentIntfId(String parentIntfId) {
			this.parentIntfId = parentIntfId;
		}
		
		public String getRootSourceObject() {
			return rootSourceObject;
		}
		
		public String getSourcePath() {
			return sourcePath;
		}
	}
}


