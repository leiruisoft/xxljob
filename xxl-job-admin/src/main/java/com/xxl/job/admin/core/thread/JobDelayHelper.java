package com.xxl.job.admin.core.thread;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.cron.CronExpression;
import com.xxl.job.admin.core.model.XxlDelayInfo;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.xxl.job.admin.core.scheduler.MisfireStrategyEnum;
import com.xxl.job.admin.core.scheduler.ScheduleTypeEnum;
import com.xxl.job.admin.core.scheduler.XxlJobScheduler;
import com.xxl.job.admin.core.trigger.TriggerTypeEnum;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.DelayTaskParam;
import com.xxl.job.core.biz.model.ReturnT;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author xuxueli 2019-05-21
 */
public class JobDelayHelper {
    private static Logger logger = LoggerFactory.getLogger(JobDelayHelper.class);

    private static JobDelayHelper instance = new JobDelayHelper();
    public static JobDelayHelper getInstance(){
        return instance;
    }

    public static final long PRE_READ_MS = 5000;    // pre read

    private Thread scheduleThread;
    private Thread cancelThread;
    private volatile boolean scheduleThreadToStop = false;
    private volatile boolean cancelThreadToStop = false;

    private static HashedWheelTimer wheelTimer = new HashedWheelTimer(new DefaultThreadFactory("xxl-job-delay wheelTimePool"), 1, TimeUnit.SECONDS, 300, false);


