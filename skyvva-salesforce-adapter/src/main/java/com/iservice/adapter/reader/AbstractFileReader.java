package com.iservice.adapter.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.lang3.StringUtils;

import com.iservice.adapter.FileMapAdapter;
import com.iservice.gui.helper.Helper;
import com.iservice.model.IMessageTree;
import com.iservice.task.GenericSFTask;
import com.iservice.task.MapIntegrationInfo;
import com.sforce.soap.schemas._class.IServices.IBean;

public abstract class AbstractFileReader extends
		AbstractRecordReader<FileMapAdapter> {
	public final static String ATTACHMENT_FILE_NAME = "FILE_NAME";
	public final static String ATTACHMENT_FILE_TYPE = "FILE_TYPE";
	public final static String ATTACHMENT_FILE_EXTENTION = "FILE_EXTENTION";
	public final static String ATTACHMENT_FILE_PARENTID = "PARENTID";
	public final static String ATTACHMENT_FILE_BODY = "FILE_BODY";
	public final static String ATTACHMENT_FILE_LENGTH = "FILE_LENGTH";
	//will check different column in count record
	protected boolean differentCol=false;
	protected List<String> processFiles = new ArrayList<String>();
	protected List<String> currentFileHeader;
	public AbstractFileReader(FileMapAdapter adapter,
			MapIntegrationInfo integrationInfo) {
		super(adapter, integrationInfo);
	}
	
	protected List<IBean> readContentFile(File f) throws FileNotFoundException, IOException {
		List<IBean> oneRecord = new ArrayList<IBean>();
		oneRecord.add(new IBean(ATTACHMENT_FILE_NAME, f.getName()));
		oneRecord.add(new IBean(ATTACHMENT_FILE_TYPE, getContentType(f)));
		oneRecord.add(new IBean(ATTACHMENT_FILE_EXTENTION, getFileExtention(f)));
		oneRecord.add(new IBean(ATTACHMENT_FILE_LENGTH, f.length() + " B"));
		// oneRecord.add(new IBean(ATTACHMENT_FILE_PARENTID, null));
		InputStream is = new FileInputStream(f);
		byte[] body = new byte[(int) f.length()];
		is.read(body);
		is.close();
		oneRecord.add(new IBean(ATTACHMENT_FILE_BODY, com.Ostermiller.util.Base64.encodeToString(body)));
		return oneRecord;
	}

	public List<IBean> readContentFile(String folder,String fileName) throws Exception, IOException {
		processFiles.add(fileName);
		File aFile = new File(folder + Helper.getFileSeparator() + fileName);
		List<IBean> beans= readContentFile(aFile);
		//lock file after read
		this.adapter.lockFile(fileName,folder);
		return beans;
	}
	
	protected boolean processFile(String fn,IProcessRecord processer, boolean isReadOnly) throws Exception{
		//content file lock before read process
//		String processFile = this.adapter.lockFile(fn);
//		processFiles.add(fn);
		return doProceFile(null,processer, isReadOnly);
	}

	protected final boolean process(String criteria) throws Exception {
		if (this.adapter != null) {
			if(!processFile(null,getProcessRecord(), false)){
				return false;
			}
		}
		return true;
	}

	@Override
	public long countRecord(String criteria, boolean isTestQuery) throws Exception {
		return doCountRecord(null);
	}

	protected abstract long doCountRecord(File f) throws Exception;

	public abstract boolean doProceFile(File f,IProcessRecord processor, boolean isReadOnly) throws Exception;

	protected IBean createBean(String name, String value) {
		return new IBean(name, value);
	}

	public static String getContentType(File file) {
		String ct = URLConnection.guessContentTypeFromName(file
				.getAbsolutePath());
		if (ct == null)
			ct = new MimetypesFileTypeMap().getContentType(file);
		return ct;
	}

	public static String getFileExtention(File file) {
		int dot = file.getAbsolutePath().lastIndexOf(".");
		return dot > -1 ? file.getAbsolutePath().substring(dot + 1) : null;
	}
	
	protected  List<List<IBean>> query(String fn,int limit)throws Exception{
		final List<List<IBean>> result = new ArrayList<List<IBean>>();
		processFile(fn, new IProcessRecord(){
			@Override
			public boolean doProcess(List<IBean> bean) throws Exception {
				result.add(bean);
				//when return fail then process file will break
				return result.size()<limit;
			}
			@Override
			public boolean doMassProcess(List<List<IBean>> beans)
					throws Exception {
				for(List<IBean> bean:beans){
					result.add(bean);
					if(result.size()==limit){
						break;
					}
				}
				return result.size()<limit;
			}
			@Override
			public boolean doProcessV3(List<IMessageTree> iMessages, int numberOfTree, List<List<IBean>> beans) throws Exception {
				for(List<IBean> bean:beans){
					result.add(bean);
					if(result.size()==limit){
						break;
					}
				}
				return false;
			}
		}, true);
		return result;
	}

	@Override
	public QueryResult doTestQuery(String criteria, boolean isTestQuery) throws Exception {
		QueryResult result = new QueryResult();
		result.setTotalRec((int)countRecord(criteria, isTestQuery));
		result.setDifferentCol(differentCol);
		List<String> files = this.adapter.getAllFiles(criteria);
		if (files != null && !files.isEmpty()) {
			processFiles.clear();
			try {
				List<List<IBean>> beans = new ArrayList<List<IBean>>();
				result.setResult(beans);
				for (String fn : files) {
					int limit = getMaxTestRecord();
					if (StringUtils.isNotBlank(fn)) {
						processFiles.add(fn);
						List<List<IBean>> queryBeans = query(fn, limit);
						if (queryBeans!=null) {
							beans.addAll(queryBeans);
						}
						if (beans.size()==getMaxTestRecord()) {
							break;
						}else{
							limit = getMaxTestRecord()-beans.size();
						}
					}
				}
			} finally {
				this.adapter.setFilesForBackup(processFiles);
				this.adapter.unlockFiles();
			}
		}
		return result;
	}
	
	
	
	/**
	 * 
	 * @param header
	 * @return true mean that different structure
	 */
	protected boolean doCompareHeader(List<String> header){
		if(!differentCol){
			if(currentFileHeader!=null){
				if(header!=null){
					if(header.size()!=currentFileHeader.size()){					
						differentCol = true;
					}else{
						for(int i=0;i<header.size();i++){
							if(!StringUtils.equalsIgnoreCase(header.get(i), currentFileHeader.get(i))){
								differentCol = true;
								break;
							}
						}
					}
				}
				
			}else{
				currentFileHeader = header;
			}
		}
		return differentCol;
	}

	public final boolean doProceFile(File fn)throws Exception{
		
		return doProceFile(fn, getProcessRecord(), false);
	}
}
