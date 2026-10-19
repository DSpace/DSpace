/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.QASourceRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * QA source Rest resource.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 *
 */
@RelNameDSpaceResource(QASourceRest.NAME)
public class QASourceResource extends DSpaceResource<QASourceRest> {

    public QASourceResource(QASourceRest data, Utils utils) {
        super(data, utils);
    }

}
