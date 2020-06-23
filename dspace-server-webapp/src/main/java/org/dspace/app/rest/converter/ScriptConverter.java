/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.model.ParameterRest;
import org.dspace.app.rest.model.ScriptRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.springframework.stereotype.Component;

/**
 * This converter will convert an object of {@Link DSpaceRunnable} to an object
 * of {@link ScriptRest}
 */
@Component
public class ScriptConverter implements DSpaceConverter<ScriptConfiguration, ScriptRest> {

    @Override
    public ScriptRest convert(ScriptConfiguration scriptConfiguration, Projection projection) {
        ScriptRest scriptRest = new ScriptRest();
        scriptRest.setProjection(projection);
        scriptRest.setDescription(scriptConfiguration.getDescription());
        scriptRest.setId(scriptConfiguration.getName());
        scriptRest.setName(scriptConfiguration.getName());

        List<ParameterRest> parameterRestList = new LinkedList<>();
        for (Option option : CollectionUtils.emptyIfNull(scriptConfiguration.getOptions().getOptions())) {
            ParameterRest parameterRest = new ParameterRest();
            parameterRest.setDescription(option.getDescription());
            parameterRest.setName((option.getOpt() != null ? "-" + option.getOpt() : "--" + option.getLongOpt()));
            parameterRest.setNameLong(option.getLongOpt() != null ? "--" + option.getLongOpt() : null);
            parameterRest.setType(((Class) option.getType()).getSimpleName());
            parameterRest.setMandatory(option.isRequired());
            parameterRestList.add(parameterRest);
        }
        scriptRest.setParameterRestList(parameterRestList);

        return scriptRest;
    }

    @Override
    public Class<ScriptConfiguration> getModelClass() {
        return ScriptConfiguration.class;
    }
}
