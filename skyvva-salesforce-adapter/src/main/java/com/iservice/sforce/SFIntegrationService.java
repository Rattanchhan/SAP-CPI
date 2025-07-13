package com.iservice.sforce;

import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.naming.LimitExceededException;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.lang3.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.iservice.adapter.FileTypeHelper;
import com.iservice.database.PropertySettingDao;
import com.iservice.gui.data.Adapter__c;
import com.iservice.gui.data.IChained_Interfaces__c;
import com.iservice.gui.data.IMapping__c;
import com.iservice.gui.data.ISFIntegrationObject;
import com.iservice.gui.data.IStructure__c;
import com.iservice.gui.data.Integration__c;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.gui.data.LoginCache;
import com.iservice.gui.data.MessageType__c;
import com.iservice.gui.data.MsgTypeFieldEntry__c;
import com.iservice.gui.data.Property__c;
import com.iservice.gui.helper.AdapterTypeHelper;
import com.iservice.gui.helper.DirectionTypeHelper;
import com.iservice.gui.helper.Helper;
import com.iservice.gui.helper.ISchedulerSettingHelper;
import com.iservice.helper.SFIntegrationObjectUtils;
import com.iservice.model.ISchedulerSetting;
import com.iservice.model.Prefs;
import com.iservice.sforce.InterfaceQueryHelper.InterfaceFieldVersion;
import com.iservice.sforce.SObjectQueryHelper.FieldVersion;
import com.iservice.sforce.SObjectQueryHelper.IntegrationField_OldestVersion;
import com.sforce.soap.SFConnectorConfig;
import com.sforce.soap.partner.DeleteResult;
import com.sforce.soap.partner.DescribeGlobalResult;
import com.sforce.soap.partner.DescribeGlobalSObjectResult;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.UpsertResult;
import com.sforce.soap.partner.fault.ApiFault;
import com.sforce.soap.partner.fault.ExceptionCode;
import com.sforce.soap.partner.fault.InvalidFieldFault;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.soap.schemas._class.IServices.IBean;
import com.sforce.soap.schemas._class.IServices.IIntegration;
import com.sforce.ws.ConnectionException;

public class SFIntegrationService {

    public final static String MESSAGE = "IMessage__c";
    public final static String LOG = "ILog__c";
    protected PartnerConnection binding;
    protected MapSFConnInfo mapConInfo;
    protected ISFService isfService;
    private int timeRetry = 0;
    private int maxNumberOfRetry;
    private int retryTimeInterval;
    protected SFServiceCache cache = new SFServiceCache();
    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(SFIntegrationService.class);

    public SFServiceCache getCache() {
        if (cache == null) {
            cache = new SFServiceCache();
        }
        return cache;
    }

    // the key of this map is username + password + token + urlserver
    String cacheKey;

    // For mapping use, in Integration Details
    public final String INTEGRATION = "integration";
    public final String ADAPTER = "adapter";
    public final String INTERFACES = "interfaces";

    // 21-08-2017 clear all map
    public void clearAllMap() {
        getCache().clearAllMap();
    }

    public String getPackage() {

        return getISFService().getQueryPackage();
    }

    public static String getIntegrationId15(String id18) {

        if (id18 != null && id18.length() > 15)
            return id18.substring(0, id18.length() - 3);
        //else
        return id18;
    }

    public SFIntegrationService(MapSFConnInfo myMap) {
        setConnectionInfo(myMap);
    }


    public boolean login() throws Exception {
        boolean bLogin = false;
        try {
            bLogin = login(mapConInfo);
        } catch (Exception ex) {
            LOGGER.error("Error occurred during login: " + ex.getMessage(), ex);
            if (!isInvalidSessionIdException(ex)) {
                throw ex;
            }
        }
        return bLogin;
    }

    public boolean login(MapSFConnInfo connInfo) throws Exception {

        cacheKey = connInfo.getUsername() + connInfo.getPassword() + connInfo.getToken() + connInfo.getSFLoginUrl();

        if (getCache().getLogingCache(cacheKey) == null) {
            isfService = new SFService(connInfo, cache);
            binding = isfService.getSFPartner();
            LoginCache lc = new LoginCache(isfService);
            getCache().addLoginCache(cacheKey, lc);
        }
        return true;
    }


    public Adapter__c getAdapterById(String adapterId) throws Exception {
        if (StringUtils.isEmpty(adapterId)) return null;
        Adapter__c ad = getCache().getCacheAdapter(adapterId);

        if (ad == null) {
            binding = getBinding();
            loadAdapterFromSF(Arrays.asList(adapterId));

        }
        return getCache().getCacheAdapter(adapterId);
    }

    protected String generateAdapterQuery(Collection<String> adapterIds) {
        String myPackage = getPackage();
        String ids = StringUtils.join(adapterIds, "','");
        String sqlQuery = "SELECT Id, "
            + myPackage + "Description__c, "
            + myPackage + "Name__c, "
            + myPackage + "Type__c, "
            + myPackage + "ExtId__c FROM " + myPackage + "Adapter__c where id in ('" + ids + "')";
        return sqlQuery;
    }

    protected void loadAdapterFromSF(Collection<String> adapterIds) throws Exception {
        if (adapterIds == null || adapterIds.isEmpty()) {
            return;
        }
        List<String> notLoaded = new ArrayList<String>();
        for (String id : adapterIds) {
            if (!getCache().isInCachAdapter(id)) {
                notLoaded.add(id);
            }
        }
        if (!notLoaded.isEmpty()) {
            String sqlQuery = generateAdapterQuery(notLoaded);
            List<Adapter__c> lstSObject = execuateQuery(sqlQuery, Adapter__c.class);
            if (lstSObject != null && lstSObject.size() > 0) {
                for (Adapter__c ad : lstSObject) {
                    getCache().addCacheAdapter(ad);
                }
            }
        }
    }

    public Interfaces__c getInterfaceById(String interfaceId) throws Exception {
        Interfaces__c intf = getCache().getInterface(interfaceId);
        if (intf == null) {
            List<Interfaces__c> lstSObject = getInterfaceByIds(interfaceId);
            if (lstSObject != null && !lstSObject.isEmpty()) {
                intf = lstSObject.get(0);
                getCache().addInterface(intf);
            }
        }
        return intf;
    }

    public List<Interfaces__c> getInterfaceByIds(String... ids) throws Exception {
        if (ids != null && ids.length > 0) {
            for (InterfaceFieldVersion fieldVersion : InterfaceQueryHelper.ORDER_INTERFACE_QUERY_FIELD) {
                if (!fieldVersion.isError()) {
                    String query = fieldVersion.generateQuery(ids, false);
                    try {
                        return execuateQuery(query, Interfaces__c.class);
                    } catch (InvalidFieldFault e) {
                        //nothing todo
                        //only test one time if error we ignore it
                        fieldVersion.setError(true);
                    }
                }
            }
        }
        throw new Exception("Your Organization is no permission to access SKYVVA objects, Please contact Admin.");
    }

