package com.apsaraconsulting.skyvvaadapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.it.api.ITApiFactory;
import com.sap.it.api.exception.InvalidContextException;
import com.sap.it.api.securestore.SecureStoreService;
import com.sap.it.api.securestore.UserCredential;
import com.sap.it.api.securestore.exception.SecureStoreException;
import lombok.Getter;
import lombok.Setter;
import org.apache.camel.RuntimeCamelException;
import com.apsaraconsulting.skyvvaadapter.internal.PayloadFormat;
import com.apsaraconsulting.skyvvaadapter.internal.SkyvvaApiVersion;
import com.apsaraconsulting.skyvvaadapter.internal.SkyvvaIntegrationMode;
import com.apsaraconsulting.skyvvaadapter.internal.dto.EventBodyExtractionStrategy;
import com.apsaraconsulting.skyvvaadapter.internal.dto.NotifyForFieldsEnum;
import com.apsaraconsulting.skyvvaadapter.internal.dto.NotifyForOperationsEnum;
import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiAdapterLoggingLevel;
import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriParams;

import java.util.*;

/**
 * Salesforce Endpoint configuration.
 */
@UriParams
public class SalesforceEndpointConfig implements Cloneable {

    public static final String DEFAULT_SALESFORCE_API_VERSION = "52.0";
    public static final String DEFAULT_SKYVVA_APP_VERSION = "2.48.0";

    public enum AdapterUiParameters {
        LOGIN_URL("loginUrl"),
        CLIENT_ID("clientId"), // for 1.0.* versions of UI
        CLIENT_SECRET("clientSecret"), // for 1.0.* versions of UI
        CPI_CLIENT_CREDENTIAL("clientCredential"), // for >= 1.1.* versions of UI
        CPI_USER_CREDENTIAL("userCredential"),
        TOPIC("topic"), // Sender Adapter topic
        // -------------------------------------
        INTEGRATION("salesforceIntegration"),
        INTERFACE("salesforceInterface"),
        MODE("skyvvaIntegrationMode"),
        REQUEST_FORMAT("requestFormat"),
        RESPONSE_FORMAT("responseFormat"),
        API_VERSION("apiVersion"),
        SKYVVA_API_VERSION("skyvvaApiVersion"),
        SKYVVA_APP_VERSION("skyvvaAppVersion"),
        APEX_METHOD("apexMethod"),
        APEX_URL("apexUrl"),
        // -------------------------------------
        LOGGING_LEVEL("loggingOption"),
        ENABLE_RETRY("enableRetry"), //enable retry functionality for fatal connectivity errors
        HTTP_CLIENT_TIMEOUT("httpClientTimeout"); // time unit: seconds


        private final String parameterName;

        AdapterUiParameters(String parameterName) {
            this.parameterName = parameterName;
        }

        public String getParameterName() {
            return parameterName;
        }
    }

    // parameters for Rest API
    public static final String REST_API = "restApi";
    public static final String SOBJECT_NAME = "sObjectName";
    public static final String SOBJECT_ID = "sObjectId";
    public static final String SOBJECT_FIELDS = "sObjectFields";
    public static final String SOBJECT_EXT_ID_NAME = "sObjectIdName";
    public static final String SOBJECT_EXT_ID_VALUE = "sObjectIdValue";
    public static final String SOBJECT_BLOB_FIELD_NAME = "sObjectBlobFieldName";
    public static final String SOBJECT_CLASS = "sObjectClass";
    public static final String SOBJECT_QUERY = "sObjectQuery";
    public static final String SOBJECT_SEARCH = "sObjectSearch";


    // parameters for Streaming API
    public static final String DEFAULT_REPLAY_ID = "defaultReplayId";
    public static final String INITIAL_REPLAY_ID_MAP = "initialReplayIdMap";
    public static final String EVENT_BODY_EXTRACTION_STRATEGY = "eventBodyExtractionStrategy";


    // default maximum authentication retries on failed authentication or expired session
    public static final int DEFAULT_MAX_AUTHENTICATION_RETRIES = 4;

    // default increment and limit for Streaming connection restart attempts
    public static final long DEFAULT_BACKOFF_INCREMENT = 1000L;
    public static final long DEFAULT_MAX_BACKOFF = 30000L;


    public static final String NOT_FOUND_BEHAVIOUR = "notFoundBehaviour";

    public static final String SERIALIZE_NULLS = "serializeNulls";

    // --------------- parameters that are configurable from CPI adapter UI  ---------------

    // 1. Tab 'Connection'
    @UriParam
    @Getter @Setter
    private String loginUrl; // authentication url or the base url
    @UriParam
    @Getter @Setter
    private String clientId; // for 1.0.* versions of UI
    @UriParam
    @Getter @Setter
    private String clientSecret; // for 1.0.* versions of UI
    @UriParam
    @Getter @Setter
    private String clientCredential; // for >= 1.1.* versions of UI
    @UriParam
    @Getter @Setter
    private String userCredential;

