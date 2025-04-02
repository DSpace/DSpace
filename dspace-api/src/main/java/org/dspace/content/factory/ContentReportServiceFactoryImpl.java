/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.factory;

import org.dspace.contentreport.service.ContentReportService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the Content Reports functionalities.
 *
 * @author Jean-François Morin (Université Laval)
 */
public class ContentReportServiceFactoryImpl extends ContentReportServiceFactory {

    @Autowired(required = true)
    private ContentReportService contentReportService;

    @Override
    public ContentReportService getContentReportService() {
        return contentReportService;
    }

}
