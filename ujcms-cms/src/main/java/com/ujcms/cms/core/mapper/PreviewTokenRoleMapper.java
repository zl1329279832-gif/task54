package com.ujcms.cms.core.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 预览令牌角色关联 Mapper
 *
 * @author UJCMS
 */
@Mapper
@Repository
public interface PreviewTokenRoleMapper {
    /**
     * 查询令牌关联的角色ID列表
     *
     * @param tokenId 预览令牌ID
     * @return 角色ID列表
     */
    List<Long> selectRoleIdsByTokenId(@Param("tokenId") Long tokenId);

    /**
     * 插入关联记录
     *
     * @param tokenId 预览令牌ID
     * @param roleId  角色ID
     * @return 插入条数
     */
    int insert(@Param("tokenId") Long tokenId, @Param("roleId") Long roleId);

    /**
     * 根据令牌ID删除关联
     *
     * @param tokenId 预览令牌ID
     * @return 删除条数
     */
    int deleteByTokenId(Long tokenId);
}