    // 2. Tab 'Skyvva'
    @UriParam
    @Getter @Setter
    private String topic; // Sender Operation - to Bypass Default uri Param for better ui
    @UriParam
    @Getter @Setter
    private String salesforceIntegration;
    @UriParam
    @Getter @Setter
    private String salesforceInterface;
    @UriParam
    @Getter @Setter
    private SkyvvaIntegrationMode skyvvaIntegrationMode = SkyvvaIntegrationMode.SYNCHRONOUS;
    @UriParam
    @Getter @Setter
    private PayloadFormat requestFormat = PayloadFormat.JSON;
    @UriParam
    @Getter @Setter
    private PayloadFormat responseFormat = PayloadFormat.JSON;
    @UriParam
    @Getter @Setter
    private SkyvvaApiVersion skyvvaApiVersion = SkyvvaApiVersion.V2;
    @UriParam
    @Getter @Setter
    private String skyvvaAppVersion = DEFAULT_SKYVVA_APP_VERSION;
    @UriParam
    @Getter @Setter
    private String apiVersion = DEFAULT_SALESFORCE_API_VERSION;
    @UriParam
    @Getter @Setter
    private String apexMethod = "POST"; // only for V2 to have backward compatibility, default value is for a full backward compatibility
    @UriParam
    @Getter @Setter
    private String apexUrl = "skyvvasolutions/Integrate"; // only for V2 to have backward compatibility is for a full backward compatibility
    @UriParam
    @Getter @Setter
    private boolean enableRetry;
    @UriParam
    @Getter @Setter
    private String loggingOption;
    @UriParam
    @Getter @Setter
    private String httpClientTimeout;
    // -------------------------------------------------------------------------------------


    // Rest API properties
    @UriParam
    @Getter @Setter
    private boolean rawPayload;
    @UriParam
    @Getter @Setter
    private String restApi; // in fact, it's used only in tests for Streaming API
    @UriParam(displayName = "SObject Name")
    @Getter @Setter
    private String sObjectName;
    @UriParam(displayName = "SObject Id")
    @Getter @Setter
    private String sObjectId;
    @UriParam(displayName = "SObject Fields")
    @Getter @Setter
    private String sObjectFields;
    @UriParam(displayName = "SObject Id Name")
    @Getter @Setter
    private String sObjectIdName;
    @UriParam(displayName = "SObject Id Value")
    @Getter @Setter
    private String sObjectIdValue;
    @UriParam(displayName = "SObject Blob Field Name")
    @Getter @Setter
    private String sObjectBlobFieldName;
    @UriParam(displayName = "SObject Class")
    @Getter @Setter
    private String sObjectClass;
    @UriParam(displayName = "SObject Query")
    @Getter @Setter
    private String sObjectQuery;
    @UriParam(displayName = "SObject Search")
    @Getter @Setter
    private String sObjectSearch;
    @UriParam(displayName = "Serialize NULL values")
    @Getter @Setter
    private boolean serializeNulls;


    // Streaming API properties
    @UriParam
    @Getter @Setter
    private EventBodyExtractionStrategy eventBodyExtractionStrategy = EventBodyExtractionStrategy.EXTRACT_BUSINESS_DATA_AS_BODY; //TODO add usage of that parameter on UI if needs
    @UriParam
    @Getter @Setter
    private boolean updateTopic;
    @UriParam
    @Getter @Setter
    private NotifyForFieldsEnum notifyForFields;
    @UriParam
    @Getter @Setter
    private NotifyForOperationsEnum notifyForOperations;
    @UriParam
    @Getter @Setter
    private Boolean notifyForOperationCreate;
    @UriParam
    @Getter @Setter
    private Boolean notifyForOperationUpdate;
    @UriParam
    @Getter @Setter
    private Boolean notifyForOperationDelete;
    @UriParam
    @Getter @Setter
    private Boolean notifyForOperationUndelete;
    @UriParam
    @Getter @Setter
    private Long defaultReplayId; // not used on adapter UI
    @UriParam
    @Setter
    private Map<String, Long> initialReplayIdMap; // not used on adapter UI
    // Streaming connection restart attempt backoff interval increment
    @UriParam
    @Getter @Setter
    private long backoffIncrement = DEFAULT_BACKOFF_INCREMENT; // Not used on UI
    // Streaming connection restart attempt maximum backoff interval
    @UriParam
    @Getter @Setter
    private long maxBackoff = DEFAULT_MAX_BACKOFF; // Not used on UI
    @UriParam
    @Getter @Setter
    private NotFoundBehaviour notFoundBehaviour = NotFoundBehaviour.EXCEPTION;

    private SalesforceLoginConfig loginConfig; // lazily loaded from login parameters

    // Salesforce Jetty9 HttpClient, set using reference
    @Getter @Setter
    private SalesforceHttpClient httpClient;

