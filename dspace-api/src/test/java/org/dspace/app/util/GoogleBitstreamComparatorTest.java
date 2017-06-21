/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import org.dspace.AbstractUnitTest;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.core.Context;
import org.junit.After;
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
import static org.mockito.Matchers.*;


@RunWith(MockitoJUnitRunner.class)
public class GoogleBitstreamComparatorTest extends AbstractUnitTest{

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


    /**
     * Create a bundle with three bitstreams
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        when(bitstream1.getName()).thenReturn("bitstream1");
        when(bitstream2.getName()).thenReturn("bitstream2");
        when(bitstream3.getName()).thenReturn("bitstream3");
        settings.put("citation.prioritized_types", "Adobe PDF, Microsoft Word, RTF, Photoshop");
        List<Bitstream> bitstreams = new ArrayList<>();
        bitstreams.add(bitstream1);
        bitstreams.add(bitstream2);
        bitstreams.add(bitstream3);
        when(bundle.getBitstreams()).thenReturn(bitstreams);
        when(bitstream1.getFormat(any(Context.class))).thenReturn(bitstreamFormat1);
        when(bitstream2.getFormat(any(Context.class))).thenReturn(bitstreamFormat2);
        when(bitstream3.getFormat(any(Context.class))).thenReturn(bitstreamFormat3);
    }

    /**
     * Create three pdf bitstreams and give them a different size, the largest one should come first
     * @throws Exception
     */
    @Test
    public void testPDFDifferentSize() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("application/pdf");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/pdf");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/pdf");
        when(bitstream1.getSize()).thenReturn(new Long(100));
        when(bitstream2.getSize()).thenReturn(new Long(200));
        when(bitstream3.getSize()).thenReturn(new Long(300));

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, new GoogleBitstreamComparator(context, settings));
        assertEquals("bitstream3", toSort.get(0).getName());
        assertEquals("bitstream2", toSort.get(1).getName());
        assertEquals("bitstream1", toSort.get(2).getName());
    }

    /**
     * Create three bitstreams with different mimetypes, order is defined in settings
     * @throws Exception
     */
    @Test
    public void testDifferentMimeTypes() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/msword");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/x-photoshop");

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, new GoogleBitstreamComparator(context, settings));
        assertEquals("WORD should be first",  "bitstream2", toSort.get(0).getName());
        assertEquals("RTF second", "bitstream1", toSort.get(1).getName());
        assertEquals("PS next", "bitstream3", toSort.get(2).getName());
    }

    /**
     * Test for two bitstreams with same mimetype, but different size.
     * Should be first ordered by mimetype and then by size (largest first)
     * @throws Exception
     */
    @Test
    public void testMimeTypesDifferentSizes() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat2.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/x-photoshop");
        when(bitstream1.getSize()).thenReturn(new Long(100));
        when(bitstream2.getSize()).thenReturn(new Long(200));
        when(bitstream3.getSize()).thenReturn(new Long(300));

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, new GoogleBitstreamComparator(context, settings));
        assertEquals("bitstream2", toSort.get(0).getName());
        assertEquals("bitstream1", toSort.get(1).getName());
        assertEquals("bitstream3", toSort.get(2).getName());
    }

    /**
     * Test for same Mimetype and same Size, if this is the case, then ordering is just as it was before sorting
     * @throws Exception
     */
    @Test
    public void testSameMimeTypeSameSize() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("application/pdf");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/pdf");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/pdf");
        when(bitstream1.getSize()).thenReturn(new Long(200));
        when(bitstream2.getSize()).thenReturn(new Long(200));
        when(bitstream3.getSize()).thenReturn(new Long(200));

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, new GoogleBitstreamComparator(context, settings));
        assertEquals("Same size and mimetype, so just pick the first one", "bitstream1", toSort.get(0).getName());
        assertEquals("bitstream2", toSort.get(1).getName());
        assertEquals("bitstream3", toSort.get(2).getName());
    }

    /**
     * Test if sorting still works when there is an undefined Mimetype, undefined mimetypes have the lowest priority
     * @throws Exception
     */
    @Test
    public void testUnknownMimeType() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("unknown");
        when(bitstreamFormat2.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat3.getMIMEType()).thenReturn("text/richtext");
        when(bitstream1.getSize()).thenReturn(new Long(400));
        when(bitstream2.getSize()).thenReturn(new Long(200));
        when(bitstream3.getSize()).thenReturn(new Long(300));

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, new GoogleBitstreamComparator(context, settings));
        assertEquals("bitstream3", toSort.get(0).getName());
        assertEquals("bitstream2", toSort.get(1).getName());
        assertEquals("bitstream1",toSort.get(2).getName());
    }

    /**
     * Test with all unknown mimetypes, ordered by size if this is the case
     * @throws Exception
     */
    @Test
    public void testAllUnknown() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("unknown");
        when(bitstreamFormat2.getMIMEType()).thenReturn("unknown");
        when(bitstreamFormat3.getMIMEType()).thenReturn("unknown");
        when(bitstream1.getSize()).thenReturn(new Long(200));
        when(bitstream2.getSize()).thenReturn(new Long(300));
        when(bitstream3.getSize()).thenReturn(new Long(100));

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, new GoogleBitstreamComparator(context, settings));
        assertEquals("bitstream2", toSort.get(0).getName());
        assertEquals("bitstream1", toSort.get(1).getName());
        assertEquals("bitstream3", toSort.get(2).getName());
    }

    /**
     * Test to see if priority is configurable, order should change according to prioritized_types property
     */
    @Test
    public void testChangePriority() throws Exception{
        settings.put("citation.prioritized_types", "Photoshop, RTF, Microsoft Word, Adobe PDF");
        when(bitstreamFormat1.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/msword");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/x-photoshop");

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, new GoogleBitstreamComparator(context, settings));
        assertEquals("PS should be first", "bitstream3", toSort.get(0).getName());
        assertEquals("RTF second", "bitstream1", toSort.get(1).getName());
        assertEquals("Word third", "bitstream2", toSort.get(2).getName());
    }

    /**
     * Test what happens when bitstreams have no mimetype, should be just ordered by size
     */
    @Test
    public void testNoMimeType() throws Exception{
        when(bitstream1.getSize()).thenReturn(new Long(200));
        when(bitstream2.getSize()).thenReturn(new Long(300));
        when(bitstream3.getSize()).thenReturn(new Long(100));

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, new GoogleBitstreamComparator(context, settings));
        assertEquals("bitstream2", toSort.get(0).getName());
        assertEquals("bitstream1", toSort.get(1).getName());
        assertEquals("bitstream3", toSort.get(2).getName());
    }

    /**
     * Three bitstreams without a size, just the same ordering as given
     * @throws Exception
     */
    @Test
    public void testNoSize() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("unknown");
        when(bitstreamFormat2.getMIMEType()).thenReturn("unknown");
        when(bitstreamFormat3.getMIMEType()).thenReturn("unknown");

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, new GoogleBitstreamComparator(context, settings));
        assertEquals("bitstream1", toSort.get(0).getName());
        assertEquals("bitstream2", toSort.get(1).getName());
        assertEquals("bitstream3", toSort.get(2).getName());
    }

    /**
     * Make sure that it doesn't crash when bitstreams have no size or mimetype
     */
    @Test
    public void testNoMimeTypeNoSize() throws Exception {
        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, new GoogleBitstreamComparator(context, settings));
        assertEquals("bitstream1", toSort.get(0).getName());
        assertEquals("bitstream2", toSort.get(1).getName());
        assertEquals("bitstream3", toSort.get(2).getName());
    }

    /**
     * Make sure it still works when the citation.prioritized_types property is empty
     */
    @Test
    public void testNoPrioritizedTypes() throws Exception {
        settings.put("citation.prioritized_types", "");
        when(bitstreamFormat1.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/msword");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/x-photoshop");
        when(bitstream1.getSize()).thenReturn(new Long(100));
        when(bitstream2.getSize()).thenReturn(new Long(200));
        when(bitstream3.getSize()).thenReturn(new Long(300));

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, new GoogleBitstreamComparator(context, settings));
        assertEquals("PS should be first", "bitstream3", toSort.get(0).getName());
        assertEquals("RTF second", "bitstream2", toSort.get(1).getName());
        assertEquals("Word third", "bitstream1", toSort.get(2).getName());
    }

    /**
     * Test to check for no crash when nothing is configured, just checks for size
     */
    @Test
    public void testNoConfig() throws Exception{
        settings.remove("citation.prioritized_types");
        when(bitstreamFormat1.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/msword");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/x-photoshop");
        when(bitstream1.getSize()).thenReturn(new Long(100));
        when(bitstream2.getSize()).thenReturn(new Long(200));
        when(bitstream3.getSize()).thenReturn(new Long(300));

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, new GoogleBitstreamComparator(context, settings));
        assertEquals("bitstream3", toSort.get(0).getName());
        assertEquals("bitstream2", toSort.get(1).getName());
        assertEquals("bitstream1", toSort.get(2).getName());
    }

    /**
     * Test to see what happens when you choose a short description that's not defined in bitstream-formats.xml
     */
    @Test
    public void testUndefinedShortDescription() {
        settings.put("citation.prioritized_types", "Photoshop, RTF, Undefined Type, Microsoft Word, Adobe PDF");
        when(bitstreamFormat1.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/msword");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/x-photoshop");
        when(bitstream1.getSize()).thenReturn(new Long(100));
        when(bitstream2.getSize()).thenReturn(new Long(200));
        when(bitstream3.getSize()).thenReturn(new Long(300));

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, new GoogleBitstreamComparator(context, settings));
        assertEquals("bitstream3", toSort.get(0).getName());
        assertEquals("bitstream1", toSort.get(1).getName());
        assertEquals("bitstream2", toSort.get(2).getName());
    }

    /**
     * Test adding a new format in the priority list
     */
    @Test
    public void testAddingNewFormat() {
        settings.put("citation.prioritized_types", "WAV, Adobe PDF, Microsoft Word, RTF, Photoshop");
        when(bitstreamFormat1.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/msword");
        when(bitstreamFormat3.getMIMEType()).thenReturn("audio/x-wav");
        when(bitstream1.getSize()).thenReturn(new Long(100));
        when(bitstream2.getSize()).thenReturn(new Long(200));
        when(bitstream3.getSize()).thenReturn(new Long(300));

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, new GoogleBitstreamComparator(context, settings));
        assertEquals("bitstream3", toSort.get(0).getName());
        assertEquals("bitstream2", toSort.get(1).getName());
        assertEquals("bitstream1", toSort.get(2).getName());
    }


    @After
    public void destroy()
    {
        settings = null;
    }








}