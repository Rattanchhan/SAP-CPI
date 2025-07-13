package com.iservice.adapter.reader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.iservice.adapter.FileMapAdapter;
import com.iservice.model.IMessageTree;
import com.iservice.task.MapIntegrationInfo;
import com.iservice.xmlparser.XmlNode;
import com.sforce.soap.schemas._class.IServices.IBean;

public class XmlFileReaderV3 extends AbstractFileReader {
	
	XmlFileReader xmlFileReader = null;
	IMsgGeneratorV3 imgGeneratorV3 = null;
	List<XmlNode> allRootNode = new ArrayList<XmlNode>();

	public XmlFileReaderV3(FileMapAdapter adapter, MapIntegrationInfo integrationInfo) throws Exception {
		super(adapter, integrationInfo);
		xmlFileReader = new XmlFileReader(adapter, integrationInfo);
		imgGeneratorV3 = new XmlToImgV3(integrationInfo, this.getIntegrationInfo().getInputData());
	}

	@Override
	protected long doCountRecord(File f) throws Exception {
		allRootNode = imgGeneratorV3.getAllRootNode();
		return allRootNode.size();
	}
	
	protected long compareHeader(File f)throws Exception {
		List<List<IBean>> records = xmlFileReader.doReadXml(this.getIntegrationInfo().getInputData());
		if(records==null) return 0;
		for(List<IBean> bean:records){
			List<String> headers = new ArrayList<String>();
			for(IBean b:bean){
				headers.add(b.getName());
			}
			if(doCompareHeader(headers)) break;
		}
		
		return records.size();
	}

	@Override
	public boolean doProceFile(File f, IProcessRecord processor, boolean isReadOnly) throws Exception {
		try {
			if(isReadOnly) {
				// use this beans to display flat data on test query
				List<List<IBean>> beans = xmlFileReader.doReadXml(f);
				if(!processor.doProcessV3(null, 0, beans)) return false;			
			}else {
				
				if(allRootNode == null) {
					allRootNode = imgGeneratorV3.getAllRootNode();
				}
				
				for(XmlNode tmp: allRootNode) {
					List<XmlNode> oneNode = new ArrayList<XmlNode>();
					oneNode.add(tmp);
					List<IMessageTree> msgTree =  imgGeneratorV3.generateIMessage(oneNode);
					if(!processor.doProcessV3(msgTree, oneNode.size(), null)) return false;	
				}
			}
		}catch(Throwable ex) {
			ex = FileMapAdapter.fileNotFound(ex,adapter);
			throw new Exception(ex);
		}finally {
			
		}
		return true;
	}

}
