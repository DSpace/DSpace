/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

import org.dspace.app.rest.iiif.model.generator.RangeGenerator;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
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
        return new RangeGenerator().setIdentifier(range.getIdentifier());
    }
}
