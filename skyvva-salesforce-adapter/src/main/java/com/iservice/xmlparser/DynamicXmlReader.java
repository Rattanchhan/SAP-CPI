package com.iservice.xmlparser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import com.iservice.adapter.reader.IMsgGeneratorV3;
import com.sforce.soap.schemas._class.IServices.IBean;


/**
 * This class is a kind of Xml Reader
 * The following are Xml file structures supported by this xml reader:
 * 
 *  ------ Structured with Root element and its Children (represent for one record or one row) ------
 * S1- <?xml  version="1.0" encoding="UTF-8"?><result><row><field name="fName">fValue</field>...</row>...</result>
 * S2- <?xml version="1.0"?><result><row><fName>fValue</fName>...</row>...</result>
 * 
 *  ------ Not so well structured: all records or rows are mixed just under the Root element ------
 * S3- <?xml version="1.0" encoding="UTF-8"?><result><field name="f1">fvalue</field>...<field name="f1">fvalue</field>...</result>
 * S4- <?xml version="1.0" encoding="UTF-8"?><result><f1>fvalue</f1>...<f1>fvalue</f1>...</result>
 * 
 * * -hierarchical structure
 * 		<Account>
 * 			<accountId>...</accountId>
 * 			<accountName>...</accountName>
 * 			<Contact>
 * 				<contactLastName>...</contactLastName>
 * 				<contactEmail>...</contactEmail>
 * 			</Contact>
 * 			<Contact>
 * 				<contactLastName>...</contactLastName>
 * 				<contactEmail>...</contactEmail>
 * 			</Contact>
 * 		</Account>
 *   for hierarchical structure call xmlNode.flat() will get flat object (repeat parent)
 *   
 */
public class DynamicXmlReader {
	
	public static final String ATTRIBUE_PREFIX = "@";
	boolean isAddPrefix;
	
	public DynamicXmlReader(boolean isAddPrefix){
		this.isAddPrefix = isAddPrefix;
	}
	
	public DynamicXmlReader() {}
	
	protected Document buildDoc(InputStream input) throws Exception {
		SAXBuilder builder = new SAXBuilder();
		Document doc = null;
		doc = builder.build(input);
		return doc;
	}
	
	public List<List<IBean>> read(InputStream input) throws Exception{
		Document doc = buildDoc(input);
		Element node = doc.getRootElement();
		List<List<IBean>> result = new ArrayList<>();
		XmlNode root = new XmlNode(false);
		//right now, we don't need flag from salesforce
		readOneNode(node, root,isAddPrefix, 0, null);
		result.addAll(root.toFlat());
		return result;
	}
	
	public XmlNode getXmlNode(File f) throws Exception{
		XmlNode xmlNode = new XmlNode(false);
		InputStream input = null;
		try {
			Document doc = buildDoc(new FileInputStream(f));
			Element node = doc.getRootElement();
			xmlNode = readOneNode(node, xmlNode, false, 0, null);
		}catch(Throwable ex) {
			throw new Exception(ex);
		}finally {
			if(input!=null) {
				input.close();
				input=null;
			}
		}
		return xmlNode;
	}
	
	// this new version is just for inboundV3 xml file adapter which no need node name as a prefix of @attribute
	public XmlNode readOneNode2(Element node, XmlNode parent,boolean useObjPrefix,int objectLevel, String nodePath) {
		boolean isRoot = (objectLevel==0? true: false);
		objectLevel++;
		XmlNode xmlNode = new XmlNode(useObjPrefix);
		if(parent!=null){
			parent.addChild(xmlNode);
		}
		String name = node.getName().replace(IMsgGeneratorV3.SEP, IMsgGeneratorV3.DOT_REPLACING);
		xmlNode.setName(name);
		List<Attribute> nodeAttr = node.getAttributes();
		for(Attribute attr:nodeAttr){
			xmlNode.addField(new XmlField(ATTRIBUE_PREFIX+attr.getName(), attr.getValue()));
		}
		List<Element> children = node.getChildren();
		
		if(nodePath == null) nodePath = xmlNode.getName().toUpperCase();
		else nodePath = nodePath+("."+xmlNode.getName().toUpperCase());
		
		// cacheTreeNode
		if(isRoot) {
			doCachingTreeNode(xmlNode, nodePath, null);
			xmlNode.setRootNode(xmlNode);
			xmlNode.isRoot(true);
		}else if(parent.getRootNode()!=null) {
			xmlNode.setRootNode(parent.getRootNode());
			doCachingTreeNode(xmlNode, nodePath, xmlNode.getRootNode());
		}
		
		for(Element e:children){			
			List<Element> eChildren = e.getChildren();
			List<Attribute> attr = e.getAttributes();
			Map<String, String> mapAttribs = converToMap(attr);
			if(eChildren==null ||eChildren.isEmpty()) {	
				if(StringUtils.equalsIgnoreCase("field", e.getName()) && mapAttribs.containsKey(XmlField.NAME_ATTR)){
					String fieldName = mapAttribs.get(XmlField.NAME_ATTR);
					xmlNode.addField( new XmlField(fieldName, e.getValue()));
				}else{
					xmlNode.addField( new XmlField(e.getName(), e.getValue()));
					for(Entry<String, String> ent:mapAttribs.entrySet()){
						xmlNode.addField(new XmlField(e.getName()+ATTRIBUE_PREFIX+ent.getKey(), ent.getValue()));
					}
				}
			}else{
				readOneNode2(e, xmlNode,useObjPrefix, objectLevel, nodePath);
			}
		}
		if(useObjPrefix && objectLevel<2 ) xmlNode.setUseObjectPrefix(false);
		return xmlNode;
	}
	
