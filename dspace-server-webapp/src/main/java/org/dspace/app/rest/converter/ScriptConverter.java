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
import org.apache.commons.lang.StringUtils;
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
            parameterRest.setType(getType(option));
            parameterRest.setMandatory(option.isRequired());
            parameterRestList.add(parameterRest);
        }
        scriptRest.setParameterRestList(parameterRestList);

        return scriptRest;
    }

    /**
     * Retrieve the type for this Option. In Apache Commons CLI, Option objects default to a "String" type.
     *
     * In most situations, the Option type will correspond to the Java Class name (e.g. "String", "boolean", etc.).
     * However, when the Option type is "String" (the default) and the Option is configured to not allow for
     * additional arguments (hasArg=false), we switch the type to be "boolean". This behavior is necessary to ensure
     * our REST API differentiates between an Option that requires an additional argument (which our REST API returns
     * as a "String" type), and an Option which does not accept additional arguments (which is just a boolean flag
     * used to trigger different behavior, hence a "boolean" type is more appropriate).
     *
     * @param option Option to retrieve the type for
     * @return the type of the option based on the aforementioned logic
     */
    private String getType(Option option) {
        String simpleName = ((Class) option.getType()).getSimpleName();
        if (StringUtils.equalsIgnoreCase(simpleName, "string")) {
            if (!option.hasArg()) {
                return boolean.class.getSimpleName();
            }
        }
        return simpleName;
    }

    @Override
    public Class<ScriptConfiguration> getModelClass() {
        return ScriptConfiguration.class;
    }
}
