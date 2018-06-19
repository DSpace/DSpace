/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.factory;

import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the versioning package, use VersionServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class VersionServiceFactoryImpl extends VersionServiceFactory {

    @Autowired(required = true)
    protected VersionHistoryService versionHistoryService;

    @Autowired(required = true)
    protected VersioningService versionService;

    @Override
    public VersionHistoryService getVersionHistoryService() {
        return versionHistoryService;
    }

    @Override
    public VersioningService getVersionService() {
        return versionService;
    }
}