    // To allow custom ObjectMapper (for registering extra datatype modules)
    @Getter @Setter
    private ObjectMapper objectMapper;


    public SalesforceEndpointConfig copy() {
        try {
            final SalesforceEndpointConfig copy = (SalesforceEndpointConfig) super.clone();
            // nothing to deep copy, getApexQueryParams() is readonly, so no need to deep copy
            return copy;
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeCamelException(ex);
        }
    }

    public SalesforceLoginConfig getLoginConfig() {
        String userName;
        String password;
        String clientId = this.clientId;
        String clientSecret = this.clientSecret;
        if (this.userCredential != null) {
            try {
                SecureStoreService secureStoreService = ITApiFactory.getService(SecureStoreService.class, null);
                UserCredential uc = secureStoreService.getUserCredential(this.userCredential);
                userName = uc.getUsername();
                password = new String(uc.getPassword());

                if (this.clientCredential != null) {
                    UserCredential cpiClientCredential = secureStoreService.getUserCredential(this.clientCredential);
                    clientId = cpiClientCredential.getUsername();
                    clientSecret = new String(cpiClientCredential.getPassword());
                }
            } catch (InvalidContextException e) {
                throw new IllegalStateException("InvalidException Context while Reading from store", e);
            } catch (SecureStoreException e) {
                throw new IllegalStateException("Secure Store access or Invalid Exception", e);
            }
            if (this.loginConfig == null) {
                loginConfig = new SalesforceLoginConfig(this.loginUrl, clientId, clientSecret, userName, password, false);
            }
        }
        return loginConfig;
    }

    public CpiLoggingDecorator.LogLevel getLoggingOptionAsLogLevel() {
        return CpiAdapterLoggingLevel.getLoggingOptionAsLogLevel(getLoggingOption());
    }

    public Map<String, Object> toValueMap() {

        final Map<String, Object> valueMap = new HashMap<>();

        valueMap.put(AdapterUiParameters.TOPIC.getParameterName(), topic);
        valueMap.put(AdapterUiParameters.INTEGRATION.getParameterName(), salesforceIntegration);
        valueMap.put(AdapterUiParameters.INTERFACE.getParameterName(), salesforceInterface);
        valueMap.put(AdapterUiParameters.MODE.getParameterName(), skyvvaIntegrationMode);
        valueMap.put(AdapterUiParameters.REQUEST_FORMAT.getParameterName(), requestFormat);
        valueMap.put(AdapterUiParameters.RESPONSE_FORMAT.getParameterName(), responseFormat);
        valueMap.put(AdapterUiParameters.SKYVVA_API_VERSION.getParameterName(), skyvvaApiVersion);
        valueMap.put(AdapterUiParameters.SKYVVA_APP_VERSION.getParameterName(), skyvvaAppVersion);
        valueMap.put(AdapterUiParameters.APEX_METHOD.getParameterName(), apexMethod);
        valueMap.put(AdapterUiParameters.APEX_URL.getParameterName(), apexUrl);
        valueMap.put(AdapterUiParameters.LOGGING_LEVEL.getParameterName(), loggingOption);
        valueMap.put(AdapterUiParameters.ENABLE_RETRY.getParameterName(), enableRetry);
        valueMap.put(AdapterUiParameters.HTTP_CLIENT_TIMEOUT.getParameterName(), httpClientTimeout);
        valueMap.put(AdapterUiParameters.API_VERSION.getParameterName(), apiVersion);

        valueMap.put(REST_API, restApi);
        valueMap.put(SOBJECT_NAME, sObjectName);
        valueMap.put(SOBJECT_ID, sObjectId);
        valueMap.put(SOBJECT_FIELDS, sObjectFields);
        valueMap.put(SOBJECT_EXT_ID_NAME, sObjectIdName);
        valueMap.put(SOBJECT_BLOB_FIELD_NAME, sObjectBlobFieldName);
        valueMap.put(SOBJECT_EXT_ID_VALUE, sObjectIdValue);
        valueMap.put(SOBJECT_CLASS, sObjectClass);
        valueMap.put(SOBJECT_QUERY, sObjectQuery);
        valueMap.put(SOBJECT_SEARCH, sObjectSearch);
        valueMap.put(SERIALIZE_NULLS, serializeNulls);

        // add streaming API properties
        valueMap.put(DEFAULT_REPLAY_ID, defaultReplayId);
        valueMap.put(INITIAL_REPLAY_ID_MAP, initialReplayIdMap);
        valueMap.put(EVENT_BODY_EXTRACTION_STRATEGY, eventBodyExtractionStrategy);

        valueMap.put(NOT_FOUND_BEHAVIOUR, notFoundBehaviour);

        return Collections.unmodifiableMap(valueMap);
    }

    public Map<String, Long> getInitialReplayIdMap() {
        return Optional.ofNullable(initialReplayIdMap).orElse(Collections.emptyMap());
    }
}
