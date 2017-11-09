package org.dspace.app.rest.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.log4j.Logger;
import org.dspace.app.rest.test.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MultipartFileSenderTest extends AbstractIntegrationTestWithDatabase {


    /** log4j category */
    private static final Logger log = Logger.getLogger(MultipartFileSenderTest.class);

    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    /**
     * BitStream instance for the tests
     */
    private Bitstream bs;

    private InputStream is;

    private HttpServletRequest request;

    private HttpServletResponse response;

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    public void init() throws AuthorizeException {
        try
        {
            //we have to create a new bitstream in the database

            this.bs = bitstreamService.create(context, IOUtils.toInputStream("0123456789", CharEncoding.UTF_8));
            this.bs.setName(context, "Test Item");
            this.is = bitstreamService.retrieve(context, bs);
            this.request = mock(HttpServletRequest.class);
            this.response = mock(HttpServletResponse.class);
        }
        catch (IOException ex) {
            log.error("IO Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        }
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    @Override
    public void destroy()
    {
        bs = null;
        super.destroy();
    }



    @Test
    public void testEtagInResponse() throws Exception {
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        MultipartFileSender multipartFileSender = MultipartFileSender.fromInputStream(is).with(requestWrapper).with(responseWrapper).withFileName(bs.getName()).withChecksum(bs.getChecksum()).withMimetype("text/plain");

        when(request.getDateHeader(eq("If-Unmodified-Since"))).thenReturn(-1L);
        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(-1L);
        when(request.getHeader(eq("Range"))).thenReturn("bytes=1-3");

        multipartFileSender.serveResource();

        String content = new String(responseWrapper.getContentAsByteArray(), CharEncoding.UTF_8);

        assertEquals("123", content);


    }

}