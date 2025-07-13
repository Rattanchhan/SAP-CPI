package com.iservice.gui.helper;

import java.util.Properties;

public class OSTypeHelper {
	
	public static String WINDOWS = "Windows";
	public static String LINUX = "Linux";
	
	public static String getOSType(){
		
		String osName = System.getProperty("os.name");
		String osType;
		
		if(osName.toLowerCase().contains("windows")){
			osType = WINDOWS;
		}
		else{
			//OS name might be Ubuntu, Kubuntu, Debian, Fedora, Linux Mint, ...
			osType = LINUX;
			
			//?????What if MacOS?????
		}
		
		return osType;
	}
	
	public static void main(String[] args) {
		
		Properties pp = System.getProperties();
		for(Object key : pp.keySet()){
			System.out.println(key + " => " + pp.get(key));
		}
		
		System.out.println(getOSType());
	}
}
