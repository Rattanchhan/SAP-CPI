package com.iservice.adapter.reader;

//import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.iservice.adapter.MapAdapter;
//import com.iservice.gui.controller.IntegrationDetailsController;
import com.iservice.gui.data.ISFIntegrationObject;
import com.iservice.gui.data.IStructure__c;
import com.iservice.gui.data.MsgTypeFieldEntry__c;
import com.iservice.model.IMessageTree;
import com.iservice.task.AbstractIntegrationEvent;
import com.iservice.task.GenericSFTask;
import com.iservice.task.MapIntegrationInfo;
import com.iservice.xmlparser.XmlNode;
import com.sforce.soap.schemas._class.IServices.IBean;

public abstract class AbstractRecordReader<A extends MapAdapter> implements IRecordReader{

	public final static int TEST_QUERY_MAX_SIZE =50;
	protected A adapter;
	protected AbstractIntegrationEvent integrationEvent;
	protected MapIntegrationInfo integrationInfo;
	protected long totalRecord;
	IMsgGeneratorV3 imgGeneratorV3 = null;
	public AbstractRecordReader(A adapter, MapIntegrationInfo integrationInfo) {
		this.adapter = adapter;
		this.integrationInfo = integrationInfo;
	}
	protected List<? extends ISFIntegrationObject> getHeaders() throws Exception {
		if(integrationInfo!=null){
			return integrationInfo.getListHeader(adapter.getAdapter());
		}
		return null;
	}
	protected int getMaxTestRecord(){
		return TEST_QUERY_MAX_SIZE;
	}
	protected List<String> getOrderHeader() throws Exception {
		List<String> colOrder = new ArrayList<String>();
		List<? extends ISFIntegrationObject> listHeader = getHeaders();
		if (listHeader != null && !listHeader.isEmpty()) {
			listHeader.sort((o1, o2) -> {
				if (o1 instanceof IStructure__c && o2 instanceof IStructure__c) {
					IStructure__c s1 = (IStructure__c) o1, s2 = (IStructure__c) o2;
					if (s1.getSequence__c() == null || s2.getSequence__c() == null) return 0;
					return s1.getSequence__c().compareToIgnoreCase(s2.getSequence__c());
				} else {
					MsgTypeFieldEntry__c s1 = (MsgTypeFieldEntry__c) o1, s2 = (MsgTypeFieldEntry__c) o2;
					if (s1.getSequence__c() == null || s2.getSequence__c() == null) return 0;
					return s1.getSequence__c().compareToIgnoreCase(s2.getSequence__c());
				}
			});
			// define length for create header from iStructures
			// create header
			// IStructure
			if (listHeader.get(0) instanceof IStructure__c) {
				for (ISFIntegrationObject iStructure : listHeader) {
					IStructure__c st =(IStructure__c)iStructure;
					colOrder.add(st.getName());
				}
			} else {// MessageType
				for (ISFIntegrationObject messageType : listHeader) {
					colOrder.add(((IStructure__c) messageType
							.get("IStructure__c")).getName().toString());
				}
			}
		}
		return colOrder ;
	}
	protected abstract boolean process(String criteria)throws Exception;
	protected void onFinish(){
		//nothing to do,maybe subclass need to do something after integration
	}
	public boolean doIntegration(String criteria) throws Exception{
		try{
			totalRecord = countRecord(criteria, false);

			//create integration event first
			createIntegrationEvent();

//			if(totalRecord==0) {
//				LOGGER.trace(">doInBoundIntegration() >doIntegration() > has no records.");
//				IntegrationDetailsController.messageText = "Has no records to be integrate!";
//				return false;
//			}

			boolean status = process(criteria);

			//do sent latest package to SF if no error
			status = integrationEvent.doIntegration(false);

			return status;
		}catch(Throwable e){
			if(integrationEvent!=null){
				integrationEvent.setError(true);
			}
			throw e;

		}finally{
			if(integrationEvent!=null){
				integrationEvent.done();
			}
			onFinish();
		}
	}
	protected void createIntegrationEvent() throws Exception {
		if(this.integrationEvent==null){
			//may some event need reader
			this.integrationEvent = this.adapter.createIntegrationEvent(totalRecord, integrationInfo,this);
		}
	}
	public abstract long countRecord(String criteria, boolean isTestQuery) throws Exception;
	public long getTotalRecord() {
		return totalRecord;
	}

	protected boolean isForceStop(){
		return adapter.isForceStop();
	}

	@Override
	public boolean doTestIntegration(String criteria) throws Exception {
		QueryResult result = doTestQuery(criteria, false);
		//create integration event first
		if(this.integrationEvent==null){
			//may some event need reader
			this.integrationEvent = this.adapter.createIntegrationEvent(result.getResult().size(), integrationInfo,this);
		}
		return this.integrationEvent.doTestIntegration(result.getResult());
	}

	protected IProcessRecord getProcessRecord(){
		return new DefaultProcessRecord<A>(this);
	}

	public static interface IProcessRecord{
		boolean doProcess(List<IBean> bean) throws Exception;
		boolean doMassProcess(List<List<IBean>> beans) throws Exception;
		boolean doProcessV3(List<IMessageTree> iMessages, int numberOfTree, List<List<IBean>> beans) throws Exception;
	}

	public static class DefaultProcessRecord<A extends MapAdapter> implements IProcessRecord{
		protected AbstractRecordReader<A> reader ;
		public DefaultProcessRecord(AbstractRecordReader<A> reader){
			this.reader = reader;
		}
		@Override
		public boolean doProcess(List<IBean> bean) throws Exception {
			return this.reader.integrationEvent.processRecord(bean);
		}
		@Override
		public boolean doMassProcess(List<List<IBean>> beans) throws Exception {
			return this.reader.integrationEvent.processRecords(beans);
		}
		@Override
		public boolean doProcessV3(List<IMessageTree> iMessages, int numberOfTree, List<List<IBean>> beans) throws Exception {
			return this.reader.integrationEvent.processIMessagesV3(iMessages, numberOfTree);
		}

	}
	
	@SuppressWarnings("null")
	protected boolean processV3__IBean(IProcessRecord processor, List<IBean> rec) throws Exception {
		if(rec == null && rec.size()==0) return false;
		if(imgGeneratorV3 == null) {
			imgGeneratorV3 = new IBeanToImsgV3(null, integrationInfo);
		}
		List<XmlNode> oneNode = new ArrayList<XmlNode>();
		oneNode.add(imgGeneratorV3.getAllRootNode(rec));
		List<IMessageTree> msgTree =  imgGeneratorV3.generateIMessage(oneNode);
		if(!processor.doProcessV3(msgTree, oneNode.size(), null)) return false;	
		return true;
	}
	
	public MapIntegrationInfo getIntegrationInfo() {
		return integrationInfo;
	}
}
