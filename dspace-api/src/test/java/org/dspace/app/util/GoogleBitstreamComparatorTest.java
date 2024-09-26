/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.dspace.AbstractUnitTest;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.service.BitstreamService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class GoogleBitstreamComparatorTest extends AbstractUnitTest {
    @Mock
    private BitstreamService bitstreamService;

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
     *
     * @throws Exception
     */
    @Before
    @Override
    public void init() {
        super.init();
        when(bitstreamService.getName(bitstream1)).thenReturn("bitstream1");
        when(bitstreamService.getName(bitstream2)).thenReturn("bitstream2");
        when(bitstreamService.getName(bitstream3)).thenReturn("bitstream3");
        settings.put("citation.prioritized_types", "Adobe PDF, Microsoft Word, RTF, Postscript");
        List<Bitstream> bitstreams = new ArrayList<>();
        bitstreams.add(bitstream1);
        bitstreams.add(bitstream2);
        bitstreams.add(bitstream3);
        when(bundle.getBitstreams()).thenReturn(bitstreams);
        try {
            when(bitstreamService.getFormat(context, bitstream1)).thenReturn(bitstreamFormat1);
            when(bitstreamService.getFormat(context, bitstream2)).thenReturn(bitstreamFormat2);
            when(bitstreamService.getFormat(context, bitstream3)).thenReturn(bitstreamFormat3);
        } catch (SQLException ex) {
            //will not happen
        }
    }

    private GoogleBitstreamComparator getComparator() {
        GoogleBitstreamComparator comparator = new GoogleBitstreamComparator(context, settings);
        comparator.bitstreamService = bitstreamService;
        return comparator;
    }

    /**
     * Create three pdf bitstreams and give them a different size, the largest one should come first
     *
     * @throws Exception
     */
    @Test
    public void testPDFDifferentSize() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("application/pdf");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/pdf");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/pdf");
        when(bitstream1.getSizeBytes()).thenReturn(Long.valueOf(100));
        when(bitstream2.getSizeBytes()).thenReturn(Long.valueOf(200));
        when(bitstream3.getSizeBytes()).thenReturn(Long.valueOf(300));

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, getComparator());
        Bitstream bitstream5 = toSort.get(0);
        assertEquals("bitstream3", bitstreamService.getName(bitstream5));
        Bitstream bitstream4 = toSort.get(1);
        assertEquals("bitstream2", bitstreamService.getName(bitstream4));
        Bitstream bitstream = toSort.get(2);
        assertEquals("bitstream1", bitstreamService.getName(bitstream));
    }

    /**
     * Create three bitstreams with different mimetypes, order is defined in settings
     *
     * @throws Exception
     */
    @Test
    public void testDifferentMimeTypes() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/msword");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/postscript");

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, getComparator());
        Bitstream bitstream5 = toSort.get(0);
        assertEquals("WORD should be first as its type has the highest priority", "bitstream2",
                     bitstreamService.getName(bitstream5)
        );
        Bitstream bitstream4 = toSort.get(1);
        assertEquals("RTF should be second as its type priority is right after Word", "bitstream1",
                     bitstreamService.getName(bitstream4)
        );
        Bitstream bitstream = toSort.get(2);
        assertEquals("PS should be last as it has the lowest type priority", "bitstream3",
                     bitstreamService.getName(bitstream)
        );
    }

    /**
     * Test for two bitstreams with same mimetype, but different size.
     * Should be first ordered by mimetype and then by size (largest first)
     *
     * @throws Exception
     */
    @Test
    public void testMimeTypesDifferentSizes() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat2.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/postscript");
        when(bitstream1.getSizeBytes()).thenReturn(Long.valueOf(100));
        when(bitstream2.getSizeBytes()).thenReturn(Long.valueOf(200));

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, getComparator());
        Bitstream bitstream5 = toSort.get(0);
        assertEquals("bitstream2", bitstreamService.getName(bitstream5));
        Bitstream bitstream4 = toSort.get(1);
        assertEquals("bitstream1", bitstreamService.getName(bitstream4));
        Bitstream bitstream = toSort.get(2);
        assertEquals("bitstream3", bitstreamService.getName(bitstream));
    }

    /**
     * Test for same Mimetype and same Size, if this is the case, then ordering is just as it was before sorting
     *
     * @throws Exception
     */
    @Test
    public void testSameMimeTypeSameSize() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("application/pdf");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/pdf");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/pdf");
        when(bitstream1.getSizeBytes()).thenReturn(Long.valueOf(200));
        when(bitstream2.getSizeBytes()).thenReturn(Long.valueOf(200));
        when(bitstream3.getSizeBytes()).thenReturn(Long.valueOf(200));

        List<Bitstream> toSort = bundle.getBitstreams();

        GoogleBitstreamComparator comparator = getComparator();

        Collections.sort(toSort, comparator);
        Bitstream bitstream5 = toSort.get(0);
        assertEquals("Bitstreams have same size and type, so order should remain unchanged", "bitstream1",
                     bitstreamService.getName(bitstream5)
        );
        Bitstream bitstream4 = toSort.get(1);
        assertEquals("Bitstreams have same size and type, so order should remain unchanged", "bitstream2",
                     bitstreamService.getName(bitstream4)
        );
        Bitstream bitstream = toSort.get(2);
        assertEquals("Bitstreams have same size and type, so order should remain unchanged", "bitstream3",
                     bitstreamService.getName(bitstream)
        );

        // Also, verify all bitstreams are considered equal (comparison returns 0)
        assertEquals(0, comparator.compare(bitstream1, bitstream2));
        assertEquals(0, comparator.compare(bitstream2, bitstream3));
        assertEquals(0, comparator.compare(bitstream3, bitstream1));
    }

    /**
     * Test if sorting still works when there is an undefined Mimetype, undefined mimetypes have the lowest priority
     *
     * @throws Exception
     */
    @Test
    public void testUnknownMimeType() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("unknown");
        when(bitstreamFormat2.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat3.getMIMEType()).thenReturn("text/richtext");
        when(bitstream2.getSizeBytes()).thenReturn(Long.valueOf(200));
        when(bitstream3.getSizeBytes()).thenReturn(Long.valueOf(300));

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, getComparator());
        Bitstream bitstream5 = toSort.get(0);
        assertEquals("bitstream3", bitstreamService.getName(bitstream5));
        Bitstream bitstream4 = toSort.get(1);
        assertEquals("bitstream2", bitstreamService.getName(bitstream4));
        Bitstream bitstream = toSort.get(2);
        assertEquals("Unknown mime-types should always have the lowest priority", "bitstream1",
                     bitstreamService.getName(bitstream)
        );
    }

    /**
     * Test with all unknown mimetypes, ordered by size if this is the case
     *
     * @throws Exception
     */
    @Test
    public void testAllUnknown() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("unknown");
        when(bitstreamFormat2.getMIMEType()).thenReturn("unknown");
        when(bitstreamFormat3.getMIMEType()).thenReturn("unknown");
        when(bitstream1.getSizeBytes()).thenReturn(Long.valueOf(200));
        when(bitstream2.getSizeBytes()).thenReturn(Long.valueOf(300));
        when(bitstream3.getSizeBytes()).thenReturn(Long.valueOf(100));

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, getComparator());
        Bitstream bitstream5 = toSort.get(0);
        assertEquals("bitstream2 should come first as it is the largest and all types are equal", "bitstream2",
                     bitstreamService.getName(bitstream5)
        );
        Bitstream bitstream4 = toSort.get(1);
        assertEquals("bitstream1 should come second as it is the second largest and all types are equal", "bitstream1",
                     bitstreamService.getName(bitstream4)
        );
        Bitstream bitstream = toSort.get(2);
        assertEquals("bitstream3 should come last as it is the smallest and all types are equal", "bitstream3",
                     bitstreamService.getName(bitstream)
        );
    }

    /**
     * Test to see if priority is configurable, order should change according to prioritized_types property
     */
    @Test
    public void testChangePriority() throws Exception {
        settings.put("citation.prioritized_types", "Postscript, RTF, Microsoft Word, Adobe PDF");
        when(bitstreamFormat1.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/msword");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/postscript");

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, getComparator());
        Bitstream bitstream5 = toSort.get(0);
        assertEquals("According to the updated type prioritization, PS should be first", "bitstream3",
                     bitstreamService.getName(bitstream5)
        );
        Bitstream bitstream4 = toSort.get(1);
        assertEquals("According to the updated type prioritization, RTF should come second", "bitstream1",
                     bitstreamService.getName(bitstream4)
        );
        Bitstream bitstream = toSort.get(2);
        assertEquals("According to the updated type prioritization, Word has to be last", "bitstream2",
                     bitstreamService.getName(bitstream)
        );
    }

    /**
     * Test what happens when bitstreams have no mimetype, should be just ordered by size
     */
    @Test
    public void testNoMimeType() throws Exception {
        when(bitstream1.getSizeBytes()).thenReturn(Long.valueOf(200));
        when(bitstream2.getSizeBytes()).thenReturn(Long.valueOf(300));
        when(bitstream3.getSizeBytes()).thenReturn(Long.valueOf(100));

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, getComparator());
        Bitstream bitstream5 = toSort.get(0);
        assertEquals("bitstream2 should come first as it is the largest and there are no types", "bitstream2",
                     bitstreamService.getName(bitstream5)
        );
        Bitstream bitstream4 = toSort.get(1);
        assertEquals("bitstream1 should come second as it is the second largest and there are no types", "bitstream1",
                     bitstreamService.getName(bitstream4)
        );
        Bitstream bitstream = toSort.get(2);
        assertEquals("bitstream3 should come last as it is the smallest and there are no types", "bitstream3",
                     bitstreamService.getName(bitstream)
        );
    }

    /**
     * Three bitstreams without a size, just the same ordering as given
     *
     * @throws Exception
     */
    @Test
    public void testNoSize() throws Exception {
        when(bitstreamFormat1.getMIMEType()).thenReturn("unknown");
        when(bitstreamFormat2.getMIMEType()).thenReturn("unknown");
        when(bitstreamFormat3.getMIMEType()).thenReturn("unknown");

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, getComparator());
        Bitstream bitstream5 = toSort.get(0);
        assertEquals("bitstream1", bitstreamService.getName(bitstream5));
        Bitstream bitstream4 = toSort.get(1);
        assertEquals("bitstream2", bitstreamService.getName(bitstream4));
        Bitstream bitstream = toSort.get(2);
        assertEquals("bitstream3", bitstreamService.getName(bitstream));
    }

    /**
     * Make sure that it doesn't crash when bitstreams have no size or mimetype
     */
    @Test
    public void testNoMimeTypeNoSize() throws Exception {
        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, getComparator());
        Bitstream bitstream5 = toSort.get(0);
        assertEquals("bitstream1", bitstreamService.getName(bitstream5));
        Bitstream bitstream4 = toSort.get(1);
        assertEquals("bitstream2", bitstreamService.getName(bitstream4));
        Bitstream bitstream = toSort.get(2);
        assertEquals("bitstream3", bitstreamService.getName(bitstream));
    }

    /**
     * Make sure it still works when the citation.prioritized_types property is empty
     */
    @Test
    public void testNoPrioritizedTypes() throws Exception {
        settings.put("citation.prioritized_types", "");
        when(bitstreamFormat1.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/msword");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/postscript");
        when(bitstream1.getSizeBytes()).thenReturn(Long.valueOf(100));
        when(bitstream2.getSizeBytes()).thenReturn(Long.valueOf(200));
        when(bitstream3.getSizeBytes()).thenReturn(Long.valueOf(300));

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, getComparator());
        Bitstream bitstream5 = toSort.get(0);
        assertEquals("PS should be first because it is the largest one and there is no type prioritization",
                     "bitstream3", bitstreamService.getName(bitstream5)
        );
        Bitstream bitstream4 = toSort.get(1);
        assertEquals("RTF should come second because it is the second largest and there is no type prioritization",
                     "bitstream2", bitstreamService.getName(bitstream4)
        );
        Bitstream bitstream = toSort.get(2);
        assertEquals("Word has to be last (third) as it is the smallest one and there is no type prioritization",
                     "bitstream1", bitstreamService.getName(bitstream)
        );
    }

    /**
     * Test to check for no crash when nothing is configured, just checks for size
     */
    @Test
    public void testNoConfig() throws Exception {
        settings.remove("citation.prioritized_types");
        when(bitstreamFormat1.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/msword");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/postscript");
        when(bitstream1.getSizeBytes()).thenReturn(Long.valueOf(100));
        when(bitstream2.getSizeBytes()).thenReturn(Long.valueOf(200));
        when(bitstream3.getSizeBytes()).thenReturn(Long.valueOf(300));

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, getComparator());
        Bitstream bitstream5 = toSort.get(0);
        assertEquals("bitstream3 should come first as it is the largest and there is no type prioritization configured",
                     "bitstream3", bitstreamService.getName(bitstream5)
        );
        Bitstream bitstream4 = toSort.get(1);
        assertEquals(
            "bitstream2 should come second as it is the second largest and there is no type prioritization configured",
            "bitstream2", bitstreamService.getName(bitstream4)
        );
        Bitstream bitstream = toSort.get(2);
        assertEquals("bitstream1 should come last as it is the smallest and there is no type prioritization configured",
                     "bitstream1", bitstreamService.getName(bitstream)
        );
    }

    /**
     * Test to see what happens when you choose a short description that's not defined in bitstream-formats.xml
     */
    @Test
    public void testUndefinedShortDescription() {
        settings.put("citation.prioritized_types", "Postscript, RTF, Undefined Type, Microsoft Word, Adobe PDF");
        when(bitstreamFormat1.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/msword");
        when(bitstreamFormat3.getMIMEType()).thenReturn("application/postscript");

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, getComparator());
        Bitstream bitstream5 = toSort.get(0);
        assertEquals("bitstream3", bitstreamService.getName(bitstream5));
        Bitstream bitstream4 = toSort.get(1);
        assertEquals("bitstream1", bitstreamService.getName(bitstream4));
        Bitstream bitstream = toSort.get(2);
        assertEquals("bitstream2", bitstreamService.getName(bitstream));
    }

    /**
     * Test adding a new format in the priority list
     */
    @Test
    public void testAddingNewFormat() {
        settings.put("citation.prioritized_types", "WAV, Adobe PDF, Microsoft Word, RTF, Postscript");
        when(bitstreamFormat1.getMIMEType()).thenReturn("text/richtext");
        when(bitstreamFormat2.getMIMEType()).thenReturn("application/msword");
        when(bitstreamFormat3.getMIMEType()).thenReturn("audio/x-wav");

        List<Bitstream> toSort = bundle.getBitstreams();
        Collections.sort(toSort, getComparator());
        Bitstream bitstream5 = toSort.get(0);
        assertEquals("bitstream3 has the type with the highest priority (thus ignoring its size) and should come first",
                     "bitstream3", bitstreamService.getName(bitstream5)
        );
        Bitstream bitstream4 = toSort.get(1);
        assertEquals(
            "bitstream2 has a type with a priority higher than bitstream1 (size is ignored) and should come second",
            "bitstream2", bitstreamService.getName(bitstream4)
        );
        Bitstream bitstream = toSort.get(2);
        assertEquals(
            "bitstream1 has a type with the lowest priority in this bundle eventhough it is the largest bitstream" +
                " and should come last",
            "bitstream1", bitstreamService.getName(bitstream)
        );
    }

    @After
    @Override
    public void destroy() {
        settings = null;
        super.destroy();
    }

}
