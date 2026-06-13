package com.ujcms.cms.core.mapper;

import com.ujcms.cms.core.domain.ScheduledPublish;
import com.ujcms.common.query.QueryInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 预约发布 Mapper
 *
 * @author PONY
 */
@Mapper
@Repository
public interface ScheduledPublishMapper {
    /**
     * 插入数据
     *
     * @param bean 实体对象
     * @return 插入条数
     */
    int insert(ScheduledPublish bean);

    /**
     * 更新数据
     *
     * @param bean 实体对象
     * @return 更新条数
     */
    int update(ScheduledPublish bean);

    /**
     * 删除数据
     *
     * @param id 主键ID
     * @return 删除条数
     */
    int delete(Long id);

    /**
     * 根据主键获取数据
     *
     * @param id 主键ID
     * @return 实体对象。没有找到数据，则返回 {@code null}
     */
    @Nullable
    ScheduledPublish select(Long id);

    /**
     * 根据查询条件获取列表
     *
     * @param queryInfo 查询条件
     * @return 数据列表
     */
    List<ScheduledPublish> selectAll(@Nullable @Param("queryInfo") QueryInfo queryInfo);

    /**
     * 根据预览令牌获取数据
     *
     * @param previewToken 预览令牌
     * @return 实体对象。没有找到数据，则返回 {@code null}
     */
    @Nullable
    ScheduledPublish selectByToken(@Param("previewToken") String previewToken);

    /**
     * 获取指定时间之前状态为待执行的任务列表
     *
     * @param dateTime 时间
     * @return 数据列表
     */
    List<ScheduledPublish> selectPendingBefore(@Param("dateTime") OffsetDateTime dateTime);

    /**
     * 根据用户ID删除数据
     *
     * @param userId 用户ID
     * @return 被删除的数据条数
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 根据站点ID删除数据
     *
     * @param siteId 站点ID
     * @return 被删除的数据条数
     */
    int deleteBySiteId(Long siteId);
}
