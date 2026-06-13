package com.ujcms.cms.core.service;

import com.ujcms.cms.core.domain.PreviewToken;
import com.ujcms.cms.core.mapper.PreviewTokenMapper;
import com.ujcms.cms.core.mapper.PreviewTokenRoleMapper;
import com.ujcms.common.db.identifier.SnowflakeSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 预览令牌 Service
 *
 * @author UJCMS
 */
@Service
public class PreviewTokenService {
    private static final Logger logger = LoggerFactory.getLogger(PreviewTokenService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PreviewTokenMapper previewTokenMapper;
    private final PreviewTokenRoleMapper previewTokenRoleMapper;
    private final SnowflakeSequence snowflakeSequence;

    public PreviewTokenService(PreviewTokenMapper previewTokenMapper,
                               PreviewTokenRoleMapper previewTokenRoleMapper,
                               SnowflakeSequence snowflakeSequence) {
        this.previewTokenMapper = previewTokenMapper;
        this.previewTokenRoleMapper = previewTokenRoleMapper;
        this.snowflakeSequence = snowflakeSequence;
    }

    /**
     * 创建预览令牌
     *
     * @param publishTaskId 预约发布任务ID
     * @param siteId        站点ID
     * @param contentType   内容类型
     * @param contentId     内容ID
     * @param expiresAt     过期时间
     * @param maxUsage      最大使用次数
     * @return 预览令牌（包含明文令牌）
     */
    @Transactional(rollbackFor = Exception.class)
    public PreviewToken createToken(Long publishTaskId, Long siteId, String contentType,
                                    Long contentId, OffsetDateTime expiresAt, int maxUsage) {
        String plainToken = generateRandomToken();
        String tokenHash = hashToken(plainToken);

        PreviewToken token = new PreviewToken();
        token.setId(snowflakeSequence.nextId());
        token.setPublishTaskId(publishTaskId);
        token.setTokenHash(tokenHash);
        token.setSiteId(siteId);
        token.setContentType(contentType);
        token.setContentId(contentId);
        token.setExpiresAt(expiresAt);
        token.setMaxUsage(maxUsage);
        token.setUsageCount(0);
        token.setCreated(OffsetDateTime.now());
        token.setPlainToken(plainToken);

        previewTokenMapper.insert(token);

        logger.info("Created preview token: id={}, expiresAt={}", token.getId(), expiresAt);
        return token;
    }

    /**
     * 验证预览令牌
     *
     * @param plainToken  明文令牌
     * @param siteId      请求站点ID
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @return 有效的预览令牌，如果无效则返回 empty
     */
    public Optional<PreviewToken> validateToken(String plainToken, Long siteId,
                                                String contentType, Long contentId) {
        String tokenHash = hashToken(plainToken);
        PreviewToken token = previewTokenMapper.selectByTokenHash(tokenHash);

        if (token == null) {
            logger.debug("Token not found: hash={}", tokenHash);
            return Optional.empty();
        }

        // 检查过期
        if (token.isExpired()) {
            logger.debug("Token expired: id={}", token.getId());
            return Optional.empty();
        }

        // 检查使用次数
        if (token.isUsageExhausted()) {
            logger.debug("Token usage exhausted: id={}", token.getId());
            return Optional.empty();
        }

        // 校验站点匹配
        if (!token.getSiteId().equals(siteId)) {
            logger.debug("Token site mismatch: token.siteId={}, requested.siteId={}",
                    token.getSiteId(), siteId);
            return Optional.empty();
        }

        // 校验内容匹配
        if (!token.getContentType().equals(contentType) || !token.getContentId().equals(contentId)) {
            logger.debug("Token content mismatch");
            return Optional.empty();
        }

        // 递增使用次数
        token.incrementUsage();
        previewTokenMapper.updateUsageCount(token.getId(), token.getUsageCount());

        return Optional.of(token);
    }

    /**
     * 清理过期令牌
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredTokens() {
        OffsetDateTime now = OffsetDateTime.now();
        int deleted = previewTokenMapper.deleteExpired(now);
        if (deleted > 0) {
            logger.info("Cleaned up {} expired preview tokens", deleted);
        }
    }

    /**
     * 根据预约发布任务ID查询令牌列表
     */
    public List<PreviewToken> selectByPublishTaskId(Long publishTaskId) {
        return previewTokenMapper.selectByPublishTaskId(publishTaskId);
    }

    @Nullable
    public PreviewToken select(Long id) {
        return previewTokenMapper.select(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public int delete(Long id) {
        previewTokenRoleMapper.deleteByTokenId(id);
        return previewTokenMapper.delete(id);
    }

    /**
     * 生成随机令牌（32字节，64字符十六进制字符串）
     */
    static String generateRandomToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    /**
     * 计算令牌的SHA-256哈希
     */
    static String hashToken(String plainToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainToken.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
