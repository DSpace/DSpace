/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.dao;

import org.dspace.core.GenericDAO;
import org.dspace.layout.DynamicLayoutMetric2Box;

/**
 * Database Access Object interface class for the DynamicLayoutMetric2Box object {@link DynamicLayoutMetric2Box}.
 * The implementation of this class is responsible for all database calls for the DynamicLayoutMetric2Box
 * object and is autowired by spring
 * 
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public interface DynamicLayoutMetric2BoxDAO extends GenericDAO<DynamicLayoutMetric2Box> {

}
