/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.relation;

import java.text.MessageFormat;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.signposting.model.Linkset;
import org.dspace.app.rest.signposting.model.Lset;
import org.dspace.app.rest.signposting.model.Relation;
import org.dspace.app.rest.signposting.processor.ItemSignPostingProcessor;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An implementation of {@link ItemSignPostingProcessor} for the author relation.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.com)
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemIdentifierProcessor extends ASignPostingProcessor
        implements ItemSignPostingProcessor {

    /**
     * log4j category
     */
    private static Logger log = Logger.getLogger(ItemIdentifierProcessor.class);

    @Autowired
    private ItemService itemService;

    public ItemIdentifierProcessor() {
        setRelation("cite-as");
    }

    @Override
    public void buildRelation(Context context, HttpServletRequest request,
                              Item item, List<Linkset> linksets, Linkset primaryLinkset) {
        try {
            List<MetadataValue> identifiers = itemService.getMetadataByMetadataString(item, getMetadataField());
            for (MetadataValue identifier : identifiers) {
                if (identifier != null) {
                    String identifierValue = identifier.getValue();
                    if (StringUtils.isNotBlank(identifierValue)) {
                        if (StringUtils.isNotBlank(getPattern())) {
                            identifierValue = MessageFormat.format(getPattern(), identifierValue);
                        }
                        primaryLinkset.getCiteAs().add(new Relation(identifierValue, null));
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void buildLset(Context context, HttpServletRequest request,
                          Item item, List<Lset> lsets) {
        try {
            List<MetadataValue> identifiers = itemService.getMetadataByMetadataString(item, getMetadataField());
            for (MetadataValue identifier : identifiers) {
                if (identifier != null) {
                    String identifierValue = identifier.getValue();
                    if (StringUtils.isNotBlank(identifierValue)) {
                        if (StringUtils.isNotBlank(getPattern())) {
                            identifierValue = MessageFormat.format(getPattern(), identifierValue);
                        }
                        lsets.add(new Lset(identifierValue, getRelation(), buildAnchor(context, item)));
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
