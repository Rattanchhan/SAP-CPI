package com.iservice.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;

import com.iservice.adapter.FileTypeHelper;
import com.iservice.gui.helper.PropertyNameHelper;

public class FolderUtils {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(FolderUtils.class);

	public static List<String> getFiles(String folderSpec, String filename) {
		return getFiles(folderSpec, filename, "");
	}

	// set fileExt to empty string for all file exts
	/**
	 * @param folderSpec
	 * @param filename
	 * @param typeExtension
	 * @return
	 */
	public static List<String> getFiles(String folderSpec, String filename, String typeExtension) {
		String[] typeExtensions = typeExtension.split("\\|");
		List<String> fileNames = new ArrayList<String>();
		//20110726
		if(filename!=null && folderSpec!=null) {
			File files[] = null;
			File dir = new File(folderSpec);
			files = (filename.isEmpty() || FileTypeHelper.FILETYPEEXT.contains(filename)) ? dir.listFiles(new FileFilter(filename)) :
				dir.listFiles((java.io.FileFilter) new WildcardFileFilter(filename, IOCase.INSENSITIVE));

			if(files!=null){
				for(File file : files) {
					if(file.isFile() && file.length()>0 && !StringUtils.endsWithIgnoreCase(file.getName(), ".lock")) {
						if(typeExtension.isEmpty()) {
							fileNames.add(file.getName());
						}else {
							for(String typeEx : typeExtensions) {
								if(StringUtils.endsWithIgnoreCase(file.getName(),typeEx)) fileNames.add(file.getName());
							}
						}
					}
				}
			}else {
				LOGGER.error("> FolderUtils : Path " + folderSpec + " not accessible or not w/r mode (check the permission)  ");
			}
		}
		return fileNames;
	}

	public static List<String> loadFiles(String folderSpec, String filename, String typeExtension, String sortby, String orderby) {
		List<String> fileNames = new ArrayList<String>();
		String[] typeExtensions = typeExtension.split("\\|");
		if(filename!=null && folderSpec!=null) {
			File files[] = null;
			File dir = new File(folderSpec);
			files = (filename.isEmpty() || FileTypeHelper.FILETYPEEXT.contains(filename)) ? dir.listFiles(new FileFilter(filename)) :
				dir.listFiles((java.io.FileFilter) new WildcardFileFilter(filename, IOCase.INSENSITIVE));
			
			if(files!=null){
				// 1.49.05 order by created date and file name
				if(sortby.equalsIgnoreCase(PropertyNameHelper.CREATEDDATE)) {
					if(orderby.equalsIgnoreCase(PropertyNameHelper.ASC)) {
						SortFile.sortFilesByDateCreatedASC(files);
					}else {
						SortFile.sortFilesByDateCreatedDESC(files);
					}
				}
				for(File file : files) {
					if(file.isFile() && file.length()>0 && !StringUtils.endsWithIgnoreCase(file.getName(), ".lock")) {
						if(typeExtension.isEmpty()) {
							fileNames.add(file.getName());
						}else {
							for(String typeEx : typeExtensions) {
								if(StringUtils.endsWithIgnoreCase(file.getName(),typeEx)) fileNames.add(file.getName());
							}
						}
					}
				}
				// 1.49.05 order by created date and file name
				if(sortby.equalsIgnoreCase(PropertyNameHelper.FILENAME)) {
					if(orderby.equalsIgnoreCase(PropertyNameHelper.ASC)) {
						SortFile.sortFilesByNameASC(fileNames);
					}else {
						SortFile.sortFilesByNameDESC(fileNames);
					}
				}
			}else {
				LOGGER.error("> FolderUtils : Path " + folderSpec + " not accessible or not w/r mode (check the permission)  ");
			}
		}
		return fileNames;
	}

	public static List<String> getFiles(URI folderSpec, String fileExt) throws Exception {
		List<String> fileNames = new ArrayList<String>();
		//20110726
		if(fileExt!=null && folderSpec!=null) {
			File fil = new File(folderSpec);
			FileFilter xmlFilter = new FileFilter(fileExt);
			File files[] = fil.listFiles(xmlFilter);
			if (files == null) return fileNames;
			for (int i = 0; i < files.length; i++) {
				//23072013: only files, not folder type
				if(!files[i].isFile()) continue;
				fileNames.add(files[i].getName());
			}
		}
		return fileNames;
	}

	public static boolean deleteFile(String fileSpec){
		//20110726
		if(fileSpec!=null && !fileSpec.trim().equals("")) {
			File fil = new File(fileSpec);
			return fil.delete();
		}
		return false;
	}

	public static boolean isFileExisting(String filePath){
		File file = new File(filePath);
		return file.exists();
	}

	public static boolean renameFile(String oldName, String newName) throws FileNotFoundException {
		boolean success = false;
		//20110726
		if(oldName!=null  && !oldName.trim().equals("") && newName!=null  && !newName.trim().equals("")) {
			// File (or directory) with old name
			File file = new File(oldName);
			//10-01-2012
			if(!file.exists())throw new FileNotFoundException(oldName+" (The system cannot find the file specified)");
			// File (or directory) with new name
			File file2 = new File(newName);
			//Check & delete if lock file already exist
			if(file2.exists()) file2.delete();
			// Rename new file (or directory)
			success = file.renameTo(file2);;
		}
		return success;
	}

	public static boolean copyfile(String srFile, String dtFile) throws FileNotFoundException, IOException{
		//20110726
		if(srFile!=null  && !srFile.trim().equals("") && dtFile!=null  && !dtFile.trim().equals("")) {
			File f1 = new File(srFile);
			File f2 = new File(dtFile);
			return copyFile(f1, f2);
		}
		return false;
	}

	public static boolean copyFile(File f1, File f2) throws FileNotFoundException, IOException {
		InputStream in = new FileInputStream(f1);
		//For Overwrite the file.
		OutputStream out = new FileOutputStream(f2);
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0){
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
		return true;
	}

	// Use lower case for filter ext
	static class FileFilter implements FilenameFilter {
		String fileExt = "";
		public FileFilter(String fileExt) {
			super();
			this.fileExt = fileExt;
			//20110727 case insen
			if(this.fileExt!=null) this.fileExt = this.fileExt.toLowerCase();
		}
		@Override
		public boolean accept(File dir, String name) {
			//20110727 if filter is null, then do not get the file
			if(fileExt==null) return false;
			String fName = manageFileExtCase(name);
			//20110922 v1.12 support excel 2007 up
			if(fileExt.equalsIgnoreCase(".xls")) {
				return (fName.endsWith(fileExt) || fName.endsWith(".xlsx"));
			}
			//24072013: to support all files which the fileExt=".*"
			return (fName.endsWith(fileExt) ||  fileExt.equals(".*"));
		}
		private String manageFileExtCase(String fName) {
			StringBuffer sb = new StringBuffer();
			String[] f = fName.split("[.]");
			for (int i = 0; i < f.length - 1; i++) {
				sb.append(f[i]);
			}
			sb.append(".");
			sb.append(f[f.length - 1].toLowerCase());
			return sb.toString();
		}
	}

	public static void main(String[] args) {

		String folderSpec = "D:\\sample data";
		//String fileExt = "acc*"; //get all file types/extensions
		List<String> files =  FolderUtils.getFiles(folderSpec,"acc*", "");
		for(String f : files) {
			System.out.println("========filename in folder:"+folderSpec+">"+f);
		}


	}

}