    public void start(){


        // schedule thread
        scheduleThread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    TimeUnit.MILLISECONDS.sleep(5000 - System.currentTimeMillis()%1000 );
                } catch (InterruptedException e) {
                    if (!scheduleThreadToStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
                logger.info(">>>>>>>>> init xxl-job-delay admin scheduler success.");

                // pre-read count: treadpool-size * trigger-qps (each trigger cost 50ms, qps = 1000/50 = 20)
                int preReadCount = (XxlJobAdminConfig.getAdminConfig().getTriggerPoolFastMax() + XxlJobAdminConfig.getAdminConfig().getTriggerPoolSlowMax()) * 20;

                while (!scheduleThreadToStop) {

                    // Scan Job
                    long start = System.currentTimeMillis();

                    Connection conn = null;
                    Boolean connAutoCommit = null;
                    PreparedStatement preparedStatement = null;

                    boolean preReadSuc = true;
                    try {

                        conn = XxlJobAdminConfig.getAdminConfig().getDataSource().getConnection();
                        connAutoCommit = conn.getAutoCommit();
                        conn.setAutoCommit(false);

                        preparedStatement = conn.prepareStatement(  "select * from xxl_job_lock where lock_name = 'delay_lock' for update" );
                        preparedStatement.execute();

                        // tx start

                        // 1、pre read
                        LocalDateTime now = LocalDateTime.now();
                        List<XxlDelayInfo> scheduleList = XxlJobAdminConfig.getAdminConfig().getXxlDelayInfoDao().scheduleJobQuery(now.plusMinutes(PRE_READ_MS / 1000), preReadCount);
                        if (scheduleList != null && scheduleList.size() > 0) {
                            //放入时间轮
                            for (XxlDelayInfo delayInfo : scheduleList) {
                                XxlDelayTimeTask xxlDelayTimeTask = new XxlDelayTimeTask();
                                xxlDelayTimeTask.setDelayId(delayInfo.getId());
                                long delay = delayInfo.getExecuteTime().toInstant(ZoneOffset.of("+8")).toEpochMilli();
                                //todo 这里需要处理过期的数据，直接调度一次
                                long delayTime = delay - System.currentTimeMillis();
                                if(delayTime <= 1000){
                                    //服务器重启了
                                    //直接调度，不用放时间轮了
                                    RUN_NOW_QUEUE.add(delayInfo.getId());
                                }else{
                                    wheelTimer.newTimeout(xxlDelayTimeTask, delayTime, TimeUnit.MILLISECONDS);
                                }
                                updateById(delayInfo.getId());
                            }
                        } else {
                            preReadSuc = false;
                        }

                        // tx stop


                    } catch (Exception e) {
                        if (!scheduleThreadToStop) {
                            logger.error(">>>>>>>>>>> xxl-job, JobScheduleHelper#scheduleThread error:{}", e);
                        }
                    } finally {

                        // commit
                        if (conn != null) {
                            try {
                                conn.commit();
                            } catch (SQLException e) {
                                if (!scheduleThreadToStop) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                            try {
                                conn.setAutoCommit(connAutoCommit);
                            } catch (SQLException e) {
                                if (!scheduleThreadToStop) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                            try {
                                conn.close();
                            } catch (SQLException e) {
                                if (!scheduleThreadToStop) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                        }

                        // close PreparedStatement
                        if (null != preparedStatement) {
                            try {
                                preparedStatement.close();
                            } catch (SQLException e) {
                                if (!scheduleThreadToStop) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                        }
                    }
                    long cost = System.currentTimeMillis()-start;


                    // Wait seconds, align second
                    if (cost < 1000) {  // scan-overtime, not wait
                        try {
                            // pre-read period: success > scan each second; fail > skip this period;
                            TimeUnit.MILLISECONDS.sleep((preReadSuc?1000:PRE_READ_MS) - System.currentTimeMillis()%1000);
                        } catch (InterruptedException e) {
                            if (!scheduleThreadToStop) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }

                }

                logger.info(">>>>>>>>>>> xxl-job-delay, JobScheduleHelper#scheduleThread stop");
            }
        });
        scheduleThread.setDaemon(true);
        scheduleThread.setName("xxl-job-delay, admin JobScheduleHelper#scheduleThread");
        scheduleThread.start();

        for (int i = 0; i < 5; i++) {
            Thread t = new Thread(() -> {
                while (!scheduleThreadToStop){
                    try {
                        XxlDelayTimeTask task = DELAY_INFO_QUEUE.take();
                        long delayId = task.getDelayId();
                        doTrigger(delayId);
                    } catch (Exception e) {
                        logger.info(">>>>>>>>>>> xxl-job-delay, triggerThread execute error");
                    }
                }
            });
            t.setDaemon(true);
            t.setName("xxl-job-delay,admin JobScheduleHelper#triggerThread" + i);
            t.start();
        }

        for (int i = 0; i < 5; i++) {
            Thread t = new Thread(() -> {
                while (!scheduleThreadToStop){
                    try {
                        Long taskId = RUN_NOW_QUEUE.take();
                        XxlDelayInfo delayInfo = XxlJobAdminConfig.getAdminConfig().getXxlDelayInfoDao().getById(taskId);
                        if(XxlDelayStatusEnum.SCHEDULE_ING.getStatus() == delayInfo.getStatus()){
                            doTrigger(taskId);
                        }else if(XxlDelayStatusEnum.INIT.getStatus() == delayInfo.getStatus()){
                            //重新放进去
                            RUN_NOW_QUEUE.add(taskId);
                        }
                    } catch (Exception e) {
                        logger.info(">>>>>>>>>>> xxl-job-delay, triggerThread execute error");
                    }
                }
            });
            t.setDaemon(true);
            t.setName("xxl-job-delay,admin JobScheduleHelper#runNowThread" + i);
            t.start();
        }

        cancelThread = new Thread(()->{
            while (!cancelThreadToStop){
                try {
                    Long taskId = CANCEL_INFO_QUEUE.take();
                    XxlJobAdminConfig.getAdminConfig().getXxlDelayInfoDao().cancelTaskById(taskId);
                } catch (InterruptedException e) {
                    logger.info(e.getMessage(),e);
                }
            }
        });
        cancelThread.setDaemon(true);
        cancelThread.setName("xxl-job-delay,admin JobScheduleHelper#cancelThread");
        cancelThread.start();
    }

    private void updateById(long id) {
        XxlDelayInfo delayInfo = new XxlDelayInfo();
        delayInfo.setId(id);
        delayInfo.setStatus(XxlDelayStatusEnum.SCHEDULE_ING.getStatus());
        XxlJobAdminConfig.getAdminConfig().getXxlDelayInfoDao().updateById(delayInfo);
    }

    private void doTrigger(long delayId) throws Exception {
        XxlDelayInfo delayInfo = XxlJobAdminConfig.getAdminConfig().getXxlDelayInfoDao().getById(delayId);
        if(delayInfo != null && XxlDelayStatusEnum.SCHEDULE_ING.getStatus() == delayInfo.getStatus()){
            XxlJobGroup jobGroup = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().findByAppName(delayInfo.getAppName());
            List<String> registryList = jobGroup.getRegistryList();
            if(registryList != null && registryList.size() > 0){
                DelayTaskParam delayTaskParam = new DelayTaskParam();
                delayTaskParam.setTaskId(delayId);
                delayTaskParam.setExecutorHandler(delayInfo.getExecutorHandler());
                delayTaskParam.setExecutorParams(delayInfo.getExecutorParams());
                ReturnT<String> route = ExecutorRouteStrategyEnum.RANDOM.getRouter().route(null, registryList);
                ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(route.getContent());
                executorBiz.scheduleDelay(delayTaskParam);
            }
            else {
                //记录日志
            }
        }
    }

    public void toStop(){
        // 1、stop schedule
        scheduleThreadToStop = true;
        try {
            TimeUnit.SECONDS.sleep(1);  // wait
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        if (scheduleThread.getState() != Thread.State.TERMINATED){
            // interrupt and wait
            scheduleThread.interrupt();
            try {
                scheduleThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }

        cancelThreadToStop = true;
        try {
            TimeUnit.SECONDS.sleep(1);  // wait
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        if (cancelThread.getState() != Thread.State.TERMINATED){
            // interrupt and wait
            cancelThread.interrupt();
            try {
                cancelThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
        logger.info(">>>>>>>>>>> xxl-job-delay, JobScheduleHelper stop");
    }

    private static final BlockingQueue<XxlDelayTimeTask> DELAY_INFO_QUEUE = new LinkedBlockingQueue<>();
    private static final BlockingQueue<Long> CANCEL_INFO_QUEUE = new LinkedBlockingQueue<>();
    private static final BlockingQueue<Long> RUN_NOW_QUEUE = new LinkedBlockingQueue<>();

    public Long addDelayTask(DelayTaskParam delayTaskParam) {
        XxlDelayInfo delayInfo = new XxlDelayInfo();
        delayInfo.setAppName(delayTaskParam.getAppName());
        delayInfo.setTaskName(delayTaskParam.getTaskName());
        delayInfo.setExecutorHandler(delayTaskParam.getExecutorHandler());
        delayInfo.setExecutorParams(delayTaskParam.getExecutorParams());
        delayInfo.setExecuteTime(delayTaskParam.getExecuteTime());
        delayInfo.setCreateTime(LocalDateTime.now());
        delayInfo.setStatus(XxlDelayStatusEnum.INIT.getStatus());
        XxlJobAdminConfig.getAdminConfig().getXxlDelayInfoDao().addDelayTask(delayInfo);
        return delayInfo.getId();
    }

    public ReturnT<String> cancelDelayTask(long taskId) {
        try {
            CANCEL_INFO_QUEUE.put(taskId);
        } catch (InterruptedException e) {
            logger.info(e.getMessage(),e);
        }
        return ReturnT.SUCCESS;
    }

    private static class XxlDelayTimeTask implements TimerTask{

        private long delayId;

        public long getDelayId() {
            return delayId;
        }

        public void setDelayId(long delayId) {
            this.delayId = delayId;
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            XxlDelayTimeTask task = (XxlDelayTimeTask) timeout.task();
            DELAY_INFO_QUEUE.put(task);
        }
    }

}
