package com.xxl.job.core.biz.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author helei
 * @description
 * @since 2023-07-11 10:21
 */
public class DelayTaskParam implements Serializable {
    private long taskId;
    private String executorHandler;
    private String executorParams;

    private String appName;
    private String taskName;
    private LocalDateTime executeTime;

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public String getExecutorHandler() {
        return executorHandler;
    }

    public void setExecutorHandler(String executorHandler) {
        this.executorHandler = executorHandler;
    }

    public String getExecutorParams() {
        return executorParams;
    }

    public void setExecutorParams(String executorParams) {
        this.executorParams = executorParams;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public LocalDateTime getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(LocalDateTime executeTime) {
        this.executeTime = executeTime;
    }
}
