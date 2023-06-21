/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor.metadata;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.dspace.content.Item.ANY;

import java.text.MessageFormat;

import org.apache.logging.log4j.util.Strings;
import org.dspace.app.rest.signposting.model.MetadataConfiguration;
import org.dspace.app.rest.signposting.processor.AbstractSignPostingProcessor;
import org.dspace.app.rest.signposting.processor.SignPostingProcessor;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;

/**
 * An abstract class represents {@link SignPostingProcessor } for a metadata.
 */
public abstract class MetadataSignpostingProcessor extends AbstractSignPostingProcessor
        implements SignPostingProcessor<Item> {

    protected final ItemService itemService;

    public MetadataSignpostingProcessor(ItemService itemService) {
        this.itemService = itemService;
    }

    public String buildAnchor(MetadataConfiguration metadataConfiguration, Item item) {
        String metadataValue = itemService
                .getMetadataFirstValue(item, new MetadataFieldName(metadataConfiguration.getMetadataField()), ANY);
        if (isNotBlank(metadataValue)) {
            return isNotBlank(metadataConfiguration.getPattern())
                    ? MessageFormat.format(metadataConfiguration.getPattern(), metadataValue)
                    : metadataValue;
        }
        return Strings.EMPTY;
    }
}
