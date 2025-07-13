package com.iservice.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;

public class RuntimeExecutor {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(RuntimeExecutor.class);

	static String errorResult;
	
	public static String execute(String cmd)throws IOException, Exception{

		StreamPumper outPumper  = null;
		StreamPumper errPumper = null;
		Process process = null;
		
		try{
			process = Runtime.getRuntime().exec(cmd);
			outPumper = new StreamPumper(process.getInputStream());
			errPumper = new StreamPumper(process.getErrorStream());
			
		}catch(Exception ex){
			throw ex;
		}

		outPumper.start();
		errPumper.start();
		process.waitFor();
		outPumper.join();
		errPumper.join();

		// 3- read the error stream if any after scripts execution
		
		errorResult = errPumper.getResult().trim();
		String exeResult = "";
		if (process.exitValue() == 0) {
			exeResult = outPumper.getResult().trim();

		} else {
			if (errorResult.equals("")) {
				errorResult = "Operation Failed.";
			}
			throw new Exception(errorResult);
		}
		
		return exeResult;
	}
	
	public static String execute(String []cmdarray)throws IOException, Exception{
		
		StreamPumper outPumper  = null;
		StreamPumper errPumper = null;
		Process process = null;
		
		try{
			process = Runtime.getRuntime().exec(cmdarray);
			outPumper = new StreamPumper(process.getInputStream());
			errPumper = new StreamPumper(process.getErrorStream());
			
		}catch(Exception ex){
			throw ex;
		}

		outPumper.start();
		errPumper.start();
		process.waitFor();
		outPumper.join();
		errPumper.join();

		// 3- read the error stream if any after scripts execution
		
		String exeResult = "";
		//Success
		if (process.exitValue() == 0) {
			exeResult = outPumper.getResult().trim();

		}
		//Some unexpected error
		else {
			errorResult = errPumper.getResult().trim(); //get error
			if (errorResult.equals("")) {
				errorResult = "Operation Failed.";
				
				if(errPumper.getErrMsg()!=null && !errPumper.getErrMsg().trim().equals("")) {
					errorResult = errorResult + " " + errPumper.getErrMsg();
				}
				
			}
			throw new Exception(errorResult);
		}
		
		return exeResult;
	}
	
	public static String getErrorResult(){
		return errorResult;
	}
	
	static class StreamPumper extends Thread {
		private InputStream is;

		String result = "";
		
		String errMsg = "";

		public StreamPumper(InputStream is) {
			this.is = is;
		}

		public void run() {
			try {
				BufferedReader br = new BufferedReader(
						new InputStreamReader(is));
				String line;

				while ((line = br.readLine()) != null) {
//					result += line + "\n";
					result += line + "\r\n";
				}
			} catch (Exception e) {
				//catch chor, error get from result, if errorResult=""
				LOGGER.error("StreamPumper.run> ERROR> "  +e, e);
				errMsg = e.getMessage();
				//e.printStackTrace();
			}
		}

		public String getResult() {
			return result;
		}

		public String getErrMsg() {
			return errMsg;
		}
		
	}
}
