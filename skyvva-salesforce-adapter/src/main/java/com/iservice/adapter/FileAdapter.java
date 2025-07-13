package com.iservice.adapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONArray;

import com.iservice.adapter.batch.BatchExecutorResponseItem;
import com.iservice.adapter.batch.IBatchExecutor;
import com.iservice.adapter.reader.ContentFileReader;
import com.iservice.adapter.reader.IRecordReader;
import com.iservice.adapter.reader.QueryResult;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.gui.helper.Helper;
import com.iservice.gui.helper.PropertyNameHelper;
import com.iservice.sforce.SFIntegrationService;
import com.iservice.task.AbstractIntegrationEvent;
import com.iservice.task.FileIntegrationEvent;
import com.iservice.task.GenericSFTask;
import com.iservice.task.MapIntegrationInfo;
import com.model.iservice.Property;
import com.sforce.soap.schemas._class.IServices.IBean;

public class FileAdapter extends FileMapAdapter {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(FileAdapter.class);

	public static final String ATTRIB_FILENAME = "NAME";
	public static final String ATTRIB_FILEBODY = "BODY";
	public static final String ATTRIB_FILE_PATHONCLIENT = "PATHONCLIENT";
	public static final String ATTRIB_FILE_VERSIONDATA = "VERSIONDATA";
	
	protected FileMapAdapter contentAdapter;

	public FileAdapter() {
	}

	/**
	 * This setter (filename and folder )is urgent use for new feature Task
	 * #3233: importing Content Documents and Attachments Need to update
	 * 
	 * @param filename
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	

	@Override
	public List<BatchExecutorResponseItem> update(IBean[][] records, String interfaceId, SFIntegrationService integrationService, Interfaces__c intff) throws Exception {

		List<BatchExecutorResponseItem> lstMsg = new ArrayList<BatchExecutorResponseItem>();

		if (records == null || records.length < 0) {
			throw new Exception("Warning! No records available!");
		}

		if (folder == null || folder.equals("")) {
			setConnectionInfo(map());
		}
		String msgStatus = "Completed", msg = "Creation of a file.";
		if (!folder.equals("")) {
			/*
			for (int i = 0; i < records.length; i++) {

				String msgStatus = "Completed", msg = "Creation of a file."; // default
																				// assume
																				// OK

				try {
					saveAFile(records[i], folder);
				} catch (Exception ex) {
					msgStatus = "Failed";
					msg = ex.getLocalizedMessage();
				}

				lstMsg.add(new BatchExecutorResponseItem(i, records[i][0]
						.getValue(), msgStatus, msg));
			}
			*/
			try {
				updateToFTPFile(records, null, folder, targetFileName, null, null, null);
			}
			catch(Exception ex) {
				LOGGER.error("> ExcelAdapter: update: ERROR> " + ex.toString(), ex);
				msgStatus = "Failed";
				msg = ex.getLocalizedMessage();
			}
			
