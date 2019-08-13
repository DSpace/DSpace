/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.factory;

import org.dspace.content.service.ProcessService;
import org.dspace.services.factory.DSpaceServicesFactory;

public abstract class ProcessServiceFactory {

    public abstract ProcessService getProcessService();

    public static ProcessServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("processServiceFactory", ProcessServiceFactory.class);
    }
}
