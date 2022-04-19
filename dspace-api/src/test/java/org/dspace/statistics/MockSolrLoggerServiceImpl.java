/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Continent;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.MaxMind;
import com.maxmind.geoip2.record.Postal;
import com.maxmind.geoip2.record.RepresentedCountry;
import com.maxmind.geoip2.record.Traits;
import org.dspace.solr.MockSolrServer;
import org.mockito.Mockito;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

/**
 * Mock service that uses an embedded SOLR server for the statistics core.
 */
@Service
public class MockSolrLoggerServiceImpl
        extends SolrLoggerServiceImpl
        implements InitializingBean, DisposableBean {

    private MockSolrServer mockSolrServer;

    public MockSolrLoggerServiceImpl() {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Initialize our service with a Mock Solr statistics core
        mockSolrServer = new MockSolrServer("statistics");
        solr = mockSolrServer.getSolrServer();

        // Mock GeoIP's DatabaseReader
        DatabaseReader reader = mock(DatabaseReader.class);
        // Ensure that any tests requesting a city() get a mock/fake CityResponse
        Mockito.lenient().when(reader.city(any(InetAddress.class))).thenReturn(mockCityResponse());
        // Save this mock DatabaseReader to be used by SolrLoggerService
        locationService = reader;
    }

    /**
     * A mock/fake GeoIP CityResponse, which will be used for *all* test
     * statistical requests.
     *
     * @return faked CityResponse
     */
    private CityResponse mockCityResponse() {
        List<String> cityLocales = new ArrayList<String>(Collections.singleton("en"));
        Map<String, String> cityNames = new HashMap<>();
        cityNames.put("en", "New York");
        City city = new City(cityLocales, 1, 1, cityNames);

        List<String> countryNames = new ArrayList<>(Collections.singleton("United States"));
        Country country = new Country(countryNames, 1, 1, "US", new HashMap());

        Location location = new Location(1, 1, 40.760498D, -73.9933D, 501, 1, "EST");

        Postal postal = new Postal("10036", 1);

        return new CityResponse(city, new Continent(), country, location, new MaxMind(), postal,
                                                 country,  new RepresentedCountry(), new ArrayList<>(0),
                                                 new Traits());
    }

    /** Reset the core for the next test.  See {@link MockSolrServer#reset()}. */
    public void reset() {
        mockSolrServer.reset();
    }

    @Override
    public void destroy() throws Exception {
        mockSolrServer.destroy();
    }
}
