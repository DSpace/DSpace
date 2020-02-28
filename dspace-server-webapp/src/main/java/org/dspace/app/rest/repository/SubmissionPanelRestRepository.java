/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.model.SubmissionDefinitionRest;
import org.dspace.app.rest.model.SubmissionSectionRest;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.core.Context;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Submission Step (aka Panel) Rest object
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component(SubmissionDefinitionRest.CATEGORY + "." + SubmissionSectionRest.NAME)
public class SubmissionPanelRestRepository extends DSpaceRestRepository<SubmissionSectionRest, String> {

    private SubmissionConfigReader submissionConfigReader;

    public SubmissionPanelRestRepository() throws SubmissionConfigReaderException {
        submissionConfigReader = new SubmissionConfigReader();
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public SubmissionSectionRest findOne(Context context, String id) {
        try {
            SubmissionStepConfig step = submissionConfigReader.getStepConfig(id);
            return converter.toRest(step, utils.obtainProjection());
        } catch (SubmissionConfigReaderException e) {
            //TODO wrap with a specific exception
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public Page<SubmissionSectionRest> findAll(Context context, Pageable pageable) {
        List<SubmissionConfig> subConfs = submissionConfigReader.getAllSubmissionConfigs(
                pageable.getPageSize(), Math.toIntExact(pageable.getOffset()));
        long total = 0;
        List<SubmissionStepConfig> stepConfs = new ArrayList<>();
        for (SubmissionConfig config : subConfs) {
            total = +config.getNumberOfSteps();
            for (int i = 0; i < config.getNumberOfSteps(); i++) {
                SubmissionStepConfig step = config.getStep(i);
                stepConfs.add(step);
            }
        }
        return converter.toRestPage(stepConfs, pageable, total, utils.obtainProjection());
    }

    @Override
    public Class<SubmissionSectionRest> getDomainClass() {
        return SubmissionSectionRest.class;
    }
}
