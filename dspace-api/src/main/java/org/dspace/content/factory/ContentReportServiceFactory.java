/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.factory;

import org.dspace.contentreport.service.ContentReportService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the Content Reports functionalities.
 *
 * @author Jean-François Morin (Université Laval)
 */
public abstract class ContentReportServiceFactory {

    public abstract ContentReportService getContentReportService();

    public static ContentReportServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("contentReportServiceFactory", ContentReportServiceFactory.class);
    }

}
