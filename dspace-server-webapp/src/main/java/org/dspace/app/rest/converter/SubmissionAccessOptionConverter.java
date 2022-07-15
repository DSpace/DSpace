/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;
import java.text.ParseException;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.AccessConditionOptionRest;
import org.dspace.app.rest.model.SubmissionAccessOptionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.submit.model.AccessConditionConfiguration;
import org.dspace.submit.model.AccessConditionOption;
import org.dspace.util.DateMathParser;
import org.springframework.stereotype.Component;

/**
 * This converter will convert an object of {@Link AccessConditionConfiguration}
 * to an object of {@link SubmissionAccessOptionRest}.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
@Component
public class SubmissionAccessOptionConverter
        implements DSpaceConverter<AccessConditionConfiguration, SubmissionAccessOptionRest> {

    DateMathParser dateMathParser = new DateMathParser();

    @Override
    public SubmissionAccessOptionRest convert(AccessConditionConfiguration config, Projection projection) {
        SubmissionAccessOptionRest model = new SubmissionAccessOptionRest();
        model.setId(config.getName());
        model.setCanChangeDiscoverable(config.getCanChangeDiscoverable());
        model.setProjection(projection);
        for (AccessConditionOption option : config.getOptions()) {
            AccessConditionOptionRest optionRest = new AccessConditionOptionRest();
            optionRest.setHasStartDate(option.getHasStartDate());
            optionRest.setHasEndDate(option.getHasEndDate());
            if (StringUtils.isNotBlank(option.getStartDateLimit())) {
                try {
                    optionRest.setMaxStartDate(dateMathParser.parseMath(option.getStartDateLimit()));
                } catch (ParseException e) {
                    throw new IllegalStateException("Wrong start date limit configuration for the access condition "
                            + "option named  " + option.getName());
                }
            }
            if (StringUtils.isNotBlank(option.getEndDateLimit())) {
                try {
                    optionRest.setMaxEndDate(dateMathParser.parseMath(option.getEndDateLimit()));
                } catch (ParseException e) {
                    throw new IllegalStateException("Wrong end date limit configuration for the access condition "
                            + "option named  " + option.getName());
                }
            }
            optionRest.setName(option.getName());
            model.getAccessConditionOptions().add(optionRest);
        }
        return model;
    }

    @Override
    public Class<AccessConditionConfiguration> getModelClass() {
        return AccessConditionConfiguration.class;
    }

}