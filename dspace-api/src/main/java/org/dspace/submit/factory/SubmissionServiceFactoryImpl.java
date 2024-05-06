/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.factory;

import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.submit.service.SubmissionConfigService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for submission, use SubmissionServiceFactory.getInstance() to
 * retrieve an implementation
 *
 * @author paulo.graca at fccn.pt
 */
public class SubmissionServiceFactoryImpl extends SubmissionServiceFactory {
    @Autowired(required = true)
    private SubmissionConfigService submissionConfigService;

    @Override
    public SubmissionConfigService getSubmissionConfigService() throws SubmissionConfigReaderException {
        return submissionConfigService;
    }
}
