/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.health;

import static org.dspace.app.rest.configuration.ActuatorConfiguration.UP_WITH_ISSUES_STATUS;

import org.dspace.statistics.GeoIpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Implementation of {@link HealthIndicator} that verifies if the GeoIP database
 * is configured correctly.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class GeoIpHealthIndicator extends AbstractHealthIndicator {

    @Autowired
    private GeoIpService geoIpService;

    @Override
    protected void doHealthCheck(Builder builder) throws Exception {

        try {
            geoIpService.getDatabaseReader();
            builder.up();
        } catch (IllegalStateException ex) {
            builder.status(UP_WITH_ISSUES_STATUS).withDetail("reason", ex.getMessage());
        }

    }

}
