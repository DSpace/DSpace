/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.helpers.stubs;

import org.dspace.core.Context;
import org.dspace.xoai.exceptions.InvalidMetadataFieldException;
import org.dspace.xoai.services.api.EarliestDateResolver;

import java.sql.SQLException;
import java.util.Date;

public class StubbedEarliestDateResolver implements EarliestDateResolver {
    private Date date = new Date();

    public StubbedEarliestDateResolver is (Date date) {
        this.date = date;
        return this;
    }

    @Override
    public Date getEarliestDate(Context context) throws InvalidMetadataFieldException, SQLException {
        return date;
    }
}
