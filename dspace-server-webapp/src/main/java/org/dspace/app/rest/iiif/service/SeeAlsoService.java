/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

import java.util.List;

import org.dspace.app.rest.iiif.model.generator.AnnotationGenerator;
import org.dspace.app.rest.iiif.model.generator.ExternalLinksGenerator;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class SeeAlsoService extends AbstractResourceService {

    private static final String SEE_ALSO_LABEL = "More descriptions of this resource";

    public SeeAlsoService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }

    @Autowired
    ExternalLinksGenerator externalLinksGenerator;

    public ExternalLinksGenerator getSeeAlso(Item item, List<Bundle> bundles) {
        return externalLinksGenerator.setIdentifier(IIIF_ENDPOINT + item.getID() + "/manifest/seeAlso")
                .setType(AnnotationGenerator.TYPE)
                .setLabel(SEE_ALSO_LABEL);

    }

}
