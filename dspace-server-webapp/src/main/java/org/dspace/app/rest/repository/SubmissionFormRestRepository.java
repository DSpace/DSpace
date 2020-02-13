/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.List;

import org.dspace.app.rest.model.SubmissionFormRest;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.core.Context;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage InputForm Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component(SubmissionFormRest.CATEGORY + "." + SubmissionFormRest.NAME)
public class SubmissionFormRestRepository extends DSpaceRestRepository<SubmissionFormRest, String> {

    private DCInputsReader inputReader;

    public SubmissionFormRestRepository() throws DCInputsReaderException {
        inputReader = new DCInputsReader();
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public SubmissionFormRest findOne(Context context, String submitName) {
        DCInputSet inputConfig;
        try {
            inputConfig = inputReader.getInputsByFormName(submitName);
        } catch (DCInputsReaderException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        if (inputConfig == null) {
            return null;
        }
        return converter.toRest(inputConfig, utils.obtainProjection());
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public Page<SubmissionFormRest> findAll(Context context, Pageable pageable) {
        try {
            long total = inputReader.countInputs();
            List<DCInputSet> subConfs = inputReader.getAllInputs(pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(subConfs, pageable, total, utils.obtainProjection());
        } catch (DCInputsReaderException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public Class<SubmissionFormRest> getDomainClass() {
        return SubmissionFormRest.class;
    }
}
