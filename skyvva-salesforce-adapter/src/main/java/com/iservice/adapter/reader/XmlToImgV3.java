package com.iservice.adapter.reader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import com.iservice.task.MapIntegrationInfo;
import com.iservice.xmlparser.DynamicXmlReader;
import com.iservice.xmlparser.XmlNode;
import com.sforce.soap.schemas._class.IServices.IBean;

public class XmlToImgV3 extends IMsgGeneratorV3 {
	
	InputStream inputData;
	
	public XmlToImgV3() {}

	public XmlToImgV3(MapIntegrationInfo mapIntegrationInfo) throws Exception {
		super(mapIntegrationInfo);
	}
	
	public XmlToImgV3(MapIntegrationInfo mapIntegrationInfo, InputStream inputData) throws Exception {
		super(mapIntegrationInfo);
		this.inputData = inputData;
	}

	@Override
	public List<XmlNode> getAllRootNode() throws Exception {
		List<XmlNode> lstNode = new ArrayList<>();
		try {
			Document doc = buildDoc(inputData);
			Element rootElement = doc.getRootElement();
			XmlNode rootNode = new XmlNode(false);
			
			rootNode = new DynamicXmlReader().readOneNode2(rootElement, rootNode, false, 0, null);
			
			// get the master nodes which has been mapped 
			lstNode.addAll(getMasterNodeMapped(rootNode));
			
		} catch (Throwable ex) {
			throw new Exception("Error occurred during fetching all root nodes: " + ex.getMessage(), ex);
		}
		return lstNode;
	}
	
	protected Document buildDoc(InputStream input) throws Exception {
		SAXBuilder builder = new SAXBuilder();
		Document doc = null;
		doc = builder.build(input);
		return doc;
	}

	@Override
	public XmlNode getAllRootNode(List<IBean> lsIBean) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
