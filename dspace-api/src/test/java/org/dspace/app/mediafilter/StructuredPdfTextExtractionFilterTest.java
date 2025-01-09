/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.InputStream;
import java.sql.SQLException;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.dspace.app.mediafilter.model.Pages;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.junit.Test;


public class StructuredPdfTextExtractionFilterTest {

    private static final StructuredPdfTextExtractionFilter filter = new StructuredPdfTextExtractionFilter();
    private static final XmlMapper xmlMapper = new XmlMapper();

    @Test
    public void testGetFilteredName() {
        assertEquals("multipage_test.pdf.xml", filter.getFilteredName("multipage_test.pdf"));
    }

    @Test
    public void testGetBundleName() {
        assertEquals("STRUCTURED_TEXT", filter.getBundleName());
    }

    @Test
    public void testGetFormatString() {
        assertEquals("XML", filter.getFormatString());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Extracted Structured Text", filter.getDescription());
    }

    @Test
    public void testGetDestinationStream() throws Exception {
        Item item = mock(Item.class);

        InputStream resultStream = filter.getDestinationStream(item, getMultiPagePDF(), true);

        assertNotNull(resultStream);

        InputStream expectedInputStream = getExpectedXml();

        String normalizedExpectedXml = normalizeXml(expectedInputStream);
        String normalizedResultXml = normalizeXml(resultStream);

        assertEquals(normalizedExpectedXml, normalizedResultXml);

        resultStream.close();
    }

    @Test
    public void testPreProcessBitstream() throws SQLException {
        Context context = mock(Context.class);
        Item item = mock(Item.class);

        Bitstream source = mock(Bitstream.class);
        BitstreamFormat bsFormat = mock(BitstreamFormat.class);
        when(source.getFormat(context)).thenReturn(bsFormat);
        when(bsFormat.getMIMEType()).thenReturn("application/pdf");

        assertTrue(filter.preProcessBitstream(context, item, source, true));

        when(bsFormat.getMIMEType()).thenReturn("image/png");
        assertFalse(filter.preProcessBitstream(context, item, source, true));
    }

    private InputStream getMultiPagePDF() {
        return getClass().getResourceAsStream("multipage_test.pdf");
    }

    private InputStream getExpectedXml() {
        return getClass().getResourceAsStream("multipage_expected_result.xml");
    }

    private static String normalizeXml(InputStream xmlInputStream) throws Exception {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(SerializationFeature.INDENT_OUTPUT, false);

        Pages pages = xmlMapper.readValue(xmlInputStream, Pages.class);
        return xmlMapper.writeValueAsString(pages);
    }

}