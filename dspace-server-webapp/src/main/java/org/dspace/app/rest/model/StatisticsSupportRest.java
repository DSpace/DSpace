/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.StatisticsRestController;

public class StatisticsSupportRest extends BaseObjectRest<String> {

    public static final String NAME = "statistics";
    public static final String CATEGORY = RestModel.STATISTICS;

    public String getCategory() {
        return CATEGORY;
    }

    public Class getController() {
        return StatisticsRestController.class;
    }

    public String getType() {
        return NAME;
    }
}
