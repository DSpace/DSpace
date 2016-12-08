/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.content.filter;

/**
 * A wrapper for some kind of Solr filter expression.
 * @author kevinvandevelde at atmire.com
 * Date: 12-mrt-2009
 * Time: 10:36:03
 */
public interface StatisticsFilter {

    /** Convert this filter's configuration to a query string fragment.
     * @return a query fragment.
     */
    public String toQuery();
}
