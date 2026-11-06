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
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.DynamicLayoutMetric2Box;
import org.dspace.service.DSpaceCRUDService;
/**
 * Interface of service to manage Metric2Box component of layout
 * 
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public interface DynamicLayoutMetric2BoxService extends DSpaceCRUDService<DynamicLayoutMetric2Box> {

    /**
     * This method stores in the database a DynamicLayoutMetric2Box {@link DynamicLayoutMetric2Box} instance.
     * @param context The relevant DSpace Context
     * @param metric a DynamicLayoutMetric2Box instance {@link DynamicLayoutMetric2Box}
     * @return the stored DynamicLayoutMetric2Box instance
     * @throws SQLException An exception that provides information on a database errors.
     */
    DynamicLayoutMetric2Box create(Context context, DynamicLayoutMetric2Box metric);

    /**
     * This method stores add to a DynamicLayoutBox metrics of type {@link DynamicLayoutMetric2Box}.
     * @param context The relevant DSpace Context
     * @param box a DynamicLayoutBox instance {@link DynamicLayoutBox}
     * @return the updated DynamicLayoutBox instance
     * @throws SQLException An exception that provides information on a database errors.
     */
    DynamicLayoutBox addMetrics(Context context, DynamicLayoutBox box, List<String> metrics);

    /**
     * This method stores append to a DynamicLayoutBox metrics of type {@link DynamicLayoutMetric2Box}.
     * @param context The relevant DSpace Context
     * @param box a DynamicLayoutBox instance {@link DynamicLayoutBox}
     * @return the updated DynamicLayoutBox instance
     * @throws SQLException An exception that provides information on a database errors.
     */
    DynamicLayoutBox appendMetrics(Context context, DynamicLayoutBox box, List<String> metrics);

}
