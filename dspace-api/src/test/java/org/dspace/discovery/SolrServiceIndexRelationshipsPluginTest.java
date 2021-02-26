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

    @Before
    public void setUp() throws Exception {
        solrServiceIndexRelationshipsPlugin = new SolrServiceIndexRelationshipsPlugin(relationshipService);
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
            relationship(relationshipType(), item, relatedItem, "isRelatedTo", "isRelatedBy", 0, 0);

        when(relationshipService.findByItem(context, item)).thenReturn(
            Collections.singletonList(relationship)
        );

        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);

        assertThat(document.getField("relation.isRelatedTo").getValue(), Is.is(relatedItemUuid));
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
            relationship(relationshipType(), relatedItem, item, "isRelatedTo", "isRelatedBy", 0, 0
            );

        when(relationshipService.findByItem(context, item)).thenReturn(
            Collections.singletonList(relationship)
        );

        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);

        assertThat(document.getField("relation.isRelatedBy").getValue(), Is.is(relatedItemUuid));
        assertNull(document.getField("relation.isRelatedTo"));
    }

    @Test
    public void itemInLeftRelationshipWithFirstPosition() throws SQLException {
        IndexableItem indexableItem = mock(IndexableItem.class);

        Item item = item(randomUUID());

        UUID relatedItemUuid = randomUUID();
        Item relatedItem = item(relatedItemUuid);

        when(indexableItem.getIndexedObject()).thenReturn(item);

        RelationshipType relationshipType = relationshipType();
        Relationship relationship =
            relationship(relationshipType, item, relatedItem, "isRelatedTo", "isRelatedBy", 0, 0);

        when(relationshipService.findByItem(context, item)).thenReturn(
            Collections.singletonList(relationship)
        );

        List<Relationship> relatedItemRelationships = Arrays.asList(
            relationship,
            relationship(relationshipType, item(randomUUID()), relatedItem, "isRelatedTo", "isRelatedBy", 1, 0),
            relationship(relationshipType, item(randomUUID()), relatedItem, "isRelatedTo", "isRelatedBy", 2, 0),
            relationship(relationshipType, item(randomUUID()), relatedItem, "isRelatedTo", "isRelatedBy", 3, 0),
            relationship(relationshipType, item(randomUUID()), relatedItem, "isRelatedTo", "isRelatedBy", 4, 0)
        );
        when(relationshipService.findByItemAndRelationshipType(context, relatedItem, relationshipType,
            false))
            .thenReturn(relatedItemRelationships);

        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);

        List<UUID> fieldValue = Collections.nCopies(5, relatedItemUuid);

        assertThat(document.getField("relation.isRelatedTo").getValue(), Is.is(fieldValue));
        assertNull(document.getField("relation.isRelatedBy"));
    }

    @Test
    public void itemInRightRelationshipWithFirstPosition() throws SQLException {
        IndexableItem indexableItem = mock(IndexableItem.class);

        Item item = item(randomUUID());

        UUID relatedItemUuid = randomUUID();
        Item relatedItem = item(relatedItemUuid);

        when(indexableItem.getIndexedObject()).thenReturn(item);

        RelationshipType relationshipType = relationshipType();
        Relationship relationship =
            relationship(relationshipType, relatedItem, item, "isRelatedTo", "isRelatedBy", 0, 0);

        when(relationshipService.findByItem(context, item)).thenReturn(
            Collections.singletonList(relationship)
        );

        List<Relationship> relatedItemRelationships = Arrays.asList(
            relationship,
            relationship(relationshipType, relatedItem, item(randomUUID()), "isRelatedTo", "isRelatedBy", 1, 1),
            relationship(relationshipType, relatedItem, item(randomUUID()), "isRelatedTo", "isRelatedBy", 2, 2),
            relationship(relationshipType, relatedItem, item(randomUUID()), "isRelatedTo", "isRelatedBy", 3, 3),
            relationship(relationshipType, relatedItem, item(randomUUID()), "isRelatedTo", "isRelatedBy", 4, 4)
        );
        when(relationshipService.findByItemAndRelationshipType(context, relatedItem, relationshipType,
            true))
            .thenReturn(relatedItemRelationships);

        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);

        List<UUID> fieldValue = Collections.nCopies(5, relatedItemUuid);

        assertThat(document.getField("relation.isRelatedBy").getValue(), Is.is(fieldValue));
        assertNull(document.getField("relation.isRelatedTo"));
    }

    @Test
    public void itemInLeftRelationshipWithLastPosition() throws SQLException {
        IndexableItem indexableItem = mock(IndexableItem.class);

        Item item = item(randomUUID());

        UUID relatedItemUuid = randomUUID();
        Item relatedItem = item(relatedItemUuid);

        when(indexableItem.getIndexedObject()).thenReturn(item);

        RelationshipType relationshipType = relationshipType();
        Relationship relationship =
            relationship(relationshipType, item, relatedItem, "isRelatedTo", "isRelatedBy", 4, 0);

        when(relationshipService.findByItem(context, item)).thenReturn(
            Collections.singletonList(relationship)
        );

        List<Relationship> relatedItemRelationships = Arrays.asList(
            relationship,
            relationship(relationshipType, item(randomUUID()), relatedItem, "isRelatedTo", "isRelatedBy", 0, 0),
            relationship(relationshipType, item(randomUUID()), relatedItem, "isRelatedTo", "isRelatedBy", 1, 0),
            relationship(relationshipType, item(randomUUID()), relatedItem, "isRelatedTo", "isRelatedBy", 2, 0),
            relationship(relationshipType, item(randomUUID()), relatedItem, "isRelatedTo", "isRelatedBy", 3, 0)
        );
        when(relationshipService.findByItemAndRelationshipType(context, relatedItem, relationshipType,
            false))
            .thenReturn(relatedItemRelationships);

        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);

        assertThat(document.getField("relation.isRelatedTo").getValue(), Is.is(relatedItemUuid));
        assertNull(document.getField("relation.isRelatedBy"));
    }

    @Test
    public void itemInRightRelationshipWithLastPosition() throws SQLException {
        IndexableItem indexableItem = mock(IndexableItem.class);

        Item item = item(randomUUID());

        UUID relatedItemUuid = randomUUID();
        Item relatedItem = item(relatedItemUuid);

        when(indexableItem.getIndexedObject()).thenReturn(item);

        RelationshipType relationshipType = relationshipType();
        Relationship relationship =
            relationship(relationshipType, relatedItem, item, "isRelatedTo", "isRelatedBy", 40, 4);

        when(relationshipService.findByItem(context, item)).thenReturn(
            Collections.singletonList(relationship)
        );

        List<Relationship> relatedItemRelationships = Arrays.asList(
            relationship,
            relationship(relationshipType, relatedItem, item(randomUUID()), "isRelatedTo", "isRelatedBy", 0, 0),
            relationship(relationshipType, relatedItem, item(randomUUID()), "isRelatedTo", "isRelatedBy", 1, 1),
            relationship(relationshipType, relatedItem, item(randomUUID()), "isRelatedTo", "isRelatedBy", 2, 2),
            relationship(relationshipType, relatedItem, item(randomUUID()), "isRelatedTo", "isRelatedBy", 3, 3)
        );
        when(relationshipService.findByItemAndRelationshipType(context, relatedItem, relationshipType,
            true))
            .thenReturn(relatedItemRelationships);

        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);

        assertThat(document.getField("relation.isRelatedBy").getValue(), Is.is(relatedItemUuid));
        assertNull(document.getField("relation.isRelatedTo"));
    }


    @Test
    public void itemInLeftRelationshipWithIntermediatePosition() throws SQLException {
        IndexableItem indexableItem = mock(IndexableItem.class);

        Item item = item(randomUUID());

        UUID relatedItemUuid = randomUUID();
        Item relatedItem = item(relatedItemUuid);

        when(indexableItem.getIndexedObject()).thenReturn(item);

        RelationshipType relationshipType = relationshipType();
        Relationship relationship =
            relationship(relationshipType, item, relatedItem, "isRelatedTo", "isRelatedBy", 2, 0);

        when(relationshipService.findByItem(context, item)).thenReturn(
            Collections.singletonList(relationship)
        );

        List<Relationship> relatedItemRelationships = Arrays.asList(
            relationship,
            relationship(relationshipType, item(randomUUID()), relatedItem, "isRelatedTo", "isRelatedBy", 0, 0),
            relationship(relationshipType, item(randomUUID()), relatedItem, "isRelatedTo", "isRelatedBy", 1, 0),
            relationship(relationshipType, item(randomUUID()), relatedItem, "isRelatedTo", "isRelatedBy", 3, 0),
            relationship(relationshipType, item(randomUUID()), relatedItem, "isRelatedTo", "isRelatedBy", 4, 0)
        );
        when(relationshipService.findByItemAndRelationshipType(context, relatedItem, relationshipType,
            false))
            .thenReturn(relatedItemRelationships);

        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);

        List<UUID> fieldValue = Collections.nCopies(3, relatedItemUuid);

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

        RelationshipType relationshipType = relationshipType();
        Relationship relationship =
            relationship(relationshipType, relatedItem, item, "isRelatedTo", "isRelatedBy", 0, 2);

        when(relationshipService.findByItem(context, item)).thenReturn(
            Collections.singletonList(relationship)
        );

        List<Relationship> relatedItemRelationships = Arrays.asList(
            relationship,
            relationship(relationshipType, relatedItem, item(randomUUID()), "isRelatedTo", "isRelatedBy", 0, 0),
            relationship(relationshipType, relatedItem, item(randomUUID()), "isRelatedTo", "isRelatedBy", 11, 1),
            relationship(relationshipType, relatedItem, item(randomUUID()), "isRelatedTo", "isRelatedBy", 3, 3),
            relationship(relationshipType, relatedItem, item(randomUUID()), "isRelatedTo", "isRelatedBy", 444, 4)
        );
        when(relationshipService.findByItemAndRelationshipType(context, relatedItem, relationshipType,
            true))
            .thenReturn(relatedItemRelationships);

        SolrInputDocument document = new SolrInputDocument();
        solrServiceIndexRelationshipsPlugin.additionalIndex(context, indexableItem, document);

        List<UUID> fieldValue = Collections.nCopies(3, relatedItemUuid);

        assertThat(document.getField("relation.isRelatedBy").getValue(), Is.is(fieldValue));
        assertNull(document.getField("relation.isRelatedTo"));
    }

    private RelationshipType relationshipType() {
        return mock(RelationshipType.class);
    }


    private Relationship relationship(RelationshipType relationshipType, Item leftItem, Item relatedItem,
                                      String leftwardValue, String rightwardValue,
                                      Integer leftPlace, Integer rightPlace) {
        Relationship relationship = mock(Relationship.class);

        when(relationship.getLeftItem()).thenReturn(leftItem);
        when(relationship.getRightItem()).thenReturn(relatedItem);
        when(relationship.getLeftwardValue()).thenReturn(leftwardValue);
        when(relationship.getRightwardValue()).thenReturn(rightwardValue);
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