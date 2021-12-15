/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.canvasdimension.factory;

import org.dspace.app.canvasdimension.service.IIIFCanvasDimensionService;
import org.springframework.beans.factory.annotation.Autowired;

public class IIIFCanvasDimensionServiceFactoryImpl extends IIIFCanvasDimensionServiceFactory {

    @Autowired(required = true)
    private IIIFCanvasDimensionService iiifCanvasDimensionService;

    @Override
    public IIIFCanvasDimensionService getIiifCanvasDimensionService() {
        return iiifCanvasDimensionService;
    }
}
