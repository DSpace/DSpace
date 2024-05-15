/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor.bitstream;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.app.rest.signposting.model.LinksetRelationType;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.util.FrontendUrlService;
import org.dspace.util.SimpleMapConverter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An extension of {@link BitstreamSignpostingProcessor} for the type relation.
 * Provides links to a specific type from schema.org.
 */
public class BitstreamTypeProcessor extends BitstreamSignpostingProcessor {

    private static final Logger log = Logger.getLogger(BitstreamTypeProcessor.class);

    @Autowired
    private SimpleMapConverter mapConverterDSpaceToSchemaOrgUri;

    @Autowired
    private BitstreamService bitstreamService;

    public BitstreamTypeProcessor(FrontendUrlService frontendUrlService) {
        super(frontendUrlService);
        setRelation(LinksetRelationType.TYPE);
    }

    @Override
    public void addLinkSetNodes(Context context, HttpServletRequest request,
                                Bitstream bitstream, List<LinksetNode> linksetNodes) {
        try {
            String type = bitstreamService.getMetadataFirstValue(bitstream, "dc", "type", null, Item.ANY);
            if (StringUtils.isNotBlank(type)) {
                String typeSchemeUri = mapConverterDSpaceToSchemaOrgUri.getValue(type);
                linksetNodes.add(new LinksetNode(typeSchemeUri, getRelation(), buildAnchor(bitstream)));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
