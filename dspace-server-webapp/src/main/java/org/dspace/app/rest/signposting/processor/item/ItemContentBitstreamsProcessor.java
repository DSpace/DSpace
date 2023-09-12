/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor.item;

import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.app.rest.signposting.model.LinksetRelationType;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.util.FrontendUrlService;

/**
 * An extension of {@link ItemSignpostingProcessor} for the item relation.
 * It links item with its content.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.com)
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemContentBitstreamsProcessor extends ItemSignpostingProcessor {

    /**
     * log4j category
     */
    private static final Logger log = Logger.getLogger(ItemContentBitstreamsProcessor.class);

    public ItemContentBitstreamsProcessor(FrontendUrlService frontendUrlService) {
        super(frontendUrlService);
        setRelation(LinksetRelationType.ITEM);
    }

    @Override
    public void addLinkSetNodes(Context context, HttpServletRequest request,
                                Item item, List<LinksetNode> linksetNodes) {
        try {
            for (Bundle bundle : item.getBundles(Constants.CONTENT_BUNDLE_NAME)) {
                for (Bitstream bitstream : bundle.getBitstreams()) {
                    String mimeType = bitstream.getFormat(context).getMIMEType();
                    String bitstreamUrl = frontendUrlService.generateUrl(bitstream);
                    linksetNodes.add(
                            new LinksetNode(bitstreamUrl, getRelation(), mimeType, buildAnchor(context, item))
                    );
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

}
