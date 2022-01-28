/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;
import java.util.Objects;

import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.SubmissionAccessOptionRest;
import org.dspace.core.Context;
import org.dspace.submit.model.AccessConditionConfiguration;
import org.dspace.submit.model.AccessConditionConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage AccessCondition section
 * during the submission.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
@Component(SubmissionAccessOptionRest.CATEGORY + "." + SubmissionAccessOptionRest.NAME)
public class SubmissionAccessOptionRestRepository extends DSpaceRestRepository<SubmissionAccessOptionRest, String> {

    @Autowired
    private AccessConditionConfigurationService accessConditionConfigurationService;

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public SubmissionAccessOptionRest findOne(Context context, String id) {
        AccessConditionConfiguration configuration = accessConditionConfigurationService.getAccessConfigurationById(id);
        return Objects.nonNull(configuration) ? converter.toRest(configuration, utils.obtainProjection()) : null;
    }

    @Override
    public Page<SubmissionAccessOptionRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(SubmissionAccessOptionRest.NAME, "findAll");
    }

    @Override
    public Class<SubmissionAccessOptionRest> getDomainClass() {
        return SubmissionAccessOptionRest.class;
    }

}