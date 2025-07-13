package com.iservice.gui.helper;

public class TimeConverter {

	public static int toMinutes(long miliseconds){
		return (int)(miliseconds * 0.000016666666666666667);
	}

	public static long toMiliseconds(int minutes){
		return minutes * 60000;
	}

	public static long toMiliseconds(long seconds){
		return seconds * 1000;
	}
	
	public static long toSeconds(int minutes){
		return minutes * 60;
	}
	
	public static long toSeconds(long miliseonds){
		return Long.parseLong("" + (miliseonds * 0.001));
	}
	
	
}
