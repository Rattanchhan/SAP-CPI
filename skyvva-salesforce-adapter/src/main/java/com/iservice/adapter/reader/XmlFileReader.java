package com.iservice.adapter.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.iservice.adapter.FileMapAdapter;
import com.iservice.task.MapIntegrationInfo;
import com.iservice.xmlparser.DynamicXmlReader;
import com.sforce.soap.schemas._class.IServices.IBean;

public class XmlFileReader extends AbstractFileReader {
	List<List<IBean>> records = null;

	public XmlFileReader(FileMapAdapter adapter,MapIntegrationInfo integrationInfo) {
		super(adapter,integrationInfo);
	}

	protected List<List<IBean>> doReadXml(File f) throws Exception{
		InputStream input = null;
		try {
			
			input = integrationInfo.getInputData();
			if(records == null) {
				records = new DynamicXmlReader(super.adapter.getIsAddPrefix()).read(input);
			}
			return records;
		} catch (Throwable ex) {
			ex = FileMapAdapter.fileNotFound(ex,adapter);
			throw new Exception(ex);
		} finally {
			if(input!=null) {
				input.close();
				input=null;
			}
		}
		
	}
	
	protected List<List<IBean>> doReadXml(InputStream input) throws Exception{
		try {
			List<List<IBean>> records = new DynamicXmlReader(super.adapter.getIsAddPrefix()).read(input);
			return records;
			
		} catch (Throwable ex) {
			ex = FileMapAdapter.fileNotFound(ex,adapter);
			throw new Exception(ex);
		} finally {
			if(input!=null) {
				input.close();
				input=null;
			}
		}
		
	}
	
	@Override
	public boolean doProceFile(File f,IProcessRecord processor, boolean isReadOnly) throws Exception{
		
		try {
			
			List<List<IBean>> records = doReadXml(f);
			if(!processor.doMassProcess(records)){
				return false;
			}
		}catch(Throwable ex) {
			ex = FileMapAdapter.fileNotFound(ex,adapter);
			throw new Exception(ex);
		}
		return true;
		
	}

	@Override
	protected long doCountRecord(File f)throws Exception {
		List<List<IBean>> records = doReadXml(f);
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

	

}
