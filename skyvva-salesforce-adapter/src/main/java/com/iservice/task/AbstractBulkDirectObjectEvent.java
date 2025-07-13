package com.iservice.task;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.lang3.StringUtils;

import com.Ostermiller.util.ExcelCSVPrinter;
import com.iservice.bulk.jobinfo.IJobInfo;
import com.iservice.database.BaseManager;
import com.iservice.database.CacheSFBulkBatchFile;
import com.iservice.database.CacheSFBulkFolder;
import com.iservice.database.SFProcessingJobDao;
import com.iservice.gui.data.IMapping__c;
import com.iservice.gui.helper.Helper;
import com.iservice.model.ProcessingJobInfo;
import com.iservice.model.ProcessingJobInfo.TJOBType;
import com.iservice.model.ProcessingJobInfo.TStatus;
import com.sforce.soap.schemas._class.IServices.IBean;

/**
 *Bulk direct object from agent to salesforce we convert all format(xml,json,...) to csv,
 *Because csv easy support bulk v1 and v2 
 *
 */
public abstract class AbstractBulkDirectObjectEvent<J extends IJobInfo> extends AbstractIntegrationEvent {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(AbstractBulkDirectObjectEvent.class);

	protected J job;
	protected int packageWriteToFile=4000;
	protected ExcelCSVPrinter csvWriter;
	protected ByteArrayOutputStream csvContent;
	protected Map<String, String> source2Target;
	protected List<String> colSourceOrders;	
	protected int recordBatchCount=0;
	protected int batchCount=0;
	protected TBulkVersion bulkVersion;
	protected ProcessingJobInfo info;
	
	public AbstractBulkDirectObjectEvent(long totalRecord,
			MapIntegrationInfo intInfo, TBulkVersion bulkVersion) {
		super(totalRecord, intInfo);
		this.bulkVersion = bulkVersion;
		
	}
	
	/**
	 * retrieve and convert IMapping__c object to map
	 * @return map key is csv's header and value is salesforce fields
	 * @throws Exception
	 */
	protected Map<String, String> getMapSource2Target() throws Exception{
		if(source2Target==null){
			source2Target = new HashMap<String, String>();
			colSourceOrders = new ArrayList<String>();
			List<IMapping__c> mappings = getMapping();
			if(mappings!=null){
				for(IMapping__c map:mappings){
					if(StringUtils.equals(map.getType__c(), "Structure")) continue;
					
					colSourceOrders.add(map.getSource__c());
					source2Target.put(map.getSource__c(), map.getTarget__c());
				}
			}
		}
		return source2Target;
	}

	/**
	 * use for upsert operation only
	 * @return field external id
	 * @throws Exception
	 */
	protected String getExternalFieldId() throws Exception{
		if(mapIntegrationInfo.getInterfaces().isExternalMapping()){			
			//when interface use external mapping then external id get from interface
			return mapIntegrationInfo.getInterfaces().getSObjectFieldExtId__c();
		}
		List<IMapping__c> mappings = getMapping();
		if(mappings!=null){
			for(IMapping__c map:mappings){
				if(map.isExternalId()){
					return map.getTarget__c();
				}
			}
		}
		//default external id when no mapping
		return "Id";
	}
	/**
	 * convert List IBean to map
	 * @param rec
	 * @return map key is the ibean.name and value is the ibean.value 
	 */
	protected Map<String, String> convertListIbean2Map(Collection<IBean> rec){
		Map<String, String> map = new HashMap<String, String>();
		if(rec!=null){
			for(IBean b:rec){
				if(b!=null){
					map.put(b.getName(), b.getValue());
				}
			}
		}
		return map;
	}
	/**
	 * convert list ibean to csv row
	 * @param rec
	 * @return
	 * @throws Exception
	 */
	protected String[] toCsvRow(List<IBean> rec) throws Exception{
		List<String> csvVals = new ArrayList<String>();
		Map<String, String> mapValue = convertListIbean2Map(rec);
		//all mapping field must be write into csv
		for(String key:this.colSourceOrders){
			csvVals.add(StringUtils.trimToEmpty(mapValue.get(key)));
		}
		return csvVals.toArray(new String[csvVals.size()]);
	}
	
	
	protected boolean isCreateAllMessage(){
		return !mapIntegrationInfo.getInterfaces().isIsNotPersistMessage__c();
	}
	
	/**
	 * all records must have the same fields order
	 */
	
