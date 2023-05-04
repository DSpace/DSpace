/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.relation;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.rest.signposting.model.Linkset;
import org.dspace.app.rest.signposting.model.Lset;
import org.dspace.app.rest.signposting.model.Relation;
import org.dspace.app.rest.signposting.processor.BitstreamSignPostingProcessor;
import org.dspace.app.rest.signposting.processor.ItemSignPostingProcessor;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.util.FrontendUrlService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An implementation of {@link ItemSignPostingProcessor} for the collection relation.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.com)
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class BitstreamPublicationBundaryProcessor extends ASignPostingProcessor
        implements BitstreamSignPostingProcessor {

    private static Logger log = Logger.getLogger(BitstreamPublicationBundaryProcessor.class);

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private FrontendUrlService frontendUrlService;

    public BitstreamPublicationBundaryProcessor() {
        setRelation("collection");
    }

    @Override
    public void buildRelation(Context context, HttpServletRequest request,
                              Bitstream bitstream, List<Linkset> linksets,
                              Linkset primaryLinkset) {
        try {
            Item item = (Item) bitstreamService.getParentObject(context, bitstream);
            if (item != null) {
                String itemUiUrl = frontendUrlService.generateUrl(item);
                primaryLinkset.getCollection().add(new Relation(itemUiUrl, "text/html"));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void buildLset(Context context, HttpServletRequest request,
                          Bitstream bitstream, List<Lset> lsets) {
        try {
            Item item = (Item) bitstreamService.getParentObject(context, bitstream);
            if (item != null) {
                String itemUiUrl = frontendUrlService.generateUrl(item);
                lsets.add(new Lset(itemUiUrl, getRelation(), "text/html", buildAnchor(bitstream)));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
