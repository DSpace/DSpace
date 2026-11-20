/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields.postprocessors;

import static java.util.Arrays.asList;
import static org.apache.commons.collections4.CollectionUtils.containsAny;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.integration.crosswalks.csl.CSLResult;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link VirtualFieldCitationsPostProcessor} that moves on
 * the first positions the citations related to publications selected by the
 * given profile item.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class FirstSelectedCitationsPostProcessor implements VirtualFieldCitationsPostProcessor {

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Override
    public CSLResult process(Context context, Item item, CSLResult cslResult) {

        UUID[] itemIds = cslResult.getItemIds();

        if (itemIds.length == 0) {
            return cslResult;
        }

        List<UUID> selectedItems = getSelectedItems(context, item);
        if (selectedItems.isEmpty() || !containsAny(selectedItems, asList(itemIds))) {
            return cslResult;
        }

        return moveSelectedItemCitationsOnTop(selectedItems, cslResult);
    }

    /**
     * Example: itemIds [A,B,C,D,E,F,G] and selectedItems [D,G] produces [D,G,A,B,C,E,F]
     */
    private CSLResult moveSelectedItemCitationsOnTop(List<UUID> selectedItems, CSLResult cslResult) {

        String[] citationEntries = cslResult.getCitationEntries();
        UUID[] itemIds = cslResult.getItemIds();

        List<Integer> selectedItemindexes = getIndexesOfSelectedItems(selectedItems, itemIds);

        String[] newCitationEntries = new String[citationEntries.length];
        String[] newItemIds = new String[itemIds.length];

        int newArraysIndex = 0;
        for (Integer selectedItemindex : selectedItemindexes) {
            newCitationEntries[newArraysIndex] = citationEntries[selectedItemindex];
            newItemIds[newArraysIndex] = itemIds[selectedItemindex].toString();
            newArraysIndex++;
        }

        for (int i = 0; i < itemIds.length; i++) {
            if (!selectedItemindexes.contains(i)) {
                newCitationEntries[newArraysIndex] = citationEntries[i];
                newItemIds[newArraysIndex] = itemIds[i].toString();
                newArraysIndex++;
            }
        }

        return new CSLResult(cslResult.getFormat(), newItemIds, newCitationEntries);
    }

    @Override
    public String getName() {
        return "first-selected";
    }

    /**
     * Get the indexes of the selected items in the array of the item ids related to
     * the citation, keeping the order established by the selection.
     */
    private List<Integer> getIndexesOfSelectedItems(List<UUID> selectedItems, UUID[] itemIds) {
        return selectedItems.stream()
            .flatMap(seletedItemId -> getItemIdIndex(seletedItemId, itemIds).stream())
            .collect(Collectors.toList());
    }

    private Optional<Integer> getItemIdIndex(UUID itemId, UUID[] itemIds) {
        for (int i = 0; i < itemIds.length; i++) {
            if (itemIds[i].equals(itemId)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private List<UUID> getSelectedItems(Context context, Item item) {

        RelationshipType relationshipType = findHasSelectedResearchoutputsRelationshipType(context);
        if (relationshipType == null) {
            return List.of();
        }

        try {
            return relationshipService.findByItemAndRelationshipType(context, item, relationshipType).stream()
                .sorted(Comparator.comparing(Relationship::getRightPlace))
                .map(Relationship::getLeftItem)
                .map(Item::getID)
                .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private RelationshipType findHasSelectedResearchoutputsRelationshipType(Context context) {

        try {

            // main requires both entity types on a relationship type, so the
            // CRIS lookup by (null leftType, Person rightType) cannot match. Identify the
            // relationship type by its type names instead, which is entity-type agnostic.
            return relationshipTypeService.findByLeftwardOrRightwardTypeName(context,
                    "isResearchoutputsSelectedFor")
                .stream()
                .filter(relationshipType -> "hasSelectedResearchoutputs"
                    .equals(relationshipType.getRightwardType()))
                .findFirst()
                .orElse(null);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

}
