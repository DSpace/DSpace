/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor.metadata;

import static org.dspace.content.Item.ANY;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.app.rest.signposting.model.LinksetRelationType;
import org.dspace.app.rest.signposting.model.MetadataConfiguration;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.util.FrontendUrlService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An extension of {@link MetadataSignpostingProcessor} for the 'describes' relation.
 */
public class MetadataDescribesSignpostingProcessor extends MetadataSignpostingProcessor {

    @Autowired
    private FrontendUrlService frontendUrlService;

    public MetadataDescribesSignpostingProcessor(ItemService itemService) {
        super(itemService);
        setRelation(LinksetRelationType.DESCRIBES);
    }

    @Override
    public void addLinkSetNodes(
            Context context,
            HttpServletRequest request,
            Item item,
            List<LinksetNode> linksetNodes
    ) {
        String metadataValue = itemService.getMetadataFirstValue(item, new MetadataFieldName(getMetadataField()), ANY);
        if (StringUtils.isNotBlank(metadataValue)) {
            String itemUrl = frontendUrlService.generateUrl(context, item);
            String anchor = buildAnchor(new MetadataConfiguration(getMetadataField(), getPattern()), item);
            linksetNodes.add(new LinksetNode(itemUrl, getRelation(), "text/html", anchor));
        }
    }
}