	public XmlNode readOneNode(Element node, XmlNode parent,boolean useObjPrefix,int objectLevel, String nodePath) {
		// we read only one 1 level. here we force record to be read in flat to use for SF bulk api.
		if(objectLevel==2) return null; 
			
		boolean isRoot = (objectLevel==0? true: false);
		objectLevel++;
		XmlNode xmlNode = new XmlNode(useObjPrefix);
		if(parent!=null){
			parent.addChild(xmlNode);
		}
		String name = node.getName();
		xmlNode.setName(name);
		List<Attribute> nodeAttr = node.getAttributes();
		for(Attribute attr:nodeAttr){
			xmlNode.addField(new XmlField(name+ATTRIBUE_PREFIX+attr.getName(), attr.getValue()));
		}
		List<Element> children = node.getChildren();
		
		if(nodePath == null) nodePath = xmlNode.getName().toUpperCase();
		else nodePath = nodePath+("."+xmlNode.getName().toUpperCase());
		
		// cacheTreeNode
		if(isRoot) {
			doCachingTreeNode(xmlNode, nodePath, null);
			xmlNode.setRootNode(xmlNode);
			xmlNode.isRoot(true);
		}else if(parent.getRootNode()!=null) {
			xmlNode.setRootNode(parent.getRootNode());
			doCachingTreeNode(xmlNode, nodePath, xmlNode.getRootNode());
		}
		
		for(Element e:children){			
			List<Element> eChildren = e.getChildren();
			List<Attribute> attr = e.getAttributes();
			Map<String, String> mapAttribs = converToMap(attr);
			if(eChildren==null ||eChildren.isEmpty()) {	
				if(StringUtils.equalsIgnoreCase("field", e.getName()) && mapAttribs.containsKey(XmlField.NAME_ATTR)){
					String fieldName = mapAttribs.get(XmlField.NAME_ATTR);
					xmlNode.addField( new XmlField(fieldName, e.getValue()));
				}else{
					xmlNode.addField( new XmlField(e.getName(), e.getValue()));
					for(Entry<String, String> ent:mapAttribs.entrySet()){
						xmlNode.addField(new XmlField(e.getName()+ATTRIBUE_PREFIX+ent.getKey(), ent.getValue()));
					}
				}
			}else{
				readOneNode(e, xmlNode,useObjPrefix, objectLevel, nodePath);
			}
		}
		if(useObjPrefix && objectLevel<2 ) xmlNode.setUseObjectPrefix(false);
		return xmlNode;
	}
	
	public static void doCachingTreeNode(XmlNode xmlNode, String nodePath, XmlNode rootNode) {
		// cache individual tree node to identify the each node
		if(xmlNode.getCacheTreeNode().get(nodePath)==null) xmlNode.getCacheTreeNode().put(nodePath, xmlNode);
		// cache all nodes and store in node root. So we can use it to find all master nodes that has been mapped by the path from mapping.
		if(rootNode==null) {
			if(xmlNode.getCacheAllTreeNodes().get(nodePath)==null) {
				List<XmlNode> cacheNodes = new ArrayList<XmlNode>();
				cacheNodes.add(xmlNode);
				xmlNode.getCacheAllTreeNodes().put(nodePath, cacheNodes);
			}else {
				xmlNode.getCacheAllTreeNodes().get(nodePath).add(xmlNode);
			}
		}else {
			if(rootNode.getCacheAllTreeNodes().get(nodePath)==null) {
				List<XmlNode> cacheNodes = new ArrayList<XmlNode>();
				cacheNodes.add(xmlNode);
				rootNode.getCacheAllTreeNodes().put(nodePath, cacheNodes);
			}else {
				rootNode.getCacheAllTreeNodes().get(nodePath).add(xmlNode);
			}
		}
	}
	
	protected boolean isUseObjectPrefix(Element node,boolean useObjPrefix){
		if(!useObjPrefix){
			List<Element> children = node.getChildren();
			for(Element e:children){	
				List<Element> eChildren = e.getChildren();
				if(eChildren==null ||eChildren.isEmpty()) {
					return true;
				}
			}
		}
		return useObjPrefix;
	}

	private static Map<String, String> converToMap(List<Attribute> attr) {
		Map<String, String> map = new HashMap<>();
		for(int i=0;i<attr.size();i++) {
			Attribute item = attr.get(i);		
			map.put(item.getName(), item.getValue());
		}
		return map;
	}
	
	public static void main(String[] args) throws Exception {
		new DynamicXmlReader().testReadXml();
	}

	private void testReadXml() {
		try {
			String filePath = "C:\\Users\\SOKDET\\Downloads\\XML Exports Oracle\\DKundeData.xml";
			//String filePathAttribute = "D:\\DATA\\Xmlfiles\\Inbound\\AccountAttribute.xml";
			//String filePathElement = "D:\\DATA\\Xmlfiles\\Inbound\\AccountElement.xml";
			InputStream input = new FileInputStream(filePath);
			Document doc = buildDoc(input);
			Element node = doc.getRootElement();
			XmlNode root = new XmlNode(false);
			List<List<IBean>> result = readOneNode(node,root,false,0, null).toFlat();
			System.out.println(result);
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
}