    public List<Interfaces__c> getInterfaceByIds_SortByInterfaceGroup(String... ids) throws Exception {
        if (ids != null && ids.length > 0) {
            for (InterfaceFieldVersion fieldVersion : InterfaceQueryHelper.ORDER_INTERFACE_QUERY_FIELD) {
                if (!fieldVersion.isError()) {
                    String query = fieldVersion.generateQuery_SortByInterfaceGroup(ids, false);
                    try {
                        return execuateQuery(query, Interfaces__c.class);
                    } catch (InvalidFieldFault e) {
                        //nothing todo
                        //only test one time if error we ignore it
                        fieldVersion.setError(true);
                    }
                }
            }
        }
        throw new Exception("Your Organization is no permission to access SKYVVA objects, Please contact Admin.");
    }

    public List<Interfaces__c> getInterfaceByIds(String[] ids, boolean isSub) throws Exception {
        if (ids != null && ids.length > 0) {
            for (InterfaceFieldVersion fieldVersion : InterfaceQueryHelper.ORDER_INTERFACE_QUERY_FIELD) {
                if (!fieldVersion.isError()) {
                    String query = fieldVersion.generateQuery(ids, isSub);
                    try {
                        return execuateQuery(query, Interfaces__c.class);
                    } catch (InvalidFieldFault e) {
                        //nothing todo
                        //only test one time if error we ignore it
                        fieldVersion.setError(true);
                    }
                }
            }
        }
        throw new Exception("Your Organization is no permission to access SKYVVA objects, Please contact Admin.");
    }

    protected <C extends ISFIntegrationObject> List<C> execuateQuery(String query, Class<C> cls) throws ConnectionException, Exception {
        binding = getBinding();
        List<C> lstObject = null;
        try {
            QueryResult qr = binding.query(query);
            lstObject = SFIntegrationObjectUtils.readSObjects(qr, cls);
            while (!qr.isDone()) {
                qr = binding.queryMore(qr.getQueryLocator());
                lstObject.addAll(SFIntegrationObjectUtils.readSObjects(qr, cls));
            }
        } catch (Exception e) {
            if (isInvalidSessionIdException(e)) {
                //try query again with old field
                return execuateQuery(query, cls);
            } else {
                throw e;
            }
        }
        return lstObject;
    }

    protected <C extends ISFIntegrationObject> List<C> execuateQuery(String query, Class<C> cls, String intfid) throws ConnectionException, Exception {
        timeRetry = 0;
        Interfaces__c intf = (intfid != null ? getInterfaceById(intfid) : null);
        maxNumberOfRetry = isfService.getMaxRetryNumber(null, intf);
        retryTimeInterval = isfService.getRetryTimeInterval(null, intf);
        return queryService(query, cls);
    }

    private <C extends ISFIntegrationObject> List<C> queryService(String query, Class<C> cls) throws Exception {
        binding = getBinding();
        List<C> lstObject = null;
        try {
            QueryResult qr = binding.query(query);
            lstObject = SFIntegrationObjectUtils.readSObjects(qr, cls);
            while (!qr.isDone()) {
                qr = binding.queryMore(qr.getQueryLocator());
                lstObject.addAll(SFIntegrationObjectUtils.readSObjects(qr, cls));
            }
        } catch (Exception e) {
            if (SFService.isErrorConnection(e) && maxNumberOfRetry > 0) {
                timeRetry++;
                maxNumberOfRetry--;
                LOGGER.warn(">SFIntegrationService.queryService> ERROR> Problem with Connection...!");
                LOGGER.warn(">SFIntegrationService.queryService> Retry Integration> (" + timeRetry + (timeRetry <= 1 ? " time" : " times") + ") Retry Time Interval=" + retryTimeInterval + "ms!");
                Thread.sleep(retryTimeInterval);
                return queryService(query, cls);
            } else if (isInvalidSessionIdException(e)) {
                //try query again with old field
                return queryService(query, cls);
            } else {
                throw e;
            }
        }
        return lstObject;
    }

    public Integration__c getIntegrationById(String intId) throws Exception {
        Integration__c intg = getCache().getIntegration(intId);
        if (intg == null) {
            binding = getBinding();
            String sqlQuery = "";
            List<Integration__c> lstSObject = new ArrayList<Integration__c>();
            for (FieldVersion fieldVersion : SObjectQueryHelper.ORDER_INTEGRATION_QUERY_FIELD) {
                if (!fieldVersion.isError()) {
                    sqlQuery = fieldVersion.generateQuery(intId);
                    try {
                        lstSObject = execuateQuery(sqlQuery, Integration__c.class);
                        break;
                    } catch (InvalidFieldFault e) {
                        fieldVersion.setError(true);
                    }
                }
            }
            if (lstSObject != null && !lstSObject.isEmpty()) {
                Integration__c integration = lstSObject.get(0);
                List<Interfaces__c> lstInterfaces = new ArrayList<Interfaces__c>();
                if (integration.getInterfaces() != null && !integration.getInterfaces().isEmpty()) {
                    List<String> lstIds = new ArrayList<String>();
                    for (ISFIntegrationObject obj : integration.getInterfaces()) {
                        Interfaces__c inter = (Interfaces__c) obj;
                        lstIds.add(inter.getId());

                    }
                    lstInterfaces = getInterfaceByIds(lstIds.toArray(new String[lstIds.size()]));
                }
                Set<String> adapterIds = new HashSet<String>();
                for (ISFIntegrationObject intf : lstInterfaces) {
                    if (intf instanceof Interfaces__c) {
                        Interfaces__c intff = (Interfaces__c) intf;
                        getCache().addInterface(intff);
                        if (StringUtils.isNotBlank(intff.getSource_Adapter__c())) {
                            adapterIds.add(intff.getSource_Adapter__c());
                        }
                        if (StringUtils.isNotBlank(intff.getAdapterId__c())) {
                            adapterIds.add(intff.getAdapterId__c());
                        }
                    }
                }
                loadAdapterFromSF(adapterIds);
                integration.setInterfaces(lstInterfaces);

                //get the adapter and its properties
//				if(integration.getAdapter()!=null){
//					Adapter__c adapter = integration.getAdapter();
//					integration.setAdapter(getAdapter(adapter.getType__c(), adapter.getId()));
//				}
                integration.setId(getIntegrationId15(integration.getId()));
                getCache().addIntegration(integration);
                intg = integration;
            }

        }
        return intg;
    }

