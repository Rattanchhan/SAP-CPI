package com.iservice.helper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

//import org.apache.ecs.xhtml.map;

import com.iservice.gui.helper.Helper;
import com.iservice.gui.helper.ServerHelper;
import com.iservice.sforce.MapSFConnInfo;

public class PropertiesFileHelper {
	
	public static void writePropertiesFileFormat(File file, Map<String, String> mapKey) throws IOException {
		StringBuffer strData = new StringBuffer();
		strData.append("#Integration Setup File\r\n\r\n");
		strData.append("#SF Connection");
		strData.append("\r\n" + MapSFConnInfo.USERNAME_P + "=" + mapKey.get(MapSFConnInfo.USERNAME_P));
		strData.append("\r\n" + MapSFConnInfo.PASSWORD_P + "=" + mapKey.get(MapSFConnInfo.PASSWORD_P));
		strData.append("\r\n" + MapSFConnInfo.TOKEN_P + "=" + mapKey.get(MapSFConnInfo.TOKEN_P));
		strData.append("\r\n" + MapSFConnInfo.PACKAGE_P + "=" + mapKey.get(MapSFConnInfo.PACKAGE_P));
		strData.append("\r\n" + MapSFConnInfo.SERVER_ENVIRONMENT + "=" + mapKey.get(MapSFConnInfo.SERVER_ENVIRONMENT));
		strData.append("\r\n\r\n#endpoint");
		strData.append("\r\n" + MapSFConnInfo.ENDPOINT_P + "=" + mapKey.get(MapSFConnInfo.ENDPOINT_P));
		strData.append("\r\n\r\n#push log after scheduler");
		strData.append("\r\n" + MapSFConnInfo.PUSH_LOGS2SF + "=" + mapKey.get(MapSFConnInfo.PUSH_LOGS2SF));
		strData.append("\r\n\r\n#AgentProxy");
		strData.append("\r\n" + MapSFConnInfo.PROXY_PORT + "=" + mapKey.get(MapSFConnInfo.PROXY_PORT));
		strData.append("\r\n" + MapSFConnInfo.PROXY_USERNAME + "=" + mapKey.get(MapSFConnInfo.PROXY_USERNAME));
		strData.append("\r\n" + MapSFConnInfo.PROXY_PASSWORD + "=" + mapKey.get(MapSFConnInfo.PROXY_PASSWORD));
		strData.append("\r\n" + MapSFConnInfo.PROXY_HOST + "="  + mapKey.get(MapSFConnInfo.PROXY_HOST));
		strData.append("\r\n" + MapSFConnInfo.PROXY_USED + "=" + mapKey.get(MapSFConnInfo.PROXY_USED));
		
		OutputStream out = new FileOutputStream(file);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
		writer.write(strData.toString());
		writer.close();
	}
	
	public static void writeIntegrationWizard(File file, Map<String, String> mapIW) throws FileNotFoundException, IOException, Exception {
					
		//read all data by lines from a existing properties file and write(replace) new value of the properties through
		// the object of IntegrationWizard in load() method
		// you can put more properties or comment (lead by "#" or "'" )into the file via addLine() method
		
		CommentedProperties props = new CommentedProperties();
		//get File Seperator based on OS running
		
		//get path from env variable
		
		
		writePropertiesFileFormat(file, mapIW);
		
//		FileInputStream input = new FileInputStream(file);
//		
//		props.load(input, mapIW);
//					
//		input.close();
//
//		FileOutputStream output = new FileOutputStream(file);
//		
//		props.store(output, "Properties Writing with comments...");
//			
//		output.close();
						
	}
	
}
