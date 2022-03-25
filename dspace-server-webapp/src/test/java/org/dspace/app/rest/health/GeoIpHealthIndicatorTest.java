/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.health;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.maxmind.geoip2.DatabaseReader;
import org.dspace.app.rest.configuration.ActuatorConfiguration;
import org.dspace.statistics.GeoIpService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

/**
 * Unit tests for {@link GeoIpHealthIndicator}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class GeoIpHealthIndicatorTest {

    @Mock
    private GeoIpService geoIpService;

    @InjectMocks
    private GeoIpHealthIndicator geoIpHealthIndicator;

    @Mock
    private DatabaseReader databaseReader;

    @Test
    public void testWithGeoIpConfiguredCorrectly() {
        when(geoIpService.getDatabaseReader()).thenReturn(databaseReader);

        Health health = geoIpHealthIndicator.health();

        assertThat(health.getStatus(), is(Status.UP));
        assertThat(health.getDetails(), anEmptyMap());
    }

    @Test
    public void testWithGeoIpWrongConfiguration() {
        when(geoIpService.getDatabaseReader()).thenThrow(new IllegalStateException("Missing db file"));

        Health health = geoIpHealthIndicator.health();

        assertThat(health.getStatus(), is(ActuatorConfiguration.UP_WITH_ISSUES_STATUS));
        assertThat(health.getDetails(), is(Map.of("reason", "Missing db file")));
    }

    @Test
    public void testWithUnexpectedError() {
        when(geoIpService.getDatabaseReader()).thenThrow(new RuntimeException("Generic error"));

        Health health = geoIpHealthIndicator.health();

        assertThat(health.getStatus(), is(Status.DOWN));
    }
}
