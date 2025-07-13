package com.iservice.adapter.reader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.iservice.task.MapIntegrationInfo;
import com.iservice.xmlparser.DynamicXmlReader;
import com.iservice.xmlparser.XmlField;
import com.iservice.xmlparser.XmlNode;
import com.sforce.soap.schemas._class.IServices.IBean;

public class JsonToImgV3 extends IMsgGeneratorV3{
	InputStream inputData;
	XmlNode root = new XmlNode(false);
	
	public JsonToImgV3(MapIntegrationInfo mapIntegrationInfo, InputStream inputData) throws Exception {
		super(mapIntegrationInfo);
		this.inputData = inputData;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<XmlNode> getAllRootNode() throws Exception {
		InputStreamReader streamReader = new InputStreamReader(inputData,"UTF-8");
		Object obj = new JSONParser().parse(streamReader);
		root = new XmlNode(false);
		root = new XmlNode(false);
		root.setName(StringUtils.toRootUpperCase(this.dtoInterfaceStructure.getRootSourceObject()));
		root.isRoot(true);

		DynamicXmlReader.doCachingTreeNode(root, root.getName(), null);
		
		if(obj instanceof JSONArray) {
			// remove root need from cache when json start with array
			if(root.getCacheAllTreeNodes().get(root.getName())!=null) {
				root.getCacheAllTreeNodes().get(root.getName()).remove(0);
			}
			
			buildNode(obj, root, root.getName(), null);	
			
		}else if(obj instanceof JSONObject){
			JSONObject jsonObj = (JSONObject) obj;
			for(String key : (Set<String>)jsonObj.keySet()) {
				if(jsonObj.get(key) instanceof JSONObject || jsonObj.get(key) instanceof JSONArray) {
					buildNode(jsonObj.get(key), root, key, root.getName());
				}else {
					root.addField(new XmlField(key, jsonObj.get(key).toString()));
				}
			}
		}else {
			throw new Exception("Unable to read json");
		}	
		return getMasterNodeMapped(root);
	}
	
	void buildNode(Object currentJsonNode, XmlNode parentNode, String nodeName, String nodePath) {
		if(currentJsonNode instanceof JSONArray) {
			JSONArray jsonNodes = (JSONArray) currentJsonNode;
			for(Object jsonNode: jsonNodes) {
				checkJsonNode(parentNode, (JSONObject) jsonNode, nodeName, nodePath);
			}
		}else if(currentJsonNode instanceof JSONObject){
			checkJsonNode(parentNode, (JSONObject) currentJsonNode, nodeName, nodePath);
		}
	}
	
	@SuppressWarnings("unchecked")
	void checkJsonNode(XmlNode parentNode, JSONObject jsonNode, String nodeName, String nodePath) {
		XmlNode childNode = new XmlNode(false);
		childNode.setName(nodeName);
		// cache node
		nodePath = nodePath==null?nodeName.toUpperCase():nodePath+"."+nodeName.toUpperCase();
		DynamicXmlReader.doCachingTreeNode(childNode, nodePath, root);
		
		parentNode.addChild(childNode);
		for(String key : (Set<String>)jsonNode.keySet()) {
			checkNodeStructur(childNode, jsonNode, key, nodePath);
		}
	}
	
	void checkNodeStructur(XmlNode childNode, JSONObject jsonNode, String key, String nodePath){
		if(jsonNode.get(key) instanceof JSONArray || jsonNode.get(key) instanceof JSONObject) {
			buildNode(jsonNode.get(key), childNode, key, nodePath);
		}else { // primitive type(String, Boolean, Double)
			childNode.addField(new XmlField(key, jsonNode.get(key).toString()));
		}
	}

	@Override
	public XmlNode getAllRootNode(List<IBean> lsIBean) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
