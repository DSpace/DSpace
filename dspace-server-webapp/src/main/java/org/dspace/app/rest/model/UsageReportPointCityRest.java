/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

/**
 * This class serves as a REST representation of a City data Point of a {@link UsageReportRest} from the DSpace
 * statistics
 *
 * @author Maria Verdonck (Atmire) on 08/06/2020
 */
public class UsageReportPointCityRest extends UsageReportPointRest {
    public static final String NAME = "city";
    public static final String PLURAL_NAME = "cities";

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @Override
    public void setId(String id) {
        super.id = id;
        super.label = id;
    }
}