    public Integration__c getIntegration__BasedInterfaceGroup(String intgId, String intfGId) throws Exception {
        binding = getBinding();
        String sqlQuery = "";
        List<Integration__c> lstSObject = new ArrayList<Integration__c>();
        for (FieldVersion fieldVersion : SObjectQueryHelper.ORDER_INTEGRATION_QUERY_FIELD) {
            if (!fieldVersion.isError()) {
                sqlQuery = ((IntegrationField_OldestVersion) fieldVersion).generateQueryV2(intgId, intfGId);
                try {
                    lstSObject = execuateQuery(sqlQuery, Integration__c.class);
                    break;
                } catch (InvalidFieldFault e) {
                    fieldVersion.setError(true);
                }
            }
        }
        if (lstSObject != null && !lstSObject.isEmpty()) {
            Integration__c integration = lstSObject.get(0);
            List<Interfaces__c> lstInterfaces = new ArrayList<Interfaces__c>();
            if (integration.getInterfaces() != null && !integration.getInterfaces().isEmpty()) {
                List<String> lstIds = new ArrayList<String>();
                for (ISFIntegrationObject obj : integration.getInterfaces()) {
                    Interfaces__c inter = (Interfaces__c) obj;
                    lstIds.add(inter.getId());

                }
                lstInterfaces = getInterfaceByIds_SortByInterfaceGroup(lstIds.toArray(new String[lstIds.size()]));
            }
            Set<String> adapterIds = new HashSet<String>();
            for (ISFIntegrationObject intf : lstInterfaces) {
                if (intf instanceof Interfaces__c) {
                    Interfaces__c intff = (Interfaces__c) intf;
                    getCache().addInterface(intff);
                    if (StringUtils.isNotBlank(intff.getSource_Adapter__c())) {
                        adapterIds.add(intff.getSource_Adapter__c());
                    }
                    if (StringUtils.isNotBlank(intff.getAdapterId__c())) {
                        adapterIds.add(intff.getAdapterId__c());
                    }
                }
            }
            loadAdapterFromSF(adapterIds);
            integration.setInterfaces(lstInterfaces);

            //get the adapter and its properties
            if (integration.getAdapter() != null) {
                Adapter__c adapter = integration.getAdapter();
                integration.setAdapter(getAdapter(adapter.getType__c(), adapter.getId()));
            }
            integration.setId(getIntegrationId15(integration.getId()));
            getCache().addIntegration(integration);
            return integration;
        }
        return null;
    }

    /**
     * Get Adapter by Type
     *
     * @param type
     * @return
     * @throws Exception
     */
    public Map<String, Adapter__c> getAdapters(String type) throws Exception {

        //changing from adapter CSV, XML, JSON to FileAdapter.
        if (type.equals(AdapterTypeHelper.FILE)) {
            binding = getBinding();
            String myPackage = getPackage();
            Map<String, Adapter__c> allFileAdapters = new HashMap<String, Adapter__c>();
            if (!getCache().isAdapterTypeLoaded(AdapterTypeHelper.FILE)) {
                searchAdapter(AdapterTypeHelper.FILE, myPackage);

            }
            if (!getCache().isAdapterTypeLoaded(FileTypeHelper.CSV)) {
                searchAdapter(FileTypeHelper.CSV, myPackage);

            }
            if (!getCache().isAdapterTypeLoaded(FileTypeHelper.XML)) {
                searchAdapter(FileTypeHelper.XML, myPackage);

            }
            if (!getCache().isAdapterTypeLoaded(FileTypeHelper.JSON)) {
                searchAdapter(FileTypeHelper.JSON, myPackage);

            }
            if (!getCache().isAdapterTypeLoaded(FileTypeHelper.EXCEL)) {
                searchAdapter(FileTypeHelper.EXCEL, myPackage);

            }

            allFileAdapters.putAll(getCache().getAdapterByType(FileTypeHelper.EXCEL));
            allFileAdapters.putAll(getCache().getAdapterByType(FileTypeHelper.JSON));
            allFileAdapters.putAll(getCache().getAdapterByType(FileTypeHelper.XML));
            allFileAdapters.putAll(getCache().getAdapterByType(FileTypeHelper.CSV));
            allFileAdapters.putAll(getCache().getAdapterByType(AdapterTypeHelper.FILE));
            getCache().putAdapterByType(AdapterTypeHelper.FILE, allFileAdapters);
        } else {
            if (!getCache().isAdapterTypeLoaded(type)) {
                binding = getBinding();
                String myPackage = getPackage();
                searchAdapter(type, myPackage);

            }
        }
        return getCache().getAdapterByType(type);
    }


    private void searchAdapter(String type, String myPackage) throws Exception {
        IBean filter[] = new IBean[3];
        IBean adapterBean1 = new IBean();
        adapterBean1.setName("SEARCH_ADAPTER");
        adapterBean1.setValue("SEARCH_ADAPTER");

        IBean adapterBean2 = new IBean();
        adapterBean2.setName("type");
        adapterBean2.setValue(type);

        IBean adapterBean3 = new IBean();
        adapterBean3.setName("ids");
        adapterBean3.setValue(null);

        filter[0] = adapterBean1;
        filter[1] = adapterBean2;
        filter[2] = adapterBean3;

        IBean[][] adapters = null;
        try {
            adapters = getISFService().search(filter);
        } catch (Exception ex) {
            LOGGER.error("SFIntegrationService> searchAdapter()> ERROR: " + ex.getMessage(), ex);
            if (isInvalidSessionIdException(ex)) {
                adapters = getISFService().search(filter);
            }
        }
        Map<String, Adapter__c> adapterMap = new HashMap<String, Adapter__c>();
        if (adapters != null) {
            for (int i = 0; i < adapters.length; i++) {
                IBean[] beans = adapters[i];
                Adapter__c ada = new Adapter__c(myPackage);

                for (int j = 0; j < beans.length; j++) {
                    if (beans[j] == null) {
                        continue;
                    }
                    if (beans[j].getName().equalsIgnoreCase("Id")) {
                        ada.setId(beans[j].getValue());

                    } else if (beans[j].getName().equalsIgnoreCase("Name")) {
                        ada.setName__c(beans[j].getValue());
                        ada.setName(beans[j].getValue());

                    } else if (beans[j].getName().equalsIgnoreCase("Type__c")) {
                        ada.setType__c(beans[j].getValue());

                    } else if (beans[j].getName().equalsIgnoreCase("Property__r")) {

                        String strP = beans[j].getValue();
                        if (strP == null || strP.equals(""))
                            continue;

                        List<Property__c> lstProps = new ArrayList<Property__c>();
                        String props[] = strP.split("\n");
                        for (String strOneProp : props) {
                            Property__c p = new Property__c(myPackage);

                            String id = strOneProp.substring(0, strOneProp.indexOf(":"));

                            String prop = strOneProp.substring(strOneProp.indexOf(":") + 1, strOneProp.length());
                            String name = prop.substring(0, prop.indexOf(":"));
                            String value = prop.substring(prop.indexOf(":") + 1, prop.length());

                            p.setId(id);
                            p.setName__c(name);
                            p.setValue2__c(value);
                            p.setValuelong__c(p.getValue2__c());
                            p.setAdapter__c(ada.getId());
                            lstProps.add(p);
                        }
                        ada.setProperties(lstProps);
                    }
                }
                if (ada != null) {
                    adapterMap.put(ada.getId(), ada);
                }
            }
        }
        getCache().putAdapterByType(type, adapterMap);
    }

    public Adapter__c getAdapter(String type, String adapterId) throws Exception {
        return getAdapters(type).get(adapterId);
    }

