/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutMetric2Box;
import org.dspace.service.DSpaceCRUDService;
/**
 * Interface of service to manage Metric2Box component of layout
 * 
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public interface CrisLayoutMetric2BoxService extends DSpaceCRUDService<CrisLayoutMetric2Box> {

    /**
     * This method stores in the database a CrisLayoutMetric2Box {@link CrisLayoutMetric2Box} instance.
     * @param context The relevant DSpace Context
     * @param metric a CrisLayoutMetric2Box instance {@link CrisLayoutMetric2Box}
     * @return the stored CrisLayoutMetric2Box instance
     * @throws SQLException An exception that provides information on a database errors.
     */
    CrisLayoutMetric2Box create(Context context, CrisLayoutMetric2Box metric) throws SQLException;

    /**
     * This method stores add to a CrisLayoutBox metrics of type {@link CrisLayoutMetric2Box}.
     * @param context The relevant DSpace Context
     * @param box a CrisLayoutBox instance {@link CrisLayoutBox}
     * @return the updated CrisLayoutBox instance
     * @throws SQLException An exception that provides information on a database errors.
     */
    CrisLayoutBox addMetrics(Context context, CrisLayoutBox box, List<String> metrics) throws SQLException;

    /**
     * This method stores append to a CrisLayoutBox metrics of type {@link CrisLayoutMetric2Box}.
     * @param context The relevant DSpace Context
     * @param box a CrisLayoutBox instance {@link CrisLayoutBox}
     * @return the updated CrisLayoutBox instance
     * @throws SQLException An exception that provides information on a database errors.
     */
    CrisLayoutBox appendMetrics(Context context, CrisLayoutBox box, List<String> metrics) throws SQLException;

}
