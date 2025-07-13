package com.iservice.adapter.reader;

import java.io.File;
import java.util.List;

import com.iservice.adapter.FileMapAdapter;
import com.iservice.task.MapIntegrationInfo;
import com.sforce.soap.schemas._class.IServices.IBean;

public class ContentFileReader extends AbstractFileReader {
	
	public ContentFileReader(FileMapAdapter adapter, MapIntegrationInfo integrationInfo) {
		super(adapter, integrationInfo);
	}

	@Override
	protected long doCountRecord(File f) {
		if (f != null && f.exists()) {
			return 1;
		}
		return 0;
	}
	
	@Override
	protected boolean processFile(String fn,IProcessRecord proceser, boolean isReadOnly) throws Exception{
		boolean result = doProceFile(this.adapter.getFile(fn),proceser, false);
		// file integration lock after read process
		this.adapter.lockFile(fn);
		return result;
	}
	
	@Override
	public boolean doProceFile(File f,IProcessRecord processor, boolean isReadOnly) throws Exception {
		List<IBean> oneRecord = readContentFile(f);
		return processor.doProcess(oneRecord);
	}
	protected int getMaxTestRecord(){
		return 1;
	}
	
}
