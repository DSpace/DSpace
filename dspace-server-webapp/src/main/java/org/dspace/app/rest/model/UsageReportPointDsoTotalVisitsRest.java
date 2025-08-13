/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.atteo.evo.inflector.English;

/**
 * This class serves as a REST representation of a TotalVisit data Point of a DSO's {@link UsageReportRest} from the
 * DSpace statistics
 *
 * @author Maria Verdonck (Atmire) on 08/06/2020
 */
public class UsageReportPointDsoTotalVisitsRest extends UsageReportPointRest {

    /**
     * Type of dso a UsageReport is being requested of (e.g. item, bitstream, ...)
     */
    private String type;

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public String getTypePlural() {
        return English.plural(getType());
    }

    /**
     * Sets the type of this {@link UsageReportPointRest} object, should be type of dso concerned (e.g. item, bitstream, ...)
     *
     * @param type Type of dso a {@link UsageReportRest} object is being requested of (e.g. item, bitstream, ...)
     */
    public void setType(String type) {
        this.type = type;
    }
}
