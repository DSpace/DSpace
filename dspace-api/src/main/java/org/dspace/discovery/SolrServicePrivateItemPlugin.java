/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.apache.logging.log4j.LogManager.getLogger;

import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This plugin prevents discovery of private items by non-administrators.
 */
public class SolrServicePrivateItemPlugin implements SolrServiceSearchPlugin {

    private static final Logger log = getLogger(SolrServicePrivateItemPlugin.class.getSimpleName());

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Override
    public void additionalSearchParameters(Context context, DiscoverQuery discoveryQuery, SolrQuery solrQuery) {
        try {
            if (!authorizeService.isAdmin(context)) {
                solrQuery.addFilterQuery("NOT(discoverable:false)");
            }
        } catch (SQLException e) {
            log.error(LogManager.getHeader(context, "Error while adding non-administrator filters to query", ""), e);
        }
    }
}