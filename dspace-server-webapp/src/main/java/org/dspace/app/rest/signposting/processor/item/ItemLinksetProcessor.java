/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor.item;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.app.rest.signposting.model.LinksetRelationType;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.util.FrontendUrlService;

/**
 * An extension of {@link ItemSignpostingProcessor} for the linkset relation.
 */
public class ItemLinksetProcessor extends ItemSignpostingProcessor {

    private static final Logger log = Logger.getLogger(ItemLinksetProcessor.class);

    private final ConfigurationService configurationService;

    public ItemLinksetProcessor(FrontendUrlService frontendUrlService,
                                ConfigurationService configurationService) {
        super(frontendUrlService);
        this.configurationService = configurationService;
        setRelation(LinksetRelationType.LINKSET);
    }

    @Override
    public void addLinkSetNodes(Context context, HttpServletRequest request,
                                Item item, List<LinksetNode> linksetNodes) {
        try {
            if (item != null) {
                String signpostingPath = configurationService.getProperty("signposting.path");
                String baseUrl = configurationService.getProperty("dspace.ui.url");

                String linksetUrl = String.format("%s/%s/linksets/%s", baseUrl, signpostingPath, item.getID());
                String linksetJsonUrl = linksetUrl + "/json";
                String anchor = buildAnchor(context, item);
                List<LinksetNode> links = List.of(
                        new LinksetNode(linksetUrl, getRelation(), "application/linkset", anchor),
                        new LinksetNode(linksetJsonUrl, getRelation(), "application/linkset+json", anchor)
                );
                linksetNodes.addAll(links);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
