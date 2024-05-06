/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor.bitstream;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.app.rest.signposting.model.LinksetRelationType;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.util.FrontendUrlService;

/**
 * An extension of {@link BitstreamSignpostingProcessor} for the linkset relation.
 */
public class BitstreamLinksetProcessor extends BitstreamSignpostingProcessor {

    private static final Logger log = Logger.getLogger(BitstreamLinksetProcessor.class);

    private final BitstreamService bitstreamService;

    private final ConfigurationService configurationService;

    public BitstreamLinksetProcessor(FrontendUrlService frontendUrlService,
                                     BitstreamService bitstreamService,
                                     ConfigurationService configurationService) {
        super(frontendUrlService);
        this.bitstreamService = bitstreamService;
        this.configurationService = configurationService;
        setRelation(LinksetRelationType.LINKSET);
    }

    @Override
    public void addLinkSetNodes(Context context, HttpServletRequest request,
                                Bitstream bitstream, List<LinksetNode> linksetNodes) {
        try {
            Item item = (Item) bitstreamService.getParentObject(context, bitstream);
            if (item != null) {
                String signpostingPath = configurationService.getProperty("signposting.path");
                String baseUrl = configurationService.getProperty("dspace.ui.url");

                String linksetUrl = String.format("%s/%s/linksets/%s", baseUrl, signpostingPath, item.getID());
                String linksetJsonUrl = linksetUrl + "/json";
                List<LinksetNode> links = List.of(
                        new LinksetNode(linksetUrl, getRelation(), "application/linkset", buildAnchor(bitstream)),
                        new LinksetNode(linksetJsonUrl, getRelation(), "application/linkset+json",
                                buildAnchor(bitstream))
                );
                linksetNodes.addAll(links);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
