/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * Build queries to the statistical subsystem and create UI elements from the
 * results.  The underlying SOLR engine uses a text-based query language.  The
 * classes here map a structure of constraint objects into that language.
 * <p>
 * {@link org.dspace.statistics.content.StatisticsDataVisits} is somewhat like a
 * factory for statistical queries.  An instance is customized with
 * DatasetGenerator instances to specify interesting facets of the data and with
 * filters to specify TBS.  The "factory methods" then produce arrays of values
 * meeting the factory's criteria, either raw or formatted for presentation.
 * <p>
 * DatasetGenerator subclasses are available for constraining the results to a
 * given DSpaceObject, object type, and time interval.
 * <p>
 * A StatisticsDataVisits object can be wrapped in a
 * {@link org.dspace.statistics.content.StatisticsDisplay}
 * subclass to format its content as a list or a table.
 */

package org.dspace.statistics.content;
