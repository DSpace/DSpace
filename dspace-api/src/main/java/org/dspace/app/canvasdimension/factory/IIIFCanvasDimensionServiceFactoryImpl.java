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