    public IBean[][] queryOutboundInterface(String strQuery, String intId, String intfId) throws Exception {
        binding = getBinding();
        IBean filter[] = new IBean[5];

        IBean adapterBean1 = new IBean(ISFService.SALESFORCE_AGENT_TEST_QUERY, ISFService.SALESFORCE_AGENT_TEST_QUERY);
        IBean adapterBean2 = new IBean(ISFService.MAX_LIMIT_TEST_QUERY, "50");
        IBean adapterBean3 = new IBean("QUERY_TEST", strQuery);
        IBean adapterBean4 = new IBean(ISFService.INTEGRATION_C, intId);
        IBean adapterBean5 = new IBean(ISFService.INTERFACEE, intfId);

        filter[0] = adapterBean1;
        filter[1] = adapterBean2;
        filter[2] = adapterBean3;
        filter[3] = adapterBean4;
        filter[4] = adapterBean5;

        IBean[][] outboundInterfaces = null;

        try {
            outboundInterfaces = getISFService().search(filter);
        } catch (Exception ex) {
            LOGGER.error("SFIntegrationService.queryOutboundInterface: Error> " + ex, ex);
            if (isInvalidSessionIdException(ex)) {
                outboundInterfaces = getISFService().search(filter);
            }
        }
        return outboundInterfaces;
    }

    public Map<String, Integration__c> getAllIntegrations() throws Exception {

        SObjectQueryHelper.restartQueryHelper();
        InterfaceQueryHelper.restartQueryHelper();
        if (getCache().getMapAllIntegrations() != null && !getCache().getMapAllIntegrations().isEmpty())
            return getCache().getMapAllIntegrations();

        String sql = "Select Id, Name from " + getPackage() + "Integration__c order by Name";

        List<Integration__c> lstSObject = execuateQuery(sql, Integration__c.class, null);

        Map<String, Integration__c> mapAllIntegrations = new HashMap<String, Integration__c>();
        if (lstSObject != null && !lstSObject.isEmpty()) {
            for (ISFIntegrationObject sobj : lstSObject) {
                Integration__c integration = (Integration__c) sobj;
                String id = getIntegrationId15(integration.getId());
                integration.setId(id);
                mapAllIntegrations.put(id, integration);
            }
        }

        mapAllIntegrations = Helper.sortByValue(mapAllIntegrations);
        getCache().setMapAllIntegrations(mapAllIntegrations);
        return mapAllIntegrations;
    }

    public List<String> upsert(SObject[] sobjects) throws Exception, LimitExceededException {

        binding = getBinding();

        List<String> lstIds = new ArrayList<String>();

        try {
            lstIds = upsert(binding, sobjects);
        } catch (Exception ex) {

            LOGGER.error("SFIntegrationService.upsert: doupsert> ERROR> " + ex, ex);
            if (isInvalidSessionIdException(ex)) {
                lstIds = upsert(binding, sobjects);
            }
        }
        return lstIds;
    }

    public static List<String> upsert(PartnerConnection binding, SObject[] sobjects) throws Exception, LimitExceededException {

        UpsertResult[] ur = null;
        ur = binding.upsert("Id", sobjects);

        List<String> ids = new ArrayList<String>();

        if (ur == null) return ids;

        for (int i = 0; i < ur.length; i++) {
            if (ur[i].isSuccess()) {
                ids.add(ur[i].getId());

            } else {
                StringBuffer strErrors = new StringBuffer();

                for (int j = 0; j < ur[i].getErrors().length; j++) {
                    if (ur[i].getErrors()[j].getMessage().toLowerCase().contains("entity is deleted")) {
                        continue;
                    } else {
                        strErrors.append(ur[i].getErrors()[j].getMessage() + "\n");
                        throw new Exception(strErrors.toString());
                    }

                }
            }
        }
        return ids;
    }

    /**
     * @param ids : id of objects to be deleted
     * @return true : delete successfully
     * false: failed to delete - raise errors
     */
    public boolean delete(String[] ids) throws Exception {

        binding = getBinding();

        DeleteResult[] dr = null;
        try {
            if (ids != null && ids.length > 0) dr = binding.delete(ids);
        } catch (Exception ex) {

            LOGGER.error("SFIntegrationService.delete: binding.delete> Error> " + ex, ex);
            if (isInvalidSessionIdException(ex)) {
                dr = binding.delete(ids);
            }
        }

        if (dr == null) return false;

        for (int i = 0; i < dr.length; i++) {
            if (!dr[i].isSuccess()) {
                StringBuffer strErrors = new StringBuffer();

                for (int j = 0; j < dr[i].getErrors().length; j++) {
                    if (dr[i].getErrors()[j].getMessage().toLowerCase().contains("entity is deleted")) {
                        continue;
                    } else {
                        strErrors.append(dr[i].getErrors()[j].getMessage());
                        throw new Exception(strErrors.toString());
                    }
                }
            }
        }
        return true;
    }


    public List<String> listAllSObjectNames() throws Exception {

        if (getCache().getLstSObjectNames() != null) {
            return getCache().getLstSObjectNames();
        }
        binding = getBinding();

        DescribeGlobalResult dgr = null;
        List<String> lstSObjectNames = new ArrayList<String>();
        try {
            dgr = binding.describeGlobal();
        } catch (Exception ex) {
            LOGGER.error("SFIntegrationService.listAllSObjectNames: describeGlobal> error> " + ex, ex);
            if (isInvalidSessionIdException(ex)) {
                dgr = binding.describeGlobal();
            }
        }

        if (dgr == null) return lstSObjectNames;

        DescribeGlobalSObjectResult results[] = dgr.getSobjects();
        for (int i = 0; i < results.length; i++) {
            lstSObjectNames.add(results[i].getName());
        }
        getCache().setLstSObjectNames(lstSObjectNames);
        return lstSObjectNames;
    }

    public List<String> listAllFieldNames(String sObject) throws Exception {

        List<String> lstFieldNames = getCache().getFields(sObject);
        if (lstFieldNames == null) {

            binding = getBinding();

            DescribeSObjectResult[] describeResult = null;
            lstFieldNames = new ArrayList<String>();

            try {
                describeResult = binding.describeSObjects(new String[]{sObject});
            } catch (Exception ex) {

                LOGGER.error("SFIntegrationService.listAllFieldNames: describeSObjects> error> " + ex, ex);
                if (isInvalidSessionIdException(ex)) {
                    describeResult = binding.describeSObjects(new String[]{sObject});
                }
            }

            if (describeResult == null) return lstFieldNames;

            for (int i = 0; i < describeResult.length; i++) {
                Field[] fields = describeResult[i].getFields();
                for (int j = 0; j < fields.length; j++) {
                    lstFieldNames.add(fields[j].getName());
                }
            }

            getCache().setFields(sObject, lstFieldNames);
        }

        return lstFieldNames;
    }

    public PartnerConnection getBinding() throws Exception {
        LoginCache loginCache = getCache().getLogingCache(cacheKey);
        if (loginCache == null) {
            this.login();
        }
        return loginCache.getSfConnection().getSFPartner();
    }

    public String getEndPoint() {
        return getCache().getLogingCache(cacheKey).getIntegrationUrl();
    }

    public String getSessionId() {
        return getCache().getLogingCache(cacheKey).getSessionId();
    }

