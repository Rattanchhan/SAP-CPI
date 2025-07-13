package com.iservice.adapter;

import org.w3c.dom.Element;

public class XMLChildNode {
	private String message;
	private Element node;
	public XMLChildNode(String msg, Element n){
		message = msg;
		node = n;
	}
	public String getMessage() {
		return message;
	}
	
	public Element getNode() {
		return node;
	}
	
	
}
