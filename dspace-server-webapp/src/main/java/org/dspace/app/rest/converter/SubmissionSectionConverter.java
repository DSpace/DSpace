/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.ScopeEnum;
import org.dspace.app.rest.model.SubmissionSectionRest;
import org.dspace.app.rest.model.SubmissionVisibilityRest;
import org.dspace.app.rest.model.VisibilityEnum;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.submit.factory.SubmissionServiceFactory;
import org.dspace.submit.service.SubmissionConfigService;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the SubmissionStepConfig in the DSpace API data
 * model and the REST data model
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component
public class SubmissionSectionConverter implements DSpaceConverter<SubmissionStepConfig, SubmissionSectionRest> {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SubmissionSectionConverter.class);

    private SubmissionConfigService submissionConfigService;

    @Override
    public SubmissionSectionRest convert(SubmissionStepConfig step, Projection projection) {
        SubmissionSectionRest sp = new SubmissionSectionRest();
        sp.setProjection(projection);
        sp.setMandatory(step.isMandatory());
        sp.setHeader(step.getHeading());
        sp.setSectionType(step.getType());
        sp.setId(step.getId());
        sp.setScope(ScopeEnum.fromString(step.getScope()));
        sp.setVisibility(new SubmissionVisibilityRest(VisibilityEnum.fromString(step.getVisibility()),
                                                      VisibilityEnum.fromString(step.getVisibilityOutside())));
        return sp;
    }

    public SubmissionStepConfig toModel(SubmissionSectionRest obj) {
        SubmissionStepConfig step;

        try {
            step = getSubmissionConfigService().getStepConfig(obj.getId());
        } catch (SQLException | IllegalStateException | SubmissionConfigReaderException e) {
            throw new RuntimeException(e);
        }
        return step;
    }

    @Override
    public Class<SubmissionStepConfig> getModelClass() {
        return SubmissionStepConfig.class;
    }

    public SubmissionConfigService getSubmissionConfigService()
            throws SubmissionConfigReaderException, SQLException, IllegalStateException {
        if (submissionConfigService == null) {
            submissionConfigService = SubmissionServiceFactory.getInstance().getSubmissionConfigService();
        }
        return submissionConfigService;
    }
}
