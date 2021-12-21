/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.ResourcePolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Converter to translate ResourcePolicy into human readable value
 * configuration.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component
public class ResourcePolicyConverter implements DSpaceConverter<ResourcePolicy, ResourcePolicyRest> {

    @Autowired
    ResourcePolicyService resourcePolicyService;

    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    ConverterService converterService;

    @Override
    public ResourcePolicyRest convert(ResourcePolicy obj, Projection projection) {

        ResourcePolicyRest model = new ResourcePolicyRest();
        model.setProjection(projection);

        model.setId(obj.getID());

        model.setName(obj.getRpName());
        model.setDescription(obj.getRpDescription());
        model.setPolicyType(obj.getRpType());

        model.setAction(resourcePolicyService.getActionText(obj));

        model.setStartDate(obj.getStartDate());
        model.setEndDate(obj.getEndDate());

        if (obj.getGroup() != null) {
            model.setGroup(converterService.toRest(obj.getGroup(), projection));
        }

        if (obj.getEPerson() != null) {
            model.setEperson(converterService.toRest(obj.getEPerson(), projection));
        }
        if (obj.getdSpaceObject() != null) {
            model.setResource(converterService.toRest(obj.getdSpaceObject(), projection));
        }
        return model;
    }

    @Override
    public Class<ResourcePolicy> getModelClass() {
        return ResourcePolicy.class;
    }

}
