package com.iservice.adapter.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import com.iservice.adapter.FileMapAdapter;
import com.iservice.task.MapIntegrationInfo;
import com.sforce.soap.schemas._class.IServices.IBean;

public class RawFileReader extends AbstractFileReader{
	
	public RawFileReader(FileMapAdapter adapter, MapIntegrationInfo integrationInfo) throws Exception {
		super(adapter, integrationInfo);
	}

	@Override
	protected long doCountRecord(File f) throws Exception {
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
	public boolean doProceFile(File f, IProcessRecord processor, boolean isReadOnly) throws Exception {
		List<IBean> oneRecord = rawContentFile(f);
		return processor.doProcess(oneRecord);
	}
	
	private List<IBean> rawContentFile(File f) throws FileNotFoundException, IOException {
		List<IBean> oneRecord = new ArrayList<IBean>();
		oneRecord.add(new IBean(ATTACHMENT_FILE_NAME, f.getName()));
		oneRecord.add(new IBean(ATTACHMENT_FILE_TYPE, getContentType(f)));
		oneRecord.add(new IBean(ATTACHMENT_FILE_EXTENTION, getFileExtention(f)));
		oneRecord.add(new IBean(ATTACHMENT_FILE_LENGTH, f.length() + " B"));
		oneRecord.add(new IBean(ATTACHMENT_FILE_BODY, FileUtils.readFileToString(f, StandardCharsets.UTF_8)));
		return oneRecord;
	}

}
