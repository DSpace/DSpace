/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.health;

import static org.dspace.app.rest.health.BitstreamStreamAnomaliesHealthIndicator.MAXIMUM_OPEN_MINUTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.dspace.app.rest.configuration.ActuatorConfiguration;
import org.dspace.storage.bitstore.BitstreamInputStreamOpenInfo;
import org.dspace.storage.bitstore.LeakTrackingInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

/**
 * Unit tests for {@link BitstreamStreamAnomaliesHealthIndicator}.
 *
 * @author Bram Maegerman (bram.maegerman at atmire.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class BitstreamStreamAnomaliesHealthIndicatorTest {

    @InjectMocks
    private BitstreamStreamAnomaliesHealthIndicator bitstreamStreamAnomaliesHealthIndicator;

    @Test
    public void testWithoutLeakedBitstream() {
        try (MockedStatic<LeakTrackingInputStream> mockedStatic = Mockito.mockStatic(LeakTrackingInputStream.class)) {
            mockedStatic.when(LeakTrackingInputStream::snapshotOpenStreams).thenReturn(Collections.emptyMap());

            Health health = bitstreamStreamAnomaliesHealthIndicator.health();

            assertEquals(Status.UP, health.getStatus());
            assertEquals(Collections.emptyMap(), health.getDetails());
        }
    }

    @Test
    public void testWithOneLeakedBitstream() {
        try (MockedStatic<LeakTrackingInputStream> mockedStatic = Mockito.mockStatic(LeakTrackingInputStream.class)) {
            UUID bitstreamInfoId = UUID.randomUUID();
            mockedStatic.when(LeakTrackingInputStream::snapshotOpenStreams)
                        .thenReturn(Map.of(
                                UUID.randomUUID(),
                                new BitstreamInputStreamOpenInfo(
                                        bitstreamInfoId,
                                        "kind1",
                                        Instant.now().minusSeconds(60 * (MAXIMUM_OPEN_MINUTES * 2)),
                                        new Exception("test")
                                )
                        ));

            Health health = bitstreamStreamAnomaliesHealthIndicator.health();

            assertEquals(ActuatorConfiguration.UP_WITH_ISSUES_STATUS, health.getStatus());

            assertEquals(2, health.getDetails().size());

            assertTrue(health.getDetails().containsKey("amount"));
            assertTrue(health.getDetails().get("amount").toString().contains("1"));

            assertTrue(health.getDetails().containsKey(bitstreamInfoId.toString()));
            assertTrue(health.getDetails().get(bitstreamInfoId.toString()).toString().contains("kind1"));
        }
    }

    @Test
    public void testWithMultipleLeakedBitstreams() {
        try (MockedStatic<LeakTrackingInputStream> mockedStatic = Mockito.mockStatic(LeakTrackingInputStream.class)) {
            UUID bitstreamInfoId1 = UUID.randomUUID();
            UUID bitstreamInfoId2 = UUID.randomUUID();
            mockedStatic.when(LeakTrackingInputStream::snapshotOpenStreams)
                        .thenReturn(Map.of(
                                UUID.randomUUID(),
                                new BitstreamInputStreamOpenInfo(
                                        bitstreamInfoId1,
                                        "kind1",
                                        Instant.now().minusSeconds(60 * (MAXIMUM_OPEN_MINUTES * 2)),
                                        new Exception("test")
                                ),
                                UUID.randomUUID(),
                                new BitstreamInputStreamOpenInfo(
                                        bitstreamInfoId2,
                                        "kind2",
                                        Instant.now().minusSeconds(60 * (MAXIMUM_OPEN_MINUTES * 2)),
                                        new Exception("test")
                                )
                        ));

            Health health = bitstreamStreamAnomaliesHealthIndicator.health();

            assertEquals(ActuatorConfiguration.UP_WITH_ISSUES_STATUS, health.getStatus());

            assertEquals(3, health.getDetails().size());

            assertTrue(health.getDetails().containsKey("amount"));
            assertTrue(health.getDetails().get("amount").toString().contains("2"));

            assertTrue(health.getDetails().containsKey(bitstreamInfoId1.toString()));
            assertTrue(health.getDetails().get(bitstreamInfoId1.toString()).toString().contains("kind1"));

            assertTrue(health.getDetails().containsKey(bitstreamInfoId2.toString()));
            assertTrue(health.getDetails().get(bitstreamInfoId2.toString()).toString().contains("kind2"));
        }
    }

    @Test
    public void testWithOneLeakedAndOneRegularBitstream() {
        try (MockedStatic<LeakTrackingInputStream> mockedStatic = Mockito.mockStatic(LeakTrackingInputStream.class)) {
            UUID bitstreamInfoId1 = UUID.randomUUID();
            UUID bitstreamInfoId2 = UUID.randomUUID();
            mockedStatic.when(LeakTrackingInputStream::snapshotOpenStreams)
                        .thenReturn(Map.of(
                                UUID.randomUUID(),
                                new BitstreamInputStreamOpenInfo(
                                        bitstreamInfoId1,
                                        "kind1",
                                        Instant.now().minusSeconds(60 * (MAXIMUM_OPEN_MINUTES * 2)),
                                        new Exception("test")
                                ),
                                UUID.randomUUID(),
                                new BitstreamInputStreamOpenInfo(
                                        bitstreamInfoId2,
                                        "kind2",
                                        Instant.now(),
                                        new Exception("test")
                                )
                        ));

            Health health = bitstreamStreamAnomaliesHealthIndicator.health();

            assertEquals(ActuatorConfiguration.UP_WITH_ISSUES_STATUS, health.getStatus());

            assertEquals(2, health.getDetails().size());

            assertTrue(health.getDetails().containsKey("amount"));
            assertTrue(health.getDetails().get("amount").toString().contains("1"));

            assertTrue(health.getDetails().containsKey(bitstreamInfoId1.toString()));
            assertTrue(health.getDetails().get(bitstreamInfoId1.toString()).toString().contains("kind1"));

            assertFalse(health.getDetails().containsKey(bitstreamInfoId2.toString()));
        }
    }
}
