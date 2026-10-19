/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.factory;

import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.submit.service.SubmissionConfigService;

/**
 * Abstract factory to get services for submission, use SubmissionServiceFactory.getInstance() to retrieve an
 * implementation
 *
 * @author paulo.graca at fccn.pt
 */
public abstract class SubmissionServiceFactory {

    public abstract SubmissionConfigService getSubmissionConfigService() throws SubmissionConfigReaderException;

    public static SubmissionServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("submissionServiceFactory", SubmissionServiceFactory.class);
    }
}
