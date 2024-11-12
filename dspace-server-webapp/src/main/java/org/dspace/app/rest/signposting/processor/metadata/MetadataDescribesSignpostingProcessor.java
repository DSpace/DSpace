/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor.metadata;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.app.rest.signposting.model.LinksetRelationType;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.util.FrontendUrlService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An extension of {@link MetadataSignpostingProcessor} for the 'describes' relation.
 */
public class MetadataDescribesSignpostingProcessor extends MetadataSignpostingProcessor {

    @Autowired
    private FrontendUrlService frontendUrlService;

    public MetadataDescribesSignpostingProcessor() {
        setRelation(LinksetRelationType.DESCRIBES);
    }

    @Override
    public void addLinkSetNodes(
            Context context,
            HttpServletRequest request,
            Item item,
            List<LinksetNode> linksetNodes
    ) {
        String itemUrl = frontendUrlService.generateUrl(context, item);
        String anchor = buildAnchor(item);
        linksetNodes.add(new LinksetNode(itemUrl, getRelation(), "text/html", anchor));
    }
}
