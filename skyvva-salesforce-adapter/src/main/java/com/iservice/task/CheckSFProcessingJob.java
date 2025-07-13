package com.iservice.task;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.apsaraconsulting.skyvvaadapter.internal.logger.CpiLoggingDecorator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.iservice.bulk.BulkV1Connection;
import com.iservice.bulk.BulkV2Connection;
import com.iservice.bulk.IBulkConnection;
import com.iservice.bulk.jobinfo.IJobInfo;
import com.iservice.bulk.jobinfo.JobV1Info;
import com.iservice.bulk.jobinfo.JobV2Info;
import com.iservice.database.BaseManager;
import com.iservice.database.CacheSFBulkBatchFile;
import com.iservice.database.PropertySettingDao;
import com.iservice.database.SFProcessingJobDao;
import com.iservice.gui.data.IMapping__c;
import com.iservice.gui.data.Interfaces__c;
import com.iservice.gui.helper.Helper;
import com.iservice.model.ProcessingJobInfo;
import com.iservice.model.ProcessingJobInfo.TJOBType;
import com.iservice.model.ProcessingJobInfo.TStatus;
import com.iservice.sforce.MapSFConnInfo;
import com.iservice.sforce.SFIntegrationService;
import com.iservice.task.AbstractBulkDirectObjectEvent.TBulkVersion;
import com.sforce.async.BatchInfo;
import com.sforce.async.OperationEnum;
import com.sforce.soap.SFConnectorConfig;
import com.sforce.ws.util.FileUtil;

import static java.lang.String.format;

/**
 * CheckSFProcessingJob starts the endless loop that checks ProcessingJobInfo entries and completes integration with
 * Skyvva App once execution of Salesforce Bulk on Salesforce side is completed. Endless loop is started in a single
 * thread and uses non-blocking approach.
 */
public class CheckSFProcessingJob {

    private static final CpiLoggingDecorator LOGGER = CpiLoggingDecorator.getLogger(CheckSFProcessingJob.class);

    /**
     * Flag is used to prevent scheduling of job runnable for the second time.
     * It's enough to have only volatile here, as there is only one thread that writes to it
     */
    private static volatile boolean started = false;

    /**
     * When it is true the loop will be end after message created.
     * It's enough to have only volatile here, as there is only one thread that writes to it at one moment
     * (it's synchronized by CheckSFProcessingJob.class)
     */
    private static volatile boolean completed = false;

    /**
     * Flag is used to enable/disable increase of waiting time between reprocessing iterations
     * It's enough to have only volatile here, as there is only one thread that writes to it at one moment (during configuration)
     */
    private static volatile boolean linearBackoffForReprocessingEnabled = true;

    private static ExecutorService sfProcessingJobExecutor;
    // reentrant lock instead of standard monitor to have a conditional check
    private static final Lock checkProcessJobLock = new ReentrantLock();
    private static final Condition checkProcessJobLockCondition = checkProcessJobLock.newCondition();

    private static final CheckProcessJobRunnable checkProcessJobRunnable = new CheckProcessJobRunnable();

    public static void setLinearBackoffForReprocessingEnabled(boolean value) {
        linearBackoffForReprocessingEnabled = value;
    }

    /**
     * Starts endless loop that checks new
     */
    // start method is synchronized on CheckSFProcessingJob.class object
    public static synchronized void start() {
        if (!started) {
            LOGGER.info("Starting job");
            sfProcessingJobExecutor = Executors.newSingleThreadExecutor();
            sfProcessingJobExecutor.submit(checkProcessJobRunnable);
            started = true;
        }
    }

