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
import org.dspace.app.bulkaccesscontrol.model.BulkAccessConditionConfiguration;
import org.dspace.app.rest.model.AccessConditionOptionRest;
import org.dspace.app.rest.model.BulkAccessConditionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.submit.model.AccessConditionOption;
import org.dspace.util.DateMathParser;
import org.springframework.stereotype.Component;

/**
 * This converter will convert an object of {@Link BulkAccessConditionConfiguration}
 * to an object of {@link BulkAccessConditionRest}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
@Component
public class BulkAccessConditionConverter
        implements DSpaceConverter<BulkAccessConditionConfiguration, BulkAccessConditionRest> {

    DateMathParser dateMathParser = new DateMathParser();

    @Override
    public BulkAccessConditionRest convert(BulkAccessConditionConfiguration config, Projection projection) {
        BulkAccessConditionRest model = new BulkAccessConditionRest();
        model.setId(config.getName());
        model.setProjection(projection);

        for (AccessConditionOption itemAccessConditionOption : config.getItemAccessConditionOptions()) {
            model.getItemAccessConditionOptions().add(convertToRest(itemAccessConditionOption));
        }

        for (AccessConditionOption bitstreamAccessConditionOption : config.getBitstreamAccessConditionOptions()) {
            model.getBitstreamAccessConditionOptions().add(convertToRest(bitstreamAccessConditionOption));
        }
        return model;
    }

    private AccessConditionOptionRest convertToRest(AccessConditionOption option) {
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
        return optionRest;
    }

    @Override
    public Class<BulkAccessConditionConfiguration> getModelClass() {
        return BulkAccessConditionConfiguration.class;
    }

}