			for (int i = 0; i < records.length; i++) {
				BatchExecutorResponseItem bMsg = new BatchExecutorResponseItem(i, records[i][0].getValue(), msgStatus, msg);
				if(msgStatus.equals("Completed") && intff!=null && intff.getResponseInterface__c()!=null) bMsg.setResponseRecords(records[i]);
				lstMsg.add(bMsg);
			}
		}
		
		return lstMsg;
	}

	public static List<String> getFileAttributes() {
		List<String> lstCol = new ArrayList<String>();
		lstCol.add(ATTRIB_FILENAME);
		lstCol.add(ATTRIB_FILEBODY);
		return lstCol;
	}

	public static void saveAFile(IBean[] recordFileData, String folder) throws Exception {
		Map<String, String> mapFileData = new HashMap<String, String>();
		// starting from the index 2 (0 & 1 are used to store other info :
		// externalId; messageInfo)
		for (int i = 2; i < recordFileData.length; i++) {
			if (recordFileData[i] != null
					&& recordFileData[i].getName() != null) {
				mapFileData.put(recordFileData[i].getName().toUpperCase(),
						recordFileData[i].getValue());
			}
		}
		String fileName = mapFileData.get(FileAdapter.ATTRIB_FILENAME);
		byte[] data = com.Ostermiller.util.Base64.decodeToBytes(mapFileData
				.get(FileAdapter.ATTRIB_FILEBODY));

		OutputStream stream = new FileOutputStream(folder
				+ Helper.getFileSeparator() + fileName);
		stream.write(data);
		stream.close();
	}
	
	@Override
	public List<File> updateToFTPFile(IBean[][] beans, List<List<IBean>> row, String folder, String targetFileName, SFIntegrationService integrationService, List<BatchExecutorResponseItem> lsMsg, Interfaces__c intf) throws Exception{
		List<File> lsFile = new ArrayList<File>();
		boolean hasVersionDataField=false;
		boolean hasPathOnClientField=false;
		boolean hasNameField=false;
		boolean hasBodyField=false;
		
		if(beans==null) return lsFile;
		//check if record data that has attachment fields
		for (int j = 2; j < beans[0].length; j++) {
			String upperCaseHeader = beans[0][j].getName().toUpperCase();
			if(upperCaseHeader.contains(FileAdapter.ATTRIB_FILE_VERSIONDATA)) {
				hasVersionDataField=true;
			}
			if(upperCaseHeader.contains(FileAdapter.ATTRIB_FILE_PATHONCLIENT)) {
				hasPathOnClientField=true;
			}
			if(upperCaseHeader.contains(FileAdapter.ATTRIB_FILENAME)) {
				hasNameField=true;
			}
			if(upperCaseHeader.contains(FileAdapter.ATTRIB_FILEBODY)) {
				hasBodyField=true;
			}
		}
		
		if((hasVersionDataField && hasPathOnClientField) || (hasNameField && hasBodyField)) {
			for(int index=0; index<beans.length; index++) {
				IBean[] recordFileData = beans[index];
				Map<String, String> mapFileData = new HashMap<String, String>();
				// starting from the index 2 (0 & 1 are used to store other info :
				// externalId; messageInfo)
				for (int i = 2; i < recordFileData.length; i++) {
					if (recordFileData[i] != null
							&& recordFileData[i].getName() != null) {
						mapFileData.put(recordFileData[i].getName().toUpperCase(),
								recordFileData[i].getValue());
					}
					String field = beans[index][i].getName().toUpperCase();
					if(field.contains(FileAdapter.ATTRIB_FILE_VERSIONDATA) || field.contains(FileAdapter.ATTRIB_FILEBODY)){
						beans[index][i].setValue("");
						//beans[index][i].setName("");
					}
				}
				String fileName = ""; 
				byte[] data = null;
				
				if(mapFileData.get(FileAdapter.ATTRIB_FILENAME)!=null && mapFileData.get(FileAdapter.ATTRIB_FILEBODY)!=null) {
					fileName = mapFileData.get(FileAdapter.ATTRIB_FILENAME);
					data = com.Ostermiller.util.Base64.decodeToBytes(mapFileData.get(FileAdapter.ATTRIB_FILEBODY));
				}
				else if(mapFileData.get("ATTACHMENT."+FileAdapter.ATTRIB_FILENAME)!=null && mapFileData.get("ATTACHMENT."+FileAdapter.ATTRIB_FILEBODY)!=null) {
					fileName = mapFileData.get("ATTACHMENT."+FileAdapter.ATTRIB_FILENAME);
					data = com.Ostermiller.util.Base64.decodeToBytes(mapFileData.get("ATTACHMENT."+FileAdapter.ATTRIB_FILEBODY));
				}
				else if(mapFileData.get(FileAdapter.ATTRIB_FILE_PATHONCLIENT)!=null && mapFileData.get(FileAdapter.ATTRIB_FILE_VERSIONDATA)!=null) {
					fileName = mapFileData.get(FileAdapter.ATTRIB_FILE_PATHONCLIENT);
					data = com.Ostermiller.util.Base64.decodeToBytes(mapFileData.get(FileAdapter.ATTRIB_FILE_VERSIONDATA));
				}
				else if(mapFileData.get("CONTENTDOCUMENTLINK."+FileAdapter.ATTRIB_FILE_PATHONCLIENT)!=null && mapFileData.get("CONTENTDOCUMENTLINK."+FileAdapter.ATTRIB_FILE_VERSIONDATA)!=null) {
					fileName = mapFileData.get("CONTENTDOCUMENTLINK."+FileAdapter.ATTRIB_FILE_PATHONCLIENT);
					data = com.Ostermiller.util.Base64.decodeToBytes(mapFileData.get("CONTENTDOCUMENTLINK."+FileAdapter.ATTRIB_FILE_VERSIONDATA));
				}
				
				if(!fileName.isEmpty() && data.length>0) {
					File file = new File(folder+"\\"+fileName);
					FileUtils.writeByteArrayToFile(file, data);
					lsFile.add(file);
				}
			}
		}
		else {
			return new ArrayList<File>();
		}
		if(lsMsg!=null) {
			new XmlAdapter().createIChainMessage(integrationService, lsMsg, intf, beans, null);
		}
		
		return lsFile;
	}

	@Override
	void validateProperties() throws Exception {}

	@Override
	public List<List<String>> getRelationship(String objectName) throws Exception {
		return null;
	}

	@Override
	public int update(String expression) throws Exception {
		return 0;
	}

	@Override
	public void setTableName(String tblName) {}

	@Override
	public void setExternalIdField(String extIdField) {}

	@Override
	public IBatchExecutor getBatchExecutor() throws Exception {
		return null;
	}

	public static void main(String[] args) {
		try {
			IBean[] recordFileData = new IBean[4];
			recordFileData[0] = new IBean("externalId", "externalId");
			recordFileData[1] = new IBean("messageId", "messageId");
			recordFileData[2] = new IBean(ATTRIB_FILENAME, "test.jpg");
			String dataTxt = "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";
			recordFileData[3] = new IBean(ATTRIB_FILEBODY, dataTxt);

			FileAdapter.saveAFile(recordFileData, "D:");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * <b>Not yet implement</b>
	 */
	@Override
	public List<BatchExecutorResponseItem> updateV3(SFIntegrationService integrationService, JSONArray payloadRecord, Interfaces__c intf) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public List<BatchExecutorResponseItem> updateChainData(SFIntegrationService integrationService, Interfaces__c intff, IBean[][] records) throws Exception {
		List<BatchExecutorResponseItem> lstMsg = new ArrayList<BatchExecutorResponseItem>();
		if (records == null || records.length < 0) {
			throw new Exception("Warning! No records available!");
		}
		if (folder == null || folder.equals("")) {
			setConnectionInfo(map());
		}
		if (!folder.equals("")) {
			try {
				updateToFTPFile(records, null, folder, targetFileName, integrationService, lstMsg, intff);
			} catch (Exception ex) {
				LOGGER.error(">FileAdapter()>updateChainData()> ERROR: " + ex);
				String msgStatus = "Failed", msg = ex.getLocalizedMessage();
				lstMsg = new ArrayList<BatchExecutorResponseItem>();
				for (int i = 0; i < records.length; i++) {
					lstMsg.add(new BatchExecutorResponseItem(i, records[i][0].getValue(), msgStatus, msg));
				}
			}
		}
		return lstMsg;
	}

	@Override
	public List<List<String>> getParameters(String objectName) throws Exception {
		return null;
	}

	@Override
	public List<BatchExecutorResponseItem> update2(SFIntegrationService integrationService, Interfaces__c intff,IBean[][] records) throws Exception {
		return null;
	}

	@Override
	protected String getExtenFilter() {
		return FileMapAdapter.getFileExtensions(FileTypeHelper.OTHER);
	}

	@Override
	public AbstractIntegrationEvent createIntegrationEvent(long totalRec, MapIntegrationInfo integrationInfo, IRecordReader reader) {
//		if (this.adapter.isAttOrContentVersionMode()) {
//			return new AttachmentAndContentVersionIntegration(totalRec, integrationInfo, getContentFileAdapter(), reader);
//		}
		return new FileIntegrationEvent(totalRec, integrationInfo);
	}
	
	@Override
	public QueryResult doTestQuery(String criteria, MapIntegrationInfo integrationInfo) throws Exception{
		String folder = mapProps.get(PropertyNameHelper.FOLDER);
		String binaryFolder = mapProps.get(PropertyNameHelper.BINARY_FOLDER);
		String origFolder = this.folder;
		String origFilename = this.filename;
		try{
			if(this.adapter.isAttOrContentVersionMode() && org.apache.commons.lang3.StringUtils.isNoneBlank(binaryFolder)) {
				this.filename = "";
				this.folder = binaryFolder;
				mapProps.put(PropertyNameHelper.FOLDER, binaryFolder);
			}
			IRecordReader reader = new ContentFileReader(this, integrationInfo);
			return reader.doTestQuery(criteria, true);
		}finally{
			this.folder = origFolder;
			this.filename = origFilename;
			mapProps.put(PropertyNameHelper.FOLDER, folder);
			System.gc();
			System.runFinalization();
		}
	}

	private FileMapAdapter getContentFileAdapter() {
		try {
			if (contentAdapter == null) {
				String adapterType = FileTypeHelper.CSV; // if there is no filename then read all csv file (default)
				String filename = this.getAdapter().getFileName();

				if (org.apache.commons.lang3.StringUtils.isNotBlank(filename)) {
					adapterType = FileTypeHelper.mapFileTypeExt
							.get(FilenameUtils.getExtension(filename));
				}
				if (adapterType.equalsIgnoreCase(FileTypeHelper.CSV))
					this.getAdapter()
							.getProperty()
							.add(new Property(PropertyNameHelper.SEPARATOR, ","));
				contentAdapter = (FileMapAdapter) GenericSFTask.createAdapter(adapterType, this.getAdapter().getProperty());
				contentAdapter.setParentAdapter(this);
			}
			return contentAdapter;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	protected IRecordReader createRecordReader(
			MapIntegrationInfo integrationInfo) throws Exception {
		if (this.adapter.isAttOrContentVersionMode()) {
			//we need to change adapter, because we need to read record first 
			return getContentFileAdapter().createRecordReader(integrationInfo);
		}
		return new ContentFileReader(this, integrationInfo);
	}
}
