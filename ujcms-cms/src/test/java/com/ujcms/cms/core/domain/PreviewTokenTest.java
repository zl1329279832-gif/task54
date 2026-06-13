package com.ujcms.cms.core.domain;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PreviewToken 领域逻辑测试
 */
class PreviewTokenTest {

    @Test
    void isExpired_whenExpiresAtInFuture_returnsFalse() {
        PreviewToken token = new PreviewToken();
        token.setExpiresAt(OffsetDateTime.now().plusHours(1));
        assertFalse(token.isExpired());
    }

    @Test
    void isExpired_whenExpiresAtInPast_returnsTrue() {
        PreviewToken token = new PreviewToken();
        token.setExpiresAt(OffsetDateTime.now().minusHours(1));
        assertTrue(token.isExpired());
    }

    @Test
    void isExpired_whenExpiresAtIsNull_returnsFalse() {
        PreviewToken token = new PreviewToken();
        token.setExpiresAt(null);
        assertFalse(token.isExpired());
    }

    @Test
    void isUsageExhausted_whenUsageCountBelowMax_returnsFalse() {
        PreviewToken token = new PreviewToken();
        token.setMaxUsage(10);
        token.setUsageCount(5);
        assertFalse(token.isUsageExhausted());
    }

    @Test
    void isUsageExhausted_whenUsageCountEqualsMax_returnsTrue() {
        PreviewToken token = new PreviewToken();
        token.setMaxUsage(10);
        token.setUsageCount(10);
        assertTrue(token.isUsageExhausted());
    }

    @Test
    void isUsageExhausted_whenUsageCountExceedsMax_returnsTrue() {
        PreviewToken token = new PreviewToken();
        token.setMaxUsage(10);
        token.setUsageCount(15);
        assertTrue(token.isUsageExhausted());
    }

    @Test
    void isUsageExhausted_whenMaxUsageIsNull_returnsFalse() {
        PreviewToken token = new PreviewToken();
        token.setMaxUsage(null);
        token.setUsageCount(100);
        assertFalse(token.isUsageExhausted());
    }

    @Test
    void isUsageExhausted_whenMaxUsageIsZero_returnsFalse() {
        PreviewToken token = new PreviewToken();
        token.setMaxUsage(0);
        token.setUsageCount(100);
        assertFalse(token.isUsageExhausted());
    }

    @Test
    void isValid_whenNotExpiredAndNotExhausted_returnsTrue() {
        PreviewToken token = new PreviewToken();
        token.setExpiresAt(OffsetDateTime.now().plusHours(1));
        token.setMaxUsage(10);
        token.setUsageCount(5);
        assertTrue(token.isValid());
    }

    @Test
    void isValid_whenExpired_returnsFalse() {
        PreviewToken token = new PreviewToken();
        token.setExpiresAt(OffsetDateTime.now().minusHours(1));
        token.setMaxUsage(10);
        token.setUsageCount(5);
        assertFalse(token.isValid());
    }

    @Test
    void isValid_whenUsageExhausted_returnsFalse() {
        PreviewToken token = new PreviewToken();
        token.setExpiresAt(OffsetDateTime.now().plusHours(1));
        token.setMaxUsage(10);
        token.setUsageCount(10);
        assertFalse(token.isValid());
    }

    @Test
    void incrementUsage_fromNonNull_incrementsByOne() {
        PreviewToken token = new PreviewToken();
        token.setUsageCount(5);
        token.incrementUsage();
        assertEquals(6, token.getUsageCount());
    }

    @Test
    void incrementUsage_fromNull_setsToOne() {
        PreviewToken token = new PreviewToken();
        token.setUsageCount(null);
        token.incrementUsage();
        assertEquals(1, token.getUsageCount());
    }

    @Test
    void incrementUsage_fromZero_setsToOne() {
        PreviewToken token = new PreviewToken();
        token.setUsageCount(0);
        token.incrementUsage();
        assertEquals(1, token.getUsageCount());
    }
}
