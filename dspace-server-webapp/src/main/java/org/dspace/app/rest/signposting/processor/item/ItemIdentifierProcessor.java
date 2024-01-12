/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor.item;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.text.MessageFormat;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.app.rest.signposting.model.LinksetRelationType;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.util.FrontendUrlService;

/**
 * An extension of {@link ItemSignpostingProcessor} for the identifier relation.
 * Identifier metadata can be specified with <code>metadataField</code> in configuration.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.com)
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemIdentifierProcessor extends ItemSignpostingProcessor {

    private final ItemService itemService;

    public ItemIdentifierProcessor(FrontendUrlService frontendUrlService, ItemService itemService) {
        super(frontendUrlService);
        this.itemService = itemService;
        setRelation(LinksetRelationType.CITE_AS);
    }

    @Override
    public void addLinkSetNodes(Context context, HttpServletRequest request,
                                Item item, List<LinksetNode> linksetNodes) {
        String identifier = itemService
                .getMetadataFirstValue(item, new MetadataFieldName(getMetadataField()), Item.ANY);
        if (nonNull(identifier)) {
            if (isNotBlank(getPattern())) {
                identifier = MessageFormat.format(getPattern(), item);
            }
            linksetNodes.add(new LinksetNode(identifier, getRelation(), buildAnchor(context, item)));
        }
    }
}
