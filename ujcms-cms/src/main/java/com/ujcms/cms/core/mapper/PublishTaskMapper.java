package com.ujcms.cms.core.mapper;

import com.ujcms.cms.core.domain.PublishTask;
import com.ujcms.common.query.QueryInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 预约发布任务 Mapper
 *
 * @author UJCMS
 */
@Mapper
@Repository
public interface PublishTaskMapper {
    int insert(PublishTask bean);

    int update(PublishTask bean);

    int delete(Long id);

    @Nullable
    PublishTask select(Long id);

    List<PublishTask> selectAll(@Nullable @Param("queryInfo") QueryInfo queryInfo);

    /**
     * 查询待执行且发布时间已到的任务
     *
     * @param dateTime 当前时间
     * @return 待执行的任务列表
     */
    List<PublishTask> selectPendingBefore(@Param("dateTime") OffsetDateTime dateTime);

    /**
     * 更新任务状态
     *
     * @param id        任务ID
     * @param status    新状态
     * @param errorInfo 错误信息
     * @return 更新条数
     */
    int updateStatus(@Param("id") Long id, @Param("status") short status,
                     @Param("errorInfo") @Nullable String errorInfo);

    int deleteBySiteId(Long siteId);

    int deleteByUserId(Long userId);
}
