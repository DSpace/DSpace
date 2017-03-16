/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FullTextContentStreamsTest {

    private static final String HANDLE = "1234567/123";
    private static final String CONTENT_TYPE = "text/plain";

    @InjectMocks
    private FullTextContentStreams streams;

    @Mock
    private BitstreamService bitstreamService;

    @Mock
    private Item item;

    @Mock
    private Bundle originalBundle;

    @Mock
    private Bundle textBundle;

    @Mock
    private Bitstream textBitstream1;

    @Mock
    private Bitstream textBitstream2;

    @Mock
    private Bitstream textBitstream3;


    @Before
    public void setUp() throws Exception {
        when(item.getHandle()).thenReturn(HANDLE);
        when(originalBundle.getName()).thenReturn("ORIGINAL");
        when(textBundle.getName()).thenReturn("TEXT");

        when(textBitstream1.getName()).thenReturn("Full Text 1");
        when(textBitstream2.getName()).thenReturn("Full Text 2");
        when(textBitstream3.getName()).thenReturn("Full Text 3");

        when(textBitstream1.getSize()).thenReturn(1L);
        when(textBitstream2.getSize()).thenReturn(2L);
        when(textBitstream3.getSize()).thenReturn(3L);

        when(bitstreamService.retrieve(null, textBitstream1)).thenReturn(new ByteArrayInputStream("This is text 1".getBytes(Charsets.UTF_8)));
        when(bitstreamService.retrieve(null, textBitstream2)).thenReturn(new ByteArrayInputStream("This is text 2".getBytes(Charsets.UTF_8)));
        when(bitstreamService.retrieve(null, textBitstream3)).thenReturn(new ByteArrayInputStream("This is text 3".getBytes(Charsets.UTF_8)));

        streams.bitstreamService = bitstreamService;
    }

    @Test
    public void testItemWithNoBundles() throws Exception {
        when(item.getBundles()).thenReturn(null);

        streams.init(item);

        assertEquals("Source info should give you the handle", HANDLE, streams.getSourceInfo());
        assertEquals("Content type should be plain text", CONTENT_TYPE, streams.getContentType());
        assertEquals("The name should be empty", "", streams.getName());
        assertEquals("The size of the streams should be zero", (Long) 0L, streams.getSize());
        assertTrue("Content stream should be empty", streams.isEmpty());
        InputStream inputStream = streams.getStream();
        assertNotNull(inputStream);
        assertEquals("Input stream should be empty", -1, inputStream.read());
    }

    @Test
    public void testItemWithOnlyOriginalBundle() throws Exception {
        when(item.getBundles()).thenReturn(Arrays.asList(originalBundle));

        streams.init(item);

        assertEquals("Source info should give you the handle", HANDLE, streams.getSourceInfo());
        assertEquals("Content type should be plain text", CONTENT_TYPE, streams.getContentType());
        assertEquals("The name should be empty", "", streams.getName());
        assertEquals("The size of the streams should be zero", (Long) 0L, streams.getSize());
        assertTrue("Content stream should be empty", streams.isEmpty());
        InputStream inputStream = streams.getStream();
        assertNotNull(inputStream);
        assertEquals("Input stream should be empty", -1, inputStream.read());
    }

    @Test
    public void testItemWithEmptyTextBundle() throws Exception {
        when(item.getBundles()).thenReturn(Arrays.asList(originalBundle, textBundle));
        when(textBundle.getBitstreams()).thenReturn(null);

        streams.init(item);

        assertEquals("Source info should give you the handle", HANDLE, streams.getSourceInfo());
        assertEquals("Content type should be plain text", CONTENT_TYPE, streams.getContentType());
        assertEquals("The name should be empty", "", streams.getName());
        assertEquals("The size of the streams should be zero", (Long) 0L, streams.getSize());
        assertTrue("Content stream should be empty", streams.isEmpty());
        InputStream inputStream = streams.getStream();
        assertNotNull(inputStream);
        assertEquals("Input stream should be empty", -1, inputStream.read());
    }

    @Test
    public void testItemWithOnlyOneTextBitstream() throws Exception {
        when(item.getBundles()).thenReturn(Arrays.asList(originalBundle, textBundle));
        when(textBundle.getBitstreams()).thenReturn(Arrays.asList(textBitstream1));

        streams.init(item);

        assertEquals("Source info should give you the handle", HANDLE, streams.getSourceInfo());
        assertEquals("Content type should be plain text", CONTENT_TYPE, streams.getContentType());
        assertEquals("The name should match the name of the bitstream", "Full Text 1", streams.getName());
        assertEquals("The size of the streams should match the size of bitstream 1", (Long) 1L, streams.getSize());
        assertFalse("Content stream should not be empty", streams.isEmpty());
        InputStream inputStream = streams.getStream();
        assertNotNull(inputStream);
        assertEquals("The data in the input stream should match the text of the bitstream", "\nThis is text 1",
                IOUtils.toString(inputStream, Charsets.UTF_8));
    }

    @Test
    public void testItemWithMultipleTextBitstreams() throws Exception {
        when(item.getBundles()).thenReturn(Arrays.asList(originalBundle, textBundle));
        when(textBundle.getBitstreams()).thenReturn(Arrays.asList(textBitstream1, textBitstream2, textBitstream3));

        streams.init(item);

        assertEquals("Source info should give you the handle", HANDLE, streams.getSourceInfo());
        assertEquals("Content type should be plain text", CONTENT_TYPE, streams.getContentType());
        assertEquals("The name should match the concatenation of the names of the bitstreams",
                "Full Text 1;Full Text 2;Full Text 3", streams.getName());
        assertEquals("The size of the streams should be the sum of the bitstream sizes", (Long) 6L, streams.getSize());
        assertFalse("Content stream should not be empty", streams.isEmpty());
        InputStream inputStream = streams.getStream();
        assertNotNull(inputStream);
        assertEquals("The data in the input stream should match 'This is text 1'", "\nThis is text 1" +
                "\nThis is text 2\nThis is text 3", IOUtils.toString(inputStream, Charsets.UTF_8));
    }

    @Test
    public void testBitstreamThrowingExceptionShouldNotStopIndexing() throws Exception {
        when(item.getBundles()).thenReturn(Arrays.asList(originalBundle, textBundle));
        when(textBundle.getBitstreams()).thenReturn(Arrays.asList(textBitstream1, textBitstream2, textBitstream3));
        when(bitstreamService.retrieve(null, textBitstream2)).thenThrow(new IOException("NOTFOUND"));

        streams.init(item);

        assertEquals("Source info should give you the handle", HANDLE, streams.getSourceInfo());
        assertEquals("Content type should be plain text", CONTENT_TYPE, streams.getContentType());
        assertEquals("The name should match the concatenation of the names of the bitstreams",
                "Full Text 1;Full Text 2;Full Text 3", streams.getName());
        assertEquals("The size of the streams should be the sum of the bitstream sizes", (Long) 6L, streams.getSize());
        assertFalse("Content stream should not be empty", streams.isEmpty());
        InputStream inputStream = streams.getStream();
        assertNotNull(inputStream);
        String content = IOUtils.toString(inputStream, Charsets.UTF_8);
        assertTrue("The data should contain data of the first bitstream that is not corrupt",
                content.contains("This is text 1"));
        assertFalse("The data should NOT contain data of the second bitstream that is corrupt",
                content.contains("This is text 2"));
        assertTrue("The data should contain data of the third bistream that is not corrupt",
                content.contains("This is text 3"));
        assertTrue("The data should contain data on the exception that occurred",
                content.contains("java.io.IOException"));
        assertTrue("The data should contain data on the exception that occurred",
                content.contains("NOTFOUND"));
    }

}