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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Continent;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.MaxMind;
import com.maxmind.geoip2.record.Postal;
import com.maxmind.geoip2.record.RepresentedCountry;
import com.maxmind.geoip2.record.Subdivision;
import com.maxmind.geoip2.record.Traits;
import mockit.Deencapsulation;
import mockit.Mock;
import mockit.MockUp;

/**
 * Mock service to mock the location Lookup Service used by the SOLR statistics
 * logger.
 */
public class FakeDatabaseReader
        extends MockUp<DatabaseReader> {

    FakeDatabaseReader() {
    }

    public FakeDatabaseReader(Object object) {
    }

    public FakeDatabaseReader $init(Builder builder) {
        return this;
    }

    /*
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
    */

    @Mock
    public CityResponse city(InetAddress address) {
        List<String> names = new ArrayList<>(1);

        names.add("New York");
        City city = new City(names, 1, 1, new HashMap());

        Continent continent = new Continent();

        names.clear();
        names.add("United States");
        Country country = new Country(names, 1, 1, "US", new HashMap());

        Location location = new Location(1, 1, 40.760498D, -73.9933D, 501, 1, "EST");

        MaxMind maxmind = new MaxMind();

        Postal postal = new Postal("10036", 1);

        RepresentedCountry representedCountry = new RepresentedCountry();

        ArrayList<Subdivision> subdivisions = new ArrayList<>(0);

        Traits traits = new Traits();

        CityResponse response = new CityResponse(city, continent, country,
                location, maxmind, postal, country, representedCountry,
                subdivisions, traits);
        return response;
    }

    public static class Builder
            extends MockUp<DatabaseReader.Builder> {

        public Builder() {}

        /**
         * Fake constructor.
         * @param file ignored.
         */
        @Mock
        public void $init(File file) {
        }

        @Mock
        public DatabaseReader build()
                throws IOException {
            return Deencapsulation.newUninitializedInstance(DatabaseReader.class);
        }
    }
}
