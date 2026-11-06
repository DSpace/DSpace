/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import static org.dspace.content.crosswalk.XSLTCrosswalk.DIM_NS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Integration tests for {@link XSLTIngestionCrosswalk#ingestDIM} applied to
 * Collections and Communities.
 */
public class XSLTIngestionCrosswalkIT extends AbstractIntegrationTestWithDatabase {

    private final CollectionService collectionService =
        ContentServiceFactory.getInstance().getCollectionService();
    private final CommunityService communityService =
        ContentServiceFactory.getInstance().getCommunityService();

    private Community community;
    private Collection collection;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        community = CommunityBuilder.createCommunity(context).build();
        collection = CollectionBuilder.createCollection(context, community).build();
        context.restoreAuthSystemState();
    }

    @Test
    public void testIngestDcAndDspaceMetadataIntoCollection() throws Exception {
        Element dim = dim(
            field("dc", "title", null, "Imported Collection"),
            field("dc", "description", "abstract", "Short description"),
            field("dspace", "entity", "type", "Publication"));

        context.turnOffAuthorisationSystem();
        XSLTIngestionCrosswalk.ingestDIM(context, collection, dim, false);
        context.restoreAuthSystemState();

        assertEquals("Imported Collection",
            collectionService.getMetadataFirstValue(collection, "dc", "title", null, Item.ANY));
        assertEquals("Short description",
            collectionService.getMetadataFirstValue(collection, "dc", "description", "abstract", Item.ANY));
        assertEquals("Publication",
            collectionService.getMetadataFirstValue(collection, "dspace", "entity", "type", Item.ANY));
    }

    @Test
    public void testIngestDcMetadataIntoCommunityIgnoresDspaceSchema() throws Exception {
        Element dim = dim(
            field("dc", "title", null, "Imported Community"),
            field("dspace", "entity", "type", "OrgUnit"));

        context.turnOffAuthorisationSystem();
        XSLTIngestionCrosswalk.ingestDIM(context, community, dim, false);
        context.restoreAuthSystemState();

        assertEquals("Imported Community",
            communityService.getMetadataFirstValue(community, "dc", "title", null, Item.ANY));
        assertNull(communityService.getMetadataFirstValue(community, "dspace", "entity", "type", Item.ANY));
    }

    @Test(expected = SQLException.class)
    public void testIngestUnknownDspaceFieldIntoCollectionThrows() throws Exception {
        context.turnOffAuthorisationSystem();
        XSLTIngestionCrosswalk.ingestDIM(context, collection,
            dim(field("dspace", "unknown", "field", "x")), false);
    }

    @Test(expected = SQLException.class)
    public void testIngestUnknownFieldIntoCollectionIgnoresCreateMissingMetadataFields() throws Exception {
        // createMissingMetadataFields is only honored for Items, not for Collections/Communities
        context.turnOffAuthorisationSystem();
        XSLTIngestionCrosswalk.ingestDIM(context, collection,
            dim(field("dspace", "unknown", "field", "x")), true);
    }

    @Test
    public void testIngestReplacesExistingValue() throws Exception {
        context.turnOffAuthorisationSystem();
        XSLTIngestionCrosswalk.ingestDIM(context, collection,
            dim(field("dspace", "entity", "type", "Publication")), false);
        XSLTIngestionCrosswalk.ingestDIM(context, collection,
            dim(field("dspace", "entity", "type", "Person")), false);
        context.restoreAuthSystemState();

        assertEquals(1,
            collectionService.getMetadata(collection, "dspace", "entity", "type", Item.ANY).size());
        assertEquals("Person",
            collectionService.getMetadataFirstValue(collection, "dspace", "entity", "type", Item.ANY));
    }

    @Test
    public void testIngestNestedDim() throws Exception {
        Element outer = new Element("dim", DIM_NS);
        outer.addContent(dim(field("dspace", "entity", "type", "Publication")));

        context.turnOffAuthorisationSystem();
        XSLTIngestionCrosswalk.ingestDIM(context, collection, outer, false);
        context.restoreAuthSystemState();

        assertEquals("Publication",
            collectionService.getMetadataFirstValue(collection, "dspace", "entity", "type", Item.ANY));
    }

    @Test
    public void testIngestIgnoresOtherSchemas() throws Exception {
        Element dim = dim(
            field("dcterms", "title", null, "Should be ignored"),
            field(null, "title", null, "No schema"));

        context.turnOffAuthorisationSystem();
        XSLTIngestionCrosswalk.ingestDIM(context, collection, dim, false);
        context.restoreAuthSystemState();

        assertNull(collectionService.getMetadataFirstValue(collection, "dcterms", "title", null, Item.ANY));
        assertTrue(collectionService.getMetadata(collection, "dc", "title", null, Item.ANY).isEmpty());
    }

    @Test
    public void testIngestIgnoresNonDimElements() throws Exception {
        Element dim = new Element("dim", DIM_NS);
        Element foreign = new Element("field");
        foreign.setAttribute("mdschema", "dspace");
        foreign.setAttribute("element", "entity");
        foreign.setAttribute("qualifier", "type");
        foreign.setText("Publication");
        dim.addContent(foreign);

        context.turnOffAuthorisationSystem();
        XSLTIngestionCrosswalk.ingestDIM(context, collection, dim, false);
        context.restoreAuthSystemState();

        assertNull(collectionService.getMetadataFirstValue(collection, "dspace", "entity", "type", Item.ANY));
    }

    @Test(expected = CrosswalkObjectNotSupported.class)
    public void testIngestUnsupportedObjectType() throws Exception {
        Bitstream bitstream = Mockito.mock(Bitstream.class);
        Mockito.when(bitstream.getType()).thenReturn(Constants.BITSTREAM);
        XSLTIngestionCrosswalk.ingestDIM(context, bitstream, dim(field("dc", "title", null, "x")), false);
    }

    private static Element dim(Element... fields) {
        Element dim = new Element("dim", DIM_NS);
        for (Element f : fields) {
            dim.addContent(f);
        }
        return dim;
    }

    private static Element field(String schema, String element, String qualifier, String value) {
        Element field = new Element("field", DIM_NS);
        if (schema != null) {
            field.setAttribute("mdschema", schema);
        }
        field.setAttribute("element", element);
        if (qualifier != null) {
            field.setAttribute("qualifier", qualifier);
        }
        field.setText(value);
        return field;
    }
}
