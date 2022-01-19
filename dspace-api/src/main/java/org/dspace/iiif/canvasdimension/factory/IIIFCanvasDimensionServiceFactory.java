/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.canvasdimension.factory;

import org.dspace.iiif.canvasdimension.service.IIIFCanvasDimensionService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Factory for the image dimension service.
 *
 * @author Michael Spalti mspalti@willamette.edu
 */
public abstract class IIIFCanvasDimensionServiceFactory {

    public static IIIFCanvasDimensionServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("iiifCanvasDimensionServiceFactory",
                                        IIIFCanvasDimensionServiceFactory.class);
    }

    public abstract IIIFCanvasDimensionService getIiifCanvasDimensionService();
}
