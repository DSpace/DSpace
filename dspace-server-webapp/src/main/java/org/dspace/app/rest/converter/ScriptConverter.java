/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.converter.processes.ParameterConverter;
import org.dspace.app.rest.model.ScriptRest;
import org.dspace.scripts.DSpaceRunnable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This converter will convert an object of {@Link DSpaceRunnable} to an object
 * of {@link ScriptRest}
 */
@Component
public class ScriptConverter implements DSpaceConverter<DSpaceRunnable, ScriptRest> {

    @Autowired
    private ParameterConverter parameterConverter;

    @Override
    public ScriptRest fromModel(DSpaceRunnable script) {
        ScriptRest scriptRest = new ScriptRest();
        scriptRest.setDescription(script.getDescription());
        scriptRest.setId(script.getName());
        scriptRest.setName(script.getName());
        scriptRest.setParameterRestList(parameterConverter.convertOptionsToParameterRestList(script.getOptions()));
        return scriptRest;
    }

    @Override
    public DSpaceRunnable toModel(ScriptRest obj) {
        return null;
    }
}
