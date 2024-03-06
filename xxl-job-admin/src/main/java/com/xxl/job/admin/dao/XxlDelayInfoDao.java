package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlDelayInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;


/**
 * job info
 * @author xuxueli 2016-1-12 18:03:45
 */
@Mapper
public interface XxlDelayInfoDao {

    List<XxlDelayInfo> scheduleJobQuery(@Param("executeTime") LocalDateTime executeTime, @Param("preReadCount") int preReadCount);

    void updateBatchById(@Param("xxlDelayInfos") List<XxlDelayInfo> xxlDelayInfos);

    XxlDelayInfo getById(@Param("delayId") long delayId);

    void updateById(@Param("delayInfo") XxlDelayInfo delayInfo);

    long addDelayTask(XxlDelayInfo delayInfo);

    void cancelTaskById(@Param("taskId") Long taskId);

    List<Long> findLostJobIds(@Param("losedTime") LocalDateTime losedTime, @Param("limit") int limit);
}
