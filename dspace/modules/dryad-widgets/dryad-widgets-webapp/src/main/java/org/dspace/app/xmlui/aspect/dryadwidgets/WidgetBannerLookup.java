/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.dryadwidgets;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.core.ConfigurationManager;

/**
 * Class for finding data package DOIs from article identifers (DOI or PMID) using solr.
 *
 * @author Dan Leehr
 */
public class WidgetBannerLookup extends AbstractLogEnabled {
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WidgetBannerLookup.class);
    private final static String DOI_HTTP_PREFIX = "http://dx.doi.org/";
    private final static String DOI_PREFIX = "doi:";
    private final static String PMID_PREFIX = "pmid:";
    // PMIDs are 1 to 8 digits: http://www.nlm.nih.gov/bsd/licensee/elements_descriptions.html#pmid
    private static Pattern PMIDPattern = Pattern.compile("\\d{1,8}");
    // DOI regex can be complex, we only need to differentiate from PMID, so simple "10."
    private static Pattern DOIPattern = Pattern.compile("10\\..+");

    // With no "doi:" or "http://dx.doi.org" prefix, is the identifier a doi?
    private Boolean rawPubIdIsDoi(String pubId) {
        Matcher m = DOIPattern.matcher(pubId);
        return m.matches();
    }

    // With no "pmid:" prefix, is the identifier a PMID?
    private Boolean rawPubIdIsPmid(String pubId) {
        // PMIDs are 8 digits
        Matcher m = PMIDPattern.matcher(pubId);
        return m.matches();
    }

    public String lookup(String pubId, String referrer, Map objectModel) throws SQLException {

            if(referrer == null || referrer.length() == 0) {
                return null;
            }
            if(pubId == null || pubId.length() == 0) {
                return null;
            }

            // Normalize the incoming pubId to what we index in solr
            String solrPubId = null;
            if(pubId.toLowerCase().startsWith(DOI_HTTP_PREFIX)) {
                solrPubId = DOI_PREFIX + pubId.substring(DOI_HTTP_PREFIX.length());
            } else if(pubId.toLowerCase().startsWith(DOI_PREFIX)) {
                solrPubId = DOI_PREFIX + pubId.substring(DOI_PREFIX.length());
            } else if(pubId.toLowerCase().startsWith(PMID_PREFIX)) {
                solrPubId = PMID_PREFIX + pubId.substring(PMID_PREFIX.length());
            } else if(rawPubIdIsDoi(pubId)) {
                solrPubId = DOI_PREFIX + pubId;
            } else if(rawPubIdIsPmid(pubId)) {
                solrPubId = PMID_PREFIX + pubId;
            } else {
                // default case, treat it raw
                solrPubId = pubId;
            }
            
            // Incoming pubId should identify a publication/article.  See if we have
            // a data package that references this article
            try {
                CommonsHttpSolrServer solrServer;
                String solrService = ConfigurationManager.getProperty("solr.search.server");
                solrServer = new CommonsHttpSolrServer(solrService);
                solrServer.setBaseURL(solrService);

                // Look it up in Solr
                SolrQuery query = new SolrQuery();
                query.setQuery("dc.relation.isreferencedby:\"" + solrPubId + "\" AND DSpaceStatus:Archived AND location.coll:2");

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
