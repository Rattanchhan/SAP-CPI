package com.model.iservice.base;

/*
 * File: UIDGenerator.java
 * Copyright (c) 2006, Endress+Hauser Infoserve GmbH & Co KG.
 */

import java.net.InetAddress;
import java.util.Random;



/**
 * A UID object is available in the java.util package for JDK 1.5 and in the
 * java.rmi package for JDK 1.4. The java.rmi.server.UID has a default
 * constructor which creates a unique object. However, this constructor pauses
 * the application for 1 second for each UID generated. If many UIDs should be
 * generated then this time delay may become cumbersome. Thus, this UIDGenerator
 * tries to generate a - very probably - unique ID using a similar approach as
 * the java.rmi.server.UID class.
 * 
 * @author Wolfram Kaiser
 */
public class UIDGenerator {

	private short counter = 0;

	private Random randomizer = new Random();

	private static final UIDGenerator singleton = new UIDGenerator();

	private static final long classTime = System.currentTimeMillis();
	
	private int ip=0;

	/**
	 * 
	 */
	private UIDGenerator() {
	
	}

	public static UIDGenerator getInstance() {
		return singleton;
	}

	public synchronized long getUID() {
		return (System.currentTimeMillis() << 20) + (ip << 32) + (classTime << 24) + counter++ + randomizer.nextLong();
	}
	
	public synchronized String getUIDAsString() {
		return String.valueOf(getUID());
	}
}

