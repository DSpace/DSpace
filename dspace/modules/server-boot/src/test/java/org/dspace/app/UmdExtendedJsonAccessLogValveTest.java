package org.dspace.app;


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import com.google.common.base.Splitter;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.AccessLogValve;
import org.junit.Before;
import org.junit.Test;

public class UmdExtendedJsonAccessLogValveTest {
    private StringWriter logOutput;
    private TestableUmdExtendedJsonAccessLogValve valve;
    // Timestamps for requests will be epoch start (January 1, 1970)
    private String expectedTime = getTimestamp(0l);

    @Before
    public void setUp() throws Exception {
        logOutput = new StringWriter();
        PrintWriter writer = new PrintWriter(logOutput);
        valve = new TestableUmdExtendedJsonAccessLogValve();
        valve.setWriter(writer);
    }

    @Test
    public void testNullFormat() throws Exception {
        valve.setPattern(null);

        simulateRequest(valve, "GET /index.html HTTP/1.1", "192.168.1.1", 200, 5123);

        // Having having a "}" in the output is what JsonAccessLogValve
        // does where there is an empty format.
        assertEquals(
            """
            }
            """,
            logOutput.toString()
        );
    }

    @Test
    public void testEmptyFormat() throws Exception {
        valve.setPattern("");

        simulateRequest(valve, "GET /index.html HTTP/1.1", "192.168.1.1", 200, 5123);

        // Having having a "}" in the output is what JsonAccessLogValve
        // does where there is an empty format.
        assertEquals("}\n", logOutput.toString());
    }

    @Test
    public void testLogFileOnlyFormat() throws Exception {
        valve.setPattern("#logFile:access.log#");

        simulateRequest(valve, "GET /index.html HTTP/1.1", "192.168.1.1", 200, 5123);

        // Having having a "}" in the output is what JsonAccessLogValve
        // does where there is an empty format.
        assertEquals(
            """
            {"logFile":"access.log"}
            """,
            logOutput.toString()
        );
    }

    @Test
    public void testHostFormat() throws Exception {
        valve.setPattern("%h");

        simulateRequest(valve, "GET /index.html HTTP/1.1", "192.168.1.1", 200, 5123);

        assertEquals(
            """
            {"host":"192.168.1.1"}
            """,
            logOutput.toString()
        );
    }

    @Test
    public void testHostLogFileFormat() throws Exception {
        valve.setPattern("%h #logFile:access.log#");


        simulateRequest(valve, "GET /index.html HTTP/1.1", "192.168.1.1", 200, 5123);

        assertEquals(
            """
            {"host":"192.168.1.1","logFile":"access.log"}
            """,
            logOutput.toString()
        );
    }

    @Test
    public void testHostInvalidLogFileFormat() throws Exception {
        valve.setPattern("%h #invalid#");

        simulateRequest(valve, "GET /index.html HTTP/1.1", "192.168.1.1", 200, 5123);

        assertEquals(
            """
            {"host":"192.168.1.1"}
            """,
            logOutput.toString()
        );
    }

    @Test
    public void testHostStatusLogFileFormat() throws Exception {
        valve.setPattern("%h %s %b #logFile:access.log#");

        simulateRequest(valve, "GET /index.html HTTP/1.1", "192.168.1.1", 200, 5123);

        assertEquals(
            """
            {"host":"192.168.1.1","statusCode":"200","size":"5123","logFile":"access.log"}
            """,
            logOutput.toString()
        );
    }

    @Test
    @SuppressWarnings("LineLength")
    public void testCommonFormat() throws Exception {
        valve.setPattern("common");

        String requestLine = "GET /index.html HTTP/1.1";
        simulateRequest(valve, requestLine, "192.168.1.1", 200, 5123);

        assertEquals(
            """
            {"host":"192.168.1.1","logicalUserName":"-","user":"-","time":"[%s]","request":"%s","statusCode":"200","size":"5123"}
            """.formatted(expectedTime, requestLine),
            logOutput.toString()
        );
    }

    @Test
    @SuppressWarnings("LineLength")
    public void testCommonLogFileFormat() throws Exception {
        valve.setPattern("common #logFile:access.log#");

        String requestLine = "GET /index.html HTTP/1.1";
        simulateRequest(valve, requestLine, "192.168.1.1", 200, 5123);

        assertEquals(
            """
            {"host":"192.168.1.1","logicalUserName":"-","user":"-","time":"[%s]","request":"%s","statusCode":"200","size":"5123","logFile":"access.log"}
            """.formatted(expectedTime, requestLine),
            logOutput.toString()
        );
    }

    @Test
    @SuppressWarnings("LineLength")
    public void testCombinedFormat() throws Exception {
        valve.setPattern("combined");

        String requestLine = "GET /index.html HTTP/1.1";
        String referer = "https://api.drum-local.lib.umd.edu/server/";
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:137.0) Gecko/20100101 Firefox/137.0";

        simulateRequest(valve, requestLine, "192.168.1.1", 200, 5123, referer, userAgent);

