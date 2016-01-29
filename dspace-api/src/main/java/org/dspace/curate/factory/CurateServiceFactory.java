/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate.factory;

import org.dspace.curate.service.WorkflowCuratorService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the curate package, use CurateServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class CurateServiceFactory {

    public abstract WorkflowCuratorService getWorkflowCuratorService();

    public static CurateServiceFactory getInstance()
    {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("curateServiceFactory", CurateServiceFactory.class);
    }
}
