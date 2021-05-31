/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.services.ConfigurationService;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link SolrServiceIndexRelationshipsPlugin} class
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */

public class SolrServiceIndexRelationshipsPluginTest {

    private SolrServiceIndexRelationshipsPlugin solrServiceIndexRelationshipsPlugin;

    private RelationshipService relationshipService = mock(RelationshipService.class);
    private Context context = mock(Context.class);
    private ConfigurationService configurationService = mock(ConfigurationService.class);

    @Before
    public void setUp() throws Exception {
        solrServiceIndexRelationshipsPlugin = new SolrServiceIndexRelationshipsPlugin(relationshipService,
                                                                                      configurationService);
    }

    @Test
    public void notAnIndexableItemDocumentNotUpdated() {

        IndexableCollection indexableCollection = mock(IndexableCollection.class);
        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableCollection, document);
        assertThat(document.getFieldNames().size(), Is.is(0));
    }

    @Test
    public void nullItemDocumentNotUpdated() {

        IndexableItem indexableItem = mock(IndexableItem.class);
        when(indexableItem.getIndexedObject()).thenReturn(null);
        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);
        assertThat(document.getFieldNames().size(), Is.is(0));
    }

    @Test
    public void noRelationshipsDocumentNotUpdated() throws SQLException {

        IndexableItem indexableItem = mock(IndexableItem.class);
        Item item = item(randomUUID());
        when(indexableItem.getIndexedObject()).thenReturn(item);
        when(relationshipService.findByItem(context, item)).thenReturn(Collections.emptyList());
        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);
        assertThat(document.getFieldNames().size(), Is.is(0));
    }

    @Test
    public void errorDuringRelationshipLookupDocumentNotUpdated() throws SQLException {

        IndexableItem indexableItem = mock(IndexableItem.class);
        Item item = item(randomUUID());
        when(indexableItem.getIndexedObject()).thenReturn(item);

        doThrow(new SQLException("SQLException")).when(relationshipService).findByItem(context, item);

        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);
        assertThat(document.getFieldNames().size(), Is.is(0));
    }

    @Test
    public void itemInLeftRelationship() throws SQLException {
        IndexableItem indexableItem = mock(IndexableItem.class);

        Item item = item(randomUUID());

        UUID relatedItemUuid = randomUUID();
        Item relatedItem = item(relatedItemUuid);

        when(indexableItem.getIndexedObject()).thenReturn(item);

        Relationship relationship =
            relationship(relationshipType("isRelatedTo", "isRelatedBy"), item, relatedItem, 0, 0);

        when(relationshipService.findByItem(context, item)).thenReturn(
            Collections.singletonList(relationship)
        );

        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);

        assertThat(document.getField("relation.isRelatedTo").getValue(), Is.is(relatedItemUuid.toString()));
        assertNull(document.getField("relation.isRelatedBy"));
    }

    @Test
    public void itemInRightRelationship() throws SQLException {
        IndexableItem indexableItem = mock(IndexableItem.class);

        Item item = item(randomUUID());

        UUID relatedItemUuid = randomUUID();
        Item relatedItem = item(relatedItemUuid);

        when(indexableItem.getIndexedObject()).thenReturn(item);

        Relationship relationship =
            relationship(relationshipType("isRelatedTo", "isRelatedBy"), relatedItem, item, 0, 0
            );

        when(relationshipService.findByItem(context, item)).thenReturn(
            Collections.singletonList(relationship)
        );

        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);

        assertThat(document.getField("relation.isRelatedBy").getValue(), Is.is(relatedItemUuid.toString()));
        assertNull(document.getField("relation.isRelatedTo"));
    }

    @Test
    public void itemInLeftPartOfRelationshipAtFirstPosition() throws SQLException {
        IndexableItem indexableItem = mock(IndexableItem.class);

        Item item = item(randomUUID());

        UUID relatedItemUuid = randomUUID();
        Item relatedItem = item(relatedItemUuid);

        when(indexableItem.getIndexedObject()).thenReturn(item);

        RelationshipType relationshipType = relationshipType("isRelatedTo", "isRelatedBy");
        Relationship relationship =
            relationship(relationshipType, item, relatedItem, 5, 0);

        when(relationshipService.findByItem(context, item)).thenReturn(
            Collections.singletonList(relationship)
        );

        List<Relationship> relatedItemRelationships = Arrays.asList(
            relationship,
            relationship(relationshipType, item(randomUUID()), relatedItem, 5, 0),
            relationship(relationshipType, item(randomUUID()), relatedItem, 5, 1),
            relationship(relationshipType, item(randomUUID()), relatedItem, 5, 2),
            relationship(relationshipType, item(randomUUID()), relatedItem, 5, 3)
        );
        when(relationshipService.findByItemAndRelationshipType(context, relatedItem, relationshipType,
            false))
            .thenReturn(relatedItemRelationships);

        when(configurationService.getArrayProperty(startsWith("relationship.places.only")))
            .thenReturn(new String[]{"null::null::isRelatedTo::isRelatedBy"});

        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);

        List<String> fieldValue = Collections.nCopies(5, relatedItemUuid.toString());

        assertThat(document.getField("relation.isRelatedTo").getValue(), Is.is(fieldValue));
        assertNull(document.getField("relation.isRelatedBy"));
    }

    @Test
    public void itemInRightPartOfRelationshipAtFirstPosition() throws SQLException {
        IndexableItem indexableItem = mock(IndexableItem.class);

        Item item = item(randomUUID());

        UUID relatedItemUuid = randomUUID();
        Item relatedItem = item(relatedItemUuid);

        when(indexableItem.getIndexedObject()).thenReturn(item);

        RelationshipType relationshipType = relationshipType("isRelatedTo", "isRelatedBy");
        Relationship relationship =
            relationship(relationshipType, relatedItem, item, 0, 5);

        when(relationshipService.findByItem(context, item)).thenReturn(
            Collections.singletonList(relationship)
        );

        List<Relationship> relatedItemRelationships = Arrays.asList(
            relationship,
            relationship(relationshipType, relatedItem, item(randomUUID()), 1, 5),
            relationship(relationshipType, relatedItem, item(randomUUID()), 2, 5),
            relationship(relationshipType, relatedItem, item(randomUUID()), 3, 5),
            relationship(relationshipType, relatedItem, item(randomUUID()), 4, 5)
        );
        when(relationshipService.findByItemAndRelationshipType(context, relatedItem, relationshipType,
            true))
            .thenReturn(relatedItemRelationships);

        when(configurationService.getArrayProperty(startsWith("relationship.places.only")))
            .thenReturn(new String[]{"null::null::isRelatedTo::isRelatedBy"});

        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);

        List<String> fieldValue = Collections.nCopies(5, relatedItemUuid.toString());

        assertThat(document.getField("relation.isRelatedBy").getValue(), Is.is(fieldValue));
        assertNull(document.getField("relation.isRelatedTo"));
    }

    @Test
    public void itemInLeftPartOfRelationshipAtLastPosition() throws SQLException {
        IndexableItem indexableItem = mock(IndexableItem.class);

        Item item = item(randomUUID());

        UUID relatedItemUuid = randomUUID();
        Item relatedItem = item(relatedItemUuid);

        when(indexableItem.getIndexedObject()).thenReturn(item);

        RelationshipType relationshipType = relationshipType("isRelatedTo", "isRelatedBy");
        Relationship relationship =
            relationship(relationshipType, item, relatedItem, 0, 4);

        when(relationshipService.findByItem(context, item)).thenReturn(
            Collections.singletonList(relationship)
        );

        List<Relationship> relatedItemRelationships = Arrays.asList(
            relationship,
            relationship(relationshipType, item(randomUUID()), relatedItem, 0, 0),
            relationship(relationshipType, item(randomUUID()), relatedItem, 0, 1),
            relationship(relationshipType, item(randomUUID()), relatedItem, 0, 2),
            relationship(relationshipType, item(randomUUID()), relatedItem, 0, 3)
        );
        when(relationshipService.findByItemAndRelationshipType(context, relatedItem, relationshipType,
            false))
            .thenReturn(relatedItemRelationships);

        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);

        assertThat(document.getField("relation.isRelatedTo").getValue(), Is.is(relatedItemUuid.toString()));
        assertNull(document.getField("relation.isRelatedBy"));
    }

    @Test
    public void itemInRightRelationshipWithLastPosition() throws SQLException {
        IndexableItem indexableItem = mock(IndexableItem.class);

        Item item = item(randomUUID());

        UUID relatedItemUuid = randomUUID();
        Item relatedItem = item(relatedItemUuid);

        when(indexableItem.getIndexedObject()).thenReturn(item);

        RelationshipType relationshipType = relationshipType("isRelatedTo", "isRelatedBy");
        Relationship relationship =
            relationship(relationshipType, relatedItem, item, 4, 40);

        when(relationshipService.findByItem(context, item)).thenReturn(
            Collections.singletonList(relationship)
        );

        List<Relationship> relatedItemRelationships = Arrays.asList(
            relationship,
            relationship(relationshipType, relatedItem, item(randomUUID()), 0, 0),
            relationship(relationshipType, relatedItem, item(randomUUID()), 1, 0),
            relationship(relationshipType, relatedItem, item(randomUUID()), 2, 0),
            relationship(relationshipType, relatedItem, item(randomUUID()), 3, 0)
        );
        when(relationshipService.findByItemAndRelationshipType(context, relatedItem, relationshipType,
            true))
            .thenReturn(relatedItemRelationships);

        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);

        assertThat(document.getField("relation.isRelatedBy").getValue(), Is.is(relatedItemUuid.toString()));
        assertNull(document.getField("relation.isRelatedTo"));
    }


    @Test
    public void itemInLeftPartOfRelationshipAtIntermediatePosition() throws SQLException {
        IndexableItem indexableItem = mock(IndexableItem.class);

        Item item = item(randomUUID());

        UUID relatedItemUuid = randomUUID();
        Item relatedItem = item(relatedItemUuid);

        when(indexableItem.getIndexedObject()).thenReturn(item);

        RelationshipType relationshipType = relationshipType("isRelatedTo", "isRelatedBy");
        Relationship relationship =
            relationship(relationshipType, item, relatedItem, 5, 2);

        when(relationshipService.findByItem(context, item)).thenReturn(
            Collections.singletonList(relationship)
        );

        List<Relationship> relatedItemRelationships = Arrays.asList(
            relationship,
            relationship(relationshipType, item(randomUUID()), relatedItem, 5, 0),
            relationship(relationshipType, item(randomUUID()), relatedItem, 5, 1),
            relationship(relationshipType, item(randomUUID()), relatedItem, 5, 2),
            relationship(relationshipType, item(randomUUID()), relatedItem, 5, 3)
        );
        when(relationshipService.findByItemAndRelationshipType(context, relatedItem, relationshipType,
            false))
            .thenReturn(relatedItemRelationships);
        when(configurationService.getArrayProperty(startsWith("relationship.places.only")))
            .thenReturn(new String[]{"null::null::isRelatedTo::isRelatedBy"});

        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);



        List<String> fieldValue = Collections.nCopies(3, relatedItemUuid.toString());

        assertThat(document.getField("relation.isRelatedTo").getValue(), Is.is(fieldValue));
        assertNull(document.getField("relation.isRelatedBy"));
    }

    @Test
    public void itemInRightRelationshipWithIntermediatePosition() throws SQLException {
        IndexableItem indexableItem = mock(IndexableItem.class);

        Item item = item(randomUUID());

        UUID relatedItemUuid = randomUUID();
        Item relatedItem = item(relatedItemUuid);

        when(indexableItem.getIndexedObject()).thenReturn(item);

        RelationshipType relationshipType = relationshipType("isRelatedTo", "isRelatedBy");
        Relationship relationship =
            relationship(relationshipType, relatedItem, item, 2, 5);

        when(relationshipService.findByItem(context, item)).thenReturn(
            Collections.singletonList(relationship)
        );

        List<Relationship> relatedItemRelationships = Arrays.asList(
            relationship,
            relationship(relationshipType, relatedItem, item(randomUUID()), 0, 5),
            relationship(relationshipType, relatedItem, item(randomUUID()), 1, 5),
            relationship(relationshipType, relatedItem, item(randomUUID()), 3, 5),
            relationship(relationshipType, relatedItem, item(randomUUID()), 4, 5)
        );
        when(relationshipService.findByItemAndRelationshipType(context, relatedItem, relationshipType,
            true))
            .thenReturn(relatedItemRelationships);

        when(configurationService.getArrayProperty(startsWith("relationship.places.only")))
            .thenReturn(new String[]{"null::null::isRelatedTo::isRelatedBy"});

        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);



        List<String> fieldValue = Collections.nCopies(3, relatedItemUuid.toString());

        assertThat(document.getField("relation.isRelatedBy").getValue(), Is.is(fieldValue));
        assertNull(document.getField("relation.isRelatedTo"));
    }

    private RelationshipType relationshipType(String leftwardType, String rightwardType) {
        RelationshipType relationshipType = mock(RelationshipType.class);
        when(relationshipType.getLeftwardType()).thenReturn(leftwardType);
        when(relationshipType.getRightwardType()).thenReturn(rightwardType);
        return relationshipType;
    }


    private Relationship relationship(RelationshipType relationshipType, Item leftItem, Item relatedItem,
                                      Integer leftPlace, Integer rightPlace) {
        Relationship relationship = mock(Relationship.class);

        when(relationship.getLeftItem()).thenReturn(leftItem);
        when(relationship.getRightItem()).thenReturn(relatedItem);
        when(relationship.getRelationshipType()).thenReturn(relationshipType);
        when(relationship.getLeftPlace()).thenReturn(leftPlace);
        when(relationship.getRightPlace()).thenReturn(rightPlace);
        return relationship;
    }

    private Item item(UUID relatedItemUuid) {
        Item relatedItem = mock(Item.class);
        when(relatedItem.getID()).thenReturn(relatedItemUuid);
        return relatedItem;
    }
}
