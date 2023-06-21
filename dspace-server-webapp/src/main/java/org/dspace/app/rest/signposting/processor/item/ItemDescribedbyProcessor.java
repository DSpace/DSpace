/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor.item;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.app.rest.signposting.model.LinksetRelationType;
import org.dspace.app.rest.signposting.model.MetadataConfiguration;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.util.FrontendUrlService;

/**
 * An extension of {@link ItemSignpostingProcessor} for the describedby relation.
 */
public class ItemDescribedbyProcessor extends ItemSignpostingProcessor {

    private static final Logger log = Logger.getLogger(ItemDescribedbyProcessor.class);

    private List<MetadataConfiguration> metadataConfigurations;

    private final ItemService itemService;

    public ItemDescribedbyProcessor(FrontendUrlService frontendUrlService, ItemService itemService) {
        super(frontendUrlService);
        this.itemService = itemService;
        setRelation(LinksetRelationType.DESCRIBED_BY);
    }

    @Override
    public void addLinkSetNodes(Context context, HttpServletRequest request,
                                Item item, List<LinksetNode> linksetNodes) {
        getMetadataConfigurations()
                .forEach(metadataHandle -> handleMetadata(context, item, linksetNodes, metadataHandle));
    }

    private void handleMetadata(Context context,
                                Item item,
                                List<LinksetNode> linksetNodes,
                                MetadataConfiguration metadataConfiguration) {
        try {
            List<MetadataValue> identifiers = itemService
                    .getMetadataByMetadataString(item, metadataConfiguration.getMetadataField());
            for (MetadataValue identifier : identifiers) {
                if (nonNull(identifier)) {
                    String identifierValue = identifier.getValue();
                    if (isNotBlank(identifierValue)) {
                        if (isNotBlank(metadataConfiguration.getPattern())) {
                            identifierValue = MessageFormat.format(metadataConfiguration.getPattern(), identifierValue);
                        }
                        LinksetNode node = new LinksetNode(identifierValue, getRelation(),
                                metadataConfiguration.getMimeType(), buildAnchor(context, item));
                        linksetNodes.add(node);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public List<MetadataConfiguration> getMetadataConfigurations() {
        if (isNull(metadataConfigurations)) {
            metadataConfigurations = new ArrayList<>();
        }
        return metadataConfigurations;
    }

    public void setMetadataConfigurations(List<MetadataConfiguration> metadataConfigurations) {
        this.metadataConfigurations = metadataConfigurations;
    }
}
