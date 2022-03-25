/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.canvasdimension.factory;

import org.dspace.iiif.canvasdimension.service.IIIFCanvasDimensionService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory for the image dimension service.
 *
 * @author Michael Spalti mspalti@willamette.edu
 */
public class IIIFCanvasDimensionServiceFactoryImpl extends IIIFCanvasDimensionServiceFactory {

    @Autowired()
    private IIIFCanvasDimensionService iiifCanvasDimensionService;

    @Override
    public IIIFCanvasDimensionService getIiifCanvasDimensionService() {
        return iiifCanvasDimensionService;
    }
}
