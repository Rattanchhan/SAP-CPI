package com.apsaraconsulting.skyvvaadapter.internal.logger;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.JSONArray;
import org.slf4j.Logger;

/**
 * Created by msebi on 2/8/2019.
 */
public class SkyvvaLogger {

    public enum LogOptionsEnum {
        A(SkyvvaLoggerConstants.LOG_NONE),
        B(SkyvvaLoggerConstants.LOG_BULK_V2_RECORDS),
        C(SkyvvaLoggerConstants.LOG_PAYLOAD),
        D(SkyvvaLoggerConstants.LOG_BULK_PAYLOAD),
        E(SkyvvaLoggerConstants.LOG_DEBUG);

        private String text;

        LogOptionsEnum(String text) {
            this.text = text;
        }

        public String getText() {
            return this.text;
        }

        public static LogOptionsEnum fromString(String text) {
            for (LogOptionsEnum b : LogOptionsEnum.values()) {
                if (b.text.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    public static JSONObject generateDebugJSONObjectFromString(final Logger LOG,
                                                               final String prefix,
                                                               final String msg) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(SkyvvaLoggerConstants.LOG_PREFIX, prefix);
            jsonObject.put(SkyvvaLoggerConstants.LOG_DEBUG_PAYLOAD, msg);
        } catch (JSONException e) {
            LOG.error(prefix + " Exception occurred while creating JSON debug message: " + e);
        }
        return jsonObject;
    }

    public static JSONObject generateDebugJSONObjectFromString(final Logger LOG,
                                                               final String prefix,
                                                               final String jobId,
                                                               final String unprocessedRecords,
                                                               final String failedRecords) {
        JSONObject jsonObject = new JSONObject();
        JSONObject unprocessedRecordsJSON = new JSONObject();
        JSONObject failedRecordsJSON = new JSONObject();
        JSONArray bulkRecordsJSON = new JSONArray();
        try {
            unprocessedRecordsJSON.put(SkyvvaLoggerConstants.LOG_BULK_V2_BULK_UNPROCESSED_RECORDS, unprocessedRecords);
            failedRecordsJSON.put(SkyvvaLoggerConstants.LOG_BULK_V2_BULK_FAILED_RECORDS, failedRecords);
            bulkRecordsJSON.add(unprocessedRecordsJSON);
            bulkRecordsJSON.add(failedRecordsJSON);
            jsonObject.put(SkyvvaLoggerConstants.LOG_PREFIX, prefix);
            jsonObject.put(SkyvvaLoggerConstants.LOG_BULK_V2_JOB_ID, jobId);
            jsonObject.put(SkyvvaLoggerConstants.LOG_BULK_V2_BATCH_ID, SkyvvaLoggerConstants.LOG_BULK_V2_NO_BATCHES);
            jsonObject.put(SkyvvaLoggerConstants.LOG_BULK_V2_BULK_RECORDS, bulkRecordsJSON);
        } catch (JSONException e) {
            LOG.error(prefix + " Exception occurred while creating bulk JSON debug message: " + e);
        }
        return jsonObject;
    }

    public static void logEntry(final Logger LOG, final String logOption, final JSONObject content) {
        LogOptionsEnum logOptionsEnum = LogOptionsEnum.fromString(logOption);

//        LOG.error("In logEntry():");
//        LOG.error("logOption: " + logOption);
//        LOG.error("logOptionsEnum: " + logOptionsEnum);

        String prefix = "";

        try {
            prefix = String.valueOf(content.get(SkyvvaLoggerConstants.LOG_PREFIX));
        } catch (JSONException e) {
            LOG.error("Exception occurred while parsing prefix from content " + content + " e: " + e);
            return;
        }

        if(logOptionsEnum == null) {
            LOG.error(prefix + " Log option " + logOption + " not supported");
            return;
        }

        // Variables used to store bulk operation related parameters
        String jobId, batchId, payload, bulkRecords, tmp;

        switch (logOptionsEnum) {
            // No logging
            case A: break;

            // Bulk V2 get unprocessed and failed records
            case B:
                try {
                    jobId = String.valueOf(content.get(SkyvvaLoggerConstants.LOG_BULK_V2_JOB_ID));
                    batchId = String.valueOf(content.get(SkyvvaLoggerConstants.LOG_BULK_V2_BATCH_ID));
                    bulkRecords = String.valueOf(content.get(SkyvvaLoggerConstants.LOG_BULK_V2_BULK_RECORDS));
                } catch (JSONException e) {
                    // Uncomment for debugging purposes; generates a lot of messages in log file if uncommented
                    // and bulk V2 log option is selected
                    // LOG.error(prefix + " Failed to get jobId/batchId in logger. Exception occurred: " + e);
                    break;
                }

                tmp = prefix + "\nStatus (unprocessed/failed records)for bulk operation with <jobId:batchId>: " +
                        "<" + jobId + ":" + batchId + ">:\n";
                tmp += new String(new char[tmp.length() - prefix.length() - 3]).replace("\0", "-") + "\n";
                LOG.error(tmp + bulkRecords + "\n");
                break;

            // Get and log payload for integrations other than bulk
            case C:
                try {
                    payload = String.valueOf(content.get(SkyvvaLoggerConstants.LOG_DEBUG_PAYLOAD));
                } catch (JSONException e) {
                    LOG.error(prefix + " Failed to get payload in logger. Exception occurred: " + e);
                    break;
                }

                if(payload != null)
                    LOG.error(prefix + " Payload: " + payload);
                break;

            // Get and log payload for bulk
            case D:
                try {
                    payload = String.valueOf(content.get(SkyvvaLoggerConstants.LOG_DEBUG_PAYLOAD));
                } catch (JSONException e) {
                    LOG.error(prefix + " Failed to get bulk payload in logger. Exception occurred: " + e);
                    break;
                }

                if(payload != null)
                    LOG.error(prefix + " Bulk payload: " + payload);
                break;

            // Log debug messages
            case E:
                try {
                    payload = String.valueOf(content.get(SkyvvaLoggerConstants.LOG_DEBUG_PAYLOAD));
                } catch (JSONException e) {
                    LOG.error(prefix + " Failed to get payload in logger. Exception occurred: " + e);
                    break;
                }

                if(payload != null)
                    LOG.error(prefix + payload);
                break;
        }
    }
}
