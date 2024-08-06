/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.AccessConditionOptionRest;
import org.dspace.app.rest.model.SubmissionUploadRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.submit.model.AccessConditionOption;
import org.dspace.submit.model.UploadConfiguration;
import org.dspace.submit.model.UploadConfigurationService;
import org.dspace.util.DateMathParser;
import org.dspace.util.TimeHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Configuration Upload section
 * during the submission
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component(SubmissionUploadRest.CATEGORY + "." + SubmissionUploadRest.PLURAL_NAME)
public class SubmissionUploadRestRepository extends DSpaceRestRepository<SubmissionUploadRest, String> {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(SubmissionUploadRestRepository.class);

    @Autowired
    private SubmissionFormRestRepository submissionFormRestRepository;

    @Autowired
    private UploadConfigurationService uploadConfigurationService;

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public SubmissionUploadRest findOne(Context context, String submitName) {
        UploadConfiguration config = uploadConfigurationService.getMap().get(submitName);
        try {
            return convert(context, config, utils.obtainProjection());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public Page<SubmissionUploadRest> findAll(Context context, Pageable pageable) {
        Collection<UploadConfiguration> uploadConfigs = uploadConfigurationService.getMap().values();
        Projection projection = utils.obtainProjection();
        List<SubmissionUploadRest> results = new ArrayList<>();
        List<String> configNames = new ArrayList<>();
        for (UploadConfiguration uploadConfig : uploadConfigs) {
            if (!configNames.contains(uploadConfig.getName())) {
                configNames.add(uploadConfig.getName());
                try {
                    results.add(convert(context, uploadConfig, projection));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return utils.getPage(results, pageable);
    }

    @Override
    public Class<SubmissionUploadRest> getDomainClass() {
        return SubmissionUploadRest.class;
    }

    private SubmissionUploadRest convert(Context context, UploadConfiguration config, Projection projection) {
        SubmissionUploadRest result = new SubmissionUploadRest();
        result.setProjection(projection);
        DateMathParser dateMathParser = new DateMathParser();
        for (AccessConditionOption option : config.getOptions()) {
            AccessConditionOptionRest optionRest = new AccessConditionOptionRest();
            optionRest.setHasStartDate(option.getHasStartDate());
            optionRest.setHasEndDate(option.getHasEndDate());
            if (StringUtils.isNotBlank(option.getStartDateLimit())) {
                try {
                    Date requested = dateMathParser.parseMath(option.getStartDateLimit());
                    optionRest.setMaxStartDate(TimeHelpers.toMidnightUTC(requested));
                } catch (ParseException e) {
                    throw new IllegalStateException("Wrong start date limit configuration for the access condition "
                            + "option named  " + option.getName());
                }
            }
            if (StringUtils.isNotBlank(option.getEndDateLimit())) {
                try {
                    Date requested = dateMathParser.parseMath(option.getEndDateLimit());
                    optionRest.setMaxEndDate(TimeHelpers.toMidnightUTC(requested));
                } catch (ParseException e) {
                    throw new IllegalStateException("Wrong end date limit configuration for the access condition "
                            + "option named  " + option.getName());
                }
            }
            optionRest.setName(option.getName());
            result.getAccessConditionOptions().add(optionRest);
        }
        result.setMetadata(submissionFormRestRepository.findOne(context, config.getMetadata()));
        result.setMaxSize(config.getMaxSize());
        result.setRequired(config.isRequired());
        result.setName(config.getName());
        return result;
    }
}
