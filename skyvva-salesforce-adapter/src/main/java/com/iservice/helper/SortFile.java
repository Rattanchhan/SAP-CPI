package com.iservice.helper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;

public class SortFile {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(SortFile.class);
	
	/**
	 * This sample code shows
	 * how to sort filenames in ascending or descending order
	 * 18-02-2020
	 */
	
	public static void sortFilesByDateCreatedASC (File[] files) {
		Arrays.sort(files, new Comparator<File>() {
			public int compare (File f1, File f2) {
				long l1 = getFileCreationEpoch(f1);
				long l2 = getFileCreationEpoch(f2);
				return Long.valueOf(l1).compareTo(l2);
			}
		});
	}
	
	public static void sortFilesByDateCreatedDESC (File[] files) {
		Arrays.sort(files, new Comparator<File>() {
			public int compare (File f1, File f2) {
				long l1 = getFileCreationEpoch(f1);
				long l2 = getFileCreationEpoch(f2);
				return Long.valueOf(l2).compareTo(l1);
			}
		});
	}

	public static long getFileCreationEpoch (File file) {
		try {
			BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			return attr.creationTime().toInstant().toEpochMilli();
		} catch (IOException e) {
			LOGGER.error("> FolderUtils >getFileCreationEpoch() >Error: "+ e.getMessage());
//			throw new RuntimeException(file.getAbsolutePath(), e);
			return 0;
		}
	}
	
	public static void printFiles(File[] files) {
		for (File file : files) {
			long m = getFileCreationEpoch(file);
			Instant instant = Instant.ofEpochMilli(m);
			LocalDateTime date = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
			System.out.println(date+" - "+file.getName());
		}
	}
	
	public static void sortFilesByNameASC(List<String> lstName) {
		Collections.sort(lstName);
	}
	
	public static void sortFilesByNameDESC(List<String> lstName) {
		Collections.sort(lstName,Collections.reverseOrder());
	}

	public static void main(String[] args) {
		
//		File dir = new File("D:\\TEST FILE");
//		File[] files = dir.listFiles();
//		System.out.println("-- printing files before sort --");
//		printFiles(files);
//		sortFilesByDateCreatedASC(files);
//		System.out.println("-- printing files after sort ASC--");
//		printFiles(files);
//		System.out.println("-- printing files after sort DESC--");
//		sortFilesByDateCreatedDESC(files);
//		printFiles(files);
		
		File fileDir = new File("C:\\temp");
		if(fileDir.isDirectory()){
			List<String> listFile = Arrays.asList(fileDir.list());
			System.out.println("Listing files unsorted");
			for(String s:listFile){
				System.out.println(s);
			}
			Collections.sort(listFile);
			System.out.println("---------------------------------------");
			System.out.println("Sorting by filename in ascending order");
			for(String s:listFile){
				System.out.println(s);
			}
			System.out.println("---------------------------------------");
			System.out.println("Sorting by filename in descending order");
			Collections.sort(listFile,Collections.reverseOrder());
			for(String s:listFile){
				System.out.println(s);
			}

		}
		else{
			System.out.println(fileDir.getAbsolutePath() + " is not a directory");
		}
	}

}
