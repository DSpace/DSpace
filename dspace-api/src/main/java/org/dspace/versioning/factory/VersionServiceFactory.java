/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;

/**
 * Abstract factory to get services for the versioning package, use VersionServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class VersionServiceFactory {

    public abstract VersionHistoryService getVersionHistoryService();

    public abstract VersioningService getVersionService();

    public static VersionServiceFactory getInstance()
    {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("versionServiceFactory", VersionServiceFactory.class);
    }
}
