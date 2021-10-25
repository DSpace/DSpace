/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service;

import org.dspace.app.iiif.model.generator.RangeGenerator;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This service provides methods for creating a {@code Range}. There should be a single instance of this service
 * per request. The {@code @RequestScope} provides a single instance created and available during complete lifecycle
 * of the HTTP request.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RequestScope
@Component
public class RangeService extends AbstractResourceService {

    @Autowired
    CanvasService canvasService;

    public RangeService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }

    /**
     * Ranges expect the Sub range object to have only an identifier.
     * 
     * @param range the sub range to reference
     * @return RangeGenerator able to create the reference
     */
    public RangeGenerator getRangeReference(RangeGenerator range) {
        return new RangeGenerator(this).setIdentifier(range.getIdentifier());
    }
}
