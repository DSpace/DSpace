/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker.factory;

import org.dspace.checker.service.SimpleReporterService;
import org.dspace.checker.service.ChecksumHistoryService;
import org.dspace.checker.service.ChecksumResultService;
import org.dspace.checker.service.MostRecentChecksumService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the checker package, use CheckerServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class CheckerServiceFactory {

    public abstract MostRecentChecksumService getMostRecentChecksumService();

    public abstract ChecksumHistoryService getChecksumHistoryService();

    public abstract SimpleReporterService getSimpleReporterService();

    public abstract ChecksumResultService getChecksumResultService();

    public static CheckerServiceFactory getInstance()
    {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("checkerServiceFactory", CheckerServiceFactory.class);
    }
}
