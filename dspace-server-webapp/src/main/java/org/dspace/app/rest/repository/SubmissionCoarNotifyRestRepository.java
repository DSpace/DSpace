/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import org.dspace.app.rest.model.SubmissionCOARNotifyRest;
import org.dspace.coarnotify.NotifySubmissionConfiguration;
import org.dspace.coarnotify.service.SubmissionNotifyService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository that is responsible to manage Submission COAR Notify Rest objects
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
@Component(SubmissionCOARNotifyRest.CATEGORY + "." + SubmissionCOARNotifyRest.PLURAL_NAME)
public class SubmissionCoarNotifyRestRepository extends DSpaceRestRepository<SubmissionCOARNotifyRest, String> {

    @Autowired
    protected SubmissionNotifyService submissionCOARNotifyService;

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public SubmissionCOARNotifyRest findOne(final Context context, final String id) {
        NotifySubmissionConfiguration coarNotifySubmissionConfiguration = submissionCOARNotifyService.findOne(id);
        if (coarNotifySubmissionConfiguration == null) {
            throw new ResourceNotFoundException(
                "No COAR Notify Submission Configuration found for ID: " + id );
        }
        return converter.toRest(coarNotifySubmissionConfiguration, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<SubmissionCOARNotifyRest> findAll(final Context context, final Pageable pageable) {
        return converter.toRestPage(submissionCOARNotifyService.findAll(),
            pageable, utils.obtainProjection());
    }

    @Override
    public Class<SubmissionCOARNotifyRest> getDomainClass() {
        return SubmissionCOARNotifyRest.class;
    }
}
