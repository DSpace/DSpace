/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SystemConfigVariableRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.SystemConfigVariable;
import org.springframework.stereotype.Component;

/**
 * Converter that converts a {@link SystemConfigVariable} domain object into a {@link SystemConfigVariableRest} object.
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
@Component
public class SystemConfigVariableConverter implements DSpaceConverter<SystemConfigVariable, SystemConfigVariableRest> {

    /**
     * Converts a {@link SystemConfigVariable} domain object into its REST representation.
     *
     * @param modelObject the domain object to convert
     * @param projection  the active projection
     * @return the converted {@link SystemConfigVariableRest} object
     */
    @Override
    public SystemConfigVariableRest convert(SystemConfigVariable modelObject, Projection projection) {
        SystemConfigVariableRest rest = new SystemConfigVariableRest();
        rest.setProjection(projection);
        rest.setId(modelObject.getKey());
        rest.setKey(modelObject.getKey());
        rest.setValue(modelObject.getValue());
        rest.setPlaceholder(modelObject.getPlaceholder());
        return rest;
    }

    /**
     * Gets the domain model class handled by this converter.
     *
     * @return the {@link SystemConfigVariable} class
     */
    @Override
    public Class<SystemConfigVariable> getModelClass() {
        return SystemConfigVariable.class;
    }
}
