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
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.DynamicLayoutBoxTypes;
import org.dspace.layout.DynamicLayoutField;
import org.dspace.layout.DynamicLayoutFieldBitstream;
import org.dspace.layout.dao.DynamicLayoutBoxDAO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for DynamicLayoutBoxServiceImpl, so far only findByItem method is tested.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */

@RunWith(MockitoJUnitRunner.Silent.class)
public class DynamicLayoutBoxServiceImplTest {

    @InjectMocks
    private DynamicLayoutBoxServiceImpl dynamicLayoutBoxService;

    @Mock
    private Context context;

    @Mock
    private DynamicLayoutBoxDAO dao;

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

        DynamicLayoutBox box = dynamicLayoutMetadataBox("Main Box", authorField, titleField);
        Item item = item(metadataValue(titleField, "John Smith"));

        assertThat(dynamicLayoutBoxService.hasContent(context, box, item), is(true));
    }

    @Test
    public void testRelationBoxHasContent() {

        DynamicLayoutBox box = dynamicLayoutBox("authors", DynamicLayoutBoxTypes.RELATION.name());
        Item item = item();

        Iterator<Item> relatedItems = arrayIterator(item(), item());
        when(searchConfigurationUtilsService.findByRelation(context, item, "authors")).thenReturn(relatedItems);
        assertThat(dynamicLayoutBoxService.hasContent(context, box, item), is(true));

    }

    @Test
    public void testRelationBoxHasNoContent() {

        DynamicLayoutBox box = dynamicLayoutBox("authors", DynamicLayoutBoxTypes.RELATION.name());
        Item item = item();

        when(searchConfigurationUtilsService.findByRelation(context, item, "authors")).thenReturn(emptyIterator());
        assertThat(dynamicLayoutBoxService.hasContent(context, box, item), is(false));

    }

    @Test
    public void testHasContentWithBoxWithoutType() {

        MetadataField titleField = metadataField("dc", "title", null);

        DynamicLayoutBox box = dynamicLayoutBox("Main Box", null, titleField);
        Item item = item(metadataValue(titleField, "John Smith"));

        assertThat(dynamicLayoutBoxService.hasContent(context, box, item), is(true));
    }

    @Test
    public void testHasContentWithItemWithoutMetadata() {

        MetadataField titleField = metadataField("dc", "title", null);
        MetadataField authorField = metadataField("dc", "contributor", "author");

        DynamicLayoutBox box = dynamicLayoutMetadataBox("Main Box", titleField, authorField);
        Item item = item();

        assertThat(dynamicLayoutBoxService.hasContent(context, box, item), is(false));
    }

    @Test
    public void testHasContentWithEmptyMetadataBox() {

        MetadataField titleField = metadataField("dc", "title", null);

        DynamicLayoutBox box = dynamicLayoutMetadataBox("Main Box");
        Item item = item(metadataValue(titleField, "John Smith"));

        assertThat(dynamicLayoutBoxService.hasContent(context, box, item), is(false));
    }

    @Test
    public void testHasContentWithBoxWithBitstream() throws SQLException {

        MetadataField titleField = metadataField("dc", "title", null);
        MetadataField typeField = metadataField("dc", "type", null);

        Item item = item();

        Bitstream bitstream = mock(Bitstream.class);

        DynamicLayoutFieldBitstream fieldBitstream = new DynamicLayoutFieldBitstream();
        fieldBitstream.setBundle("ORIGINAL");
        fieldBitstream.setMetadataValue("thumbnail");
        fieldBitstream.setMetadataField(typeField);

        DynamicLayoutBox box = new DynamicLayoutBox();
        box.addLayoutField(dynamicLayoutField(titleField));
        box.addLayoutField(fieldBitstream);
        box.setShortname("Main Box");
        box.setType("METADATA");

        when(bitstreamService.findShowableByItem(context, item.getID(), "ORIGINAL", Map.of("dc.type", "thumbnail")))
            .thenReturn(List.of(bitstream));

        assertThat(dynamicLayoutBoxService.hasContent(context, box, item), is(true));

        verify(bitstreamService).findShowableByItem(context, item.getID(), "ORIGINAL", Map.of("dc.type", "thumbnail"));

    }

    @Test
    public void testHasNoContentWithBoxWithBitstream() throws SQLException {

        MetadataField titleField = metadataField("dc", "title", null);
        MetadataField typeField = metadataField("dc", "type", null);

        Item item = item();

        DynamicLayoutFieldBitstream fieldBitstream = new DynamicLayoutFieldBitstream();
        fieldBitstream.setBundle("ORIGINAL");
        fieldBitstream.setMetadataValue("thumbnail");
        fieldBitstream.setMetadataField(typeField);

        DynamicLayoutBox box = new DynamicLayoutBox();
        box.addLayoutField(dynamicLayoutField(titleField));
        box.addLayoutField(fieldBitstream);
        box.setShortname("Main Box");
        box.setType("METADATA");

        when(bitstreamService.findShowableByItem(context, item.getID(), "ORIGINAL", Map.of("dc.type", "thumbnal")))
            .thenReturn(List.of());

        assertThat(dynamicLayoutBoxService.hasContent(context, box, item), is(false));

        verify(bitstreamService).findShowableByItem(context, item.getID(), "ORIGINAL", Map.of("dc.type", "thumbnail"));

    }

    @Test
    public void testHasContentWithBoxWithBitstreamWithBlankMetadataValue() throws SQLException {

        MetadataField titleField = metadataField("dc", "title", null);
        MetadataField typeField = metadataField("dc", "type", null);

        Item item = item();

        Bitstream bitstream = mock(Bitstream.class);

        DynamicLayoutFieldBitstream fieldBitstream = new DynamicLayoutFieldBitstream();
        fieldBitstream.setBundle("ORIGINAL");
        fieldBitstream.setMetadataField(typeField);
        fieldBitstream.setMetadataValue(null);

        DynamicLayoutBox box = new DynamicLayoutBox();
        box.addLayoutField(dynamicLayoutField(titleField));
        box.addLayoutField(fieldBitstream);
        box.setShortname("Main Box");
        box.setType("METADATA");

        when(bitstreamService.findShowableByItem(context, item.getID(), "ORIGINAL", Map.of()))
                .thenReturn(List.of(bitstream));

        assertThat(dynamicLayoutBoxService.hasContent(context, box, item), is(true));

        verify(bitstreamService).findShowableByItem(context, item.getID(), "ORIGINAL", Map.of());

    }

    @Test
    public void testIiifBoxHasContentWithMetadataTrue() {
        Item item = item();

        when(itemService.getMetadataFirstValue(item, new MetadataFieldName("dspace", "iiif", "enabled"),
        Item.ANY)).thenReturn("true");

        DynamicLayoutBox box = dynamicLayoutBox("Box", "IIIFVIEWER");

        assertTrue(dynamicLayoutBoxService.hasContent(context, box, item));
    }

    @Test
    public void testIiifBoxHasNoContentWithMetadataFalse() {
        Item item = item();

        when(itemService.getMetadataFirstValue(item, new MetadataFieldName("dspace", "iiif", "enabled"),
        Item.ANY)).thenReturn("false");

        DynamicLayoutBox box = dynamicLayoutBox("Box", "IIIFVIEWER");

        assertFalse(dynamicLayoutBoxService.hasContent(context, box, item));
    }

    @Test
    public void testIiifBoxHasNoContentWithMetadataUndefined() {
        Item item = item();

        DynamicLayoutBox box = dynamicLayoutBox("Box", "IIIFVIEWER");

        assertFalse(dynamicLayoutBoxService.hasContent(context, box, item));
    }

    @Test
    public void testSingleMetadataboxBitstreamWithoutField() throws SQLException {

        DynamicLayoutBox singleBitstreamBox = new DynamicLayoutBox();
        singleBitstreamBox.setShortname("File");
        singleBitstreamBox.setType(null);

        Item item = item();
        Bitstream bitstream = mock(Bitstream.class);

        DynamicLayoutFieldBitstream fieldBitstream = new DynamicLayoutFieldBitstream();
        fieldBitstream.setBundle("ORIGINAL");
        fieldBitstream.setMetadataValue(null);
        fieldBitstream.setMetadataField(null);
        fieldBitstream.setRendering("attachment");

        singleBitstreamBox.addLayoutField(fieldBitstream);

        when(bitstreamService.findShowableByItem(context, item.getID(), "ORIGINAL", Map.of()))
                .thenReturn(List.of(bitstream));

        assertThat(dynamicLayoutBoxService.hasContent(context, singleBitstreamBox, item), is(true));

    }

    @Test
    public void testNetworkLabBoxHasContentWithMetadataTrue() {
        Item item = item();

        when(itemService.getMetadataFirstValue(item, new MetadataFieldName("dspace", "networklab", "enabled"),
                Item.ANY)).thenReturn("true");

        DynamicLayoutBox box = dynamicLayoutBox("Box", "NETWORKLAB");

        assertTrue(dynamicLayoutBoxService.hasContent(context, box, item));
    }

    @Test
    public void testNetworkLabBoxHasNoContentWithMetadataFalse() {
        Item item = item();

        when(itemService.getMetadataFirstValue(item, new MetadataFieldName("dspace", "networklab", "enabled"),
                Item.ANY)).thenReturn("false");

        DynamicLayoutBox box = dynamicLayoutBox("Box", "NETWORKLAB");

        assertFalse(dynamicLayoutBoxService.hasContent(context, box, item));
    }

    @Test
    public void testNetworkLabBoxHasNoContentWithMetadataUndefined() {
        Item item = item();

        DynamicLayoutBox box = dynamicLayoutBox("Box", "NETWORKLAB");

        assertFalse(dynamicLayoutBoxService.hasContent(context, box, item));
    }

    private DynamicLayoutBox dynamicLayoutMetadataBox(String shortname, MetadataField... metadataFields) {
        return dynamicLayoutBox(shortname, DynamicLayoutBoxTypes.METADATA.name(), metadataFields);
    }

    private DynamicLayoutBox dynamicLayoutBox(String shortname, String boxType, MetadataField... metadataFields) {
        DynamicLayoutBox box = new DynamicLayoutBox();
        for (MetadataField metadataField : metadataFields) {
            box.addLayoutField(dynamicLayoutField(metadataField));
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

    private DynamicLayoutField dynamicLayoutField(MetadataField metadataField) {
        DynamicLayoutField dynamicLayoutField = new DynamicLayoutField();
        dynamicLayoutField.setMetadataField(metadataField);
        return dynamicLayoutField;
    }

}
