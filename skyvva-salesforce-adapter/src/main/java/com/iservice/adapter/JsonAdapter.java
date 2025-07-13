package com.iservice.adapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.iservice.adapter.batch.BatchExecutorResponseItem;
import com.iservice.adapter.batch.IBatchExecutor;
import com.iservice.adapter.reader.IRecordReader;
import com.iservice.adapter.reader.JSonFileReader;
import com.iservice.gui.data.ISFIntegrationObject;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.gui.helper.Helper;
import com.iservice.sforce.SFIntegrationService;
import com.iservice.task.MapIntegrationInfo;
import com.sforce.soap.schemas._class.IServices.IBean;

public class JsonAdapter extends FileMapAdapter {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(JsonAdapter.class);
	
	public JsonAdapter() {
		super();
	}

	
	@Override
	public List<BatchExecutorResponseItem> updateV3(SFIntegrationService integrationService, JSONArray payloadRecord, Interfaces__c intf)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public List<BatchExecutorResponseItem> updateChainData(SFIntegrationService integrationService, Interfaces__c intff, IBean[][] records) throws Exception {
		return null;
	}
	@Override
	public List<BatchExecutorResponseItem> update2(SFIntegrationService integrationService, Interfaces__c intff,
			IBean[][] records) throws Exception {
		return null;
	}

	@Override
	public List<BatchExecutorResponseItem> update(IBean[][] beans, String interfaceType, SFIntegrationService integrationService, Interfaces__c intff) throws Exception {

		
		if (beans == null || beans.length < 0)
			throw new Exception("JsonAdapter> update> No records available for an update or create process in the source system.");
		
		if(folder==null || folder.equals("")  || targetFileName==null || targetFileName.trim().equals("") ) {
			setConnectionInfo(map());
		}
		
		List<BatchExecutorResponseItem> lstMsg = new ArrayList<BatchExecutorResponseItem>();
		String msgStatus = "Completed", msg = "Creation of a new record.";
		
		if (!folder.trim().equals("") && !targetFileName.equals("")) {
			try {
				
				saveToJsonFile(beans, folder, targetFileName);
				
				msgStatus = "Completed";
				msg = "Creation of a new record.";
			} 
			catch (IOException ex) {
				LOGGER.error("> JsonAdapter: update()> ERROR> " + ex);
				msgStatus = "Failed";
				msg = ex.getLocalizedMessage();
			}
		}
		
		for (int i = 0; i < beans.length; i++) {
			BatchExecutorResponseItem bMsg = new BatchExecutorResponseItem(i, beans[i][0].getValue(), msgStatus, msg);
			if(msgStatus.equals("Completed") && intff!=null && intff.getResponseInterface__c()!=null) bMsg.setResponseRecords(beans[i]);
			lstMsg.add(bMsg);
		}
		
		return lstMsg;
	
	}
	
	@SuppressWarnings("unchecked")
	private File saveToJsonFile(IBean[][] beans, String folder, String targetFileName) throws Exception {
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		
		String filename = (targetFileName + formatter.format(Calendar.getInstance().getTime()) + ".json").trim();
		File file = new File(folder + Helper.getFileSeparator() + filename);
		
		OutputStream out1 = new FileOutputStream(file);
		OutputStreamWriter out = new OutputStreamWriter(out1,"UTF-8");
		
		JSONArray jsonArray = new JSONArray();
		
		for (int i = 0; i < beans.length; i++) {
			
			JSONObject field = new JSONObject();
			for (int j = 2; j < beans[i].length; j++) {
				field.put(beans[i][j].getName(), (beans[i][j].getValue()==null?"":new String(beans[i][j].getValue().getBytes("UTF-8"),"UTF-8")));
			}
			jsonArray.add(field);
		}
		String content = jsonArray.toString();
		
		out.write(content);
		out.close();
		return file;
	}

	@Override
	public int update(String expression) throws Exception {
		return 0;
	}

	

	@Override
	public List<List<String>> getColumns(String objectName, List<? extends ISFIntegrationObject> listHeader) throws Exception {
		// TODO 
		throw new Exception("JsonAdapter.getColumns not yet implements!");
	}

	@Override
	public List<List<String>> getRelationship(String objectName) throws Exception {
		return null;
	}

	

	@Override
	public void setExternalIdField(String extIdField) {}

	@Override
	public IBatchExecutor getBatchExecutor() throws Exception {
		return null;
	}

	@Override
	void validateProperties() throws Exception {}

	@Override
	public List<List<String>> getParameters(String objectName) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getExtenFilter() {
		
		return FileMapAdapter.getFileExtensions(FileTypeHelper.JSON);
	}

	@Override
	protected IRecordReader createRecordReader(
			MapIntegrationInfo integrationInfo) throws Exception {
		
		return new JSonFileReader(this, integrationInfo);
	}

}
