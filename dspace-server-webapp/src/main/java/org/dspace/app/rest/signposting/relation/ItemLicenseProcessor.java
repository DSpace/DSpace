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
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An implementation of {@link ItemSignPostingProcessor} for the license relation.
 */
public class ItemLicenseProcessor extends ASignPostingProcessor
        implements ItemSignPostingProcessor {

    private static Logger log = Logger.getLogger(ItemLicenseProcessor.class);

    @Autowired
    private ItemService itemService;

    public ItemLicenseProcessor() {
        setRelation("license");
    }

    @Override
    public void buildRelation(Context context, HttpServletRequest request,
                              Item item, List<Linkset> linksets, Linkset primaryLinkset) {
        try {
            if (StringUtils.isNotEmpty(getMetadataField())) {
                String license = itemService
                        .getMetadataFirstValue(item, new MetadataFieldName(getMetadataField()), Item.ANY);
                if (StringUtils.isNotBlank(license)) {
                    if (StringUtils.isNotBlank(getPattern())) {
                        license = MessageFormat.format(getPattern(), license);
                    }
                    primaryLinkset.getLicense().add(new Relation(license, null));
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
            if (StringUtils.isNotEmpty(getMetadataField())) {
                String license = itemService
                        .getMetadataFirstValue(item, new MetadataFieldName(getMetadataField()), Item.ANY);
                if (StringUtils.isNotBlank(license)) {
                    if (StringUtils.isNotBlank(getPattern())) {
                        license = MessageFormat.format(getPattern(), license);
                    }
                    lsets.add(new Lset(license, getRelation(), buildAnchor(context, item)));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
