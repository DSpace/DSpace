/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor.item;

import static org.dspace.content.Item.ANY;

import java.text.MessageFormat;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.app.rest.signposting.model.LinksetRelationType;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.dspace.util.FrontendUrlService;

/**
 * An extension of {@link ItemSignpostingProcessor} for the author relation.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.com)
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemAuthorProcessor extends ItemSignpostingProcessor {

    private static final String IS_AUTHOR_OF = "isAuthorOf";

    /**
     * log4j category
     */
    private static final Logger log = Logger.getLogger(ItemAuthorProcessor.class);

    private final ItemService itemService;

    private final RelationshipService relationshipService;

    private final EntityTypeService entityTypeService;

    private String orcidMetadata;

    public ItemAuthorProcessor(FrontendUrlService frontendUrlService,
                               ItemService itemService,
                               RelationshipService relationshipService,
                               EntityTypeService entityTypeService) {
        super(frontendUrlService);
        this.itemService = itemService;
        this.relationshipService = relationshipService;
        this.entityTypeService = entityTypeService;
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
            EntityType personType = entityTypeService.findByEntityType(context, "Author");
            List<Relationship> itemRelationships = relationshipService.findByItem(context, item);
            for (Relationship relationship : itemRelationships) {

                RelationshipType relationshipType = relationship.getRelationshipType();
                boolean hasPersonType = relationshipType.getLeftType().equals(personType)
                        || relationshipType.getRightType().equals(personType);
                boolean isAuthor = relationshipType.getLeftwardType().startsWith(IS_AUTHOR_OF)
                        || relationshipType.getRightwardType().startsWith(IS_AUTHOR_OF);

                if (hasPersonType && isAuthor) {
                    Item authorItem = relationship.getLeftItem().getID().equals(item.getID())
                            ? relationship.getRightItem()
                            : relationship.getLeftItem();

                    String authorOrcid = itemService.getMetadataFirstValue(
                            authorItem, new MetadataFieldName(getOrcidMetadata()), ANY
                    );
                    if (StringUtils.isNotBlank(authorOrcid)) {
                        String authorLink = StringUtils.isBlank(getPattern())
                                ? authorOrcid
                                : MessageFormat.format(getPattern(), authorOrcid);
                        linksetNodes.add(
                                new LinksetNode(authorLink, getRelation(), "text/html", buildAnchor(context, item))
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.error("Problem to add signposting pattern", e);
        }
    }
}
