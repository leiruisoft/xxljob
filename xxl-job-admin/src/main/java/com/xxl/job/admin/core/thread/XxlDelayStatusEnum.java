package com.xxl.job.admin.core.thread;

/**
 * @author helei
 * @description
 * @since 2023-07-11 9:47
 */
public enum XxlDelayStatusEnum {
    INIT(0,"未完成--初始化状态"),
    CANCEL(1,"取消"),
    SCHEDULE_ING(2,"调度中"),
    SECCESS(3,"成功"),
    FAIL(4,"失败"),
    ;
    private final int status;
    private final String desc;

    public int getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }

    XxlDelayStatusEnum(int status, String desc) {
        this.status = status;
        this.desc = desc;
    }
}
