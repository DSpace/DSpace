/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link SolrServiceIndexPlugin} that adds item relationships informations
 * to solr document.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class SolrServiceIndexRelationshipsPlugin implements SolrServiceIndexPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrServiceIndexRelationshipsPlugin.class);

    private final RelationshipService relationshipService;

    @Autowired
    public SolrServiceIndexRelationshipsPlugin(RelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    @Override
    public void additionalIndex(Context context, IndexableObject indexableObject, SolrInputDocument document) {


        Optional<Item> item = item(indexableObject);

        item.ifPresent(i -> {
            try {

                tryToUpdateDocument(context, document, i);

            } catch (SQLException e) {
                LOGGER.error("An error occurred during relations indexing", e);
            }
        });


    }

    private Optional<Item> item(IndexableObject indexableObject) {
        if (!(indexableObject instanceof IndexableItem)) {
            return Optional.empty();
        }
        return Optional.ofNullable(((IndexableItem) indexableObject).getIndexedObject());
    }

    private void tryToUpdateDocument(Context context, SolrInputDocument document, Item item) throws SQLException {

        for (Relationship relationship : relationshipService.findByItem(context, item)) {

            DirectionalRelationship directionalRelationship = DirectionalRelationship.from(relationship, item);

            directionalRelationship.addFields(context, document, relationshipService);
        }
    }

    private static class DirectionalRelationship {

        private final Relationship relationship;
        private final Direction direction;
        private final Item sourceItem;

        private DirectionalRelationship(Relationship relationship,
                                        Direction direction,
                                        Item sourceItem) {
            this.relationship = relationship;
            this.direction = direction;
            this.sourceItem = sourceItem;
        }

        static DirectionalRelationship from(Relationship relationship, Item item) {
            return new DirectionalRelationship(relationship,
                Direction.from(relationship, item), item);
        }

        void addFields(Context context, SolrInputDocument document,
                       RelationshipService relationshipService) throws SQLException {
            addField(document);
            fillPositions(context, document, relationshipService);
        }

        private void fillPositions(Context context, SolrInputDocument document, RelationshipService relationshipService)
            throws SQLException {

            int relationshipPlace = this.direction.place(this.relationship);

            int otherRelationsWithTargetItem = targetItemRelationships(context, relationship, relationshipService)
                .size();
            String targetItemId = UUIDUtils.toString(targetItem().getID());

            IntStream.range(relationshipPlace, otherRelationsWithTargetItem)
                .forEach(ignored -> document.addField(fieldName(), targetItemId));
        }

        private void addField(SolrInputDocument document) {

            String value = UUIDUtils.toString(targetItem().getID());
            document.addField(fieldName(), value);
        }

        private Item targetItem() {
            return direction.targetItem(this.relationship);
        }

        private String fieldName() {
            return String.format("relation.%s", direction.label(this.relationship));
        }

        private List<Relationship> targetItemRelationships(Context context, Relationship relationship,
                                                           RelationshipService relationshipService)
            throws SQLException {

            Predicate<Relationship> notLeftSource = r -> !r.getLeftItem().getID().equals(sourceItem.getID());
            Predicate<Relationship> notRightSource = r -> !r.getRightItem().getID().equals(sourceItem.getID());
            Predicate<Relationship> notInvolvingSourceItem = notLeftSource.and(notRightSource);

            return direction.findRelationsWithSameTargetItem(context, relationship, relationshipService)
                .stream()
                .filter(notInvolvingSourceItem)
                .collect(Collectors.toList());
        }

        private enum Direction {
            LEFT(Relationship::getLeftItem,
                RelationshipType::getLeftwardType,
                Relationship::getRightItem,
                Relationship::getLeftPlace),

            RIGHT(Relationship::getRightItem,
                RelationshipType::getRightwardType,
                Relationship::getLeftItem,
                Relationship::getRightPlace);

            private final Function<Relationship, Item> sourceItem;
            private final Function<RelationshipType, String> type;
            private final Function<Relationship, Item> targetItem;
            private final Function<Relationship, Integer> place;

            Direction(
                Function<Relationship, Item> sourceItem,
                Function<RelationshipType, String> type,
                Function<Relationship, Item> targetItem,
                Function<Relationship, Integer> place) {
                this.sourceItem = sourceItem;
                this.type = type;
                this.targetItem = targetItem;
                this.place = place;
            }

            static Direction from(Relationship relationship, Item item) {
                return Arrays.stream(values())
                    .filter(direction -> direction.appliesTo(relationship, item))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Provided item is not part of the relationship"));
            }

            String label(Relationship relationship) {
                return type.apply(relationship.getRelationshipType());
            }

            Item targetItem(Relationship relationship) {
                return targetItem.apply(relationship);
            }

            int place(Relationship relationship) {
                return place.apply(relationship);
            }


            Collection<Relationship> findRelationsWithSameTargetItem(Context context,
                                                                     Relationship relationship,
                                                                     RelationshipService relationshipService)
                throws SQLException {
                return relationshipService
                    .findByItemAndRelationshipType(context,
                        targetItem(relationship),
                        relationship.getRelationshipType(),
                        RIGHT.equals(this));
            }

            private boolean appliesTo(Relationship relationship, Item item) {
                return sourceItem.apply(relationship).getID().equals(item.getID());
            }
        }

    }

}
