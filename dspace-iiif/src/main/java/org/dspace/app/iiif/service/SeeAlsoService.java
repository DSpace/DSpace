/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service;

import org.dspace.app.iiif.model.generator.AnnotationGenerator;
import org.dspace.app.iiif.model.generator.ExternalLinksGenerator;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This service provides methods for creating {@code seAlso} external link. There should be a single instance of
 * this service per request. The {@code @RequestScope} provides a single instance created and available during
 * complete lifecycle of the HTTP request.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RequestScope
@Component
public class SeeAlsoService extends AbstractResourceService {

    private static final String SEE_ALSO_LABEL = "More descriptions of this resource";

    public SeeAlsoService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }

    public ExternalLinksGenerator getSeeAlso(Item item) {
        return new ExternalLinksGenerator(IIIF_ENDPOINT + item.getID() + "/manifest/seeAlso")
                .setType(AnnotationGenerator.TYPE)
                .setLabel(SEE_ALSO_LABEL);
    }

}
