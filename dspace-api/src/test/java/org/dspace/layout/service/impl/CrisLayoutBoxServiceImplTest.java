/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;

import static org.apache.commons.collections4.IteratorUtils.arrayIterator;
import static org.apache.commons.collections4.IteratorUtils.emptyIterator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoveryConfigurationUtilsService;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBoxTypes;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutFieldBitstream;
import org.dspace.layout.dao.CrisLayoutBoxDAO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for CrisLayoutBoxServiceImpl, so far only findByItem method is tested.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */

@RunWith(MockitoJUnitRunner.Silent.class)
public class CrisLayoutBoxServiceImplTest {

    @InjectMocks
    private CrisLayoutBoxServiceImpl crisLayoutBoxService;

    @Mock
    private Context context;

    @Mock
    private CrisLayoutBoxDAO dao;

    @Mock
    private AuthorizeService authorizeService;

    @Mock
    private DiscoveryConfigurationUtilsService searchConfigurationUtilsService;

    @Mock
    private ItemService itemService;

    @Mock
    private BitstreamService bitstreamService;

    @Test
    public void testHasContentWithMetadataBox() {

        MetadataField titleField = metadataField("dc", "title", null);
        MetadataField authorField = metadataField("dc", "contributor", "author");

        CrisLayoutBox box = crisLayoutMetadataBox("Main Box", authorField, titleField);
        Item item = item(metadataValue(titleField, "John Smith"));

        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(true));
    }

    @Test
    public void testRelationBoxHasContent() {

        CrisLayoutBox box = crisLayoutBox("authors", CrisLayoutBoxTypes.RELATION.name());
        Item item = item();

        Iterator<Item> relatedItems = arrayIterator(item(), item());
        when(searchConfigurationUtilsService.findByRelation(context, item, "authors")).thenReturn(relatedItems);
        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(true));

    }

    @Test
    public void testRelationBoxHasNoContent() {

        CrisLayoutBox box = crisLayoutBox("authors", CrisLayoutBoxTypes.RELATION.name());
        Item item = item();

        when(searchConfigurationUtilsService.findByRelation(context, item, "authors")).thenReturn(emptyIterator());
        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(false));

    }

    @Test
    public void testHasContentWithBoxWithoutType() {

        MetadataField titleField = metadataField("dc", "title", null);

        CrisLayoutBox box = crisLayoutBox("Main Box", null, titleField);
        Item item = item(metadataValue(titleField, "John Smith"));

        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(true));
    }

    @Test
    public void testHasContentWithItemWithoutMetadata() {

        MetadataField titleField = metadataField("dc", "title", null);
        MetadataField authorField = metadataField("dc", "contributor", "author");

        CrisLayoutBox box = crisLayoutMetadataBox("Main Box", titleField, authorField);
        Item item = item();

        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(false));
    }

    @Test
    public void testHasContentWithEmptyMetadataBox() {

        MetadataField titleField = metadataField("dc", "title", null);

        CrisLayoutBox box = crisLayoutMetadataBox("Main Box");
        Item item = item(metadataValue(titleField, "John Smith"));

        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(false));
    }

    @Test
    public void testHasContentWithBoxWithBitstream() throws SQLException {

        MetadataField titleField = metadataField("dc", "title", null);
        MetadataField typeField = metadataField("dc", "type", null);

        Item item = item();

        Bitstream bitstream = mock(Bitstream.class);

        CrisLayoutFieldBitstream fieldBitstream = new CrisLayoutFieldBitstream();
        fieldBitstream.setBundle("ORIGINAL");
        fieldBitstream.setMetadataValue("thumbnail");
        fieldBitstream.setMetadataField(typeField);

        CrisLayoutBox box = new CrisLayoutBox();
        box.addLayoutField(crisLayoutField(titleField));
        box.addLayoutField(fieldBitstream);
        box.setShortname("Main Box");
        box.setType("METADATA");

        when(bitstreamService.findShowableByItem(context, item.getID(), "ORIGINAL", Map.of("dc.type", "thumbnail")))
            .thenReturn(List.of(bitstream));

        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(true));

        verify(bitstreamService).findShowableByItem(context, item.getID(), "ORIGINAL", Map.of("dc.type", "thumbnail"));

    }

    @Test
    public void testHasNoContentWithBoxWithBitstream() throws SQLException {

        MetadataField titleField = metadataField("dc", "title", null);
        MetadataField typeField = metadataField("dc", "type", null);

        Item item = item();

        CrisLayoutFieldBitstream fieldBitstream = new CrisLayoutFieldBitstream();
        fieldBitstream.setBundle("ORIGINAL");
        fieldBitstream.setMetadataValue("thumbnail");
        fieldBitstream.setMetadataField(typeField);

        CrisLayoutBox box = new CrisLayoutBox();
        box.addLayoutField(crisLayoutField(titleField));
        box.addLayoutField(fieldBitstream);
        box.setShortname("Main Box");
        box.setType("METADATA");

        when(bitstreamService.findShowableByItem(context, item.getID(), "ORIGINAL", Map.of("dc.type", "thumbnal")))
            .thenReturn(List.of());

        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(false));

        verify(bitstreamService).findShowableByItem(context, item.getID(), "ORIGINAL", Map.of("dc.type", "thumbnail"));

    }

    @Test
    public void testHasContentWithBoxWithBitstreamWithBlankMetadataValue() throws SQLException {

        MetadataField titleField = metadataField("dc", "title", null);
        MetadataField typeField = metadataField("dc", "type", null);

        Item item = item();

        Bitstream bitstream = mock(Bitstream.class);

        CrisLayoutFieldBitstream fieldBitstream = new CrisLayoutFieldBitstream();
        fieldBitstream.setBundle("ORIGINAL");
        fieldBitstream.setMetadataField(typeField);
        fieldBitstream.setMetadataValue(null);

        CrisLayoutBox box = new CrisLayoutBox();
        box.addLayoutField(crisLayoutField(titleField));
        box.addLayoutField(fieldBitstream);
        box.setShortname("Main Box");
        box.setType("METADATA");

        when(bitstreamService.findShowableByItem(context, item.getID(), "ORIGINAL", Map.of()))
                .thenReturn(List.of(bitstream));

        assertThat(crisLayoutBoxService.hasContent(context, box, item), is(true));

        verify(bitstreamService).findShowableByItem(context, item.getID(), "ORIGINAL", Map.of());

    }

    @Test
    public void testIiifBoxHasContentWithMetadataTrue() {
        Item item = item();

        when(itemService.getMetadataFirstValue(item, new MetadataFieldName("dspace", "iiif", "enabled"),
        Item.ANY)).thenReturn("true");

        CrisLayoutBox box = crisLayoutBox("Box", "IIIFVIEWER");

        assertTrue(crisLayoutBoxService.hasContent(context, box, item));
    }

    @Test
    public void testIiifBoxHasNoContentWithMetadataFalse() {
        Item item = item();

        when(itemService.getMetadataFirstValue(item, new MetadataFieldName("dspace", "iiif", "enabled"),
        Item.ANY)).thenReturn("false");

        CrisLayoutBox box = crisLayoutBox("Box", "IIIFVIEWER");

        assertFalse(crisLayoutBoxService.hasContent(context, box, item));
    }

    @Test
    public void testIiifBoxHasNoContentWithMetadataUndefined() {
        Item item = item();

        CrisLayoutBox box = crisLayoutBox("Box", "IIIFVIEWER");

        assertFalse(crisLayoutBoxService.hasContent(context, box, item));
    }

    @Test
    public void testSingleMetadataboxBitstreamWithoutField() throws SQLException {

        CrisLayoutBox singleBitstreamBox = new CrisLayoutBox();
        singleBitstreamBox.setShortname("File");
        singleBitstreamBox.setType(null);

        Item item = item();
        Bitstream bitstream = mock(Bitstream.class);

        CrisLayoutFieldBitstream fieldBitstream = new CrisLayoutFieldBitstream();
        fieldBitstream.setBundle("ORIGINAL");
        fieldBitstream.setMetadataValue(null);
        fieldBitstream.setMetadataField(null);
        fieldBitstream.setRendering("attachment");

        singleBitstreamBox.addLayoutField(fieldBitstream);

        when(bitstreamService.findShowableByItem(context, item.getID(), "ORIGINAL", Map.of()))
                .thenReturn(List.of(bitstream));

        assertThat(crisLayoutBoxService.hasContent(context, singleBitstreamBox, item), is(true));

    }

    @Test
    public void testNetworkLabBoxHasContentWithMetadataTrue() {
        Item item = item();

        when(itemService.getMetadataFirstValue(item, new MetadataFieldName("dspace", "networklab", "enabled"),
                Item.ANY)).thenReturn("true");

        CrisLayoutBox box = crisLayoutBox("Box", "NETWORKLAB");

        assertTrue(crisLayoutBoxService.hasContent(context, box, item));
    }

    @Test
    public void testNetworkLabBoxHasNoContentWithMetadataFalse() {
        Item item = item();

        when(itemService.getMetadataFirstValue(item, new MetadataFieldName("dspace", "networklab", "enabled"),
                Item.ANY)).thenReturn("false");

        CrisLayoutBox box = crisLayoutBox("Box", "NETWORKLAB");

        assertFalse(crisLayoutBoxService.hasContent(context, box, item));
    }

    @Test
    public void testNetworkLabBoxHasNoContentWithMetadataUndefined() {
        Item item = item();

        CrisLayoutBox box = crisLayoutBox("Box", "NETWORKLAB");

        assertFalse(crisLayoutBoxService.hasContent(context, box, item));
    }

    private CrisLayoutBox crisLayoutMetadataBox(String shortname, MetadataField... metadataFields) {
        return crisLayoutBox(shortname, CrisLayoutBoxTypes.METADATA.name(), metadataFields);
    }

    private CrisLayoutBox crisLayoutBox(String shortname, String boxType, MetadataField... metadataFields) {
        CrisLayoutBox box = new CrisLayoutBox();
        for (MetadataField metadataField : metadataFields) {
            box.addLayoutField(crisLayoutField(metadataField));
        }
        box.setShortname(shortname);
        box.setType(boxType);
        return box;
    }

    private Item item(MetadataValue... metadataValues) {
        Item item = mock(Item.class);
        when(item.getMetadata()).thenReturn(List.of(metadataValues));
        when(item.getID()).thenReturn(UUID.randomUUID());
        return item;
    }

    private MetadataField metadataField(String schema, String element, String qualifier) {
        MetadataField metadataField = mock(MetadataField.class);
        when(metadataField.getElement()).thenReturn(element);
        when(metadataField.getQualifier()).thenReturn(qualifier);

        MetadataSchema metadataSchema = mock(MetadataSchema.class);
        when(metadataSchema.getName()).thenReturn(schema);
        when(metadataField.getMetadataSchema()).thenReturn(metadataSchema);
        if (qualifier == null) {
            when(metadataField.toString('.')).thenReturn(schema + "." + element);
        } else {
            when(metadataField.toString('.')).thenReturn(schema + "." + element + "." + qualifier);
        }

        return metadataField;
    }

    private MetadataValue metadataValue(MetadataField field, String value) {
        return metadataValue(field, value, null);
    }

    private MetadataValue metadataValue(MetadataField field, String value, String authority) {
        MetadataValue metadataValue = mock(MetadataValue.class);
        when(metadataValue.getMetadataField()).thenReturn(field);
        when(metadataValue.getValue()).thenReturn(value);
        when(metadataValue.getAuthority()).thenReturn(authority);
        return metadataValue;
    }

    private CrisLayoutField crisLayoutField(MetadataField metadataField) {
        CrisLayoutField crisLayoutField = new CrisLayoutField();
        crisLayoutField.setMetadataField(metadataField);
        return crisLayoutField;
    }

}
