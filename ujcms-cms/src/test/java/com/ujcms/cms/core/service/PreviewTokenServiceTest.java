package com.ujcms.cms.core.service;

import com.ujcms.cms.core.domain.PreviewToken;
import com.ujcms.cms.core.mapper.PreviewTokenMapper;
import com.ujcms.cms.core.mapper.PreviewTokenRoleMapper;
import com.ujcms.common.db.identifier.SnowflakeSequence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * PreviewTokenService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class PreviewTokenServiceTest {

    @Mock
    private PreviewTokenMapper previewTokenMapper;
    @Mock
    private PreviewTokenRoleMapper previewTokenRoleMapper;
    @Mock
    private SnowflakeSequence snowflakeSequence;

    @InjectMocks
    private PreviewTokenService previewTokenService;

    private static final Long TASK_ID = 100L;
    private static final Long SITE_ID = 200L;
    private static final Long CONTENT_ID = 300L;
    private static final Long TOKEN_ID = 400L;

    @Test
    void createToken_generatesValidHashAndStores() {
        when(snowflakeSequence.nextId()).thenReturn(TOKEN_ID);
        when(previewTokenMapper.insert(any())).thenReturn(1);

        PreviewToken token = previewTokenService.createToken(
                TASK_ID, SITE_ID, "article", CONTENT_ID,
                OffsetDateTime.now().plusHours(24), 100);

        assertNotNull(token);
        assertNotNull(token.getPlainToken());
        assertNotNull(token.getTokenHash());
        assertEquals(64, token.getTokenHash().length()); // SHA-256 hex
        assertEquals(TOKEN_ID, token.getId());
        assertEquals(TASK_ID, token.getPublishTaskId());
        assertEquals(SITE_ID, token.getSiteId());
        assertEquals("article", token.getContentType());
        assertEquals(CONTENT_ID, token.getContentId());
        assertEquals(0, token.getUsageCount());

        verify(previewTokenMapper).insert(any(PreviewToken.class));
    }

    @Test
    void createToken_plainTokenIs64CharsHex() {
        when(snowflakeSequence.nextId()).thenReturn(TOKEN_ID);

        PreviewToken token = previewTokenService.createToken(
                TASK_ID, SITE_ID, "article", CONTENT_ID,
                OffsetDateTime.now().plusHours(24), 100);

        assertNotNull(token.getPlainToken());
        assertEquals(64, token.getPlainToken().length()); // 32 bytes = 64 hex chars
        assertTrue(token.getPlainToken().matches("[0-9a-f]+"));
    }

    @Test
    void hashToken_isDeterministic() {
        String plainToken = "test-token-123";
        String hash1 = PreviewTokenService.hashToken(plainToken);
        String hash2 = PreviewTokenService.hashToken(plainToken);

        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length());
    }

    @Test
    void hashToken_differentInputs_produceDifferentHashes() {
        String hash1 = PreviewTokenService.hashToken("token-a");
        String hash2 = PreviewTokenService.hashToken("token-b");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void validateToken_whenTokenNotFound_returnsEmpty() {
        when(previewTokenMapper.selectByTokenHash(anyString())).thenReturn(null);

        Optional<PreviewToken> result = previewTokenService.validateToken(
                "nonexistent-token", SITE_ID, "article", CONTENT_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void validateToken_whenTokenExpired_returnsEmpty() {
        PreviewToken token = createValidStoredToken();
        token.setExpiresAt(OffsetDateTime.now().minusHours(1));
        when(previewTokenMapper.selectByTokenHash(anyString())).thenReturn(token);

        // We need to use the same plain token that was hashed
        String plainToken = "some-plain-token";
        // Override the mock to match the actual hash
        String expectedHash = PreviewTokenService.hashToken(plainToken);
        token.setTokenHash(expectedHash);
        when(previewTokenMapper.selectByTokenHash(expectedHash)).thenReturn(token);

        Optional<PreviewToken> result = previewTokenService.validateToken(
                plainToken, SITE_ID, "article", CONTENT_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void validateToken_whenUsageExhausted_returnsEmpty() {
        String plainToken = "some-plain-token";
        String expectedHash = PreviewTokenService.hashToken(plainToken);

        PreviewToken token = createValidStoredToken();
        token.setTokenHash(expectedHash);
        token.setMaxUsage(10);
        token.setUsageCount(10);
        when(previewTokenMapper.selectByTokenHash(expectedHash)).thenReturn(token);

        Optional<PreviewToken> result = previewTokenService.validateToken(
                plainToken, SITE_ID, "article", CONTENT_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void validateToken_whenSiteMismatch_returnsEmpty() {
        String plainToken = "some-plain-token";
        String expectedHash = PreviewTokenService.hashToken(plainToken);

        PreviewToken token = createValidStoredToken();
        token.setTokenHash(expectedHash);
        when(previewTokenMapper.selectByTokenHash(expectedHash)).thenReturn(token);

        // Request with different site ID
        Optional<PreviewToken> result = previewTokenService.validateToken(
                plainToken, 999L, "article", CONTENT_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void validateToken_whenContentTypeMismatch_returnsEmpty() {
        String plainToken = "some-plain-token";
        String expectedHash = PreviewTokenService.hashToken(plainToken);

        PreviewToken token = createValidStoredToken();
        token.setTokenHash(expectedHash);
        when(previewTokenMapper.selectByTokenHash(expectedHash)).thenReturn(token);

        Optional<PreviewToken> result = previewTokenService.validateToken(
                plainToken, SITE_ID, "channel", CONTENT_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void validateToken_whenValid_incrementsUsageAndReturnsToken() {
        String plainToken = "some-plain-token";
        String expectedHash = PreviewTokenService.hashToken(plainToken);

        PreviewToken token = createValidStoredToken();
        token.setTokenHash(expectedHash);
        token.setUsageCount(5);
        when(previewTokenMapper.selectByTokenHash(expectedHash)).thenReturn(token);

        Optional<PreviewToken> result = previewTokenService.validateToken(
                plainToken, SITE_ID, "article", CONTENT_ID);

        assertTrue(result.isPresent());
        assertEquals(6, result.get().getUsageCount());
        verify(previewTokenMapper).updateUsageCount(token.getId(), 6);
    }

    @Test
    void cleanupExpiredTokens_deletesExpired() {
        when(previewTokenMapper.deleteExpired(any())).thenReturn(5);

        previewTokenService.cleanupExpiredTokens();

        verify(previewTokenMapper).deleteExpired(any(OffsetDateTime.class));
    }

    private PreviewToken createValidStoredToken() {
        PreviewToken token = new PreviewToken();
        token.setId(TOKEN_ID);
        token.setPublishTaskId(TASK_ID);
        token.setSiteId(SITE_ID);
        token.setContentType("article");
        token.setContentId(CONTENT_ID);
        token.setExpiresAt(OffsetDateTime.now().plusHours(24));
        token.setMaxUsage(100);
        token.setUsageCount(0);
        return token;
    }
}