    public void setConnectionInfo(MapSFConnInfo mapConInfo) {
        this.mapConInfo = mapConInfo;
    }

    public MapSFConnInfo getConnectionInfo() {
        mapConInfo.setEndPoint(getCache().getLogingCache(cacheKey).getIntegrationUrl());
        return mapConInfo;
    }

    public void integrate(IIntegration integration) throws RemoteException, Exception {
        try {
            getISFService().integrate(integration);
        } catch (Exception ex) {
            LOGGER.error("SFIntegrationService.integrate: dointegrate> error> " + ex, ex);
            if (isInvalidSessionIdException(ex)) {
                getISFService().integrate(integration);
            }
        }
    }


    public Map<String, List<? extends ISFIntegrationObject>> getIMessageILog(String integrationId, String interfaceId, int size) throws Exception {

        Map<String, List<? extends ISFIntegrationObject>> mapMsgLog = new HashMap<>();
        String myPackage = getPackage();
        String strQuery = "Select id, "
            //Select IMessage__c
            + "(Select id, Name, " + myPackage + "Status__c, CreatedDate, LastModifiedDate, "
            + myPackage + "Interface__c, " + myPackage + "Integration__c, " + myPackage + "Comment__c from "
            + myPackage + "Messages__r where " + myPackage + "Interface__c='" + interfaceId
            + "' order by CreatedDate desc limit " + size + "),  "

            //Select ILog__c
            + "(Select id, " + myPackage + "Description__c, CreatedDate, "
            + myPackage + "Error_Level__c, " + myPackage + "Integration__c, " + myPackage + "Source__c from "
            + myPackage + "Logs__r where " + myPackage + "Integration__c = '" + integrationId
            + "'  order by CreatedDate desc limit 5) "

            + " from " + myPackage + "Integration__c where id='" + integrationId + "'";

        List<Integration__c> lstSObject = execuateQuery(strQuery, Integration__c.class);

        if (lstSObject != null && !lstSObject.isEmpty()) {
            Integration__c integration = lstSObject.get(0);
            if (integration.getMessages() != null) {
                mapMsgLog.put(SFIntegrationService.MESSAGE, integration.getMessages());
            }
            if (integration.getLogs() != null) {
                mapMsgLog.put(SFIntegrationService.LOG, integration.getLogs());
            }
        }
        return mapMsgLog;
    }

    //03-07-2019

    public MessageType__c getMsgType(String id) throws Exception {
        String myPackage = getPackage();
        String sql = "Select id, name, "
            + myPackage + "Type__c, "
            + myPackage + "SObjectType__c, "
            + myPackage + "ExternalName__c, "
            + myPackage + "HLevel__c, (Select id From "
            + myPackage + "Message_Types__r order by "
            + myPackage + "SequenceNumber__c asc), (Select "
            + myPackage + "IstructureField__r." + myPackage + "Name__c, "
            + myPackage + "IstructureField__r." + myPackage + "Type__c From "
            + myPackage + "Message_Structures__r order by "
            + myPackage + "Sequence__c) From "
            + myPackage + "MessageType__c Where id='" + id + "'";

        List<MessageType__c> msgTypes = execuateQuery(sql, MessageType__c.class);
        MessageType__c msgType = new MessageType__c(myPackage);
        if (!msgTypes.isEmpty()) msgType = msgTypes.get(0);
        return msgType;
    }

    public List<Interfaces__c> getInterfaceByMessageType(String[] ids, String id) throws Exception {
        if (ids != null && ids.length > 0) {
            for (InterfaceFieldVersion fieldVersion : InterfaceQueryHelper.ORDER_INTERFACE_QUERY_FIELD) {
                if (!fieldVersion.isError()) {
                    String query = fieldVersion.generateQuery(ids, id);
                    try {
                        return execuateQuery(query, Interfaces__c.class);
                    } catch (InvalidFieldFault e) {
                        //nothing todo
                        //only test one time if error we ignore it
                        fieldVersion.setError(true);
                    }
                }
            }
        }
        throw new Exception("Your Organization is no permission to access SKYVVA objects, Please contact Admin.");
    }

    public Map<String, MessageType__c> getMapMessageType(String repository) throws Exception {
        String myPackage = getPackage();
        String sql = "Select id, name, "
            + myPackage + "Type__c, "
            + myPackage + "SObjectType__c, "
            + myPackage + "ExternalName__c, "
            + myPackage + "HLevel__c, (Select id From "
            + myPackage + "Message_Types__r order by "
            + myPackage + "SequenceNumber__c asc), (Select "
            + myPackage + "IstructureField__r." + myPackage + "Name__c, "
            + myPackage + "IstructureField__r." + myPackage + "Type__c From "
            + myPackage + "Message_Structures__r order by "
            + myPackage + "Sequence__c) From "
            + myPackage + "MessageType__c Where "
            + myPackage + "iRepository__c='" + repository + "'";

        List<MessageType__c> msgTypes = execuateQuery(sql, MessageType__c.class);
        Map<String, MessageType__c> mapMessageType = new HashMap<String, MessageType__c>();

        for (MessageType__c msgType : msgTypes) {
            mapMessageType.put(msgType.getId(), msgType);
        }
        return mapMessageType;
    }

    //02-01-2017
    public List<MessageType__c> getMessageType__c(String messagetypeId, String msgtypeType) throws Exception {

        String myPackage = getPackage();

        String sql = "select id, Name, "
            + myPackage + "SequenceNumber__c, (Select id, Name, "
            + myPackage + "Type__c, "
            + myPackage + "SequenceNumber__c From "
            + myPackage + "Message_Types__r where "
            + myPackage + "Type__c='" + msgtypeType + "' order by "
            + myPackage + "SequenceNumber__c asc) from "
            + myPackage + "MessageType__c where id='" + messagetypeId + "'";

        List<MessageType__c> msgtype = execuateQuery(sql, MessageType__c.class);


        if (msgtype != null && !msgtype.isEmpty()) {
            return msgtype.get(0).getMessageTypes__c();
        }
        return null;
    }

    //02-01-2017 get messagetype id that type__c = XSD COMPLEXTYPE
    private int i = 1;

