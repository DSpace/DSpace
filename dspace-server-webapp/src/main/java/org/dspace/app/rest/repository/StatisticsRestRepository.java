/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import org.dspace.app.rest.model.StatisticsSupportRest;
import org.springframework.stereotype.Component;

@Component(StatisticsSupportRest.CATEGORY + "." + StatisticsSupportRest.NAME)
public class StatisticsRestRepository extends AbstractDSpaceRestRepository {

    public StatisticsSupportRest getStatisticsSupport() {
        return new StatisticsSupportRest();
    }
}
