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

/**
 * This converter will convert the Options of a script to a list of ParameterRest objects
 */
@Component
public class ParameterConverter {

    /**
     * This method will convert the Options of a script to a list of ParameterRest objects
     * @param options   The options of a script
     * @return          The resulting list of ParameterRest objects
     */
    public List<ParameterRest> convertOptionsToParameterRestList(Options options) {
        List<ParameterRest> listToReturn = new LinkedList<>();

        for (Option option : CollectionUtils.emptyIfNull(options.getOptions())) {
            ParameterRest parameterRest = new ParameterRest();
            parameterRest.setDescription(option.getDescription());
            parameterRest.setName((option.getOpt() != null ? "-" + option.getOpt() : "--" + option.getLongOpt()));
            parameterRest.setType(((Class) option.getType()).getSimpleName());
            listToReturn.add(parameterRest);
        }

        return listToReturn;
    }
}
