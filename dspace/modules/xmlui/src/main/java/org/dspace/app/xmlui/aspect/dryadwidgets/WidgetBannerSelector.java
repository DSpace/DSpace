/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.dryadwidgets;

import java.util.Map;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.selection.Selector;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowItem;

/**
 * This simple selector checks if a data package exists for a given article DOI.
 * It returns true if the identifier is found, false if not.
 * Can be used to determine which widget banner to render.
 *
 * The publisher parameter must be provided, but is not currently recorded here.
 * 
 * @author Dan Leehr
 */

public class WidgetBannerSelector extends AbstractLogEnabled implements
        Selector
{

    private static Logger log = Logger.getLogger(WidgetBannerSelector.class);

    /**
     * Determine if the provided identifier is resolvable
     */
    public boolean select(String expression, Map objectModel,
            Parameters parameters) {
        try
        {

            Context context = ContextUtil.obtainContext(objectModel);
            String publisher = parameters.getParameter("publisher","");
            if(publisher.length() == 0 || expression.length() == 0) {
                return false;
            }

            // incoming identifier will be an Article DOI.  See if we have
            // a Data package for this article

            String articleDOI = expression;
            if(articleDOI.startsWith("doi:")) {
                articleDOI = articleDOI.substring("doi:".length());
            }

            CommonsHttpSolrServer solrServer;
            String solrService = ConfigurationManager.getProperty("solr.search.server");
            solrServer = new CommonsHttpSolrServer(solrService);
            solrServer.setBaseURL(solrService);

            // Look it up in Solr
            SolrQuery query = new SolrQuery();
            query = query.setQuery("dc.relation.isreferencedby:l2 AND DSpaceStatus:Archived AND dc.type.embargo:none AND location.coll:2");
            QueryResponse response = solrServer.query(query);
            SolrDocumentList documentList = response.getResults();
            if(documentList.isEmpty()) {
                return false;
            }
            SolrDocument document = documentList.get(0);
            Object obj = document.get("dc.identifier");
            obj.toString();
            return true;


        }
        catch (Exception e)
        {
            // Log it and returned no match.
            log.error("Error selecting based on provided identifier " +
                    expression + " : " + e.getMessage());
            return false;
        }
    }

}
