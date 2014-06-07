/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components.statistics;

import org.apache.solr.client.solrj.SolrQuery;

public class StatCrisViewSelectedObjectComponent extends
        StatSelectedObjectComponent
{
  

    protected void _prepareBasicQuery(SolrQuery solrQuery,
            Integer yearsQuery)
    {
        _addBasicConfiguration(solrQuery, yearsQuery);
        solrQuery.addFacetField(_CONTINENT, _COUNTRY_CODE, _CITY, ID,
                _LOCATION, _FISCALYEAR, _SOLARYEAR);
        solrQuery.set("facet.missing", true);
        solrQuery.set("f." + _LOCATION + ".facet.missing", false);
        solrQuery.set("f." + ID + ".facet.missing", false);
        solrQuery.set("f." + _FISCALYEAR + ".facet.missing", false);
        solrQuery.set("f." + _SOLARYEAR + ".facet.missing", false);
        solrQuery.set("f." + _FISCALYEAR + ".facet.sort", false);
        solrQuery.set("f." + _SOLARYEAR + ".facet.sort", false);

        solrQuery.set("f." + _CONTINENT + ".facet.mincount", 1);
        solrQuery.set("f." + _COUNTRY_CODE + ".facet.mincount", 1);
        solrQuery.set("f." + _CITY + ".facet.mincount", 1);
        solrQuery.set("f." + _LOCATION + ".facet.mincount", 1);
        solrQuery.set("f." + _FISCALYEAR + ".facet.mincount", 1);
        solrQuery.set("f." + _SOLARYEAR + ".facet.mincount", 1);
    }

 
}
