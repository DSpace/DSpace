/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.StatisticsSupportRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

@RelNameDSpaceResource(StatisticsSupportRest.NAME)
public class StatisticsSupportResource extends HALResource<StatisticsSupportRest> {
    public StatisticsSupportResource(StatisticsSupportRest content) {
        super(content);
    }
}
