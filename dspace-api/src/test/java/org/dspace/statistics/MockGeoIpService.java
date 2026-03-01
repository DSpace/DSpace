/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

/**
 * Mock {@link GeoIpService} for tests that skips startup validation
 * of the GeoLite2-City database file.
 */
public class MockGeoIpService extends GeoIpService {

    @Override
    public void afterPropertiesSet() throws Exception {
        // Skip GeoIP database validation in tests
    }
}
