/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.iiif.searchindex.factory;

import org.dspace.iiif.searchindex.service.IIIFSearchIndexService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Factory for IIIFSearchIndexService.
 *
 *  @author Michael Spalti  mspalti@willamette.edu
 */
public abstract class IIIFSearchIndexServiceFactory {

    public static IIIFSearchIndexServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("iiifSearchIndexServiceFactory",
                                        IIIFSearchIndexServiceFactory.class);
    }

    public abstract IIIFSearchIndexService getIiifSearchIndexService();
}
