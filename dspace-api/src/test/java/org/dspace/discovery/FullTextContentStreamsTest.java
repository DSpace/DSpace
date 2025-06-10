/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
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


    @BeforeEach
    public void setUp() throws Exception {
        when(item.getHandle()).thenReturn(HANDLE);
        when(originalBundle.getName()).thenReturn("ORIGINAL");
        when(textBundle.getName()).thenReturn("TEXT");

        when(textBitstream1.getName()).thenReturn("Full Text 1");
        when(textBitstream2.getName()).thenReturn("Full Text 2");
        when(textBitstream3.getName()).thenReturn("Full Text 3");

        when(textBitstream1.getSizeBytes()).thenReturn(1L);
        when(textBitstream2.getSizeBytes()).thenReturn(2L);
        when(textBitstream3.getSizeBytes()).thenReturn(3L);

        when(bitstreamService.retrieve(null, textBitstream1))
            .thenReturn(new ByteArrayInputStream("This is text 1".getBytes(StandardCharsets.UTF_8)));
        when(bitstreamService.retrieve(null, textBitstream2))
            .thenReturn(new ByteArrayInputStream("This is text 2".getBytes(StandardCharsets.UTF_8)));
        when(bitstreamService.retrieve(null, textBitstream3))
            .thenReturn(new ByteArrayInputStream("This is text 3".getBytes(StandardCharsets.UTF_8)));

        streams.bitstreamService = bitstreamService;
    }

    @Test
    public void testItemWithNoBundles() throws Exception {
        when(item.getBundles()).thenReturn(null);

        streams.init(item);

        assertEquals(HANDLE, streams.getSourceInfo(), "Source info should give you the handle");
        assertEquals(CONTENT_TYPE, streams.getContentType(), "Content type should be plain text");
        assertEquals("", streams.getName(), "The name should be empty");
        assertEquals((Long) 0L, streams.getSize(), "The size of the streams should be zero");
        assertTrue(streams.isEmpty(), "Content stream should be empty");
        InputStream inputStream = streams.getStream();
        assertNotNull(inputStream);
        assertEquals(-1, inputStream.read(), "Input stream should be empty");
    }

    @Test
    public void testItemWithOnlyOriginalBundle() throws Exception {
        when(item.getBundles()).thenReturn(Arrays.asList(originalBundle));

        streams.init(item);

        assertEquals(HANDLE, streams.getSourceInfo(), "Source info should give you the handle");
        assertEquals(CONTENT_TYPE, streams.getContentType(), "Content type should be plain text");
        assertEquals("", streams.getName(), "The name should be empty");
        assertEquals((Long) 0L, streams.getSize(), "The size of the streams should be zero");
        assertTrue(streams.isEmpty(), "Content stream should be empty");
        InputStream inputStream = streams.getStream();
        assertNotNull(inputStream);
        assertEquals(-1, inputStream.read(), "Input stream should be empty");
    }

    @Test
    public void testItemWithEmptyTextBundle() throws Exception {
        when(item.getBundles()).thenReturn(Arrays.asList(originalBundle, textBundle));
        when(textBundle.getBitstreams()).thenReturn(null);

        streams.init(item);

        assertEquals(HANDLE, streams.getSourceInfo(), "Source info should give you the handle");
        assertEquals(CONTENT_TYPE, streams.getContentType(), "Content type should be plain text");
        assertEquals("", streams.getName(), "The name should be empty");
        assertEquals((Long) 0L, streams.getSize(), "The size of the streams should be zero");
        assertTrue(streams.isEmpty(), "Content stream should be empty");
        InputStream inputStream = streams.getStream();
        assertNotNull(inputStream);
        assertEquals(-1, inputStream.read(), "Input stream should be empty");
    }

    @Test
    public void testItemWithOnlyOneTextBitstream() throws Exception {
        when(item.getBundles()).thenReturn(Arrays.asList(originalBundle, textBundle));
        when(textBundle.getBitstreams()).thenReturn(Arrays.asList(textBitstream1));

        streams.init(item);

        assertEquals(HANDLE, streams.getSourceInfo(), "Source info should give you the handle");
        assertEquals(CONTENT_TYPE, streams.getContentType(), "Content type should be plain text");
        assertEquals("Full Text 1", streams.getName(), "The name should match the name of the bitstream");
        assertEquals((Long) 1L, streams.getSize(), "The size of the streams should match the size of bitstream 1");
        assertFalse(streams.isEmpty(), "Content stream should not be empty");
        InputStream inputStream = streams.getStream();
        assertNotNull(inputStream);
        assertEquals("\nThis is text 1",
            IOUtils.toString(inputStream, StandardCharsets.UTF_8),
            "The data in the input stream should match the text of the bitstream");
    }

    @Test
    public void testItemWithMultipleTextBitstreams() throws Exception {
        when(item.getBundles()).thenReturn(Arrays.asList(originalBundle, textBundle));
        when(textBundle.getBitstreams()).thenReturn(Arrays.asList(textBitstream1, textBitstream2, textBitstream3));

        streams.init(item);

        assertEquals(HANDLE, streams.getSourceInfo(), "Source info should give you the handle");
        assertEquals(CONTENT_TYPE, streams.getContentType(), "Content type should be plain text");
        assertEquals("Full Text 1;Full Text 2;Full Text 3", streams.getName(), "The name should match the concatenation of the names of the bitstreams");
        assertEquals((Long) 6L, streams.getSize(), "The size of the streams should be the sum of the bitstream sizes");
        assertFalse(streams.isEmpty(), "Content stream should not be empty");
        InputStream inputStream = streams.getStream();
        assertNotNull(inputStream);
        assertEquals("\nThis is text 1" +
            "\nThis is text 2\nThis is text 3", IOUtils.toString(inputStream, StandardCharsets.UTF_8), "The data in the input stream should match 'This is text 1'");
    }

    @Test
    public void testBitstreamThrowingExceptionShouldNotStopIndexing() throws Exception {
        when(item.getBundles()).thenReturn(Arrays.asList(originalBundle, textBundle));
        when(textBundle.getBitstreams()).thenReturn(Arrays.asList(textBitstream1, textBitstream2, textBitstream3));
        when(bitstreamService.retrieve(null, textBitstream2)).thenThrow(new IOException("NOTFOUND"));

        streams.init(item);

        assertEquals(HANDLE, streams.getSourceInfo(), "Source info should give you the handle");
        assertEquals(CONTENT_TYPE, streams.getContentType(), "Content type should be plain text");
        assertEquals("Full Text 1;Full Text 2;Full Text 3", streams.getName(), "The name should match the concatenation of the names of the bitstreams");
        assertEquals((Long) 6L, streams.getSize(), "The size of the streams should be the sum of the bitstream sizes");
        assertFalse(streams.isEmpty(), "Content stream should not be empty");
        InputStream inputStream = streams.getStream();
        assertNotNull(inputStream);
        String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        assertTrue(content.contains("This is text 1"),
            "The data should contain data of the first bitstream that is not corrupt");
        assertFalse(content.contains("This is text 2"),
            "The data should NOT contain data of the second bitstream that is corrupt");
        assertTrue(content.contains("This is text 3"),
            "The data should contain data of the third bitstream that is not corrupt");
        assertTrue(content.contains("java.io.IOException"),
            "The data should contain data on the exception that occurred");
        assertTrue(content.contains("NOTFOUND"),
            "The data should contain data on the exception that occurred");
    }

}
