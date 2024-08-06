/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SubmissionCOARNotifyRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.coarnotify.NotifySubmissionConfiguration;
import org.springframework.stereotype.Component;

/**
 * This converter is responsible for transforming the model representation of an COARNotify to the REST
 * representation of an COARNotifySubmissionConfiguration and vice versa
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 **/
@Component
public class SubmissionCOARNotifyConverter
    implements DSpaceConverter<NotifySubmissionConfiguration, SubmissionCOARNotifyRest> {

    /**
     * Convert a COARNotify to its REST representation
     * @param modelObject   - the COARNotify to convert
     * @param projection    - the projection
     * @return the corresponding SubmissionCOARNotifyRest object
     */
    @Override
    public SubmissionCOARNotifyRest convert(final NotifySubmissionConfiguration modelObject,
                                            final Projection projection) {

        SubmissionCOARNotifyRest submissionCOARNotifyRest = new SubmissionCOARNotifyRest();
        submissionCOARNotifyRest.setProjection(projection);
        submissionCOARNotifyRest.setId(modelObject.getId());
        submissionCOARNotifyRest.setPatterns(modelObject.getPatterns());
        return submissionCOARNotifyRest;
    }

    @Override
    public Class<NotifySubmissionConfiguration> getModelClass() {
        return NotifySubmissionConfiguration.class;
    }

}
