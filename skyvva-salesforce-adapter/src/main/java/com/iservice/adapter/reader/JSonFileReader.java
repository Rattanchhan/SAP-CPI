package com.iservice.adapter.reader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.iservice.adapter.FileMapAdapter;
import com.iservice.model.IMessageTree;
import com.iservice.task.MapIntegrationInfo;
import com.iservice.xmlparser.XmlNode;

public class JSonFileReader extends AbstractFileReader {
	IMsgGeneratorV3 imgGeneratorV3;
	List<XmlNode> masterNodeMapped = new ArrayList<XmlNode>();
	
	public JSonFileReader(FileMapAdapter adapter,MapIntegrationInfo integrationInfo) throws Exception {
		super(adapter,integrationInfo);
		imgGeneratorV3 = new JsonToImgV3(integrationInfo, integrationInfo.getInputData());
	}

	@Override
	public boolean doProceFile(File f,IProcessRecord processor, boolean isReadOnly)throws Exception {
		try{
			doReadJson(f,processor,false);
			return true;
		}catch(CancelException e){
			return false;
		}
	}

	public void doReadJson(File f,IProcessRecord processer,boolean count) throws Exception, IOException {
		try {
			if(masterNodeMapped.size()==0 && count) {

				masterNodeMapped = imgGeneratorV3.getAllRootNode();
				
			}else {
				for(XmlNode masterNode : masterNodeMapped) {
					List<XmlNode> oneNode = new ArrayList<XmlNode>();
					oneNode.add(masterNode);
					List<IMessageTree> msgTree =  imgGeneratorV3.generateIMessage(oneNode);
					processer.doProcessV3(msgTree, oneNode.size(), null);
				}
			}
		} catch (Throwable ex){
			ex = FileMapAdapter.fileNotFound(ex, adapter);
			throw new Exception(ex);
		}
	}
	
	public static class CancelException extends RuntimeException{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public CancelException() {
			super();
		
		}

		public CancelException(String message, Throwable cause,
				boolean enableSuppression, boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
			
		}

		public CancelException(String message, Throwable cause) {
			super(message, cause);
			
		}

		public CancelException(String message) {
			super(message);
			
		}

		public CancelException(Throwable cause) {
			super(cause);
			
		}
		
	}

	@Override
	protected long doCountRecord(File f) throws Exception {
		doReadJson(f,null, true);
		if(masterNodeMapped.size()>0) {
			return masterNodeMapped.size();
		}else {
			return 0;
		}
	}

}
