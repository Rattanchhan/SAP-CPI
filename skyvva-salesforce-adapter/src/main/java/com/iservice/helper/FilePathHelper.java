package com.iservice.helper;

import com.iservice.gui.helper.Helper;

public class FilePathHelper {

	public static String removeExtraPath(String path, String extraPath){

		if(path!=null && path.contains(extraPath)){
			
			path = path.substring(0, path.indexOf(extraPath));
			
		}		
		return path;
	}
	
	public static String getRuntimePath(String path, String extraPath, String codingPath){
		
		if(path!=null && !path.contains(extraPath)){
			
			path = codingPath;
			
		}
		return path;
	}
	
	public static String addExtraPath(String path, String extraPath){
		if(path!=null && path.lastIndexOf(extraPath)<0){
			return path + extraPath;
		}
		return path;
	}
//		
//	public static String getGUIImagePath(){
//		String path = "";
//		try {
//			path = new File(".").getCanonicalPath();
//		} catch (IOException e) {
//			JOptionPane.showMessageDialog(null, e.getLocalizedMessage(), "Image not found", JOptionPane.OK_OPTION);
//		}
//		path = path.contains(Helper.LIBS_DIRECTORY)?
//				addExtraPath(path, "\\images")
//				:
//				".\\src\\com\\iservice\\gui";
//		return path;
//	}
}
