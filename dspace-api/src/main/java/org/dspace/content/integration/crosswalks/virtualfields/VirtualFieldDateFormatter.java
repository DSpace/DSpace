/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.util.MultiFormatDateParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link VirtualField} that format a date present on a
 * specific metadata or the current timestamp.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class VirtualFieldDateFormatter implements VirtualField {

    private final static Logger LOGGER = LoggerFactory.getLogger(VirtualFieldDateFormatter.class);

    private final static String CURRENT_TIMESTAMP = "TIMESTAMP";

    private final ItemService itemService;

    public VirtualFieldDateFormatter(ItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {

        String[] virtualFieldName = fieldName.split("\\.", 4);

        if (virtualFieldName.length != 4) {
            LOGGER.warn("Invalid date formatter virtual field: " + fieldName);
            return new String[] {};
        }

        String fieldToFormat = virtualFieldName[2];
        String pattern = virtualFieldName[3];

        return getDates(item, fieldToFormat)
            .flatMap(dateToFormat -> formatDate(dateToFormat, pattern).stream())
            .toArray(String[]::new);

    }

    private Stream<ZonedDateTime> getDates(Item item, String fieldToFormat) {
        if (CURRENT_TIMESTAMP.equalsIgnoreCase(fieldToFormat)) {
            return Stream.of(ZonedDateTime.now());
        }

        String metadataField = fieldToFormat.replaceAll("-", ".");
        return itemService.getMetadataByMetadataString(item, metadataField).stream()
            .filter(metadataValue -> metadataValue.getValue() != null)
            .map(metadataValue -> MultiFormatDateParser.parse(metadataValue.getValue()))
                          .filter(Objects::nonNull);
    }

    private Optional<String> formatDate(ZonedDateTime dateToFormat, String pattern) {
        try {
            return Optional.of(DateTimeFormatter.ofPattern(pattern).format(dateToFormat));
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("Invalid pattern specified for date formatter virtual field: " + pattern, ex);
            return Optional.empty();
        }
    }

}
