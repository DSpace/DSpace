/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.dryadwidgets;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Map;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * Class for finding data package DOIs from article DOIs using solr.
 *
 * @author Dan Leehr
 */
public class WidgetBannerLookup extends AbstractLogEnabled {
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WidgetBannerLookup.class);
    private final static String DOI_HTTP_PREFIX = "http://dx.doi.org/";

    public String lookup(String pubId, String referrer, Map objectModel) throws SQLException {

            Context context = ContextUtil.obtainContext(objectModel);
            if(referrer == null || referrer.length() == 0) {
                return null;
            }
            if(pubId == null || pubId.length() == 0) {
                return null;
            }

            // Incoming pubId should identify a publication/article.  See if we have
            // a data package that references this article
            String pubDoi = null;
            if(pubId.toLowerCase().startsWith(DOI_HTTP_PREFIX)) {
                pubDoi = "doi:" + pubId.substring(DOI_HTTP_PREFIX.length());
            } else if(!pubId.toLowerCase().startsWith("doi:")) {
                pubDoi = "doi:" + pubId;
            } else {
                pubDoi = pubId;
            }
            
            try {
                CommonsHttpSolrServer solrServer;
                String solrService = ConfigurationManager.getProperty("solr.search.server");
                solrServer = new CommonsHttpSolrServer(solrService);
                solrServer.setBaseURL(solrService);

                // Look it up in Solr
                SolrQuery query = new SolrQuery();
                query.setQuery("dc.relation.isreferencedby:\"" + pubDoi + "\" AND DSpaceStatus:Archived AND dc.type.embargo:none AND location.coll:2");

                QueryResponse response = solrServer.query(query);
                SolrDocumentList documentList = response.getResults();
                if(documentList.isEmpty()) {
                    return null;
                }

                SolrDocument document = documentList.get(0);
                String firstDOI = (String)document.getFirstValue("dc.identifier");
                return firstDOI;
                
        } catch (MalformedURLException ex) {
            log.error("Malformed URL Exception when instantiating solr server", ex);
            return null;
        } catch (SolrServerException ex) {
            log.error("Error querying SOLR", ex);
            return null;
        }
    }

}
