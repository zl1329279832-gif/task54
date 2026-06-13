package com.ujcms.cms.core.mapper;

import com.ujcms.cms.core.domain.PreviewToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 预览令牌 Mapper
 *
 * @author UJCMS
 */
@Mapper
@Repository
public interface PreviewTokenMapper {
    int insert(PreviewToken bean);

    int delete(Long id);

    @Nullable
    PreviewToken select(Long id);

    /**
     * 根据令牌哈希查找
     *
     * @param tokenHash SHA-256 哈希
     * @return 预览令牌实体
     */
    @Nullable
    PreviewToken selectByTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * 根据预约发布任务ID查询
     *
     * @param publishTaskId 预约发布任务ID
     * @return 预览令牌列表
     */
    List<PreviewToken> selectByPublishTaskId(@Param("publishTaskId") Long publishTaskId);

    /**
     * 更新使用次数
     *
     * @param id         令牌ID
     * @param usageCount 新使用次数
     * @return 更新条数
     */
    int updateUsageCount(@Param("id") Long id, @Param("usageCount") int usageCount);

    /**
     * 根据预约发布任务ID删除
     *
     * @param publishTaskId 预约发布任务ID
     * @return 删除条数
     */
    int deleteByPublishTaskId(Long publishTaskId);

    /**
     * 删除已过期的令牌
     *
     * @param dateTime 截止时间
     * @return 删除条数
     */
    int deleteExpired(@Param("dateTime") OffsetDateTime dateTime);
}
