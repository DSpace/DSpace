/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter.processes;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.model.ParameterRest;
import org.springframework.stereotype.Component;

@Component
public class ParameterConverter {

    public List<ParameterRest> convertOptionsToParameterRestList(Options options) {
        List<ParameterRest> listToReturn = new LinkedList<>();

        for (Option option : CollectionUtils.emptyIfNull(options.getOptions())) {
            ParameterRest parameterRest = new ParameterRest();
            parameterRest.setDescription(option.getDescription());
            parameterRest.setName(option.getOpt());
            parameterRest.setType(((Class) option.getType()).getSimpleName());
            listToReturn.add(parameterRest);
        }

        return listToReturn;
    }
}
