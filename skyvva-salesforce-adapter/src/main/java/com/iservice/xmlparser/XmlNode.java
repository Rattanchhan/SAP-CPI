package com.iservice.xmlparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.iservice.gui.data.IMapping__c;
import com.sforce.soap.schemas._class.IServices.IBean;

/**
 * This is bean that represent a single object (both hierarchical and normal xml object)
 * 
 * -hierarchical structure
 * 		<Account>
 * 			<accountId>...</accountId>
 * 			<accountName>...</accountName>
 * 			<Contact>
 * 				<contactLastName>...</contactLastName>
 * 				<contactEmail>...</contactEmail>
 * 			</Contact>
 * 			<Contact>
 * 				<contactLastName>...</contactLastName>
 * 				<contactEmail>...</contactEmail>
 * 			</Contact>
 * 		</Account>
 *   for hierarchical structure call xmlNode.flat() will get flat object (repeat parent)
 * -normal structure
 * 		<result>
 * 			<row>
 * 				<field name='accountId'>...</field>
 * 				<field name='accountName'>...</field>
 * 				<field name='contactName'>...</field>
 * 				<field name='contactEmail'>...</field>
 * 			</row>
 * 			<row>
 * 				<field name='accountId'>...</field>
 * 				<field name='accountName'>...</field>
 * 				<field name='contactName'>...</field>
 * 				<field name='contactEmail'>...</field>
 * 			</row>
 * 		</result>
 * 
 * @author Ponnreay
 *
 */
public class XmlNode {
	
	private static final String DOT = ".";
	private String name;
	private List<XmlField> fields;
	private List<XmlNode> children;
	private XmlNode parent;
	private XmlNode rootNode;
	protected boolean useObjectPrefix;
	private Map<String, XmlNode> cacheTreeNode = new HashMap<String, XmlNode>();
	private Map<String, List<XmlNode>> cacheAllTreeNodes = new HashMap<String, List<XmlNode>>();
	private boolean isRoot = false;


	public XmlNode(boolean useObjectPrefix) {
		fields = new ArrayList<>();
		children = new ArrayList<>();
		this.useObjectPrefix = useObjectPrefix;
	}
	
	public XmlNode(String name) {
		this(false);
		this.name = name;
	}
	
	public XmlNode(List<XmlField> fields, List<XmlNode> children) {
		this(false);
		this.fields = fields;
		this.children = children;
	}
	
	public List<XmlField> getFields() {
		return fields;
	}
	
	public void setFields(List<XmlField> fields) {
		this.fields = fields;
	}
	
	//if we want to add children please use @addChild
	public List<XmlNode> getChildren() {
		return this.children;
	}
	
	public void setChildren(List<XmlNode> children) {
		if(children!=null){
			for(XmlNode node:children){
				node.parent = this;
			}
		}
		this.children = children;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getNameWithPrefix() {
		if(this.parent!=null && this.parent.useObjectPrefix) {
			return this.parent.getNameWithPrefix()+DOT+this.name;
		}
		return this.name;
	}
	
	public String getPrefix() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void addField(XmlField field){
		this.fields.add(field);
	}
	
	public void addChild(XmlNode node){
		node.parent=this;
		this.children.add(node);
	}
	
	protected List<IBean> toOneRecord(){
		List<IBean> onRecs = new ArrayList<IBean>();
		if(parent!=null){
			onRecs.addAll(this.parent.toOneRecord());
		}
		onRecs.addAll(this.field2Bean());
		return onRecs;
	}
	
	protected List<XmlNode> getAllLeafs(){
		List<XmlNode> leafs = new ArrayList<XmlNode>();
		
		if(getChildren().isEmpty()){
			leafs.add(this);
		}else{
			for(XmlNode ch:getChildren()){
				leafs.addAll(ch.getAllLeafs());
			}
		}
		return leafs;
	}

	public List<List<IBean>> toFlat(){
		List<List<IBean>> result = new ArrayList<>();		
		for(XmlNode leaf: getAllLeafs()) {
			result.add(leaf.toOneRecord());			
		}
		return result;
	}
	
	protected List<IBean> field2Bean(){
		List<IBean> beans = new ArrayList<IBean>();
		if(fields!=null && !fields.isEmpty()){
			//String pname = this.getNameWithPrefix();
			String pname = this.getPrefix();
			for(XmlField f:this.fields){
				String value = f.getValue();
				String name = f.getName();
				if(useObjectPrefix){
					name = pname+DOT+name;
				}
				beans.add(createIBean(name, value));
			}
		}
		return beans;
	}
	
	private IBean createIBean(String name,String value){
		IBean b = new IBean();
		b.setName(name);
		b.setValue(value);	
		return b;
	}
	
	public Map<String,String> getMapData(List<IMapping__c> lsMapping, boolean isCheckMapping) {
		Map<String,String> mapData = new HashMap<String, String>();
		if(fields!=null && !fields.isEmpty()){
			//String pname = this.getName();
			for(XmlField field : this.fields){
				if(isCheckMapping) {
					for(IMapping__c oneMapping : lsMapping) {
						if(oneMapping.getSource__c().toUpperCase().equals(field.getName().toUpperCase())) {
							String value = field.getValue();
							String name = field.getName();
							if(name!=null && mapData.get(name)==null) mapData.put(name, value);
							break;
						}
					}
				}else {
					String value = field.getValue();
					String name = field.getName();
					if(name!=null && mapData.get(name)==null) mapData.put(name, value);
				}
			}
		}
		return mapData;
	}
	
	public void setUseObjectPrefix(boolean useObjectPrefix) {
		this.useObjectPrefix = useObjectPrefix;
	}
	
	public XmlNode getParent() {
		return parent;
	}

	public void setParent(XmlNode parent) {
		this.parent = parent;
	}
	
	public Map<String, XmlNode> getCacheTreeNode() {
		return cacheTreeNode;
	}
	
	public XmlNode getRootNode() {
		return rootNode;
	}

	public void setRootNode(XmlNode rootNode) {
		this.rootNode = rootNode;
	}
	
	public Map<String, List<XmlNode>> getCacheAllTreeNodes(){
		return this.cacheAllTreeNodes;
	}
	
	public boolean isRoot() {
		return isRoot;
	}

	public void isRoot(boolean isRoot) {
		this.isRoot = isRoot;
	}
}
