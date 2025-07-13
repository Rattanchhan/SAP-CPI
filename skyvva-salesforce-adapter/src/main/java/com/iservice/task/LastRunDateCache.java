package com.iservice.task;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class LastRunDateCache implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Map<String,String> mIntfLastRunDate;
	private Map<String,String> mFilename_FileModifiedDate;

	public Map<String,String> getmIntfLastRunDate() {
		if(mIntfLastRunDate == null)mIntfLastRunDate=new HashMap<String, String>();
		return mIntfLastRunDate;
	}
	public Map<String,String> getmFilename_FileModifiedDate(){
		if(mFilename_FileModifiedDate==null)mFilename_FileModifiedDate=new HashMap<String, String>();
		return mFilename_FileModifiedDate;
	}

}
