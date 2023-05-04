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
import org.dspace.app.rest.signposting.model.TypedLinkRest;
import org.dspace.app.rest.signposting.processor.ItemSignPostingProcessor;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.util.FrontendUrlService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An implementation of {@link ItemSignPostingProcessor} for the Landing Page relation.
 */
public class ItemLandingPageProcessor extends ASignPostingProcessor implements ItemSignPostingProcessor {

    private static Logger log = Logger.getLogger(ItemLandingPageProcessor.class);

    @Autowired
    private FrontendUrlService frontendUrlService;

    public ItemLandingPageProcessor() {
        setRelation(TypedLinkRest.Relation.LANDING_PAGE.getName());
    }

    @Override
    public void buildRelation(Context context, HttpServletRequest request,
                              Item item, List<Linkset> linksets, Linkset primaryLinkset) {
        try {
            String landingPageUrl = frontendUrlService.generateUrl(item);
            primaryLinkset.getLandingPage().add(new Relation(landingPageUrl, "text/html"));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void buildLset(Context context, HttpServletRequest request, Item item, List<Lset> lsets) {
        try {
            String landingPageUrl = frontendUrlService.generateUrl(item);
            lsets.add(new Lset(landingPageUrl, getRelation(), "text/html", buildAnchor(context, item)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
