package com.iservice.sforce;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.iservice.gui.data.Adapter__c;
import com.iservice.gui.data.Integration__c;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.gui.data.LoginCache;

public class SFServiceCache {
	// to get adapters, the dapter type(first string in the outter map), 
		// and the adapter(inner map including its id(string) and the adapter)
	protected Map<String, Map<String, Adapter__c>> adapterMMap = new HashMap<>() ;

	// Map<AdapterId, adapter>
	protected Map<String, Adapter__c> cacheAdapter = new HashMap<>();

	protected List<String> lstSObjectNames;
	protected Map<String, List<String>> mapFieldNames = new HashMap<>();
	
	protected Map<String, Integration__c> mapAllIntegration = new HashMap<>();
	protected Map<String, Integration__c> mapIntegration = new HashMap<>();
	protected Map<String, Interfaces__c> mapInterface = new HashMap<>();
	protected Map<String, LoginCache> mapLoginCache = new HashMap<>();
	
	
	public  void clearAllMap() {
		if(adapterMMap!=null)adapterMMap.clear();
		if(mapIntegration!=null)mapIntegration.clear();
		if(mapInterface!=null)mapInterface.clear();
		if(cacheAdapter!=null)cacheAdapter.clear();
	}
	
	public LoginCache getLogingCache(String key){
		return mapLoginCache.get(key);
	}
	
	public void addLoginCache(String key,LoginCache ch){
		mapLoginCache.put(key, ch);
	}
	
	public Interfaces__c getInterface(String id){
		return mapInterface.get(id);
	}
	public void addInterface(Interfaces__c intf){
		mapInterface.put(intf.getId(), intf);
	}

	public Integration__c getIntegration(String id){
		return mapIntegration.get(id);
	}
	public void addIntegration(Integration__c intg){
		mapIntegration.put(intg.getId(), intg);
	}
	public void removeIntegraionFromCache(String intgId){
		mapIntegration.remove(intgId);
	}
	
	public Map<String, Integration__c> getMapIntegrations() {
		return mapIntegration;
	}

	public void setMapIntegrations(Map<String, Integration__c> mapIntegrations) {
		this.mapIntegration = mapIntegrations;
	}
	
	public Map<String, Integration__c> getMapAllIntegrations() {
		return mapAllIntegration;
	}

	public void setMapAllIntegrations(Map<String, Integration__c> mapAllIntegrations) {
		this.mapAllIntegration = mapAllIntegrations;
	}

	public List<String> getLstSObjectNames() {
		return lstSObjectNames;
	}

	public void setLstSObjectNames(List<String> lstSObjectNames) {
		this.lstSObjectNames = lstSObjectNames;
	}
	
	public List<String> getFields(String entity){
		return mapFieldNames.get(entity);
	}
	public void setFields(String entity,List<String> fields){
		mapFieldNames.put(entity, fields);
	}
	
	public Adapter__c getCacheAdapter(String id){
		return cacheAdapter.get(id);
	}
	public void addCacheAdapter(Adapter__c ad){
		cacheAdapter.put(ad.getId(), ad);
	}
	public void removeCacheAdapter(String id){
		cacheAdapter.remove(id);
	}
	public boolean isInCachAdapter(String id){
		return cacheAdapter.containsKey(id);
	}
	
	public void clearCacheAdapter() {
		cacheAdapter.clear();
	}

	public void clearAllAdapters(){
		adapterMMap.clear();
	}
	
	public void clearMapIntegrations() {
		mapIntegration.clear();
	}
	
	public void clearMapAllIntegrations() {
		mapAllIntegration.clear();
	}
	
	public void putAdapterByType(String type,Map<String, Adapter__c> ads){
		adapterMMap.put(type, ads);
	}
	public Map<String, Adapter__c> getAdapterByType(String type){
		return adapterMMap.get(type);
	}
	public boolean isAdapterTypeLoaded(String type){
		return adapterMMap.containsKey(type);
	}
	public Interfaces__c getInterfaceByName(String intfName) {
		for(String id :mapInterface.keySet()) {
			if(mapInterface.get(id).getName__c().equals(intfName)) return mapInterface.get(id);
		}
		return null;
	}
}
