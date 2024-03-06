package com.xxl.job.core.context;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.DelayTaskParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.executor.XxlJobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * @author helei
 * @description
 * @since 2023-07-11 15:36
 */
public class XxlJobDelayHelper {

    private static Logger logger = LoggerFactory.getLogger(XxlJobDelayHelper.class);

    /**
     * 添加延迟任务
     * @param taskName 任务名称
     * @param executorHandler 执行器名称
     * @param executeParams 执行器参数
     * @param executeAt 延迟时间点，需要执行的那个时间
     * @return 任务id，（用于取消任务）
     */
    public static Long pushDelayTask(String taskName, String executorHandler, String executeParams, LocalDateTime executeAt){
        DelayTaskParam taskParam = new DelayTaskParam();
        taskParam.setAppName(XxlJobExecutor.getAppName());
        taskParam.setTaskName(taskName);
        taskParam.setExecutorHandler(executorHandler);
        taskParam.setExecutorParams(executeParams);
        taskParam.setExecuteTime(executeAt);
        for (AdminBiz adminBiz: XxlJobExecutor.getAdminBizList()) {
            try {
                ReturnT<String> registryResult = adminBiz.addDelayTask(taskParam);
                if (registryResult!=null && ReturnT.SUCCESS_CODE == registryResult.getCode()) {
                    logger.debug(">>>>>>>>>>> xxl-job-delay add success, registryParam:{}, registryResult:{}", new Object[]{taskParam, registryResult});
                    return Long.valueOf(String.valueOf(registryResult.getContent()));
                } else {
                    logger.info(">>>>>>>>>>> xxl-job-delay add fail, registryParam:{}, registryResult:{}", new Object[]{taskParam, registryResult});
                }
            } catch (Exception e) {
                logger.info(">>>>>>>>>>> xxl-job-delay add error, registryParam:{}", taskParam, e);
            }
        }
        return null;
    }

    /**
     * 取消任务
     * @param taskId 任务id
     */
    public static void cancel(Long taskId){
        if(taskId == null){
            return;
        }
        for (AdminBiz adminBiz: XxlJobExecutor.getAdminBizList()) {
            try {
                ReturnT<String> returnT = adminBiz.cancelDelayTask(taskId);
                if(returnT != null && ReturnT.SUCCESS_CODE == returnT.getCode()){
                    logger.info(">>>>>>>>>>> xxl-job-delay cancel success");
                    break;
                }
            } catch (Exception e) {
                logger.info(">>>>>>>>>>> xxl-job-delay cancel error, registryParam: ", e);
            }
        }
    }
}
