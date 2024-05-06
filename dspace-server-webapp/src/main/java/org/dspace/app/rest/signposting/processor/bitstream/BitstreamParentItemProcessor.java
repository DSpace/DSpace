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
import org.dspace.util.FrontendUrlService;

/**
 * An extension of {@link BitstreamSignpostingProcessor} for the collection relation.
 * It links the Bitstream to the parent Item.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.com)
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class BitstreamParentItemProcessor extends BitstreamSignpostingProcessor {

    private static final Logger log = Logger.getLogger(BitstreamParentItemProcessor.class);

    private final BitstreamService bitstreamService;

    public BitstreamParentItemProcessor(FrontendUrlService frontendUrlService,
                                        BitstreamService bitstreamService) {
        super(frontendUrlService);
        this.bitstreamService = bitstreamService;
        setRelation(LinksetRelationType.COLLECTION);
    }

    @Override
    public void addLinkSetNodes(Context context, HttpServletRequest request,
                                Bitstream bitstream, List<LinksetNode> linksetNodes) {
        try {
            Item item = (Item) bitstreamService.getParentObject(context, bitstream);
            if (item != null) {
                String itemUiUrl = frontendUrlService.generateUrl(context, item);
                linksetNodes.add(new LinksetNode(itemUiUrl, getRelation(), "text/html", buildAnchor(bitstream)));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