        assertEquals(
            """
            {"host":"192.168.1.1","logicalUserName":"-","user":"-","time":"[%s]","request":"%s","statusCode":"200","size":"5123","requestHeaders": {"Referer":"%s","User-Agent":"%s"}}
            """.formatted(expectedTime, requestLine, referer, userAgent),
            logOutput.toString()
        );
    }

    @Test
    @SuppressWarnings("LineLength")
    public void testCombinedLogFileFormat() throws Exception {
        valve.setPattern("combined #logFile:access.log#");

        String requestLine = "GET /index.html HTTP/1.1";
        String referer = "https://api.drum-local.lib.umd.edu/server/";
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:137.0) Gecko/20100101 Firefox/137.0";

        simulateRequest(valve, requestLine, "192.168.1.1", 200, 5123, referer, userAgent);

        assertEquals(
            """
            {"host":"192.168.1.1","logicalUserName":"-","user":"-","time":"[%s]","request":"%s","statusCode":"200","size":"5123","requestHeaders": {"Referer":"%s","User-Agent":"%s"},"logFile":"access.log"}
            """.formatted(expectedTime, requestLine, referer, userAgent),
            logOutput.toString()
        );
    }

    /**
     * Simulate an HTTP request. This simpler method is used with the "common"
     * log format.
     *
     * @param valve the AccessLogValve to use for logging
     * @param requestLine the HTTP request (i.e., "GET /index.html HTTP/1.1")
     * @param remoteIP the IP address making the request
     * @param status the response status code (typically "200" for HTTP OK)
     * @param bytes the number of bytes in the response (arbitrary)
     */
    private void simulateRequest(AccessLogValve valve, String requestLine, String remoteIP, int status, int bytes) {
        simulateRequest(valve, requestLine, remoteIP, status, bytes, "", "");
    }

    /**
     * Simulate an HTTP request. This extended method is used with the
     * "combined" log format
     *
     * @param valve the AccessLogValve to use for logging
     * @param requestLine the HTTP request (i.e., "GET /index.html HTTP/1.1")
     * @param remoteIP the IP address making the request
     * @param status the response status code (typically "200" for HTTP OK)
     * @param bytes the number of bytes in the response (arbitrary)
     * @param referer a String representing the URL of the referer
     * @param userAgent a String representing the user agent.
     */
    private void simulateRequest(
        AccessLogValve valve, String requestLine, String remoteIP, int status, int bytes,
        String referer, String userAgent
    ) {
        // Create a mock Request and Response from Tomcat
        Request mockRequest = mock(Request.class);
        Response mockResponse = mock(Response.class);

        // Create a mock CoyoteRequest
        org.apache.coyote.Request mockCoyoteRequest = mock(org.apache.coyote.Request.class);
        when(mockRequest.getCoyoteRequest()).thenReturn(mockCoyoteRequest);
        // Arbitrarily set "time" at epoch start
        when(mockCoyoteRequest.getStartTime()).thenReturn(0l);

        // Set up the mock Response to return expected values
        when(mockRequest.getRemoteHost()).thenReturn(remoteIP);

        List<String> requestParams = Splitter.on(' ').splitToList(requestLine);
        if (requestParams.size() == 3) {
            when(mockRequest.getMethod()).thenReturn(requestParams.get(0));
            when(mockRequest.getRequestURI()).thenReturn(requestParams.get(1));
            when(mockRequest.getProtocol()).thenReturn(requestParams.get(2));
        }

        // Set up the mock Response to return expected values
        when(mockResponse.getStatus()).thenReturn(status);
        when(mockResponse.getBytesWritten(false)).thenReturn(Long.valueOf(bytes));

        Enumeration<String> enumReferer = Collections.enumeration(List.of(referer));
        when(mockRequest.getHeaders("Referer")).thenReturn(enumReferer);

        Enumeration<String> enumUserAgent = Collections.enumeration(List.of(userAgent));
        when(mockRequest.getHeaders("User-Agent")).thenReturn(enumUserAgent);

        // Invoke the logging logic of the JsonAccessLogValve
        valve.log(mockRequest, mockResponse, bytes);
    }

    /**
     * Returns the expected timestamp string from the given epoch timestamp
     *
     * @param epochMillis the number of milliseconds since the epoch
     */
    private String getTimestamp(long epochMillis) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");
        return formatter.format(new Date(epochMillis));
    }
}

/**
 * Testable implementation of the UmdExtendedJsonAccessLogValve that provides
 * the ability to set the output Writer, and manages the lifecycle state.
 */
class TestableUmdExtendedJsonAccessLogValve extends UmdExtendedJsonAccessLogValve {
    public TestableUmdExtendedJsonAccessLogValve() {
        super();
        this.setRotatable(false);
    }

    /**
     * Set the PrintWriter the formatted log entries are sent to.
     *
     * @param writer the PrintWriter to send formatted log entries to.
     */
    public void setWriter(PrintWriter writer) {
        this.writer = writer;
    }

    @Override
    public LifecycleState getState() {
        return LifecycleState.STARTED;
    }
}
