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
 * Default implementation of ZDBServicesFactory.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class ZDBServicesFactoryImpl extends ZDBServicesFactory {

    private ZDBService zdbService;
    private List<ZDBExtraMetadataGenerator> metadataGenerators;

    public ZDBServicesFactoryImpl() {
        DSpace dspace = new DSpace();
        this.zdbService = dspace.getServiceManager().getServiceByName("ZDBSource", ZDBService.class);
        this.metadataGenerators = dspace.getServiceManager().getServicesByType(ZDBExtraMetadataGenerator.class);
    }

    public ZDBServicesFactoryImpl(ZDBService zdbService) {
        this.zdbService = zdbService;
        this.metadataGenerators = new DSpace().getServiceManager().getServicesByType(ZDBExtraMetadataGenerator.class);
    }

    @Override
    public ZDBService getZDBService() {
        return zdbService;
    }

    @Override
    public List<ZDBExtraMetadataGenerator> getMetadataGenerators() {
        return metadataGenerators;
    }

    public void setZDBService(ZDBService zdbService) {
        this.zdbService = zdbService;
    }
}
