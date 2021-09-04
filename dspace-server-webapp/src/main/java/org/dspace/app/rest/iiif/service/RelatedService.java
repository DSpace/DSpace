/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

import org.dspace.app.rest.iiif.model.generator.ExternalLinksGenerator;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class RelatedService extends AbstractResourceService {

    private static final String RELATED_ITEM_LABEL = "DSpace item view";

    public RelatedService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }

    @Autowired
    ExternalLinksGenerator externalLinksGenerator;

    public ExternalLinksGenerator getRelated(Item item) {
        String url = CLIENT_URL + "/items/" + item.getID();
        externalLinksGenerator.setIdentifier(url);
        externalLinksGenerator.setFormat("text/html");
        externalLinksGenerator.setLabel(RELATED_ITEM_LABEL);
        return externalLinksGenerator;
    }
}
