/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.relation;

import static org.dspace.content.Item.ANY;
import static org.dspace.content.MetadataSchemaEnum.PERSON;

import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.signposting.model.Linkset;
import org.dspace.app.rest.signposting.model.Lset;
import org.dspace.app.rest.signposting.model.Relation;
import org.dspace.app.rest.signposting.processor.ItemSignPostingProcessor;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.util.FrontendUrlService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An implementation of {@link ItemSignPostingProcessor} for the author relation.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.com)
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemAuthorProcessor extends ASignPostingProcessor
    implements ItemSignPostingProcessor {

    /** log4j category */
    private static Logger log = Logger.getLogger(ItemAuthorProcessor.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private FrontendUrlService frontendUrlService;

    private String retrievedExternally;

    public String getRetrievedExternally() {
        return retrievedExternally;
    }

    public void setRetrievedExternally(String retrievedExternally) {
        this.retrievedExternally = retrievedExternally;
    }

    public ItemAuthorProcessor() {
        setRelation("author");
    }

    @Override
    public void buildRelation(Context context, HttpServletRequest request,
                              Item item, List<Linkset> linksets, Linkset primaryLinkset) {
        try {
            List<MetadataValue> authors = itemService
                    .getMetadata(item, MetadataSchemaEnum.DC.getName(), "contributor", ANY, ANY);
            for (MetadataValue author : authors) {
                if (author.getAuthority() != null) {
                    String authorUuid = author.getAuthority();
                    Item authorItem = itemService.find(context, UUID.fromString(authorUuid));
                    String authorOrcid = itemService
                            .getMetadataFirstValue(authorItem, PERSON.getName(), "identifier", "orcid", ANY);
                    if (StringUtils.isNotBlank(authorOrcid)) {
                        String href = frontendUrlService.generateUrl(authorItem);
                        primaryLinkset.getAuthor().add(new Relation(href, authorOrcid));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Problem to add signposting pattern", e);
        }
    }

    @Override
    public void buildLset(Context context, HttpServletRequest request,
                          Item item, List<Lset> lsets) {
        try {
            List<MetadataValue> authors = itemService
                    .getMetadata(item, MetadataSchemaEnum.DC.getName(), "contributor", ANY, ANY);
            for (MetadataValue author : authors) {
                if (author.getAuthority() != null) {
                    String authorUuid = author.getAuthority();
                    Item authorItem = itemService.find(context, UUID.fromString(authorUuid));
                    String authorOrcid = itemService
                            .getMetadataFirstValue(authorItem, PERSON.getName(), "identifier", "orcid", ANY);
                    if (StringUtils.isNotBlank(authorOrcid)) {
                        String href = frontendUrlService.generateUrl(authorItem);
                        lsets.add(new Lset(href, getRelation(), authorOrcid, buildAnchor(context, item)));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Problem to add signposting pattern", e);
        }
    }

}
