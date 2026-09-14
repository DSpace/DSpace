/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.zdb;

import java.util.List;

import org.dspace.content.authority.ZDBExtraMetadataGenerator;
import org.dspace.utils.DSpace;

/**
 * Default implementation of {@link ZDBServicesFactory} that retrieves
 * {@link ZDBService} and {@link ZDBExtraMetadataGenerator} instances from the
 * DSpace service manager.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class ZDBServicesFactoryImpl extends ZDBServicesFactory {

    private ZDBService zdbService;
    private List<ZDBExtraMetadataGenerator> metadataGenerators;

    /**
     * Default constructor; resolves services from the DSpace service manager.
     */
    public ZDBServicesFactoryImpl() {
        DSpace dspace = new DSpace();
        this.zdbService = dspace.getServiceManager().getServiceByName("ZDBSource", ZDBService.class);
        this.metadataGenerators = dspace.getServiceManager().getServicesByType(ZDBExtraMetadataGenerator.class);
    }

    /**
     * Constructor with an explicit {@link ZDBService} instance (used for testing).
     *
     * @param zdbService the ZDB service to use
     */
    public ZDBServicesFactoryImpl(ZDBService zdbService) {
        this.zdbService = zdbService;
        this.metadataGenerators = new DSpace().getServiceManager().getServicesByType(ZDBExtraMetadataGenerator.class);
    }

    /** {@inheritDoc} */
    @Override
    public ZDBService getZDBService() {
        return zdbService;
    }

    /** {@inheritDoc} */
    @Override
    public List<ZDBExtraMetadataGenerator> getMetadataGenerators() {
        return metadataGenerators;
    }

    /**
     * Replace the ZDB service instance (used for testing).
     *
     * @param zdbService the ZDB service to set
     */
    public void setZDBService(ZDBService zdbService) {
        this.zdbService = zdbService;
    }
}
