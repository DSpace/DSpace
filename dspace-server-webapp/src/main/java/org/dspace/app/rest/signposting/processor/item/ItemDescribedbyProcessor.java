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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.app.rest.signposting.model.LinksetRelationType;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.util.FrontendUrlService;

/**
 * An extension of {@link ItemSignpostingProcessor} for the describedby relation.
 */
public class ItemDescribedbyProcessor extends ItemSignpostingProcessor {

    private static final Logger log = LogManager.getLogger(ItemDescribedbyProcessor.class);

    private final ConfigurationService configurationService;

    public ItemDescribedbyProcessor(FrontendUrlService frontendUrlService, ConfigurationService configurationService) {
        super(frontendUrlService);
        this.configurationService = configurationService;
        setRelation(LinksetRelationType.DESCRIBED_BY);
    }

    @Override
    public void addLinkSetNodes(Context context, HttpServletRequest request,
                                Item item, List<LinksetNode> linksetNodes) {
        try {
            String signpostingPath = configurationService.getProperty("signposting.path");
            String baseUrl = configurationService.getProperty("dspace.ui.url");
            String mimeType = configurationService.getProperty("signposting.describedby.mime-type");
            String describedByUrl = baseUrl + "/" + signpostingPath + "/describedby/" + item.getID();
            LinksetNode node = new LinksetNode(describedByUrl, getRelation(), mimeType, buildAnchor(context, item));
            linksetNodes.add(node);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
