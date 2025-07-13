package com.iservice.adapter.reader;

import java.io.File;
import java.util.List;

import com.iservice.task.MapIntegrationInfo;
import com.iservice.xmlparser.XmlField;
import com.iservice.xmlparser.XmlNode;
import com.sforce.soap.schemas._class.IServices.IBean;

public class IBeanToImsgV3 extends IMsgGeneratorV3{

	public IBeanToImsgV3(File file, MapIntegrationInfo mapIntegrationInfo) throws Exception {
		super(mapIntegrationInfo);
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<XmlNode> getAllRootNode() throws Exception {
		return null;
	}

	@Override
	public XmlNode getAllRootNode(List<IBean> lsIBean) throws Exception {
		XmlNode xmlNode = new XmlNode(false);
		xmlNode.setName(dtoInterfaceStructure.getRootSourceObject());
		for(IBean tmp:lsIBean) {
			XmlField oneField = new XmlField(tmp.getName(), tmp.getValue());
			xmlNode.getFields().add(oneField);
		}
		return xmlNode;
	}

}
