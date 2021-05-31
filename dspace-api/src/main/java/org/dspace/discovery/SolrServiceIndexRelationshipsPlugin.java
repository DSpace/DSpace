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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.services.ConfigurationService;
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
    private final ConfigurationService configurationService;

    @Autowired
    public SolrServiceIndexRelationshipsPlugin(RelationshipService relationshipService,
                                               final ConfigurationService configurationService) {
        this.relationshipService = relationshipService;
        this.configurationService = configurationService;
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

            directionalRelationship.addFields(context, document, configurationService);
        }
    }

    private static class DirectionalRelationship {

        private final Relationship relationship;
        private final Direction direction;

        private DirectionalRelationship(Relationship relationship,
                                        Direction direction) {
            this.relationship = relationship;
            this.direction = direction;
        }

        static DirectionalRelationship from(Relationship relationship, Item item) {
            return new DirectionalRelationship(relationship,
                                               Direction.from(relationship, item));
        }

        void addFields(Context context, SolrInputDocument document,
                       final ConfigurationService configurationService) throws SQLException {
            addField(document);
            fillPositions(context, document, configurationService);
        }

        private void fillPositions(Context context, SolrInputDocument document ,
                                   ConfigurationService configurationService) {

            int otherRelationsWithTargetItem;
            int relationshipPlace = this.direction.place(this.relationship);

            if (direction.isPlaceAppliedOnOneSide(this.relationship, configurationService)) {
                otherRelationsWithTargetItem = direction.maxRelationPlace(this.relationship) - 1;
                String targetItemId = UUIDUtils.toString(targetItem().getID());
                IntStream.range(relationshipPlace, otherRelationsWithTargetItem)
                         .forEach(ignored -> document.addField(fieldName(), targetItemId));
            }
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

        private enum Direction {
            LEFT(Relationship::getLeftItem,
                 RelationshipType::getLeftwardType,
                 Relationship::getRightItem,
                 Relationship::getRightPlace,
                 Relationship::getLeftPlace,
                 "right"),

            RIGHT(Relationship::getRightItem,
                  RelationshipType::getRightwardType,
                  Relationship::getLeftItem,
                  Relationship::getLeftPlace,
                  Relationship::getRightPlace,
                  "left");

            private final Function<Relationship, Item> sourceItem;
            private final Function<RelationshipType, String> type;
            private final Function<Relationship, Item> targetItem;
            private final Function<Relationship, Integer> place;
            private final Function<Relationship, Integer> maxPlaces;
            private final String singleDirectionPlacesLabel;

            Direction(
                Function<Relationship, Item> sourceItem,
                Function<RelationshipType, String> type,
                Function<Relationship, Item> targetItem,
                Function<Relationship, Integer> place,
                final Function<Relationship, Integer> maxPlaces,
                final String singleDirectionPlacesLabel) {
                this.sourceItem = sourceItem;
                this.type = type;
                this.targetItem = targetItem;
                this.place = place;
                this.maxPlaces = maxPlaces;
                this.singleDirectionPlacesLabel = singleDirectionPlacesLabel;
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

            boolean isPlaceAppliedOnOneSide(final Relationship relationship,
                                                   final ConfigurationService configurationService) {

                final RelationshipType relationshipType = relationship.getRelationshipType();
                final String leftTypeLabel = Optional.ofNullable(relationshipType.getLeftType())
                                                     .map(EntityType::getLabel).orElse("null");
                final String rightTypeLabel = Optional.ofNullable(relationshipType.getRightType())
                                                      .map(EntityType::getLabel).orElse("null");
                final String[] relationshipPlaceSettings = configurationService
                                                               .getArrayProperty("relationship.places.only" +
                                                                                     singleDirectionPlacesLabel);
                if (relationshipPlaceSettings == null) {
                    return false;
                }
                return Arrays.stream(relationshipPlaceSettings)
                             .anyMatch(v -> v.equals(String.join("::",
                                                                 leftTypeLabel,
                                                                 rightTypeLabel,
                                                                 relationshipType.getLeftwardType(),
                                                                 relationshipType
                                                                             .getRightwardType())));
            }

            public int maxRelationPlace(final Relationship relationship) {
                return maxPlaces.apply(relationship);
            }
        }

    }

}
