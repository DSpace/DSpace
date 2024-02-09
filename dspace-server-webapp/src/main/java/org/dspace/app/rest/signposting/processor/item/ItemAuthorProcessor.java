/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor.item;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.dspace.content.Item.ANY;

import java.text.MessageFormat;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.app.rest.signposting.model.LinksetRelationType;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.util.FrontendUrlService;

/**
 * An extension of {@link ItemSignpostingProcessor} for the author relation.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.com)
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemAuthorProcessor extends ItemSignpostingProcessor {

    /**
     * log4j category
     */
    private static final Logger log = Logger.getLogger(ItemAuthorProcessor.class);

    private final ItemService itemService;

    private String orcidMetadata;

    public ItemAuthorProcessor(FrontendUrlService frontendUrlService,
                               ItemService itemService) {
        super(frontendUrlService);
        this.itemService = itemService;
        setRelation(LinksetRelationType.AUTHOR);
    }

    public String getOrcidMetadata() {
        return orcidMetadata;
    }

    public void setOrcidMetadata(String orcidMetadata) {
        this.orcidMetadata = orcidMetadata;
    }

    @Override
    public void addLinkSetNodes(Context context, HttpServletRequest request,
                                Item item, List<LinksetNode> linksetNodes) {
        try {
            String authorId = itemService.getMetadataFirstValue(item, MetadataSchemaEnum.RELATION.getName(),
                    "isAuthorOfPublication", null, ANY);
            if (isNotBlank(authorId)) {
                Item author = itemService.findByIdOrLegacyId(context, authorId);
                if (nonNull(author)) {
                    String authorOrcid = itemService.getMetadataFirstValue(
                            author, new MetadataFieldName(getOrcidMetadata()), ANY
                    );
                    if (isNotBlank(authorOrcid)) {
                        String authorLink = isBlank(getPattern())
                                ? authorOrcid
                                : MessageFormat.format(getPattern(), authorOrcid);
                        linksetNodes.add(new LinksetNode(authorLink, getRelation(), buildAnchor(context, item)));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Problem to add signposting pattern", e);
        }
    }
}
