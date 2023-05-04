/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.relation;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.rest.signposting.model.Linkset;
import org.dspace.app.rest.signposting.model.Lset;
import org.dspace.app.rest.signposting.model.Relation;
import org.dspace.app.rest.signposting.processor.ItemSignPostingProcessor;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An implementation of {@link ItemSignPostingProcessor} for the item relation.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.com)
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemPublicationBundaryProcessor extends ASignPostingProcessor
        implements ItemSignPostingProcessor {

    /**
     * log4j category
     */
    private static Logger log = Logger
            .getLogger(ItemPublicationBundaryProcessor.class);

    @Autowired
    private ConfigurationService configurationService;

    public ItemPublicationBundaryProcessor() {
        setRelation("item");
    }

    @Override
    public void buildRelation(Context context, HttpServletRequest request,
                              Item item, List<Linkset> linksets, Linkset primaryLinkset) {
        String url = configurationService.getProperty("dspace.ui.url");
        try {
            for (Bundle bundle : item.getBundles(Constants.CONTENT_BUNDLE_NAME)) {
                for (Bitstream bitstream : bundle.getBitstreams()) {
                    String mimeType = bitstream.getFormat(context).getMIMEType();
                    primaryLinkset.getItem().add(
                            new Relation(
                                    MessageFormat.format(getPattern(),
                                            url, "bitstreams", bitstream.getID() + "/download"), mimeType));
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void buildLset(Context context, HttpServletRequest request,
                          Item item, List<Lset> lsets) {
        String url = configurationService.getProperty("dspace.ui.url");
        try {
            for (Bundle bundle : item.getBundles(Constants.CONTENT_BUNDLE_NAME)) {
                for (Bitstream bitstream : bundle.getBitstreams()) {
                    String mimeType = bitstream.getFormat(context).getMIMEType();
                    lsets.add(new Lset(MessageFormat.format(getPattern(),
                            url, "bitstreams", bitstream.getID() + "/download"),
                            getRelation(), mimeType, buildAnchor(context, item)));
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

}
