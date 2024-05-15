/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor.item;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.app.rest.signposting.model.LinksetRelationType;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.util.FrontendUrlService;
import org.dspace.util.SimpleMapConverter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An extension of {@link ItemSignpostingProcessor} for the type relation.
 * Provides links to a specific type from schema.org.
 */
public class ItemTypeProcessor extends ItemSignpostingProcessor {

    private static final Logger log = Logger.getLogger(ItemTypeProcessor.class);
    private static final String ABOUT_PAGE_URI = "https://schema.org/AboutPage";

    @Autowired
    private SimpleMapConverter mapConverterDSpaceToSchemaOrgUri;

    @Autowired
    private ItemService itemService;

    public ItemTypeProcessor(FrontendUrlService frontendUrlService) {
        super(frontendUrlService);
        setRelation(LinksetRelationType.TYPE);
    }

    @Override
    public void addLinkSetNodes(Context context, HttpServletRequest request,
                                Item item, List<LinksetNode> linksetNodes) {
        try {
            linksetNodes.add(new LinksetNode(ABOUT_PAGE_URI, getRelation(), buildAnchor(context, item)));
            String type = itemService.getMetadataFirstValue(item, "dc", "type", null, Item.ANY);
            if (StringUtils.isNotBlank(type)) {
                String typeSchemeUri = mapConverterDSpaceToSchemaOrgUri.getValue(type);
                linksetNodes.add(
                        new LinksetNode(typeSchemeUri, getRelation(), buildAnchor(context, item))
                );
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
