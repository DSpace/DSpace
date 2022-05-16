
package edu.umd.lib.dspace.content.factory;

import org.dspace.services.factory.DSpaceServicesFactory;

import edu.umd.lib.dspace.content.service.EmbargoDTOService;

/**
 * Abstract factory to get services for the drum package, use DrumServiceFactory.getInstance() to retrieve an implementation
 *
 * @author mohideen at umd.edu
 */
public abstract class DrumServiceFactory {

    public abstract EmbargoDTOService getEmbargoDTOService();

    public static DrumServiceFactory getInstance()
    {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("drumServiceFactory", DrumServiceFactory.class);
    }
}
