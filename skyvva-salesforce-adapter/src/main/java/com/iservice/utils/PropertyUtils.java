package com.iservice.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

public class PropertyUtils {

	private final Properties properties;
	
	public PropertyUtils(InputStream stream) throws IOException {
		this(new InputStreamReader(stream,"UTF-8"));
	
	}
	public PropertyUtils (Reader reader) throws IOException {
		properties = new Properties();
        properties.load(reader);
    }
	
	
	
	
	


}
