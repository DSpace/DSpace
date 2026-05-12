/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ror.service;

import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Factory that handles {@code RorImportMetadataSourceService} instance
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public abstract class RorServicesFactory {

    public abstract RorImportMetadataSourceService getRorImportMetadataSourceService();

    public static RorServicesFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("rorServiceFactory", RorServicesFactory.class);
    }

}