	/**
	 * retrieve csv fields
	 * @param rec
	 * @return String[] salesforce field for csv header
	 * @throws Exception
	 */
	protected String[] getCsvHeader(List<IBean> rec) throws Exception{
		Map<String, String> source2Target = getMapSource2Target();
		if(source2Target==null||source2Target.isEmpty()){
			//no mapping then use external mapping 
			source2Target = new HashMap<String, String>();
			colSourceOrders = new ArrayList<String>();
			for(int i=0;i<rec.size();i++){
				IBean bean = rec.get(i);		
				colSourceOrders.add(bean.getName());
				source2Target.put(bean.getName(), bean.getName());
			}
			this.source2Target = source2Target;
		}
		
		
		List<String> csvVals = new ArrayList<String>();
		for(String header:colSourceOrders){
			csvVals.add(source2Target.get(header));
		}
		return  csvVals.toArray(new String[csvVals.size()]);
	}
	/**
	 * create job on salesforce
	 * @throws Exception
	 */
	protected final J doCreateJob()throws Exception{
		if(job==null){
			try {
				job = createJob();
			}catch(Exception ex) {
				if(isSessionExpired(ex)) {
					reLoginSF();
					job = createJob();
				}else {
					throw ex;
				}
			}
            LOGGER.trace("Created job {}: ", job);
			//save job to db
			this.info = new ProcessingJobInfo();
			info.setJobid(job.getId());
			info.setIntegrationId(getIntegrationInfo().getIntegrationId());
			info.setInterfaceId(getIntegrationInfo().getInterfaceId());
			//save agent name
			info.setBulkversion(bulkVersion);
			info.setExternalFieldId(getExternalFieldId());
			info.setStatus(TStatus.OK);
			info.setJobType(TJOBType.SOBJECT);
			info.setSalesforce_user(getIntegrationInfo().getSFIntegrationService().getConnectionInfo().getUsername());
			BaseManager.getDao(SFProcessingJobDao.class).upsert(info);
            CheckSFProcessingJob.notifyAboutNewJobToCheck();
		}
		return job;
	}
	/**
	 * create new batch's content
	 * @throws Exception
	 */
	protected void createNewWriter() throws Exception{
		csvContent = new ByteArrayOutputStream();
		csvWriter = new ExcelCSVPrinter(new OutputStreamWriter(csvContent,"UTF-8"));
		csvWriter.setLineEnding("\r\n"); // TODO is it a common rule that CSV must have CRLF line ending on all OS?
		String[] headers = getCsvHeader(currentPackage.get(0));
		csvWriter.writeln(headers);
	}
	/**
	 * create new batch
	 * @param forceCreateBatch if true then sent create batch on salesforce else then write only to memory
	 * @throws Exception
	 */
	protected void createBatch(boolean forceCreateBatch) throws Exception{
		if(job==null){
			doCreateJob();
		}
		if(currentPackage!=null && !currentPackage.isEmpty()){
			if(csvWriter ==null){
				createNewWriter();				
			}
			//write data to csv
			List<IBean> rec = currentPackage.get(0);
			recordBatchCount++;
			
			String[] row = toCsvRow(rec);
			if(!isCanWriteToCurrentContent(row)){
				//create batch
				doCreateBatch(true);
				//create new content batch
				createNewWriter();
			}
			
			csvWriter.writeln(row);
		}
		doCreateBatch(forceCreateBatch);
		
	}
	
	
	
		
	
	
	
	
	
	/**
	 * check the batch size before appen new row
	 * @param row
	 * @return true if the csvcontent not exceed
	 */
	protected boolean isCanWriteToCurrentContent(String[] row){
		int length =0;
		for(String v:row){
			length+=v.getBytes().length;
		}
		int lengthAsMB = length/(1024*1024);
		int total = lengthAsMB+getContentLengthAsMB();
		if(total>getMaxContentLength()){
			return false;
		}
		
		return true;
		
	}
	/**
	 * generate file name for job
	 * @return batch filename
	 */
	protected String getBatchFileName(){
		return "batch_"+batchCount+".csv";
	}
	/**
	 * create new batch on salesforce
	 * @param forceCreateBatch
	 * @throws Exception
	 */
	protected boolean doCreateBatch(boolean forceCreateBatch)throws Exception{
		if(forceCreateBatch||isMaxLenght()){
			//close the writer before send to salesforce
			csvWriter.close();
			byte[] csvContent = getCSVContent();
			batchCount++;
			
			//start send content to salesforce
			try {
				createBatch(job,csvContent);
			}catch(Exception ex) {
				if(isSessionExpired(ex)) {
					reLoginSF();
					createBatch(job,csvContent);
				}else {
					throw ex;
				}
			}
			
			//cache csv content after created batch success
			CacheSFBulkFolder cacheSFBulkFolder = new CacheSFBulkFolder();
			cacheSFBulkFolder.setCsvContent(this.csvContent);
			CacheSFBulkBatchFile.getInstance().getCacheFolderJob().put(info.getJobFolder(), cacheSFBulkFolder);
			
			//reset content after create batch on salesforce
			initWriter();
			return true;
		}
		return false;
	}
	/**
	 *
	 * @return byte[] conent of csv as byte array
	 */
	protected byte[] getCSVContent(){
		return csvContent.toByteArray();
	}
	
