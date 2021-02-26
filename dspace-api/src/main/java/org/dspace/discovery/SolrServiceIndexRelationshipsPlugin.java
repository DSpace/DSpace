/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link SolrServiceIndexPlugin} that adds item relationships informations
 * to solr document.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class SolrServiceIndexRelationshipsPlugin implements SolrServiceIndexPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrServiceIndexRelationshipsPlugin.class);
    public static final String RELATION_PREFIX = "relation.%s";

    private final RelationshipService relationshipService;

    @Autowired
    public SolrServiceIndexRelationshipsPlugin(RelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    @Override
    public void additionalIndex(Context context, IndexableObject indexableObject, SolrInputDocument document) {

        if (!(indexableObject instanceof IndexableItem)) {
            return;
        }
        Item item = ((IndexableItem) indexableObject).getIndexedObject();
        if (Objects.isNull(item)) {
            return;
        }

        try {

            List<Relationship> relationships = relationshipService.findByItem(context, item);
            for (Relationship relationship : relationships) {
                updateDocument(context, document, relationship, item.getID());
            }

        } catch (SQLException e) {
            LOGGER.error("An error occurred during relations indexing", e);
        }

    }

    private void updateDocument(Context context, SolrInputDocument document, Relationship relationship,
                                UUID itemId) throws SQLException {

        boolean isLeftwardRelationship = relationship.getLeftItem().getID().equals(itemId);
        String label = isLeftwardRelationship ? relationship.getLeftwardValue() : relationship.getRightwardValue();
        Item otherItem = isLeftwardRelationship ? relationship.getRightItem() : relationship.getLeftItem();

        String field = String.format(RELATION_PREFIX, label);
        document.addField(field, otherItem.getID());

        //update priorities (find same relations involving other item)
        List<Relationship> relationshipsInvolvingOtherItem =
            otherRelationships(context, relationship, isLeftwardRelationship, itemId);
        int relationshipPlace = isLeftwardRelationship ? relationship.getLeftPlace() : relationship.getRightPlace();

        IntStream.range(relationshipPlace, relationshipsInvolvingOtherItem.size())
            .forEach(ignored -> document.addField(field, otherItem.getID()));
    }

    private List<Relationship> otherRelationships(Context context, Relationship relationship,
                                                  boolean isLeftwardRelationship,
                                                  UUID itemId) throws SQLException {
        Item item = isLeftwardRelationship ? relationship.getRightItem() : relationship.getLeftItem();

        return relationshipService
            .findByItemAndRelationshipType(context, item, relationship.getRelationshipType(), !isLeftwardRelationship)
            .stream().filter(r -> !(r.getLeftItem().getID().equals(itemId) || r.getRightItem().getID().equals(itemId)))
            .collect(Collectors.toList());
    }

}