    public String getMessageTypeId(String messagetypeId, String msgtypeType) {
        List<MessageType__c> lstMessageTypes = null;
        try {
            if (i < 3) {//getMessageType__c in level i=2
                i++;
                if ((lstMessageTypes = getMessageType__c(messagetypeId, msgtypeType)) != null) {
                    messagetypeId = getMessageTypeId(lstMessageTypes.get(0).getId(), MessageType__c.TYPE_XSD_COMPLEXTYPE);
                } else {
                    return messagetypeId;
                }
            } else {
                return messagetypeId;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        i = 1;
        return messagetypeId;
    }

    //02-01-2017
    public List<MsgTypeFieldEntry__c> getMsgTypeFieldEntry__c(String messagetypeId) throws Exception {

        messagetypeId = getMessageTypeId(messagetypeId, MessageType__c.TYPE_WSDL_RESPONSE);

        String myPackage = getPackage();

        String sql = "select id, Name, "
            + myPackage + "Sequence__c, "
            + myPackage + "IstructureField__r.Name From "
            + myPackage + "MsgTypeFieldEntry__c where "
            + myPackage + "MessageType__c ='" + messagetypeId + "'";

        List<MsgTypeFieldEntry__c> latMsgTypeFieldEntrys = execuateQuery(sql, MsgTypeFieldEntry__c.class);

        if (latMsgTypeFieldEntrys != null && !latMsgTypeFieldEntrys.isEmpty()) {
            return latMsgTypeFieldEntrys;
        }
        return null;
    }

    public List<IMapping__c> getIMapping__c(String interfaceId) throws Exception {
        binding = getBinding();
        String sqlQuery;
        List<IMapping__c> lstMappings = null;

        for (FieldVersion fieldVersion : SObjectQueryHelper.ORDER_IMAPPING_QUERY_FIELD) {
            if (!fieldVersion.isError()) {
                sqlQuery = fieldVersion.generateQuery(interfaceId);
                try {
                    lstMappings = execuateQuery(sqlQuery, IMapping__c.class, interfaceId);
                    break;
                } catch (InvalidFieldFault e) {
                    fieldVersion.setError(true);
                }
            }
        }
        return lstMappings;
    }

    public List<IStructure__c> getIStructure__c(String interfaceID) throws Exception {

        String myPackage = getPackage();

        String sql = "select id, "
            //IStructures
            + "(Select id, Name, " + myPackage + "Type__c From " + myPackage + "IStructure__r)"

            + " from " + myPackage + "Interfaces__c where id='" + interfaceID + "'";

        List<Interfaces__c> lstObject = execuateQuery(sql, Interfaces__c.class);

        if (lstObject != null && !lstObject.isEmpty()) {
            return lstObject.get(0).getIStructures();
        }
        return null;
    }

    public List<IChained_Interfaces__c> getIChained_Interfaces__c(String interfaceId) throws Exception {

        String myPackage = getPackage();

        String sql = "Select Id, Name, "
            + myPackage + "ChildInterfaceId__c,"
            + myPackage + "Init_Operation__c,"
            + myPackage + "Interface_Name__c,"
            + myPackage + "ParentInterfaceId__c,"
            + myPackage + "Parent_Relationship_Name__c,"
            + myPackage + "Sequence__c,"
            + myPackage + "Type__c From "
            + myPackage + "IChained_Interfaces__c where " + myPackage + "ParentInterfaceId__c = '" + interfaceId + "'";

        List<IChained_Interfaces__c> lstChain = execuateQuery(sql, IChained_Interfaces__c.class, interfaceId);

        if (lstChain != null && !lstChain.isEmpty()) {
            return lstChain;
        }
        return null;
    }

    public List<IChained_Interfaces__c> getIChainedIntfByRootIntf(String rootIntfId) throws Exception {

        String myPackage = getPackage();

        String sql = "Select Id, Name, "
            + myPackage + "ChildInterfaceId__c,"
            + myPackage + "Init_Operation__c,"
            + myPackage + "Interface_Name__c,"
            + myPackage + "ParentInterfaceId__c,"
            + myPackage + "Parent_Relationship_Name__c,"
            + myPackage + "Sequence__c,"
            + myPackage + "Type__c From "
            + myPackage + "IChained_Interfaces__c where " + myPackage + "RootInterface__c = '" + rootIntfId + "'";

        List<IChained_Interfaces__c> lstChain = execuateQuery(sql, IChained_Interfaces__c.class);

        if (lstChain != null && !lstChain.isEmpty()) {
            return lstChain;
        }
        return null;
    }

    public void clearCacheAdapter() {
        getCache().clearCacheAdapter();
    }

    public void clearAllAdapters() {
        getCache().clearAllAdapters();
    }

    public void clearMapAllIntegrations() {
        getCache().clearMapAllIntegrations();
    }

    public ISFService getISFService() {
        if (isfService == null) {
            isfService = new SFService(getConnectionInfo(), cache);
            //clearDoubleSlash();
            isfService.setSessionId(this.getSessionId());
        }
        return isfService;
    }

    /**
     * @param ex
     * @return true ---means---> the exception message is _INVALID_SESSION_ID and login() is successful (re-login to get session)
     * @throws Exception
     */
    private boolean isInvalidSessionIdException(Exception ex) throws Exception {
        if (
            (ex.getMessage() != null && ex.getMessage().contains(ExceptionCode.INVALID_SESSION_ID.toString()))
            || (ex instanceof ApiFault && ((ApiFault) ex).getExceptionMessage() != null
                && (
                    ((ApiFault) ex).getExceptionMessage().contains(ExceptionCode.INVALID_SESSION_ID.toString())
                    || ((ApiFault) ex).getExceptionCode() == ExceptionCode.INVALID_SESSION_ID
                )
            )
            || ex instanceof SocketTimeoutException
            //|| ex instanceof SocketException
        ) {
            getCache().addLoginCache(cacheKey, null);
            login();
            return true;
        } else {
            throw ex;
        }
    }


    public void removeIntegraionFromCache(String intgId) {
        getCache().removeIntegraionFromCache(intgId);
    }

    public SFConnectorConfig getSalesforceConfiguration() throws Exception {
        return (SFConnectorConfig) getISFService().getSFPartner().getConfig();
    }

    // PushToSF
    public boolean isAgentControlBoard() {
        try {
            String query = "Select Id From " + getPackage() + "AgentSetting__c limit 1";
            getBinding().query(query);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Interfaces__c getInterface(Integration__c integration, String name) throws Exception {
        if (integration != null && integration.getInterfaces() != null && integration.getInterfaces().size() > 0) {
            for (Interfaces__c interf : integration.getInterfaces()) {
                if (interf.getName__c().equals(name)) {
                    return interf;
                }
            }
        }
        return null;
    }

    public Integration__c getIntegrationByName(String name) throws Exception {
        Map<String, Integration__c> allInteg = getAllIntegrations();
        for (String key : allInteg.keySet()) {
            Integration__c integ = allInteg.get(key);
            if (integ.getName().equals(name)) {

                return getIntegrationById(integ.getId());
            }
        }
        return null;
    }

    public List<JsonObject> createISchedulerSetting(List<ISchedulerSetting> lsIsch) {
        List<JsonObject> lsRes = new ArrayList<JsonObject>();
        try {
            boolean isPushToSF = isAgentControlBoard();
            String myPackage = getPackage();
            if (lsIsch != null && lsIsch.size() > 0) {
                PartnerConnection binding = getBinding();

                for (ISchedulerSetting isch : lsIsch) {
                    String integrationId = isch.getIntegrationid();
                    String interfaceId = isch.getInterfaceid();
                    if (StringUtils.isBlank(integrationId) && StringUtils.isNotBlank(isch.getIntegrationname())) {
                        Integration__c integration = getIntegrationByName(isch.getIntegrationname());
                        if (integration != null) {
                            //found integration
                            isch.setIntegrationid(integration.getId());
                            //get interface from integration
                            if (StringUtils.isBlank(interfaceId)) {
                                Interfaces__c interf = getInterface(integration, isch.getInterfacename());
                                if (interf != null) {
                                    isch.setInterfaceid(interf.getId());
                                }
                            }
                        }
                    }
                    String stype = StringUtils.equalsIgnoreCase(isch.getDirection(), DirectionTypeHelper.INBOUND) ? ISchedulerSettingHelper.PER_INTEGRATION_ALL_IN_BOUND_AGENT_SCHEDULER : ISchedulerSettingHelper.PER_INTEGRATION_ALL_IN_OUTBOUND_AGENT_SCHEDULER;
                    stype = StringUtils.isNotBlank(isch.getInterfacename()) ? null : stype;
                    String query = "Select Id,myPackage:Scheduler_Type2__c,myPackage:Integration__c,myPackage:Interface__c From myPackage:IScheduler_Setting__c where myPackage:Integration__c='" + isch.getIntegrationid() + "' and " + (StringUtils.isNotBlank(stype) ? " myPackage:Scheduler_Type2__c like '" + stype + "%'" : " myPackage:Interface__c = '" + isch.getInterfaceid() + "' limit 1");
                    query = query.replace("myPackage:", myPackage);
                    QueryResult qr = null;
                    if (isPushToSF) qr = binding.query(query);
                    SObject obj = new SObject();
                    obj.setType(getPackage() + "IScheduler_Setting__c");
                    if (qr != null && qr.getSize() > 0) {
                        //found IScheduler_Setting__c
                        SObject sobj = qr.getRecords()[0];
                        String extid = (String) sobj.getField(getPackage() + "Scheduler_Type2__c");
                        String integrationid = (String) sobj.getField(getPackage() + "Integration__c");
                        String interfaceid = (String) sobj.getField(getPackage() + "Interface__c");
                        obj.setField(getPackage() + "Integration__c", integrationid);
                        obj.setField(getPackage() + "Interface__c", interfaceid);
                        obj.setField(getPackage() + "Scheduler_Type2__c", extid);
                        isch.setIntegrationid(integrationid);
                        isch.setInterfaceid(interfaceid);
                        isch.setSchedulertype(extid);
                        //mode update
                    } else {
                        //if not found IScheduler_Setting__c, if it has no interfaceid and integration id we query from sf
                        if (StringUtils.isNotBlank(isch.getIntegrationid()) && StringUtils.isNotBlank(isch.getInterfaceid())) {
                            String type = ISchedulerSettingHelper.getSchedulerTypeKey(ISchedulerSettingHelper.PER_INTERFACE_AGENT_SCHEDULER, isch.getInterfaceid());
                            isch.setSchedulertype(type);
                            obj.setField(getPackage() + "Scheduler_Type2__c", type);
                            obj.setField(getPackage() + "Job_Name__c", ISchedulerSettingHelper.getJobName(isch.getInterfaceid()));
                        } else if (StringUtils.isNotBlank(isch.getIntegrationid())) {
                            String type = ISchedulerSettingHelper.getSchedulerTypeKey(stype, isch.getIntegrationid());
                            obj.setField(getPackage() + "Scheduler_Type2__c", type);
                            obj.setField(getPackage() + "Job_Name__c", ISchedulerSettingHelper.getJobName(isch.getIntegrationid()));
                            isch.setSchedulertype(type);
                        } else {
                            //no integration id , interface id and find in salesforce not found
                            continue;
                        }
                    }

                    // expression here
                    JsonObject cronexpression = isch.getCronExpression();
                    obj.setField(getPackage() + "Integration__c", isch.getIntegrationid());
                    obj.setField(getPackage() + "Interface__c", isch.getInterfaceid());
                    obj.setField(getPackage() + "Frequency__c", com.iservice.utils.StringUtils.getString(cronexpression, ISchedulerSettingHelper.FREQUENCY));
                    obj.setField(getPackage() + "Time_Interval__c", com.iservice.utils.StringUtils.getString(cronexpression, ISchedulerSettingHelper.RUN_EVERY));
                    obj.setField(getPackage() + "Description__c", "");
                    obj.setField(getPackage() + "Cron_Expression__c", cronexpression.toString());
                    SObject[] sobjs = new SObject[1];
                    sobjs[0] = obj;

                    if (isPushToSF) {
                        isch.setSyncstatus(ISchedulerSettingHelper.SFSYNC);
                        //Scheduler_Type2__c is unique
                        UpsertResult[] sr = binding.upsert(getPackage() + "Scheduler_Type2__c", sobjs);
                        //handle error here if we need
                        if (sr != null) {
                            for (int k = 0; k < sr.length; k++) {
                                if (!sr[k].isSuccess() && sr[k].getErrors() != null) {
                                    isch.setSyncstatus(ISchedulerSettingHelper.ERROR);
                                }
                            }
                        }
                    }
                    lsRes.add(new Gson().toJsonTree(isch).getAsJsonObject());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return lsRes;
    }

    public ISchedulerSetting createISchedulerSetting(ISchedulerSetting isch) throws Exception {
        // Auto-generated method stub a070Y00000B0V0KQAV a090Y00000G0wfwQAB AgentProcessPerInterface#a070Y00000B0V0K

        if (isAgentControlBoard()) {

            SObject obj = new SObject();
            obj.setType(getPackage() + "IScheduler_Setting__c");
            JsonObject cronexpression = isch.getCronExpression();
            obj.setField(getPackage() + "Integration__c", isch.getIntegrationid());
            obj.setField(getPackage() + "Interface__c", isch.getInterfaceid());
            obj.setField(getPackage() + "Frequency__c", com.iservice.utils.StringUtils.getString(cronexpression, ISchedulerSettingHelper.FREQUENCY));
            obj.setField(getPackage() + "Time_Interval__c", com.iservice.utils.StringUtils.getString(cronexpression, ISchedulerSettingHelper.RUN_EVERY));
            obj.setField(getPackage() + "Description__c", "");
            obj.setField(getPackage() + "Cron_Expression__c", cronexpression.toString());
            obj.setField(getPackage() + "Scheduler_Type2__c", isch.getSchedulertype());
            obj.setField(getPackage() + "Job_Name__c", isch.getJobName());

            SObject[] sobjs = new SObject[1];
            sobjs[0] = obj;

            isch.setSyncstatus(ISchedulerSettingHelper.SFSYNC);
            UpsertResult[] sr = getBinding().upsert(getPackage() + "Scheduler_Type2__c", sobjs);

            //handle error here if we need
            if (sr != null) {
                for (int k = 0; k < sr.length; k++) {
                    if (!sr[k].isSuccess() && sr[k].getErrors() != null) {
                        isch.setSyncstatus(ISchedulerSettingHelper.ERROR);
                    }
                }
            }
        }
        return isch;
    }

    public List<SObject> getAgentProperty(String settingId) throws Exception {
        List<SObject> lstSObject = new ArrayList<SObject>();
        String query = "SELECT Id, " + getPackage() + "AgentSetting__c, " + getPackage() + "Name__c, " + getPackage() + "Value__c From " + getPackage() + "AgentProperty__c where " + getPackage() + "AgentSetting__c = '" + settingId + "'";
        QueryResult qr = getBinding().query(query);
        if (qr != null && qr.isDone() && qr.getSize() > 0) {
            for (SObject sObj : qr.getRecords()) {
                lstSObject.add(sObj);
            }
        }
        return lstSObject;
    }

    public String getAgentSettingIdByFileName(String filename) throws Exception {
        String query = "Select Id, Name From " + getPackage() + "AgentSetting__c where Name = '" + filename + "'";
        QueryResult qr = getBinding().query(query);
        if (qr != null && qr.isDone() && qr.getSize() > 0) {
            return (String) qr.getRecords()[0].getField("Id");
        }
        return null;
    }

    public void createSettingToSF() throws Exception {

        if (isAgentControlBoard()) {

            String fileName = PropertySettingDao.getInstance().getFilename();
            MapSFConnInfo mapSFConnInfo = getConnectionInfo();

            String myPackage = getPackage();
            String settingId = "";
            SObject obj = new SObject();
            obj.setType(myPackage + "AgentSetting__c");
            obj.setField("Name", fileName);

            settingId = getAgentSettingIdByFileName(fileName);

            if (StringUtils.isBlank(settingId)) {
                List<String> ids = upsert(new SObject[]{obj});
                settingId = ids.get(0);
            } else {
                obj.setField("Id", settingId);
                SaveResult[] sr = getBinding().update(new SObject[]{obj});
                //handle error here if we need
                if (sr != null) {
                    for (int k = 0; k < sr.length; k++) {
                        if (!sr[k].isSuccess() && sr[k].getErrors() != null) {
                            throw new Exception(sr[k].toString());
                        }
                    }
                }
            }

            // get AgentProperty
            List<SObject> agentProperties = getAgentProperty(settingId);

            Map<String, String> mapProperties = new HashMap<String, String>();
            mapProperties.put(MapSFConnInfo.USERNAME_P, mapSFConnInfo.getUsername());
            mapProperties.put(MapSFConnInfo.PASSWORD_P, mapSFConnInfo.getPassword());
            mapProperties.put(MapSFConnInfo.TOKEN_P, mapSFConnInfo.getToken());
            mapProperties.put(MapSFConnInfo.SERVER_ENVIRONMENT, mapSFConnInfo.getServerEnvironment());
            mapProperties.put(MapSFConnInfo.PACKAGE_P, mapSFConnInfo.getSkyvvaPackage());
            mapProperties.put(MapSFConnInfo.PUSH_LOGS2SF, mapSFConnInfo.getPushLogs2SF());

            mapProperties.put(MapSFConnInfo.PROXY_USED, mapSFConnInfo.getProxyUse());
            mapProperties.put(MapSFConnInfo.PROXY_HOST, mapSFConnInfo.getProxyHost());
            mapProperties.put(MapSFConnInfo.PROXY_PORT, mapSFConnInfo.getProxyPort());
            mapProperties.put(MapSFConnInfo.PROXY_USERNAME, mapSFConnInfo.getProxyUsername());
            mapProperties.put(MapSFConnInfo.PROXY_PASSWORD, mapSFConnInfo.getProxyPassword());

            mapProperties.put(MapSFConnInfo.AGENT_USERNAME, mapSFConnInfo.getAgentUsername());
            mapProperties.put(MapSFConnInfo.AGENT_PASSWORD, mapSFConnInfo.getAgentPassword());
            mapProperties.put(MapSFConnInfo.HOST_NAME, mapSFConnInfo.getHostName());
            mapProperties.put(MapSFConnInfo.PORT_FORWARD, String.valueOf(mapSFConnInfo.getPortForward()));

            List<SObject> lstSObj = new ArrayList<SObject>();
            List<Prefs> lstPrefs = new ArrayList<Prefs>();
            for (String key : mapProperties.keySet()) {
                SObject sObj = new SObject();
                sObj.setType(myPackage + "AgentProperty__c");
                sObj.setField(myPackage + "Name__c", key);
                sObj.setField(myPackage + "Value__c", mapProperties.get(key));
                sObj.setField(myPackage + "AgentSetting__c", settingId);
                lstSObj.add(sObj);

                Prefs pref = new Prefs();
                pref.setUsername(mapSFConnInfo.getUsername());
                pref.setKey(key);
                pref.setValue(mapProperties.get(key));
                lstPrefs.add(pref);
            }

            SObject sObj[] = new SObject[lstSObj.size()];

            // agent properties empty
            if (agentProperties != null && agentProperties.size() == 0) {
                for (int i = 0; i < lstSObj.size(); i++) {
                    sObj[i] = lstSObj.get(i);
                }
                List<String> propertyIds = upsert(sObj);
                Map<String, Prefs> mapPrefs = new HashMap<String, Prefs>();
                int i, j;
                for (i = 0; i < propertyIds.size(); i++) {
                    j = i;
                    lstPrefs.get(j).setSfid(propertyIds.get(i));
                    Prefs prefs = new Prefs();
                    prefs.setKey(lstPrefs.get(j).getKey());
                    prefs.setSfid(propertyIds.get(i));
                    mapPrefs.put(lstPrefs.get(j).getKey(), prefs);
                }
                PropertySettingDao.getInstance().setMapPrefs(mapPrefs);
            }
            // update
            else {
                Map<String, Prefs> mapPrefs = new HashMap<String, Prefs>();
                int i, j;
                for (i = 0; i < agentProperties.size(); i++) {
                    j = i;
                    if (lstPrefs.size() <= i) continue;
                    lstPrefs.get(j).setValue(String.valueOf(agentProperties.get(i).getChild(myPackage + "Value__c").getValue()));
                    lstPrefs.get(j).setSfid(String.valueOf(agentProperties.get(i).getChild("Id").getValue()));
                    Prefs prefs = new Prefs();
                    prefs.setKey(lstPrefs.get(j).getKey());
                    prefs.setSfid(String.valueOf(agentProperties.get(i).getChild("Id").getValue()));
                    mapPrefs.put(lstPrefs.get(j).getKey(), prefs);
                }
                PropertySettingDao.getInstance().setMapPrefs(mapPrefs);

                for (i = 0; i < lstSObj.size(); i++) {
                    String objId = PropertySettingDao.getInstance().getMapPrefs().get(lstSObj.get(i).getField(myPackage + "Name__c")).getSfid();
                    lstSObj.get(i).setField("Id", objId);
                    sObj[i] = lstSObj.get(i);
                }

                SaveResult[] sr = getBinding().update(sObj);
                //handle error here if we need
                if (sr != null) {
                    for (int k = 0; k < sr.length; k++) {
                        if (!sr[k].isSuccess() && sr[k].getErrors() != null) {
                            throw new Exception(sr[k].toString());
                        }
                    }
                }
            }
            PropertySettingDao.getInstance().savePropertiesToDatabase(mapConInfo);
        }
    }

    public void removeAgentSetting() {
        if (isAgentControlBoard()) {

        }
    }
    //EndPush

    public void clearMapIntegrations() {
        getCache().clearMapIntegrations();
    }

}