package com.iservice.gui.helper;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

public class JCrontabHelper {
	
	public static final String HOUR = "Hour";
	public static final String MINUTE = "Minute";
	public static final String DAYS_OF_MONTH = "Days Of Month";
	public static final String DAY_OF_WEEK = "Days Of Week";
	public static final String MONTHLY = "Monthly";
	
	public enum HourMinute{
		hour,
		minute
	}
	
	public static String[] getDaysOfMonth(Locale locale){
		Calendar calendar = Calendar.getInstance();
		int numDays = calendar.getActualMaximum(Calendar.DATE);
		String []days = new String[numDays];
		for(int i=0; i < days.length; i++){
			days[i] = ""+(i+1);
		}
		return days;
	}
	
	public static String[] getDaysOfWeek(Locale locale){
		DateFormatSymbols dateFormat = new DateFormatSymbols(locale);
		String weeks [] = dateFormat.getWeekdays();
	
		//cut the first element of the array due to its extra empty element at the beginning
		String []days = new String[weeks.length-1];
		for(int i=0; i<=days.length-1; i++){
			days[i] = weeks[i+1];
		}
		
		return days;
	}
	
	public static String[] getMonths(Locale locale){
		DateFormatSymbols dateFormat = new DateFormatSymbols(locale);
		String m [] = dateFormat.getMonths();
		
		//cut the last element of the array due to its extra empty element at the end
		String[] months = new String[m.length-1];
		for(int i=0; i<=months.length-1; i++){
			months[i] = m[i];
		}
		
		return months;
	}
	
	/**
	 * parse the given string into an array string (elements : start and end)
	 * range format : "start-end"
	 * @param range <code>String</code> range to be parsed
	 * @return array of <code>String</code> - upper and lower bound of the given range
	 */
	public static String[] parseRange(String range, HourMinute hourMinute){
		
		String rangeValues[] = range.split("-");
		if(rangeValues.length == 1){
			rangeValues = new String[2];
        	
			if(range.equals("*")){
				switch(hourMinute){
					case hour:
						rangeValues[0] = "0";
						rangeValues[1] = "23";
						break;
						
					case minute:
						rangeValues[0] = "0";
						rangeValues[1] = "59";
						break;
				}
			
			}else{
        		rangeValues[0] = rangeValues[1] = range;
        		
        	}
			
        }
		return rangeValues;
	}
	
	
	public static String getTimeRange(String start, String end, HourMinute hourMinute){
		String range = "*";
		int hStart = new Integer(start);
		int hEnd = new Integer(end);
		int r = hEnd-hStart;
		
		int MAX = 0;
		switch(hourMinute){
			case hour:
				MAX = 23;
				break;
				
			case minute:
				MAX = 59;
				break;
		}		
		if(r == MAX){
			range = "*";
			
		}else if (r == 0) {
			range = String.valueOf(hStart);
						
		}else if(r < 0){
			range = hEnd + "-" + hStart;
			
		}else{			
			range = hStart + "-" + hEnd;
		}
		return range;
	}
	
	
	public static String addHours(String time, int hourAdded)throws Exception{
		String times[] = time.split(" ");
		String strMinutes = times[0];
		String strHours = times[1];
		
		String min = strMinutes, hrs = strHours;
		
		if(strHours.contains("-") || strHours.equals("*")){ 
			// every 10 minutes from 10 to 18 o'clock, format is :  */10 10-18
			// every 10 minutes , format is :  */10 *
			
			min = "*/" + String.valueOf(getNumber(strMinutes) + ( hourAdded * 60)); // => every 70 minutes 
			
		}else if(strHours.contains("/")){	// every 10 hours, format is :  0 */10
			
			hrs = "*/" + String.valueOf(getNumber(strHours) + hourAdded); // => every 11 hours
			
		}else{ // run at 23:35, format is :  35 23 (mm hh)
			int h = Integer.parseInt(hrs) + hourAdded;
			if(h > 23){
				h = 0;
			}
			hrs = String.valueOf(h); // => run at 0:35
		}
		
		return min + " " + hrs;
	}
	
	
	private static int getNumber(String str)throws Exception{
		int num = 0;
		if(str.contains("/")){
			num = Integer.parseInt(str.substring(str.indexOf("/") + 1, str.length()));			
		}
		return num;
	}
	
	public static String retrieveHourMinute(String start, String end,String range, HourMinute hourMinute, String every) {
		String result = "*";
		int hRange = new Integer(range);
		int hStart = new Integer(start);
		int hEnd = new Integer(end);
		int r = hEnd-hStart;
		
		int MAX = 0;
		switch(hourMinute){
			case hour:
				MAX = 23;
				break;
				
			case minute:
				MAX = 59;
				break;
		}
		
		if(hRange == 1) {
			if(r == MAX) result = "*";
			else result = String.valueOf(hStart) + "-" + String.valueOf(hEnd);
		}else {
			if(every.equals("Minute(s)") && hourMinute.equals(HourMinute.hour)) {
				if(r == MAX) result = "*";
				else result = String.valueOf(hStart) + "-" + String.valueOf(hEnd);
			}else {
				if(r == MAX) {
					result = "*/" + String.valueOf(hRange);
				}else {
					result = String.valueOf(hStart);
					while(hStart<=hEnd) {
						hStart = hStart + hRange;
						if(hStart<=hEnd) {
							result = result + ",";
							result = result + String.valueOf(hStart);
						}
					}
				}
			}
		}
		
		return result;
	}
}
