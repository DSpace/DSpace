/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.email.oauth2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * Offline tests for {@link OAuth2TokenManager}.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class OAuth2TokenManagerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    /**
     * Verify that a valid token is reused without another request.
     */
    @Test
    public void reusesValidToken() {
        MutableClock clock = new MutableClock(NOW);
        FakeTokenClient client = new FakeTokenClient();
        client.addResponse(new OAuth2Token("access-1", null, NOW.plusSeconds(3600)));
        OAuth2TokenManager manager = new OAuth2TokenManager(client, settings(), clock);

        assertEquals("access-1", manager.getAccessToken());
        assertEquals("access-1", manager.getAccessToken());
        assertEquals(1, client.calls.get());
    }

    /**
     * Verify that the token is refreshed inside the safety window before expiry.
     */
    @Test
    public void refreshesWithinExpirySkew() {
        MutableClock clock = new MutableClock(NOW);
        FakeTokenClient client = new FakeTokenClient();
        client.addResponse(new OAuth2Token("access-1", null, NOW.plusSeconds(3600)));
        client.addResponse(new OAuth2Token("access-2", null, NOW.plusSeconds(7200)));
        OAuth2TokenManager manager = new OAuth2TokenManager(client, settings(), clock);

        assertEquals("access-1", manager.getAccessToken());
        clock.advance(Duration.ofSeconds(3539));
        assertEquals("access-1", manager.getAccessToken());
        assertEquals(1, client.calls.get());

        clock.advance(Duration.ofSeconds(1));
        assertEquals("access-2", manager.getAccessToken());
        assertEquals(2, client.calls.get());
    }

    /**
     * Verify that a rotated refresh token is used for the next refresh.
     */
    @Test
    public void usesRotatedRefreshToken() {
        MutableClock clock = new MutableClock(NOW);
        FakeTokenClient client = new FakeTokenClient();
        client.addResponse(new OAuth2Token("access-1", "rotated", NOW.plusSeconds(3600)));
        client.addResponse(new OAuth2Token("access-2", null, NOW.plusSeconds(7200)));
        OAuth2TokenManager manager = new OAuth2TokenManager(client, settings(), clock);

        assertEquals("access-1", manager.getAccessToken());
        clock.advance(Duration.ofSeconds(3600));
        assertEquals("access-2", manager.getAccessToken());

        assertEquals(Arrays.asList("initial", "rotated"), client.refreshTokens);
    }

    /**
     * Verify that a failed refresh is propagated and can be retried.
     */
    @Test
    public void propagatesFailureAndRetries() {
        MutableClock clock = new MutableClock(NOW);
        FakeTokenClient client = new FakeTokenClient();
        client.failWith(new OAuth2Exception("boom"));
        OAuth2TokenManager manager = new OAuth2TokenManager(client, settings(), clock);

        OAuth2Exception exception = assertThrows(OAuth2Exception.class, manager::getAccessToken);
        assertEquals("boom", exception.getMessage());
        assertEquals(1, client.calls.get());

        client.addResponse(new OAuth2Token("access-1", null, NOW.plusSeconds(3600)));
        assertEquals("access-1", manager.getAccessToken());
        assertEquals(2, client.calls.get());
    }

    /**
     * Verify that concurrent callers share a single refresh request.
     *
     * @throws Exception when the concurrent test fails
     */
    @Test
    public void refreshesOnceUnderConcurrency() throws Exception {
        BlockingTokenClient client = new BlockingTokenClient(NOW.plusSeconds(3600));
        OAuth2TokenManager manager = new OAuth2TokenManager(client, settings(), new MutableClock(NOW));
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 8; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return manager.getAccessToken();
                }));
            }
            start.countDown();
            assertTrue(client.entered.await(5, TimeUnit.SECONDS));
            client.release.countDown();

            for (Future<String> future : futures) {
                assertEquals("access", future.get(5, TimeUnit.SECONDS));
            }
            assertEquals(1, client.calls.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private static OAuth2Settings settings() {
        return new OAuth2Settings(
            "gmail",
            "client-id",
            "client-secret",
            "initial",
            null,
            "scope",
            "user@example.com",
            URI.create("https://example.com/token")
        );
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private static final class FakeTokenClient implements OAuth2TokenClient {

        private final List<OAuth2Token> responses = new ArrayList<>();
        private final List<String> refreshTokens = new ArrayList<>();
        private final AtomicInteger calls = new AtomicInteger();
        private RuntimeException failure;

        private void addResponse(OAuth2Token token) {
            responses.add(token);
        }

        private void failWith(RuntimeException exception) {
            failure = exception;
        }

        @Override
        public OAuth2Token requestToken(OAuth2Settings settings, String refreshToken) {
            calls.incrementAndGet();
            refreshTokens.add(refreshToken);
            if (failure != null) {
                RuntimeException exception = failure;
                failure = null;
                throw exception;
            }
            if (responses.isEmpty()) {
                throw new IllegalStateException("No token response configured");
            }
            return responses.remove(0);
        }
    }

    private static final class BlockingTokenClient implements OAuth2TokenClient {

        private final AtomicInteger calls = new AtomicInteger();
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final Instant expiresAt;

        private BlockingTokenClient(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }

        @Override
        public OAuth2Token requestToken(OAuth2Settings settings, String refreshToken) {
            calls.incrementAndGet();
            entered.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new OAuth2Exception("Interrupted", e);
            }
            return new OAuth2Token("access", null, expiresAt);
        }
    }
}
