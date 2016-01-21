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
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the checker package, use CheckerServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class CheckerServiceFactoryImpl extends CheckerServiceFactory {

    @Autowired(required = true)
    private MostRecentChecksumService mostRecentChecksumService;
    @Autowired(required = true)
    private ChecksumHistoryService checksumHistoryService;
    @Autowired(required = true)
    private ChecksumResultService checksumResultService;
    @Autowired(required = true)
    private SimpleReporterService simpleReporterService;

    @Override
    public MostRecentChecksumService getMostRecentChecksumService() {
        return mostRecentChecksumService;
    }

    @Override
    public ChecksumHistoryService getChecksumHistoryService() {
        return checksumHistoryService;
    }

    @Override
    public SimpleReporterService getSimpleReporterService() {
        return simpleReporterService;
    }

    @Override
    public ChecksumResultService getChecksumResultService() {
        return checksumResultService;
    }
}
