/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.SubmissionCOARNotifyRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * COARNotify HAL Resource. This resource adds the data from the REST object together with embedded objects
 * and a set of links if applicable
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
@RelNameDSpaceResource(SubmissionCOARNotifyRest.NAME)
public class SubmissionCOARNotifyResource extends DSpaceResource<SubmissionCOARNotifyRest> {
    public SubmissionCOARNotifyResource(SubmissionCOARNotifyRest submissionCOARNotifyRest, Utils utils) {
        super(submissionCOARNotifyRest, utils);
    }
}
