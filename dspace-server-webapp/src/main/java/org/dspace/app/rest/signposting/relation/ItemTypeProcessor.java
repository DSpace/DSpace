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
 * An implementation of {@link ItemSignPostingProcessor} for the type relation.
 */
public class ItemTypeProcessor extends ASignPostingProcessor
        implements ItemSignPostingProcessor {

    private static Logger log = Logger.getLogger(ItemTypeProcessor.class);

    @Autowired
    private ItemService itemService;

    public ItemTypeProcessor() {
        setRelation("type");
    }

    @Override
    public void buildRelation(Context context, HttpServletRequest request,
                              Item item, List<Linkset> linksets, Linkset primaryLinkset) {
        try {
            if (StringUtils.isNotBlank(getMetadataField())) {
                String itemType = itemService
                        .getMetadataFirstValue(item, new MetadataFieldName(getMetadataField()), Item.ANY);
                if (StringUtils.isNotBlank(itemType)) {
                    if (StringUtils.isNotBlank(getPattern())) {
                        itemType = MessageFormat.format(getPattern(), itemType);
                    }
                    primaryLinkset.getType().add(new Relation(itemType, null));
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
                String itemType = itemService
                        .getMetadataFirstValue(item, new MetadataFieldName(getMetadataField()), Item.ANY);
                if (StringUtils.isNotBlank(itemType)) {
                    if (StringUtils.isNotBlank(getPattern())) {
                        itemType = MessageFormat.format(getPattern(), itemType);
                    }
                    lsets.add(new Lset(itemType, getRelation(), buildAnchor(context, item)));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
