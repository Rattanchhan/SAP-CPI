package com.apsaraconsulting.skyvvaadapter.internal.logger;

/**
 * Created by msebi on 2/8/2019.
 */
public class SkyvvaLoggerConstants {
    // Sample unprocessed/ failed records
//    Unprocessed records:
//    --------------------
//    Birthdate,Department,Email,FirstName,LastName,Phone,Title,ERP_Contact_ID__c,Account.ERP_DEBTOR_ID__c
//    "21-03-1978",Sales,seba@gmail.com,Sebastian,Miga,087 2113-4444,Java Developer,15,0000301557
//
//    Failed records:
//    ---------------
//    "sf__Id","sf__Error",Account.ERP_DEBTOR_ID__c,Birthdate,Department,ERP_Contact_ID__c,Email,FirstName,LastName,Phone,Title

    // Values used to get settings from HCI config
    public static final String LOG_NONE = "None";
    public static final String LOG_BULK_V2_RECORDS = "Bulk v2 fetch unprocessed and failed records";
    public static final String LOG_PAYLOAD = "Payload";
    public static final String LOG_BULK_PAYLOAD = "Bulk payload";
    public static final String LOG_DEBUG = "Debug logging";

    // Values used to get fields from the actual payloads sent back from Salesforce
    public static final String LOG_BULK_V2_JOB_ID = "jobId";
    public static final String LOG_BULK_V2_BATCH_ID = "batchId";
    public static final String LOG_BULK_V2_BULK_RECORDS = "bulkRecords";
    public static final String LOG_BULK_V2_BULK_UNPROCESSED_RECORDS = "unprocessedBulkRecords";
    public static final String LOG_BULK_V2_BULK_FAILED_RECORDS = "failedBulkRecords";
    public static final String LOG_DEBUG_PAYLOAD = "debugPayload";
    public static final String LOG_PREFIX = "prefix";
    public static final String LOG_BULK_V2_NO_BATCHES = "N/A";
}
