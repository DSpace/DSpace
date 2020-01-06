/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.springframework.stereotype.Component;

/**
 * This converter will convert an object of {@Link DSpaceCommandLineParameter} to an object
 * of {@link ParameterValueRest}
 */
@Component
public class DSpaceRunnableParameterConverter
    implements DSpaceConverter<DSpaceCommandLineParameter, ParameterValueRest> {

    @Override
    public ParameterValueRest convert(DSpaceCommandLineParameter dSpaceCommandLineParameter, Projection projection) {
        ParameterValueRest parameterValueRest = new ParameterValueRest();
        parameterValueRest.setName(dSpaceCommandLineParameter.getName());
        parameterValueRest.setValue(dSpaceCommandLineParameter.getValue());
        return parameterValueRest;
    }

    @Override
    public Class<DSpaceCommandLineParameter> getModelClass() {
        return DSpaceCommandLineParameter.class;
    }

    public DSpaceCommandLineParameter toModel(ParameterValueRest parameterValueRest) {
        return new DSpaceCommandLineParameter(parameterValueRest.getName(), parameterValueRest.getValue());
    }
}
