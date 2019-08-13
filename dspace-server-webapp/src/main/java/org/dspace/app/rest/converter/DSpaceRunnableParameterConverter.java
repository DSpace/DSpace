/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.springframework.stereotype.Component;

@Component
public class DSpaceRunnableParameterConverter
    implements DSpaceConverter<DSpaceCommandLineParameter, ParameterValueRest> {

    public ParameterValueRest fromModel(DSpaceCommandLineParameter dSpaceCommandLineParameter) {
        ParameterValueRest parameterValueRest = new ParameterValueRest();
        parameterValueRest.setName(dSpaceCommandLineParameter.getName());
        parameterValueRest.setValue(dSpaceCommandLineParameter.getValue());
        return parameterValueRest;
    }

    public DSpaceCommandLineParameter toModel(ParameterValueRest parameterValueRest) {
        return new DSpaceCommandLineParameter(parameterValueRest.getName(), parameterValueRest.getValue());
    }
}
