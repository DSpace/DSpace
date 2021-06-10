/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link VirtualField} that elaborate a controlled-vocabulary
 * originated metadata to take a specific section of its value.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class VirtualFieldVocabulary implements VirtualField {

    private final static Logger LOGGER = LoggerFactory.getLogger(VirtualFieldVocabulary.class);

    private final static String LAST_ELEMENT = "LEAF";

    private ItemService itemService;

    public VirtualFieldVocabulary(ItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {

        String[] virtualFieldName = fieldName.split("\\.", 4);

        if (virtualFieldName.length != 4) {
            LOGGER.warn("Invalid vocabulary virtual field: " + fieldName);
            return new String[] {};
        }

        String metadataField = virtualFieldName[2].replaceAll("-", ".");
        String position = virtualFieldName[3].toUpperCase();

        return itemService.getMetadataByMetadataString(item, metadataField).stream()
            .filter(metadataValue -> metadataValue.getValue() != null)
            .flatMap(metadataValue -> getElementAtPosition(metadataValue.getValue(), position).stream())
            .toArray(String[]::new);

    }

    private Optional<String> getElementAtPosition(String controlledVocabulary, String position) {
        String[] elements = controlledVocabulary.split("::");
        int index = calculateIndex(elements.length, position);
        return index >= 0 ? Optional.of(elements[index]) : Optional.empty();
    }

    private int calculateIndex(int length, String position) {

        if (position.equals(LAST_ELEMENT)) {
            return length - 1;
        }

        if (position.startsWith(LAST_ELEMENT + "-")) {
            return calculateIndexFromLeaf(length, StringUtils.removeStart(position, LAST_ELEMENT + "-"));
        }

        return calculateIndexFromRoot(length, position);
    }

    private int calculateIndexFromLeaf(int length, String position) {
        if (isInvalidOrNegativeNumber(position)) {
            return -1;
        }
        int index = Integer.valueOf(position);
        return index >= length ? 0 : (length - 1) - index;
    }

    private int calculateIndexFromRoot(int length, String position) {
        if (isInvalidOrNegativeNumber(position)) {
            return -1;
        }
        int index = Integer.valueOf(position);
        return index >= length ? length - 1 : index;
    }

    private boolean isInvalidOrNegativeNumber(String position) {
        try {
            return Integer.valueOf(position) < 0;
        } catch (NumberFormatException ex) {
            LOGGER.warn("invalid position specified for vocabulary virtual field: {}", position);
            return true;
        }
    }

}
