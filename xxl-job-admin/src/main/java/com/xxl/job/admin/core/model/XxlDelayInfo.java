package com.xxl.job.admin.core.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author helei
 * @description
 * @since 2023-07-10 16:57
 */
public class XxlDelayInfo implements Serializable {
    /**
     * 主键
     */
    private long id;
    /**
     * 应用名称
     */
    private String appName;
    /**
     * 任务名称
     */
    private String taskName;
    /**
     * 执行器名称
     */
    private String executorHandler;
    /**
     * 执行时间
     */
    private LocalDateTime executeTime;
    /**
     * 执行参数
     */
    private String executorParams;
    /**
     * 状态：0未完成，1已取消，2调度中，3已完成，4失败
     */
    private int status;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getExecutorHandler() {
        return executorHandler;
    }

    public void setExecutorHandler(String executorHandler) {
        this.executorHandler = executorHandler;
    }

    public LocalDateTime getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(LocalDateTime executeTime) {
        this.executeTime = executeTime;
    }

    public String getExecutorParams() {
        return executorParams;
    }

    public void setExecutorParams(String executorParams) {
        this.executorParams = executorParams;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
