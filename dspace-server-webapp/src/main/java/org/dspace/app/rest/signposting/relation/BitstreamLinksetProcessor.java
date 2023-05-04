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
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An implementation of {@link ItemSignPostingProcessor} for the linkset relation.
 */
public class BitstreamLinksetProcessor extends ASignPostingProcessor
        implements BitstreamSignPostingProcessor {

    private static Logger log = Logger.getLogger(BitstreamLinksetProcessor.class);

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private ConfigurationService configurationService;

    public BitstreamLinksetProcessor() {
        setRelation("linkset");
    }

    @Override
    public void buildRelation(Context context, HttpServletRequest request,
                              Bitstream bitstream, List<Linkset> linksets,
                              Linkset primaryLinkset) {
        try {
            Item item = (Item) bitstreamService.getParentObject(context, bitstream);
            if (item != null) {
                String baseUrl = configurationService.getProperty("dspace.server.url");
                String linksetUrl = baseUrl + "/signposting/linksets/" + item.getID();
                String linksetJsonUrl = baseUrl + "/signposting/linksets/" + item.getID() + "/json";
                List<Relation> relations = List.of(
                        new Relation(linksetUrl, "application/linkset"),
                        new Relation(linksetJsonUrl, "application/linkset+json")
                );
                primaryLinkset.getLinkset().addAll(relations);
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
                String baseUrl = configurationService.getProperty("dspace.server.url");
                String linksetUrl = baseUrl + "/signposting/linksets/" + item.getID();
                String linksetJsonUrl = baseUrl + "/signposting/linksets/" + item.getID() + "/json";
                List<Lset> links = List.of(
                        new Lset(linksetUrl, getRelation(), "application/linkset", buildAnchor(bitstream)),
                        new Lset(linksetJsonUrl, getRelation(), "application/linkset+json", buildAnchor(bitstream))
                );
                lsets.addAll(links);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