	/**
	 * check limit for batch
	 * @return true when the batch's content is exceeded
	 */
	protected boolean isMaxLenght() {
		if(csvContent!=null){
			int csvContentMB = getContentLengthAsMB();			
			return  csvContentMB>=getMaxContentLength();
		}
		return false;
	}
	/**
	 * start process record from orignal file
	 */
	@Override
	public boolean doIntegration(boolean checkPackage) throws Exception {
		if(preProcess>=packageWriteToFile||!checkPackage){
			onInProgressProcess(preProcess);
		}
		//sent info back to caller
		preProcess+=currentPackage.size();
		
		this.createBatch(!checkPackage);
		//reset package
		currentPackage = new ArrayList<List<IBean>>();
		return true;
	}
	/**
	 * 
	 * @return max content length of csv for bulk v1 or v2
	 */
	protected abstract int getMaxContentLength();
	/**
	 * 
	 * @return size of csv content as MB
	 */
	protected int getContentLengthAsMB(){
		if(csvContent!=null){
			return csvContent.size()/(1024*1024);
		}
		return 0;
	}
	
	protected abstract  J createJob() throws Exception;
	protected abstract void createBatch(J job,byte[] content)throws Exception;
	protected abstract void closeJob(J job) throws Exception;
	protected abstract void cancelJob(J job) throws Exception;
	
	/**
	 * reset batch's conent
	 */
	protected void initWriter(){
		csvContent = null;
		csvWriter=null;
		recordBatchCount=0;
	}
	
	@Override
	public void done() {
		updateJobStatus(job);
	}

	protected void updateJobStatus(J job) {
		try {
			if(job!=null){
				if( !isError()){
				//close the job on salesforce if there is no error
				try {
					closeJob(job);
				}catch(Exception ex) {
					if(isSessionExpired(ex)) {
						reLoginSF();
						closeJob(job);
					}else {
						throw ex;
					}
				}
				info.setStatus(TStatus.CREATE_IMESSAGE);
				SFProcessingJobDao.getDao(SFProcessingJobDao.class).update(info);
			}else{
				//abort the job when has any error, because can do with the old file
				try {
					cancelJob(job);
				}catch(Exception ex) {
					if(isSessionExpired(ex)) {
						reLoginSF();
						cancelJob(job);
					}else {
						throw ex;
					}
				}
				//when cancel job success then delete the job folder
				Helper.deleteJobFolder(info);
			}
		 }
		} catch (Exception e) {
			//close job failed
			LOGGER.warn(e.getMessage(),e);
			if(isError()){
				//we will do abort the job later
				info.setStatus(TStatus.ERR);
			}else{
				//we do close the job later when we cannot close it
				info.setStatus(TStatus.OK);
			}
		}
	}
	
	protected void createIMessageAndIDataCsv(){
		
	}
	
	
	public static enum TBulkVersion{
		SKYVVA("SKYVVA Bulk 1.0"),SFV1("SFDC Bulk API 1.0"),SFV2("SFDC Bulk API 2.0");
		
		private String sfApiName;
		private TBulkVersion(String sfApiName){
			this.sfApiName = sfApiName;
		}
		public String getSkyvvaName(){
			return sfApiName;
		}
		@Override
		public String toString() {
			return name();
		}
		
		public static TBulkVersion getByApiName(String apiName){
			for(TBulkVersion tb:values()){
				if(StringUtils.equalsIgnoreCase(apiName, tb.sfApiName)){
					return tb;
				}
			}
			//default is skyvva
			return SKYVVA;
		}
	}
	
}
