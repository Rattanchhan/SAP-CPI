package com.iservice.database;

import java.util.*;

import com.iservice.model.ProcessingJobInfo;
import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;

import static java.util.function.Function.identity;

/**
 * Store all processing jobs created by agent
 */
public class SFProcessingJobDao extends BaseDao {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(SFProcessingJobDao.class);

    public static final String SALESFORCE_USER = "salesforce_user";
    public static final String BULKVERSION = "bulkversion";
    public static final String INTEGRATION_ID = "integrationId";
    public static final String INTERFACE_ID = "interfaceId";
    public static final String EXTERNAL_ID = "externalFieldId";
    public static final String JOB_TYPE = "jobType";
    public static final String STATUS = "status";
    public static final String JOBID = "jobid";
    public static final String PARENT_JOBID = "parent_jobid";
    public static final String TBL_SF_PROCESSING_JOB = "tblSFProcessingJob";
    public static final List<String> TABLE_FIELDS = Arrays.asList(JOBID, STATUS, INTERFACE_ID, INTEGRATION_ID, BULKVERSION, SALESFORCE_USER, PARENT_JOBID, EXTERNAL_ID, JOB_TYPE);

    @Override
    public List<String> getFields() {
        return new ArrayList<String>(TABLE_FIELDS);
    }

    @Override
    public String getTableName() {
        return TBL_SF_PROCESSING_JOB;
    }

    @Override
    public String[] getIndexes() {
        return new String[]{JOBID, INTERFACE_ID, INTEGRATION_ID, PARENT_JOBID};
    }

    @Override
    public String[] getUniques() {
        return new String[]{JOBID};
    }

    public synchronized void insert(ProcessingJobInfo job) {
        LOGGER.trace("Job {}: inserting {}", job.getJobid(), job);
        Long maxAgentIdFromCachedJobs = CacheSqlite.getInstance().getProcessingJobInfor().values().stream()
            .map(ProcessingJobInfo::getAgent_id)
            .max(Comparator.comparing(identity()))
            .orElse(0L);

        Long agentId = maxAgentIdFromCachedJobs + 1;
        LOGGER.trace("Job {}: generated agent id {}", job.getJobid(), agentId);
        job.setAgent_id(agentId);
        CacheSqlite.getInstance().saveProcessingJobInfor(job);
    }

    public void upsert(ProcessingJobInfo job) {
        LOGGER.trace("Job {}: upserting {}", job.getJobid(), job);
        if (job.getAgent_id() != null) {
            CacheSqlite.getInstance().saveProcessingJobInfor(job);
        } else {
            insert(job);
        }
    }

    public void update(ProcessingJobInfo job) {
        LOGGER.trace("Job {}: updating {}", job.getJobid(), job);
        CacheSqlite.getInstance().saveProcessingJobInfor(job);
    }

    public List<ProcessingJobInfo> getAllProcessingJobs() {
        List<ProcessingJobInfo> allProcessingJobs = new ArrayList<>(CacheSqlite.getInstance().getProcessingJobInfor().values());
        LOGGER.trace("Found {} processing jobs: {}", allProcessingJobs.size(), allProcessingJobs);
        return allProcessingJobs;
    }

    public void deleteById(String id) {
        LOGGER.trace("Deleting job by agent id '{}'", id);
        if (CacheSqlite.getInstance().getProcessingJobInfor().get(Long.valueOf(id)) != null) {
            CacheSqlite.getInstance().getProcessingJobInfor().remove(Long.valueOf(id));
        }
    }

}
