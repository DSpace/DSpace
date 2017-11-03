/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import java.io.File;
import java.io.IOException;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import org.dspace.app.rest.test.AbstractDSpaceIntegrationTest;

/**
 * Mock service to mock the location Lookup Service used by the SOLR statistics logger
 */
public class MockLookupService extends LookupService {

    public MockLookupService() throws IOException {
        //Just give our super class a file so that he's happy. We'll mock all responses anyway.
        super(AbstractDSpaceIntegrationTest.getDspaceDir() + File.separator + "config" + File.separator + "dspace.cfg");
    }

    @Override
    public Location getLocation(String str) {
        Location location = new Location();
        location.countryCode = "US";
        location.countryName = "United States";
        location.region = "NY";
        location.city = "New York";
        location.postalCode = "10036";
        location.latitude = 40.760498F;
        location.longitude = -73.9933F;
        location.dma_code = 501;
        location.area_code = 212;
        location.metro_code = 501;

        return location;
    }
}
