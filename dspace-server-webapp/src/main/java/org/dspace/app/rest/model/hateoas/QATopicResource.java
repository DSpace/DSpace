/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.QATopicRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * QA topic Rest resource.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RelNameDSpaceResource(QATopicRest.NAME)
public class QATopicResource extends DSpaceResource<QATopicRest> {

    public QATopicResource(QATopicRest data, Utils utils) {
        super(data, utils);
    }

}
