/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;
import org.dspace.app.rest.model.SubmissionAccessOptionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.submit.model.AccessConditionConfiguration;
import org.springframework.stereotype.Component;

/**
 * This converter will convert an object of {@Link AccessConditionConfiguration}
 * to an object of {@link SubmissionAccessOptionRest}.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
@Component
public class SubmissionAccessOptionConverter
        implements DSpaceConverter<AccessConditionConfiguration, SubmissionAccessOptionRest> {

    @Override
    public SubmissionAccessOptionRest convert(AccessConditionConfiguration obj, Projection projection) {
        SubmissionAccessOptionRest model = new SubmissionAccessOptionRest();
        model.setId(obj.getName());
        model.setDiscoverable(obj.getDiscoverable());
        model.setAccessConditionOptions(obj.getOptions());
        return model;
    }

    @Override
    public Class<AccessConditionConfiguration> getModelClass() {
        return AccessConditionConfiguration.class;
    }

}