    // current implementation doesn't guarantees that all incomplete jobs will be processed fully,
    // it's just trying to wait for completion of the iteration, otherwise thread pool will be terminated in defined timeout
    // it's possible to enhance it to support that case as well
    public static synchronized void stopGracefully(int timeoutInMillis) {
        timeoutInMillis = timeoutInMillis > 0 ? timeoutInMillis : 30000;
        if (sfProcessingJobExecutor == null) {
            LOGGER.info("Nothing to do, executor has been already stopped");
            return; // nothing to do
        }
        try {
            LOGGER.info("Processing graceful shutdown. Timeout: {} ms", timeoutInMillis);
            completed = true;
            if (checkProcessJobLock.tryLock()) {
                try {
                    LOGGER.trace("Obtained lock inside #stopGracefully");
                    LOGGER.trace("Sending a notification to awake");
                    // awake from wait state
                    checkProcessJobLockCondition.signal();
                } finally {
                    LOGGER.trace("Releasing lock inside #stopGracefully");
                    checkProcessJobLock.unlock();
                }
            }
        } finally {
            started = false;
            try {
                sfProcessingJobExecutor.shutdown();
                boolean isTerminatedGracefully = sfProcessingJobExecutor.awaitTermination(timeoutInMillis, TimeUnit.MILLISECONDS);
                if (isTerminatedGracefully) {
                    LOGGER.info("Graceful shutdown completed");
                } else {
                    LOGGER.warn("Timeout elapsed during graceful shutdown");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Interrupted exception occurred during graceful shutdown");
            } finally {
                sfProcessingJobExecutor = null;
                completed = false;
            }
        }
    }

    public static void notifyAboutNewJobToCheck() {
        // we really need to execute notify only when lock is free,
        // otherwise it means that:
        // 1. it's either obtained by runnable logic - no need to notify in that case because
        // in the next iteration of the loop collection of jobs will be checked again
        // 2. or lock obtained by graceful shutdown - then no need to notify as well
        // Also, if we require a lock here, caller method will stuck until it's able to obtain the lock
        LOGGER.debug("Notifying about new ProcessingJobInfo job to check");
        if (checkProcessJobLock.tryLock()) {
            try {
                LOGGER.trace("Sending a notification");
                checkProcessJobLockCondition.signal();
            } finally {
                checkProcessJobLock.unlock();
            }
        } else {
            LOGGER.trace("No need to send notification, job checking is already running");
        }
    }

    public static boolean isStarted() {
        return started;
    }

    public static boolean isCompleted() {
        return completed;
    }

    // for unit tests or debugging
    static boolean isReprocessingEnabled() {
        return checkProcessJobRunnable.reprocessing;
    }

    // for unit tests or debugging
    static long getReprocessingIterationNumber() {
        return checkProcessJobRunnable.reprocessingIterationNumber.get();
    }

    // for unit tests or debugging
    static long getCurrentReprocessingSleepTimeout() {
        return checkProcessJobRunnable.calculateSleepTimeout();
    }

    // for unit tests or debugging
    protected static CheckProcessJobRunnable getCheckProcessJobRunnable() {
        return checkProcessJobRunnable;
    }

    // runnable is designed to be executed by single thread
    public static class CheckProcessJobRunnable implements Runnable {

        protected static final long INITIAL_REPROCESSING_ITERATION_DURATION_IN_MS = 10000; // 10 sec
        protected static final long MAX_REPROCESSING_ITERATION_DURATION_IN_MS = 600000; // 10 min

        /**
         * Used to do iteration without waiting because there is a case when ProcessingJobInfo is added to cache
         * inside runnable. Reprocessing flag is set back to false once it sees that a ProcessingJobInfo cache is empty
         */
        private volatile boolean reprocessing = false;

        /**
         * User to increase waiting time between reprocessing iterations from initial value.
         * We could also use just volatile here, because only one thread can write to that value,
         * but let's use atomic increment here
         */
        private volatile AtomicLong reprocessingIterationNumber = new AtomicLong(1);

        SFProcessingJobDao sfProcessingJobDao = SFProcessingJobDao.getDao(SFProcessingJobDao.class);
        Interfaces__c intf;

        public CheckProcessJobRunnable() {}

        @Override
        public void run() {
            LOGGER.trace("Starting endless loop");
            while (true) {
                //check job
                try {
                    checkProcessJobLock.lock();
                    LOGGER.trace("Obtained lock inside CheckProcessJob, reprocessing flag: {}, reprocessing iteration: {}",
                        reprocessing, reprocessingIterationNumber.get()
                    );
                    List<ProcessingJobInfo> jobs = sfProcessingJobDao.getAllProcessingJobs();

                    if (!reprocessing && jobs.isEmpty()) {
                        // wait for notification about new entry to check
                        LOGGER.trace("Waiting for notification about new entry to check");
                        checkProcessJobLockCondition.await();
                        // jobs should be fetched again because waiting period can be quite long
                        jobs = sfProcessingJobDao.getAllProcessingJobs();
                    } else if (reprocessing) {
                        // in case of reprocessing we need to wait for some period of time before the next check
                        long sleepTimeout = calculateSleepTimeout();
                        LOGGER.trace("Waiting {} ms to continue reprocessing iteration #{}",
                            sleepTimeout,
                            reprocessingIterationNumber.get()
                        );
                        Thread.sleep(sleepTimeout);
                    }

                    LOGGER.debug("Found {} jobs to check: {}", jobs.size(), jobs);

                    if (completed) {
                        // end the loop
                        deactivateReprocessingForCheckingJob();
                        LOGGER.trace("Completion flag is true, breaking the loop");
                        if (jobs.size() > 0) {
                            LOGGER.warn("{} jobs will be lost due to termination: {}",
                                jobs.size(), jobs.stream().map(ProcessingJobInfo::getJobid).collect(Collectors.joining(", "))
                            );
                        }
                        break;
                    }

                    if (jobs.isEmpty()) {
                        deactivateReprocessingForCheckingJob();
                        LOGGER.trace("List of jobs to check is empty, starting new iteration");
                        continue;
                    }

                    Map<String, List<ProcessingJobInfo>> user2Jobs = new HashMap<>();
                    for (ProcessingJobInfo job : jobs) {
                        List<ProcessingJobInfo> lst = user2Jobs.get(job.getSalesforce_user());
                        if (lst == null) {
                            lst = new ArrayList<>();
                            user2Jobs.put(job.getSalesforce_user(), lst);
                        }
                        lst.add(job);
                    }
                    //do check by user
                    for (String sfUser : user2Jobs.keySet()) {
                        List<ProcessingJobInfo> lst = user2Jobs.get(sfUser);
                        MapSFConnInfo connInfo = PropertySettingDao.getInstance().getAllValues(sfUser);
                        SFIntegrationService service = new SFIntegrationService(connInfo);

                        if (service.login()) {

                            for (ProcessingJobInfo job : lst) {
                                try {
                                    IBulkConnection conn = createBulkConnection(job, service);
                                    if (job.getStatus() == TStatus.ERR) {
                                        LOGGER.trace("Job {} has status ERR, cancelling and deleting job", job.getJobid());
                                        conn.cancelJob(job.getJobid());
                                        //clear batch data from harddisk
                                        deleteJobFromDB(job);
                                    } else if (job.getStatus() == TStatus.OK && reprocessing && reprocessingIterationNumber.get() > 50) {
                                        // TODO remove that else-block once core logic will be fixed
                                        // there is a problem with the transition from OK to CREATE_IMESSAGE,
                                        // if some error occurs during that operation, message became stuck with OK state
                                        LOGGER.trace("Job {} has status OK and reprocessing iteration count is {}, " +
                                            "cancelling and deleting job", job.getJobid(), reprocessingIterationNumber.get());
                                        conn.cancelJob(job.getJobid());
                                        deleteJobFromDB(job);
                                    } else {
                                        doCheckJob(service, conn, job);
                                    }
                                } catch (Throwable e) {
                                    // do not throw any error, only log error
                                    LOGGER.error(format("Error occurred during the processing job %s: %s", job.getJobid(), e.getMessage()), e);
                                }
                            }
                        }
                    }
                    // if reprocessing is still active, increment iteration number
                    if (this.reprocessing) {
                        incReprocessingIterationNumber();
                    }
                } catch (InterruptedException ex) {
                    // keep interrupted state
                    Thread.currentThread().interrupt();
                    LOGGER.error("Interrupted exception occurred during checking bulk jobs", ex);
                } catch (Throwable ex) {
                    // do not throw any error, only log error
                    LOGGER.error("Error occurred during checking bulk jobs: " + ex.getMessage(), ex);
                } finally {
                    LOGGER.trace("Releasing lock inside CheckProcessJob");
                    checkProcessJobLock.unlock();
                }
            }
            LOGGER.trace("Endless loop is terminated");
        }

        protected Map<String, String> sfField2CSVHeader(SFIntegrationService service, String interfaceId) throws Exception {
            Map<String, String> result = new HashMap<String, String>();
            if (StringUtils.isNotBlank(interfaceId) && intf != null && !intf.isExternalMapping()) {
                List<IMapping__c> mappings = service.getIMapping__c(interfaceId);
                if (mappings != null) {
                    for (IMapping__c map : mappings) {
                        if (!StringUtils.equalsIgnoreCase(map.getType__c(), "Formula")) {
                            result.put(map.getTarget__c(), map.getSource__c());
                        }
                    }
                }
            }
            return result;
        }

        public boolean isJobFinish(IBulkConnection conn, ProcessingJobInfo job) throws Exception {

            String[] jobIds = job.getJobIds();
            if (jobIds != null) {
                for (String jobId : jobIds) {
                    IJobInfo jobInfo = conn.getJobInfo(jobId);
                    if (!jobInfo.isFinish()) {
                        return false;
                    }
                }
                //all jobs are finish
                return true;
            }
            return false;
        }

        /**
         * check job status from salesforce
         *
         * @param service
         * @param conn
         * @param job
         * @throws Exception
         */
        protected void doCheckJob(
            SFIntegrationService service,
            IBulkConnection conn,
            ProcessingJobInfo job
        ) throws Exception {
            LOGGER.info("Checking job {}/{}. Integration id: {}, Interface id: {}",
                job.getJobid(),
                job.getStatus(),
                job.getIntegrationId(),
                job.getInterfaceId()
            );
            LOGGER.trace("Job metadata: {}", job);
            // check job status
            if (isJobFinish(conn, job)) {
                intf = service.getInterfaceById(job.getInterfaceId());
                boolean isCreateOnlyError = intf.isIsNotPersistMessage__c() || service.getIntegrationById(intf.getIntegration__c()).getClear_After_Processing__c();
                LOGGER.debug("Job id: {}, Job status: {}, isCreateOnlyError: {}",
                    job.getJobid(),
                    job.getStatus(),
                    isCreateOnlyError
                );
                switch (job.getStatus()) {
                    case CREATE_IMESSAGE:
                        LOGGER.trace("Job {}: processing CREATE_IMESSAGE", job.getJobid());
                        // in this case has only one job id
                        IJobInfo jobInfo = conn.getJobInfo(job.getJobid());
                        if (intf.isBy_Passing_Message__c()) {
                            // do not create any messages (bypass)
                            LOGGER.trace("Job {}: bypassing message option is enabled, so message won't be created, deleting job",
                                job.getJobid()
                            );
                            deleteJobFromDB(job);
                        } else if (isCreateOnlyError) {
                            // create fail message
                            if (jobInfo.getNumberRecordsFailed() > 0) {
                                LOGGER.trace("Job {}: {} records failed. Creating message",
                                    job.getJobid(),
                                    jobInfo.getNumberRecordsFailed()
                                );
                                createMessage(service, conn, job, isCreateOnlyError);
                                // if there is not error
                                // record then job is completed
                                // delete job from db after create message
                            } else {
                                LOGGER.trace("Job {}: no records failed. Deleting job", job.getJobid());
                                deleteJobFromDB(job);
                            }
                        } else if (!intf.isBy_Passing_Message__c() && !isCreateOnlyError) {
                            LOGGER.trace("Job {}: creating message", job.getJobid());
                            createMessage(service, conn, job, isCreateOnlyError);
                        }

                        break;
                    case SEND_MESSAGE:
                        LOGGER.trace("Job {}: processing SEND_MESSAGE", job.getJobid());
                        //check if has reprocess then send to sf first
                        reprocessRecords(service, conn, intf, job);
                        //create imessage on salesforce
                        sendMessageToSalesforce(conn, job);
                        break;
                    case SEND_IDATA:
                        LOGGER.trace("Job {}: processing SEND_IDATA", job.getJobid());
                        //create idata on salesforce
                        sendIDataToSalesforce(conn, job);
                        break;
                    case FINISH:
                        LOGGER.trace("Job {}: processing FINISH", job.getJobid());
                        //delete folder
                        deleteJobFromDB(job);
                        break;
                    case OK:
                        LOGGER.trace("Job {}: processing OK", job.getJobid());
                        // shouldn't happen
                        activateReprocessingForCheckingJob();
                        break;
                    case ERR:
                        LOGGER.trace("Job {}: processing ERR", job.getJobid());
                        // shouldn't happen, but if it is, we must clean that job from cache otherwise we will get
                        // infinite processing on CPI until the redeployment of the IFlow
                        deleteJobFromDB(job);
                        break;
                }

            } else {
                // in this case has only one job id
                IJobInfo jobInfo = conn.getJobInfo(job.getJobid());
                LOGGER.trace("Job id: {}, job info: {}", job.getJobid(), jobInfo);
                if (!jobInfo.isClose()) {
                    if (jobInfo.isInprogress()) {
                        activateReprocessingForCheckingJob();
                        LOGGER.trace("Job {} is in progress, skipping check iteration", job.getJobid());
                        return;
                    }
                    // close the job maybe during integration has problem during
                    // close job
                    conn.closeJobById(job.getJobid());
                    // next step we need to create message
                    job.setStatus(TStatus.CREATE_IMESSAGE);
                    sfProcessingJobDao.upsert(job);
                    LOGGER.trace("Job {} new status: {}", job.getJobid(), job.getStatus());
                    activateReprocessingForCheckingJob();
                } else if (jobInfo.isAbort()) {
                    LOGGER.trace("Job {} is aborted, deleting job", job.getJobid());
                    // delete job from db after user abort the job
                    deleteJobFromDB(job);
                } else {
                    LOGGER.trace("Job {} has state {}, activating reprocessing", job.getJobid(), jobInfo.getJobStatus());
                    activateReprocessingForCheckingJob();
                }
            }

        }

        protected void deleteJobFromDB(ProcessingJobInfo job) throws IOException {
            LOGGER.debug("Deleting job {} (status: {}) from cache", job.getJobid(), job.getStatus());
            sfProcessingJobDao.deleteById("" + job.getAgent_id());
            CacheSFBulkBatchFile.getInstance().getCacheFolderJob().remove(job.getJobFolder());
        }

        protected void sendIDataToSalesforce(IBulkConnection conn,
                                             ProcessingJobInfo job) throws Exception {
            //create idata to salesforce
            File dataFolder = Helper.getJobDataFolder(job);
            File[] dataFiles = dataFolder.listFiles();
            if (dataFiles != null && dataFiles.length > 0) {
                BulkV2Connection conn2 = new BulkV2Connection((SFConnectorConfig) conn.getConfig());
                boolean first = true;
                StringBuffer jobIds = new StringBuffer();
                for (File messageFile : dataFolder.listFiles()) {
                    JobV2Info jobData = null;
                    try {
                        //Found multipe contents for job: <7500Y000008sf1o>, please 'Close' / 'Abort' / 'Delete' the current Job
                        //then create a new Job and make sure you only do 'PUT' once on a given Job.
                        jobData = conn2.createJob(ensureSkyvvaName("IData__c"), OperationEnum.upsert.name(), ensureSkyvvaName("ExtId__c"), "CRLF");
                        if (!first) {
                            jobIds.append(ProcessingJobInfo.ID_SPLITER);
                        }
                        //now add job is message
                        jobIds.append(jobData.getId());
                        first = false;
                        conn2.createBatch(jobData, FileUtil.toBytes(new FileInputStream(messageFile)));
                        conn2.closeJob(jobData);
                    } catch (Exception e) {
                        conn2.abortJob(jobData);
                        throw e;
                    }
                }
                job.setJobid(jobIds.toString());

            }
            job.setJobType(TJOBType.IDATA);
            job.setStatus(TStatus.FINISH);
            job.setBulkversion(TBulkVersion.SFV2);
            sfProcessingJobDao.update(job);
            LOGGER.trace("Job {} new status: {}", job.getJobid(), job.getStatus());
            activateReprocessingForCheckingJob();
        }

        protected String ensureSkyvvaName(String name) {
            String packageName = PropertySettingDao.getInstance().getQueryPackage();
            return packageName + name;
        }

        protected void sendMessageToSalesforce(IBulkConnection conn, ProcessingJobInfo job) throws Exception {
            //create message to salesforce
            List<ByteArrayOutputStream> messages = CacheSFBulkBatchFile.getInstance().getCacheFolderJob().get(job.getJobFolder()).getCsvMessageContent();
            if (messages != null && messages.size() > 0) {
                LOGGER.trace("Job {}: creating and sending {} messages", job.getJobid(), messages.size());
                BulkV2Connection conn2 = new BulkV2Connection((SFConnectorConfig) conn.getConfig());
                //original job
                job.setParent_jobid(job.getJobid());
                //Found multipe contents for job: <7500Y000008sf1o>, please 'Close' / 'Abort' / 'Delete' the current Job
                //then create a new Job and make sure you only do 'PUT' once on a given Job.
                boolean first = true;
                StringBuffer jobIds = new StringBuffer();
                for (ByteArrayOutputStream messageFile : messages) {
                    JobV2Info jobMessage = null;
                    try {

                        jobMessage = conn2.createJob(ensureSkyvvaName("IMessage__c"), OperationEnum.upsert.name(), ensureSkyvvaName("External_Id__c"), "CRLF");
                        if (!first) {
                            jobIds.append(ProcessingJobInfo.ID_SPLITER);
                        }
                        //now add job is message
                        jobIds.append(jobMessage.getId());
                        first = false;
                        conn2.createBatch(jobMessage, messageFile.toByteArray());
                        conn2.closeJob(jobMessage);
                    } catch (Exception ex) {
                        LOGGER.trace(format("Job %s: error occurred during sending IMessage: %s", job.getJobid(), ex.getMessage()), ex);
                        conn2.abortJob(jobMessage);
                        throw ex;
                    }
                }
                String messageJobId = jobIds.toString();
                LOGGER.trace("Job {}: updating job id {} by message job id {}", job.getJobid(), job.getJobid(), messageJobId);
                job.setJobid(messageJobId);
            }
            job.setJobType(TJOBType.IMESSAGE);
            job.setBulkversion(TBulkVersion.SFV2);
            job.setStatus(TStatus.SEND_IDATA);
            sfProcessingJobDao.update(job);
            LOGGER.trace("Job {} new status: {}", job.getJobid(), job.getStatus());
            activateReprocessingForCheckingJob();
        }

        protected void createMessage(
            SFIntegrationService service,
            IBulkConnection conn,
            ProcessingJobInfo job,
            boolean isCreateOnlyError
        ) throws Exception {
            //create message first when message finish then create idata for each message
            conn.createMessage(sfField2CSVHeader(service, job.getInterfaceId()), job, isCreateOnlyError);
            //next step create message on salesforce
            job.setStatus(TStatus.SEND_MESSAGE);
            sfProcessingJobDao.update(job);
            LOGGER.trace("Job {} new status: {}", job.getJobid(), job.getStatus());
            activateReprocessingForCheckingJob();
        }

        protected IBulkConnection createBulkConnection(ProcessingJobInfo job, SFIntegrationService service) throws Exception {
            IBulkConnection conn = null;
            if (job.getBulkversion() == TBulkVersion.SFV1) {
                conn = new BulkV1Connection(service.getSalesforceConfiguration());
            } else {
                conn = new BulkV2Connection(service.getSalesforceConfiguration());
            }

            return conn;
        }

        // re-process record not process on salesforce
        protected void reprocessRecords(SFIntegrationService service, IBulkConnection conn, Interfaces__c intf, ProcessingJobInfo job) throws Exception {
            File reprocessFolder = Helper.getReprocessFolder(job);
            if (reprocessFolder.exists()) {
                File[] files = reprocessFolder.listFiles();
                LOGGER.trace("Reprocessing {} files", ArrayUtils.getLength(files));
                if (files != null && files.length > 0) {
                    if (conn instanceof BulkV1Connection) {
                        reprocessBulkV1(service, (BulkV1Connection) conn, intf, job, files);
                    } else {
                        reprocessBulkV2(service, (BulkV2Connection) conn, intf, job, files);
                    }
                }
            }
        }

        protected void reprocessBulkV1(
            SFIntegrationService service,
            BulkV1Connection bconn,
            Interfaces__c intf,
            ProcessingJobInfo job,
            File[] files
        ) throws Exception {
            LOGGER.trace("Job {}: reprocessing bulk V1");
            JobV1Info job2 = (JobV1Info) bconn.createJob(intf.getSource_Name__c(), intf.getOperationType__c(), job.getExternalFieldId(), intf.getBulk_Processing_Mode__c(), BulkDirectObjectV2Integration.CRLF);
            ProcessingJobInfo info = createJobInfo(job2, service, intf, job, TBulkVersion.SFV1);
            try {
                for (File f : files) {
                    BatchInfo batch = bconn.createBatchFromStream(job2, new FileInputStream(f));
                    File jobFolder = Helper.getJobFolder(info);
                    File batchFile = new File(jobFolder, batch.getId() + ".csv");
                    FileUtils.copyFile(f, batchFile);
                }
                closeOrAbortJob(job2, info, bconn, false);
            } catch (Exception e) {
                closeOrAbortJob(job2, info, bconn, true);
            }
        }

        protected void reprocessBulkV2(
            SFIntegrationService service,
            BulkV2Connection bconn,
            Interfaces__c intf,
            ProcessingJobInfo job,
            File[] files
        ) throws Exception {
            LOGGER.trace("Job {}: reprocessing bulk V2");
            int i = 1;
            for (File f : files) {
                JobV2Info job2 = null;
                ProcessingJobInfo info = null;
                try {
                    job2 = bconn.createJob(intf.getSource_Name__c(), intf.getOperationType__c(), job.getExternalFieldId(), BulkDirectObjectV2Integration.CRLF);
                    info = createJobInfo(job2, service, intf, job, TBulkVersion.SFV2);
                    bconn.createBatch(job2, FileUtil.toBytes(new FileInputStream(f)));
                    File jobFolder = Helper.getJobFolder(info);
                    File batchFile = new File(jobFolder, "batch-" + i + ".csv");
                    i++;
                    FileUtils.copyFile(f, batchFile);
                    closeOrAbortJob(job2, info, bconn, false);

                } catch (Exception e) {
                    closeOrAbortJob(job2, info, bconn, true);
                }
            }
        }

        protected ProcessingJobInfo createJobInfo(IJobInfo job, SFIntegrationService service, Interfaces__c intf, ProcessingJobInfo oldJob, TBulkVersion bulkV) {
            ProcessingJobInfo info = new ProcessingJobInfo();
            info.setJobid(job.getId());
            info.setIntegrationId(intf.getIntegration__c());
            info.setInterfaceId(intf.getId());
            //save agent name
            info.setBulkversion(bulkV);
            info.setExternalFieldId(oldJob.getExternalFieldId());
            info.setStatus(TStatus.OK);
            info.setJobType(TJOBType.SOBJECT);
            info.setSalesforce_user(service.getConnectionInfo().getUsername());
            BaseManager.getDao(SFProcessingJobDao.class).upsert(info);
            activateReprocessingForCheckingJob();
            return info;
        }

        protected void closeOrAbortJob(IJobInfo job, ProcessingJobInfo info, IBulkConnection conn, boolean error) {
            try {
                LOGGER.trace("Closing job {} ({}). Error flag: {}", info, job, error);
                if (job != null) {
                    if (!error) {
                        //close the job on salesforce if there is no error
                        conn.closeJobById(job.getId());
                        info.setStatus(TStatus.CREATE_IMESSAGE);
                        sfProcessingJobDao.update(info);
                        activateReprocessingForCheckingJob();
                        LOGGER.trace("Job {} was updated with status CREATE_IMESSAGE and closed", job.getId());
                    } else {
                        //abort the job when has any error, because can do with the old file
                        conn.cancelJob(job.getId());

                        //when cancel job success then delete the job folder
                        Helper.deleteJobFolder(info);
                        LOGGER.trace("Job {} was cancelled", job.getId());
                    }
                }
            } catch (Exception e) {
                //close job failed
                LOGGER.error(format("Couldn't close or abort job %s: %s", job.getId(), e.getMessage()), e);
                if (error) {
                    //we will do abort the job later
                    info.setStatus(TStatus.ERR);
                } else {
                    //we do close the job later when we cannot close it
                    info.setStatus(TStatus.OK);
                }
                sfProcessingJobDao.update(info);
                activateReprocessingForCheckingJob();
            }
        }

        protected long calculateSleepTimeout() {
            if (linearBackoffForReprocessingEnabled) {
                // when iteration < 10 then initial value
                // 10 <= iteration < 20 then initial value * 2
                // 20 <= iteration < 30 then initial value * 3
                // ...
                return Math.min(
                    (reprocessingIterationNumber.get() / 10 + 1) * INITIAL_REPROCESSING_ITERATION_DURATION_IN_MS,
                    MAX_REPROCESSING_ITERATION_DURATION_IN_MS
                );
            } else {
                return INITIAL_REPROCESSING_ITERATION_DURATION_IN_MS;
            }
        }


        protected void incReprocessingIterationNumber() {
            LOGGER.trace("Incrementing reprocessing iteration number from {} to {}",
                this.reprocessingIterationNumber,
                this.reprocessingIterationNumber.get() + 1
            );
            this.reprocessingIterationNumber.getAndIncrement();
        }

        protected void activateReprocessingForCheckingJob() {
            LOGGER.trace("Activating reprocessing");
            this.reprocessing = true;
        }

        protected void deactivateReprocessingForCheckingJob() {
            LOGGER.trace("Deactivating reprocessing");
            this.reprocessingIterationNumber.set(1);
            this.reprocessing = false;
        }

    }
}
