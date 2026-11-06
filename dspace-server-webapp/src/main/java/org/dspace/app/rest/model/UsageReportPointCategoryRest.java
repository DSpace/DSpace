/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

/**
 * This class serves as a REST representation of a Category data Point of a
 * {@link UsageReportRest} from the DSpace statistics
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 */
public class UsageReportPointCategoryRest extends UsageReportPointRest {

    private static final long serialVersionUID = 4973944632981356291L;

    public static final String NAME = "category";

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public void setId(String id) {
        super.id = id;
        super.label = id;
    }
}
