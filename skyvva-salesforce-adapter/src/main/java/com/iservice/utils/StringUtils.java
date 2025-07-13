package com.iservice.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;

public class StringUtils {

	public static boolean isEmpty(String field) {
		if (field == null || field.equals("")) {
			return true;
		}
		return false;
	}

	public static boolean isEmpty(Object field) {
		if (field == null || field.equals("")) {
			return true;
		}
		return false;
	}

	public static boolean isNotEmpty(String field) {
		return !isEmpty(field);
	}

	public static boolean isWhitespace(String character) {
		switch (character) {
		case " ":
		case "\t":
		case "\r":
		case "\n":
		case "\f":
			return true;

		default:
			return false;
		}
	}

	public static boolean beginsWith(String p_string, String p_begin) {
		if (p_string == null) {
			return false;
		}
		return p_string.indexOf(p_begin) == 0;
	}

	public static String trim(String str) {
		if (StringUtils.isEmpty(str)) {
			return "";
		}

		int startIndex = 0;
		while (startIndex < str.length() && isWhitespace(str.charAt(startIndex) + "")) {
			++startIndex;
		}

		int endIndex = str.length() - 1;
		while (endIndex >= 0 && isWhitespace(str.charAt(endIndex) + "")) {
			--endIndex;
		}

		if (endIndex >= startIndex) {
			return str.substring(startIndex, endIndex + 1);
		}
		else {
			return "";
		}
	}

	public static String getStringNotNull(Object object) {
		return getString(object, "");
	}

	public static String getString(Object object, String defaultValue) {
		if (object != null) {
			return object.toString();
		}
		return defaultValue;
	}
	
	public static String getString(JsonObject object, String key) {
		  if (object==null || object.get(key) == null || object.get(key).toString().equals("null") || object.get(key).isJsonNull()) {
		   return "";
		  }
		  return object.get(key).getAsJsonPrimitive().getAsString();
	}
	
	public static boolean checkInteger(String value) {
		if (isEmpty(value)) {
			return true;
		}
		try {
			Integer.valueOf(value).toString();
			return true;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}

	public static boolean checkFloat(String value) {
		if (isEmpty(value)) {
			return true;
		}
		try {
			Float.valueOf(value).toString();
			return true;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}

	public static int getNumber(String s) {
		try {
			return Integer.parseInt(s);
		}
		catch (Exception e) {
			return 0;
		}

	}

	public static Boolean getBoolean(Object value) {
		if (value == null) {
			return false;
		}
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		return getBoolean(value.toString());
	}

	public static Boolean getBoolean(String value) {
		if (value != null
				&& (value.equals("1") || value.equalsIgnoreCase("Y") || value.equalsIgnoreCase("'Y'") || value.equalsIgnoreCase("true") || value
						.equalsIgnoreCase("yes"))) {
			return true;
		}
		return false;
	}

	public static List<String> getFieldsFromFormula(String formula) {
		List<String> fields = new ArrayList<String>();
		while (true) {
			int start = formula.indexOf("{");
			if (start == -1) {
				break;
			}
			int end = formula.indexOf("}", start);
			if (end == -1) {
				break;
			}
			String tmp = formula.substring(start + 1, end);
			if (tmp.indexOf(":") != -1) {
				tmp = tmp.substring(tmp.indexOf(":") + 1);
			}
			fields.add(tmp);
			formula = formula.substring(end + 1);
		}
		return fields;
	}

	public static String computeDisplayName(String s) {
		StringBuilder displayName = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(s.charAt(i - 1))) {
				displayName.append(' ');
			}
			displayName.append(c);
		}
		return displayName.toString();
	}

	public static String fixSlash(String s) {
		return s.replace('\\', '/');
	}

	public static String upperCaseFirst(String value) {
		// Convert String to char array.
		char[] array = value.toCharArray();
		// Modify first element in array.
		array[0] = Character.toUpperCase(array[0]);
		// Return string.
		return new String(array);
	}

	public static String md5hash(String stringToHash) {
		String generatedHash = null;
		try {
			// Create MessageDigest instance for MD5
			MessageDigest md = MessageDigest.getInstance("MD5");
			// Add password bytes to digest
			md.update(stringToHash.getBytes());
			// Get the hash's bytes
			byte[] bytes = md.digest();
			// This bytes[] has bytes in decimal format;
			// Convert it to hexadecimal format
			StringBuilder sb = new StringBuilder();
			for (byte b : bytes) {
				sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
			}
			// Get complete hashed password in hex format
			generatedHash = sb.toString();
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return generatedHash;
	}

	public static boolean isLocalOracleId(String value) {
		if (isEmpty(value)) {
			return false;
		}
		return value.charAt(0) == '#';
	}

	public static Set<String> parseStringCommaToSet(String requiredFields) {
		// Set<String> columns = new HashSet<>();
		// if(StringUtils.isEmpty(requiredFields)) return columns;
		// String[] requiredColumns = requiredFields.split(",");
		// for(String requiredColumn: requiredColumns){
		// columns.add(requiredColumn.trim());
		// }
		// return columns;
		return new HashSet<>(parseStringCommaToList(requiredFields));
	}

	public static List<String> parseStringCommaToList(String requiredFields) {
		List<String> columns = new ArrayList<>();
		if (StringUtils.isEmpty(requiredFields)) {
			return columns;
		}
		String[] requiredColumns = requiredFields.split(",");
		for (String requiredColumn : requiredColumns) {
			columns.add(requiredColumn.trim());
		}
		return columns;
	}

}
