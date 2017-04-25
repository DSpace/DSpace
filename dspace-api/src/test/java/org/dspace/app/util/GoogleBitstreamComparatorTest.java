package org.dspace.app.util;

import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by frederic on 24/04/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class GoogleBitstreamComparatorTest {

    @Mock
    private Bundle bundle;

    @Mock
    private Bitstream bitstream1;

    @Mock
    private Bitstream bitstream2;

    @Mock
    private Bitstream bitstream3;

    @Mock
    private BitstreamFormat bitstreamFormat1;

    @Mock
    private BitstreamFormat bitstreamFormat2;

    @Mock
    private BitstreamFormat bitstreamFormat3;


    private HashMap<String, String> settings = new HashMap<>();



    @Before
    public void setUp() throws Exception {
        when(bitstream1.getName()).thenReturn("bitstream1");
        when(bitstream2.getName()).thenReturn("bitstream2");
        when(bitstream3.getName()).thenReturn("bitstream3");
        settings.put("citation.prioritized_types", "PDF, WORD, RTF, PS");
        settings.put("citation.mimetypes.PDF" , "application/pdf");
        settings.put("citation.mimetypes.WORD", "application/msword, application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        settings.put("citation.mimetypes.RTF", "text/richtext");
        settings.put("citation.mimetypes.PS", "application/x-photoshop");
        when(bundle.getBitstreams()).thenReturn(new Bitstream[] {bitstream1, bitstream2, bitstream3});
        when(bitstream1.getFormat()).thenReturn(bitstreamFormat1);
        when(bitstream2.getFormat()).thenReturn(bitstreamFormat2);
        when(bitstream3.getFormat()).thenReturn(bitstreamFormat3);
    }

    @Test
    public void testPDFDifferentSize() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("application/pdf");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/pdf");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/pdf");
        when(bitstream1.getSize()).thenReturn(new Long(100));
        when(bitstream2.getSize()).thenReturn(new Long(200));
        when(bitstream3.getSize()).thenReturn(new Long(300));

        List<Bitstream> toSort = Arrays.asList(bundle.getBitstreams());
        Collections.sort(toSort, new GoogleBitstreamComparator(settings));
        assertEquals(toSort.get(0).getName(), "bitstream3");
        assertEquals(toSort.get(1).getName(), "bitstream2");
        assertEquals(toSort.get(2).getName(), "bitstream1");
    }

    @Test
    public void testDifferentMimeTypes() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/msword");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/x-photoshop");

        List<Bitstream> toSort = Arrays.asList(bundle.getBitstreams());
        Collections.sort(toSort, new GoogleBitstreamComparator(settings));
        assertEquals("WORD should be first", toSort.get(0).getName(), "bitstream2");
        assertEquals("RTF second", toSort.get(1).getName(), "bitstream1");
        assertEquals("PS next",toSort.get(2).getName(), "bitstream3");
    }

    @Test
    public void testDifferentMimeTypesDifferentSizes() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat2.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/x-photoshop");
        when(bitstream1.getSize()).thenReturn(new Long(100));
        when(bitstream2.getSize()).thenReturn(new Long(200));
        when(bitstream3.getSize()).thenReturn(new Long(300));

        List<Bitstream> toSort = Arrays.asList(bundle.getBitstreams());
        Collections.sort(toSort, new GoogleBitstreamComparator(settings));
        assertEquals(toSort.get(0).getName(), "bitstream2");
        assertEquals(toSort.get(1).getName(), "bitstream1");
        assertEquals(toSort.get(2).getName(), "bitstream3");
    }

    @Test
    public void testSameMimeTypeSameSize() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("application/pdf");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/pdf");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/pdf");
        when(bitstream1.getSize()).thenReturn(new Long(200));
        when(bitstream2.getSize()).thenReturn(new Long(200));
        when(bitstream3.getSize()).thenReturn(new Long(200));

        List<Bitstream> toSort = Arrays.asList(bundle.getBitstreams());
        Collections.sort(toSort, new GoogleBitstreamComparator(settings));
        assertEquals("Same size and mimetype, so just pick the first one", toSort.get(0).getName(), "bitstream1");
        assertEquals(toSort.get(1).getName(), "bitstream2");
        assertEquals(toSort.get(2).getName(), "bitstream3");
    }

    @Test
    public void testUnknownMimeType() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("unknown");
        when(bitstreamFormat2.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat3.getMIMEType()).thenReturn("text/richtext");
        when(bitstream1.getSize()).thenReturn(new Long(100));
        when(bitstream2.getSize()).thenReturn(new Long(200));
        when(bitstream3.getSize()).thenReturn(new Long(300));

        List<Bitstream> toSort = Arrays.asList(bundle.getBitstreams());
        Collections.sort(toSort, new GoogleBitstreamComparator(settings));
        assertEquals(toSort.get(0).getName(), "bitstream3");
        assertEquals(toSort.get(1).getName(), "bitstream2");
        assertEquals(toSort.get(2).getName(), "bitstream1");
    }

    @Test
    public void testAllUnknown() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("unknown");
        when(bitstreamFormat2.getMIMEType()).thenReturn("unknown");
        when(bitstreamFormat3.getMIMEType()).thenReturn("unknown");
        when(bitstream1.getSize()).thenReturn(new Long(200));
        when(bitstream2.getSize()).thenReturn(new Long(300));
        when(bitstream3.getSize()).thenReturn(new Long(100));

        List<Bitstream> toSort = Arrays.asList(bundle.getBitstreams());
        Collections.sort(toSort, new GoogleBitstreamComparator(settings));
        assertEquals(toSort.get(0).getName(), "bitstream2");
        assertEquals(toSort.get(1).getName(), "bitstream1");
        assertEquals(toSort.get(2).getName(), "bitstream3");
    }

    @Test
    public void testChangePriority() {
        settings.put("citation.prioritized_types", "PS, RTF, WORD, PDF");
        when(bitstreamFormat1.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/msword");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/x-photoshop");

        List<Bitstream> toSort = Arrays.asList(bundle.getBitstreams());
        Collections.sort(toSort, new GoogleBitstreamComparator(settings));
        assertEquals("PS should be first", toSort.get(0).getName(), "bitstream3");
        assertEquals("RTF second", toSort.get(1).getName(), "bitstream1");
        assertEquals("Word third",toSort.get(2).getName(), "bitstream2");
    }







}