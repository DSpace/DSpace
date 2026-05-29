/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.dspace.AbstractUnitTest;
import org.junit.Test;

public class MatomoAbstractClientTest extends AbstractUnitTest {

    private static class MockHttpURLConnection extends HttpURLConnection {
        private Map<String, String> requestProperties = new HashMap<>();

        protected MockHttpURLConnection() {
            super(null);
        }

        @Override
        public void setRequestProperty(String key, String value) {
            requestProperties.put(key, value);
        }

        @Override
        public String getRequestProperty(String key) {
            return requestProperties.get(key);
        }

        // Stub implementations for abstract methods
        @Override
        public void disconnect() {

        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {

        }
    }

    /**
     * Test case for addCookies method when a single cookie is provided.
     * This test verifies that the method correctly sets the Cookie request property
     * when given a map with a single cookie, and the resulting cookie string
     * does not end with a semicolon.
     */
    @Test
    public void test_addCookies_singleCookie() {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        Map<String, String> cookies = new LinkedHashMap<>();
        cookies.put("testCookie", "testValue");

        MatomoAbstractClient.addCookies(connection, cookies);

        verify(connection).setRequestProperty("Cookie", "testCookie=testValue");
    }

    /**
     * Test case for static void addCookies(HttpURLConnection connection, Map<String, String> cookies)
     * This test verifies that when given a non-null but empty cookies map,
     * no request property is set on the connection.
     */
    @Test
    public void test_addCookies_withEmptyCookiesMap() {
        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        Map<String, String> cookies = new LinkedHashMap<>();

        MatomoAbstractClient.addCookies(mockConnection, cookies);

        verify(mockConnection, never()).setRequestProperty(anyString(), anyString());
    }

    /**
     * Tests the addCookies method when cookies are present and added successfully.
     * Verifies that the Cookie request property is set correctly with multiple cookies.
     */
    @Test
    public void test_addCookies_withMultipleCookies() {
        HttpURLConnection mockConnection = new MockHttpURLConnection();
        Map<String, String> cookies = new HashMap<>();
        cookies.put("cookie1", "value1");
        cookies.put("cookie2", "value2");

        MatomoAbstractClient.addCookies(mockConnection, cookies);

        String cookieHeader = mockConnection.getRequestProperty("Cookie");
        assertThat("Cookie header should contain both cookies",
                   cookieHeader,
                   allOf(
                       notNullValue(),
                       containsString("cookie1=value1"),
                       containsString("cookie2=value2")
                   )
        );
    }

    /**
     * Tests the addCookies method with a null cookies map.
     * This scenario is explicitly handled in the method implementation.
     * Expected behavior: No exception should be thrown, and no request property should be set.
     */
    @Test
    public void test_addCookies_withNullCookiesMap() {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        Map<String, String> cookies = null;

        MatomoAbstractClient.addCookies(connection, cookies);

        verify(connection, never()).setRequestProperty(anyString(), anyString());
    }

    /**
     * Test case for addCookies method when cookies map is null but a default cookie is set.
     * This test verifies that even when the cookies map is null, if a default cookie is present,
     * it will be added to the connection's request property.
     */
    @Test
    public void test_addCookies_withNullMapAndDefaultCookie() {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        Map<String, String> cookies = null;

        MatomoAbstractClient.addCookies(connection, cookies);

        verify(connection, never()).setRequestProperty(eq("Cookie"), anyString());
        verify(connection, never()).getRequestProperty(eq("Cookie"));
    }

}
