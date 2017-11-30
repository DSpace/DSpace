/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Test class for MultipartFileSender
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 */
public class MultipartFileSenderTest {

    /**
     * log4j category
     */
    private static final Logger log = Logger.getLogger(MultipartFileSenderTest.class);

    private InputStream is;
    private String mimeType;
    private long lastModified;
    private long length;
    private String fileName;
    private String checksum;


    private HttpServletRequest request;

    private HttpServletResponse response;

    private ContentCachingRequestWrapper requestWrapper;
    private ContentCachingResponseWrapper responseWrapper;


    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     * <p>
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    public void init() throws AuthorizeException {
        try {
            String content = "0123456789";

            this.is = IOUtils.toInputStream(content, CharEncoding.UTF_8);
            this.fileName = "Test-Item.txt";
            this.mimeType = "text/plain";
            this.lastModified = new Date().getTime();
            this.length = content.getBytes().length;
            this.checksum = "testsum";

            this.request = mock(HttpServletRequest.class);
            this.response = new MockHttpServletResponse();

            //Using wrappers so we can save the content of the bodies and use them for tests
            this.requestWrapper = new ContentCachingRequestWrapper(request);
            this.responseWrapper = new ContentCachingResponseWrapper(response);
        } catch (IOException ex) {
            log.error("IO Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        }
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     * <p>
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    public void destroy() {
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Test if Range header is supported and gives back the right range
     * @throws Exception
     */
    @Test
    public void testRangeHeader() throws Exception {
        MultipartFileSender multipartFileSender = MultipartFileSender
                .fromInputStream(is)
                .with(requestWrapper)
                .with(responseWrapper)
                .withFileName(fileName)
                .withChecksum(checksum)
                .withMimetype(mimeType)
                .withLength(length);

        when(request.getHeader(eq("If-Range"))).thenReturn("not_file_to_serve.txt");
        when(request.getHeader(eq("Range"))).thenReturn("bytes=1-3");

        multipartFileSender.serveResource();

        String content = new String(responseWrapper.getContentAsByteArray(), CharEncoding.UTF_8);

        assertEquals("123", content);
    }

    /**
     * Test if we can just request the full file without ranges
     * @throws Exception
     */
    @Test
    public void testFullFileReturn() throws Exception {
        MultipartFileSender multipartFileSender = MultipartFileSender
                .fromInputStream(is)
                .with(requestWrapper)
                .with(responseWrapper)
                .withFileName(fileName)
                .withChecksum(checksum)
                .withMimetype(mimeType)
                .withLength(length);

        multipartFileSender.serveResource();

        String content = new String(responseWrapper.getContentAsByteArray(), CharEncoding.UTF_8);

        assertEquals("0123456789", content);
        assertEquals(checksum, responseWrapper.getHeader("ETag"));
    }

    /**
     * Test for support of Open ranges
     * @throws Exception
     */
    @Test
    public void testOpenRange() throws Exception {
        MultipartFileSender multipartFileSender = MultipartFileSender
                .fromInputStream(is)
                .with(requestWrapper)
                .with(responseWrapper)
                .withFileName(fileName)
                .withChecksum(checksum)
                .withMimetype(mimeType)
                .withLength(length);

        when(request.getHeader(eq("Range"))).thenReturn("bytes=5-");

        multipartFileSender.serveResource();

        String content = new String(responseWrapper.getContentAsByteArray(), CharEncoding.UTF_8);

        assertEquals("56789", content);
    }

    /**
     * Test support for multiple ranges
     * @throws Exception
     */
    @Test
    public void testMultipleRanges() throws Exception {
        MultipartFileSender multipartFileSender = MultipartFileSender
                .fromInputStream(is)
                .with(requestWrapper)
                .with(responseWrapper)
                .withFileName(fileName)
                .withChecksum(checksum)
                .withMimetype(mimeType)
                .withLength(length);

        when(request.getHeader(eq("Range"))).thenReturn("bytes=1-2,3-4,5-9");

        multipartFileSender.serveResource();

        String content = new String(responseWrapper.getContentAsByteArray(), CharEncoding.UTF_8);

        assertEquals("--MULTIPART_BYTERANGES" +
                        "Content-Type: text/plain" +
                        "Content-Range: bytes 1-2/10" +
                        "12" +
                        "--MULTIPART_BYTERANGES" +
                        "Content-Type: text/plain" +
                        "Content-Range: bytes 3-4/10" +
                        "34" +
                        "--MULTIPART_BYTERANGES" +
                        "Content-Type: text/plain" +
                        "Content-Range: bytes 5-9/10" +
                        "56789" +
                        "--MULTIPART_BYTERANGES--".replace("\n", "").replace("\r", "")
                , content.replace("\n", "").replace("\r", "")
        );

    }

    /**
     * Test with a unvalid Range header, should return status 416
     * @throws Exception
     */
    @Test
    public void testInvalidRange() throws Exception {
        MultipartFileSender multipartFileSender = MultipartFileSender
                .fromInputStream(is)
                .with(requestWrapper)
                .with(responseWrapper)
                .withFileName(fileName)
                .withChecksum(checksum)
                .withMimetype(mimeType)
                .withLength(length);

        when(request.getHeader(eq("Range"))).thenReturn("bytes=invalid");

        multipartFileSender.serveResource();

        assertEquals(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, responseWrapper.getStatusCode());
    }

    /**
     * Test if the ETAG is in the response header
     * @throws Exception
     */
    @Test
    public void testEtagInResponse() throws Exception {
        MultipartFileSender multipartFileSender = MultipartFileSender
                .fromInputStream(is)
                .with(requestWrapper)
                .with(responseWrapper)
                .withFileName(fileName)
                .withChecksum(checksum)
                .withMimetype(mimeType)
                .withLength(length);


        when(request.getHeader(eq("Range"))).thenReturn("bytes=1-3");

        multipartFileSender.serveResource();

        String etag = responseWrapper.getHeader("Etag");

        assertEquals(checksum, etag);
    }

    //Check that a head request doesn't return any body, but returns the headers
    @Test
    public void testHeadRequest() throws Exception {
        MultipartFileSender multipartFileSender = MultipartFileSender
                .fromInputStream(is)
                .with(requestWrapper)
                .with(responseWrapper)
                .withFileName(fileName)
                .withChecksum(checksum)
                .withMimetype(mimeType)
                .withLength(length);


        when(request.getMethod()).thenReturn("HEAD");

        multipartFileSender.serveResource();

        String content = new String(responseWrapper.getContentAsByteArray(), CharEncoding.UTF_8);

        assertEquals("bytes", responseWrapper.getHeader("Accept-Ranges"));
        assertEquals(checksum, responseWrapper.getHeader("ETag"));
        assertEquals("", content);
        assertEquals(200, responseWrapper.getStatusCode());

    }

    /**
     * If ETAG is equal to that of the requested Resource then this should return 304
     *
     * @throws Exception
     */
    @Test
    public void testIfNoneMatchFail() throws Exception {
        MultipartFileSender multipartFileSender = MultipartFileSender
                .fromInputStream(is)
                .with(requestWrapper)
                .with(responseWrapper)
                .withFileName(fileName)
                .withChecksum(checksum)
                .withMimetype(mimeType)
                .withLength(length);

        when(request.getHeader(eq("If-None-Match"))).thenReturn(checksum);

        multipartFileSender.isValid();

        assertEquals(HttpServletResponse.SC_NOT_MODIFIED, responseWrapper.getStatusCode());
    }

    /**
     * Happy path of If-None-Match header
     * @throws Exception
     */
    @Test
    public void testIfNoneMatchPass() throws Exception {
        MultipartFileSender multipartFileSender = MultipartFileSender
                .fromInputStream(is)
                .with(requestWrapper)
                .with(responseWrapper)
                .withFileName(fileName)
                .withChecksum(checksum)
                .withMimetype(mimeType)
                .withLength(length);


        when(request.getHeader(eq("If-None-Match"))).thenReturn("pretendthisisarandomchecksumnotequaltotherequestedbitstream");

        multipartFileSender.isValid();
        multipartFileSender.serveResource();

        assertEquals(HttpServletResponse.SC_OK, responseWrapper.getStatusCode());
    }

    /**
     * If the bitstream has no filename this should throw an internal server error
     * @throws Exception
     */
    @Test
    public void testNoFileName() throws Exception {
        MultipartFileSender multipartFileSender = MultipartFileSender
                .fromInputStream(is)
                .with(requestWrapper)
                .with(responseWrapper)
                .withChecksum(checksum)
                .withMimetype(mimeType)
                .withLength(length);


        multipartFileSender.isValid();


        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseWrapper.getStatusCode());
    }

    /**
     * Test if the Modified Since precondition works, should return 304 if it hasn't been modified
     * @throws Exception
     */
    @Test
    public void testIfModifiedSinceNotModifiedSince() throws Exception {
        Long time = new Date().getTime();
        MultipartFileSender multipartFileSender = MultipartFileSender
                .fromInputStream(is)
                .with(requestWrapper)
                .withFileName(fileName)
                .with(responseWrapper)
                .withChecksum(checksum)
                .withMimetype(mimeType)
                .withLength(length)
                .withLastModified(time);

        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(time + 100000);
        when(request.getDateHeader(eq("If-Unmodified-Since"))).thenReturn(-1L);

        multipartFileSender.isValid();

        assertEquals(HttpServletResponse.SC_NOT_MODIFIED, responseWrapper.getStatusCode());


    }

    /**
     * Happy path for modified since
     * @throws Exception
     */
    @Test
    public void testIfModifiedSinceModifiedSince() throws Exception {
        Long time = new Date().getTime();
        MultipartFileSender multipartFileSender = MultipartFileSender
                .fromInputStream(is)
                .with(requestWrapper)
                .withFileName(fileName)
                .with(responseWrapper)
                .withChecksum(checksum)
                .withMimetype(mimeType)
                .withLength(length)
                .withLastModified(time);

        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(time - 100000);
        when(request.getDateHeader(eq("If-Unmodified-Since"))).thenReturn(-1L);

        multipartFileSender.isValid();
        multipartFileSender.serveResource();

        assertEquals(HttpServletResponse.SC_OK, responseWrapper.getStatusCode());

    }

    /**
     * If the If-Match doesn't match the ETAG then return 416 Status code
     * @throws Exception
     */
    @Test
    public void testIfMatchNoMatch() throws Exception {
        Long time = new Date().getTime();
        MultipartFileSender multipartFileSender = MultipartFileSender
                .fromInputStream(is)
                .with(requestWrapper)
                .withFileName(fileName)
                .with(responseWrapper)
                .withChecksum(checksum)
                .withMimetype(mimeType)
                .withLength(length)
                .withLastModified(time);

        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(-1L);
        when(request.getDateHeader(eq("If-Unmodified-Since"))).thenReturn(-1L);
        when(request.getHeader(eq("If-Match"))).thenReturn("None-Matching-ETAG");

        multipartFileSender.isValid();

        assertEquals(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, responseWrapper.getStatusCode());
    }

    /**
     * If matches then just return resource
     * @throws Exception
     */
    @Test
    public void testIfMatchMatch() throws Exception {
        Long time = new Date().getTime();
        MultipartFileSender multipartFileSender = MultipartFileSender
                .fromInputStream(is)
                .with(requestWrapper)
                .withFileName(fileName)
                .with(responseWrapper)
                .withChecksum(checksum)
                .withMimetype(mimeType)
                .withLength(length)
                .withLastModified(time);

        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(-1L);
        when(request.getDateHeader(eq("If-Unmodified-Since"))).thenReturn(-1L);
        when(request.getHeader(eq("If-Match"))).thenReturn(checksum);

        multipartFileSender.isValid();

        assertEquals(HttpServletResponse.SC_OK, responseWrapper.getStatusCode());
    }

    /**
     * If not modified since given date then return resource
     * @throws Exception
     */
    @Test
    public void testIfUnmodifiedSinceNotModifiedSince() throws Exception {
        Long time = new Date().getTime();
        MultipartFileSender multipartFileSender = MultipartFileSender
                .fromInputStream(is)
                .with(requestWrapper)
                .withFileName(fileName)
                .with(responseWrapper)
                .withChecksum(checksum)
                .withMimetype(mimeType)
                .withLength(length)
                .withLastModified(time);

        when(request.getDateHeader(eq("If-Unmodified-Since"))).thenReturn(time + 100000);
        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(-1L);

        multipartFileSender.isValid();

        assertEquals(HttpServletResponse.SC_OK, responseWrapper.getStatusCode());

    }

    /**
     * If modified since given date then return 412
     * @throws Exception
     */
    @Test
    public void testIfUnmodifiedSinceModifiedSince() throws Exception {
        Long time = new Date().getTime();
        MultipartFileSender multipartFileSender = MultipartFileSender
                .fromInputStream(is)
                .with(requestWrapper)
                .withFileName(fileName)
                .with(responseWrapper)
                .withChecksum(checksum)
                .withMimetype(mimeType)
                .withLength(length)
                .withLastModified(time);

        when(request.getDateHeader(eq("If-Unmodified-Since"))).thenReturn(time - 100000);
        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(-1L);

        multipartFileSender.isValid();

        assertEquals(HttpServletResponse.SC_PRECONDITION_FAILED, responseWrapper.getStatusCode());

    }








}