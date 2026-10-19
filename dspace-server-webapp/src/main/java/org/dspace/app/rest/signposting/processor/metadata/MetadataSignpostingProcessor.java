/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor.metadata;

import org.dspace.app.rest.signposting.processor.AbstractSignPostingProcessor;
import org.dspace.app.rest.signposting.processor.SignPostingProcessor;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * An abstract class represents {@link SignPostingProcessor } for a metadata.
 */
public abstract class MetadataSignpostingProcessor extends AbstractSignPostingProcessor
        implements SignPostingProcessor<Item> {

    private final ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();

    public String buildAnchor(Item item) {
        String baseUrl = configurationService.getProperty("dspace.ui.url");
        String signpostingPath = configurationService.getProperty("signposting.path");
        return baseUrl + "/" + signpostingPath + "/describedby/" + item.getID();
    }
}
