
package edu.umd.lib.dspace.content.factory;

import org.dspace.services.factory.DSpaceServicesFactory;

import edu.umd.lib.dspace.content.service.EmbargoDTOService;

/**
 * Abstract factory to get services for the drum package, use
 * DrumServiceFactory.getInstance() to retrieve an implementation
 */
public abstract class DrumServiceFactory {

    /**
     * Service for returning a list of embargoed items.
     */
    public abstract EmbargoDTOService getEmbargoDTOService();

    public static DrumServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(
            "drumServiceFactory", DrumServiceFactory.class);
    }
}
