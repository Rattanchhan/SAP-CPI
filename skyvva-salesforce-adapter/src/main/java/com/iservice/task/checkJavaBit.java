package com.iservice.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;




public class checkJavaBit {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(checkJavaBit.class);
	
	public static Logger log = LogManager.getLogger("IServiceLog");

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
			checkAndSaveJRE32or64();
		} 
		catch (IOException e) {
			LOGGER.error("checkJavaBit: ERROR: " + e);
		}

	}
	
	public static void checkAndSaveJRE32or64() throws IOException {
		   
		   String jreBits = System.getProperty("sun.arch.data.model");
		   log.trace("checkAndSaveJRE32or64(): jreBits>" + jreBits);
		   
		   String bit = "";
		   if(jreBits.contains("32")) {
			   bit = "32";
		   }
		   else if(jreBits.contains("64")) {
			   bit = "64";
		   }
		   else {
			   String jvmName = System.getProperty("java.vm.name");
			   log.trace("checkAndSaveJRE32or64(): jvmName>" + jvmName);
			   if(jvmName.contains("64-Bit")) {
				   bit = "64";
			   }
			   else {
				   bit = "32";
			   }
		   }
		   
		   log.trace("checkAndSaveJRE32or64(): bit>" + bit);
		   
		   String agentHome = System.getenv("SKYVVA_AGENT_HOME");
		   agentHome =  (agentHome == null || agentHome.equals(""))? new File(".").getCanonicalPath() : agentHome;
		   File file = new File(agentHome + System.getProperty("file.separator").trim() + "jreBit.log");
			
			OutputStream out1 = new FileOutputStream(file);
			OutputStreamWriter out = new OutputStreamWriter(out1,"UTF-8");		
			
			out.write(bit);
			
			out.close();
		   
	   }

}
