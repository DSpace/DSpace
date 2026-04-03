/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.health;

import static org.dspace.app.rest.configuration.ActuatorConfiguration.UP_WITH_ISSUES_STATUS;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.storage.bitstore.BitstreamInputStreamOpenInfo;
import org.dspace.storage.bitstore.LeakTrackingInputStream;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

/**
 * Implementation of {@link HealthIndicator}
 * to verify that bitstreams are not open longer than a certain amount of time
 *
 * @author Bram Maegerman (bram.maegerman at atmire.com)
 */
public class BitstreamStreamAnomaliesHealthIndicator extends AbstractHealthIndicator {

    public static final long MAXIMUM_OPEN_MINUTES = 5;

    @Override
    protected void doHealthCheck(Builder builder) throws Exception {
        Map<UUID, BitstreamInputStreamOpenInfo> openStreams = LeakTrackingInputStream.snapshotOpenStreams();
        List<BitstreamInputStreamOpenInfo> leakedStreams = new ArrayList<>();
        for (BitstreamInputStreamOpenInfo openStream : openStreams.values()) {
            if (openStream.openedAt().isBefore(
                    Instant.now().minusSeconds(60 * MAXIMUM_OPEN_MINUTES))) {
                leakedStreams.add(openStream);
            }
        }

        if (!leakedStreams.isEmpty()) {
            builder.status(UP_WITH_ISSUES_STATUS)
                   .withDetail("amount",
                               leakedStreams.size() + " bitstream input streams may not have been properly closed");
            for (BitstreamInputStreamOpenInfo leakedStream : leakedStreams) {
                builder.withDetail(leakedStream.id().toString(), leakedStream.toString());
            }
        } else {
            builder.status(Status.UP);
        }
    }
}
