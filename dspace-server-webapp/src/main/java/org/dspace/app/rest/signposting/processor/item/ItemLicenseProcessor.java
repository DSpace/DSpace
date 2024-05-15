/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor.item;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.app.rest.signposting.model.LinksetRelationType;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.license.factory.LicenseServiceFactory;
import org.dspace.license.service.CreativeCommonsService;
import org.dspace.util.FrontendUrlService;

/**
 * An extension of {@link ItemSignpostingProcessor} for the license relation.
 */
public class ItemLicenseProcessor extends ItemSignpostingProcessor {

    private static final Logger log = Logger.getLogger(ItemLicenseProcessor.class);

    private final CreativeCommonsService creativeCommonsService =
            LicenseServiceFactory.getInstance().getCreativeCommonsService();

    public ItemLicenseProcessor(FrontendUrlService frontendUrlService) {
        super(frontendUrlService);
        setRelation(LinksetRelationType.LICENSE);
    }

    @Override
    public void addLinkSetNodes(Context context, HttpServletRequest request,
                                Item item, List<LinksetNode> linksetNodes) {
        try {
            String licenseUrl = creativeCommonsService.getLicenseURL(context, item);
            if (StringUtils.isNotBlank(licenseUrl)) {
                linksetNodes.add(new LinksetNode(licenseUrl, getRelation(), buildAnchor(context, item)));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
