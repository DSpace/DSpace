/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.dspace.AbstractDSpaceTest;
import org.dspace.external.provider.orcid.xml.ExpandedSearchConverter;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cache.Cache;
import org.springframework.cache.jcache.JCacheCacheManager;

public class CachingOrcidRestConnectorTest extends AbstractDSpaceTest {

    //This token should be valid for 20 years
    private static final String sandboxToken = "4bed1e13-7792-4129-9f07-aaf7b88ba88f";

    private static final String orcid = "0000-0002-9150-2529";
    private static final String expectedLabel = "Connor, John";

    private CachingOrcidRestConnector sut;

    @Before
    public void setup() {
        sut = new CachingOrcidRestConnector();
    }

    @Test(expected = RuntimeException.class)
    public void getAccessToken_badUrl() {
        String accessToken = sut.getAccessToken("secret","id", "http://example.com");
        assertNull("Expecting accessToken to be null", accessToken);
    }

    @Test(expected = RuntimeException.class)
    public void getAccessToken_badParams() {
        //expect an exception to be thrown
        sut.getAccessToken(null, null, null);
    }

    @Test(expected = RuntimeException.class)
    public void getAccessToken() {
        String accessToken = sut.getAccessToken("DEAD", "BEEF", "https://sandbox.orcid.org/oauth/token");
        assertNotNull("Expecting accessToken to be not null", accessToken);
    }

    @Test
    public void getLabel() {
        sut = Mockito.spy(sut);
        sut.setApiURL("https://pub.sandbox.orcid.org/v3.0");
        //Mock the CachingOrcidRestConnector so that getAccessToken returns sandboxToken
        doReturn(sandboxToken).when(sut).getAccessToken(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        String label = sut.getLabel(orcid);
        assertEquals(expectedLabel, label);
    }
    @Test
    public void search() {
        sut = Mockito.spy(sut);
        sut.setApiURL("https://pub.sandbox.orcid.org/v3.0");
        //Mock the CachingOrcidRestConnector so that getAccessToken returns sandboxToken
        doReturn(sandboxToken).when(sut).getAccessToken(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        ExpandedSearchConverter.Results search = sut.search("joh", 0, 1);
        //Should match all Johns also, because edismax with wildcard
        assertTrue(search.numFound() > 1000);
    }

    @Test
    public void search_fail() {
        sut = Mockito.spy(sut);
        sut.setApiURL("https://pub.sandbox.orcid.org/v3.0");
        //Mock the CachingOrcidRestConnector so that getAccessToken returns and invalid token
        doReturn("FAKE").when(sut).getAccessToken(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());

        ExpandedSearchConverter.Results search = sut.search("joh", 0, 1);

        assertFalse(search.isOk());

        //Further calls fail too, token is stored
        search = sut.search("joh", 0, 1);
        assertFalse(search.isOk());

        verify(sut, times(1)).getAccessToken(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());


    }

    @Test
    public void testCachable() {
        CachingOrcidRestConnector c = new DSpace().getServiceManager().getServiceByName(
                "CachingOrcidRestConnector", CachingOrcidRestConnector.class);

        Cache cache = prepareCache();

        assertNull(cache.get(orcid));

        /*
        I have issues trying to mock/spy when the class a spring bean modified by cglib
        doReturn(sandboxToken).when(c).getAccessToken(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        verify(c, times(1)).getLabel(orcid);
        */

        c.setApiURL("https://pub.sandbox.orcid.org/v3.0");
        c.forceAccessToken(sandboxToken);

        String r1 = c.getLabel(orcid);
        assertEquals(expectedLabel, r1);
        String r2 = c.getLabel(orcid);
        assertEquals(expectedLabel, r2);
        //get the orcid-labels cache and verify that the label is there
        assertEquals(expectedLabel, cache.get(orcid).get());
    }

    @Test
    public void testCacheableWithError() {
        CachingOrcidRestConnector c = new DSpace().getServiceManager().getServiceByName(
                "CachingOrcidRestConnector", CachingOrcidRestConnector.class);

        Cache cache = prepareCache();
        assertNull(cache.get(orcid));

        //skip init
        c.forceAccessToken(sandboxToken);
        //set bad ApiURL to provoke an error
        c.setApiURL("https://api.sandbox.orcid.org/");
        String r1 = c.getLabel(orcid);
        //on error, getLabel should return null
        assertNull(r1);
        //the cache should not contain a value for this id
        assertNull(cache.get(orcid));

        //fix the error
        c.setApiURL("https://pub.sandbox.orcid.org/v3.0");
        // the error flipped the initialized flag, this reset it
        c.forceAccessToken(sandboxToken);
        String r2 = c.getLabel(orcid);
        assertEquals(expectedLabel, r2);
        //the cache should now contain a value for this id
        assertEquals(expectedLabel, cache.get(orcid).get());
    }

    private Cache prepareCache() {
        //get the cacheManager from the serviceManager
        JCacheCacheManager cacheManager = new DSpace().getServiceManager().getServiceByName("cacheManager",
                JCacheCacheManager.class);

        Cache cache = cacheManager.getCache("orcid-labels");
        //each test should have a clean cache
        cache.clear();
        return cache;
    }

}
