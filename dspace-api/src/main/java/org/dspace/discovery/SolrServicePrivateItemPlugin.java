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


    @Autowired(required = true)
    protected SearchService searchService;

    @Override
    public void additionalSearchParameters(Context context, DiscoverQuery discoveryQuery, SolrQuery solrQuery) {
        try {
            // Prevents access if user has no administrative rights on the community or collection.
            // NOTE: the resource restriction plugin adds location filters for community and collection admins.
            if ( !authorizeService.isAdmin(context) && !authorizeService.isCommunityAdmin(context)
                && !authorizeService.isCollectionAdmin(context)) {
                solrQuery.addFilterQuery("NOT(discoverable:false)");
            }
        } catch (SQLException ex) {
            log.error(LogManager.getHeader(context, "Error looking up authorization rights of current user",
                ""), ex);
        }
    }
}