package org.dspace.app.canvasdimension.factory;

import org.dspace.app.canvasdimension.service.IIIFCanvasDimensionService;
import org.dspace.services.factory.DSpaceServicesFactory;

public abstract class IIIFCanvasDimensionServiceFactory {

    public static IIIFCanvasDimensionServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("iiifCanvasDimensionServiceFactory",
                                        IIIFCanvasDimensionServiceFactory.class);
    }

    public abstract IIIFCanvasDimensionService getIiifCanvasDimensionService();
}
