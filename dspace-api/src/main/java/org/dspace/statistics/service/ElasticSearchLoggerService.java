/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.service;

import org.dspace.content.DSpaceObject;
import org.dspace.eperson.EPerson;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Service interface class for the Elastic Search logging.
 * The implementation of this class is responsible for all business logic calls for the Elastic Search logging and is autowired by spring
 * 
 * @deprecated  As of DSpace 6.0, ElasticSearch statistics are replaced by Solr statistics
 * @see org.dspace.statistics.service.SolrLoggerService#SolrLoggerService
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ElasticSearchLoggerService {

    public static enum ClientType {
        NODE, LOCAL, TRANSPORT
    }


    public void post(DSpaceObject dspaceObject, HttpServletRequest request, EPerson currentUser);

    public void post(DSpaceObject dspaceObject, String ip, String userAgent, String xforwardedfor, EPerson currentUser);

    public void buildParents(DSpaceObject dso, HashMap<String, ArrayList<String>> parents) throws SQLException;

    public HashMap<String, ArrayList<String>> getParents(DSpaceObject dso)
            throws SQLException;

    public String getClusterName();

    public void setClusterName(String clusterName);

    public String getIndexName();

    public void setIndexName(String indexName);

    public String getIndexType();

    public void setIndexType(String indexType);

    public String getAddress();

    public void setAddress(String address);

    public int getPort();

    public void setPort(int port);

    public void storeParents(XContentBuilder docBuilder, HashMap<String, ArrayList<String>> parents) throws IOException;

    public boolean isUseProxies();

    public void createTransportClient();

    public Client getClient();

    public Client getClient(ClientType clientType);

    // Node Client will discover other ES nodes running in local JVM
    public Client createNodeClient(ClientType clientType);

    public String getConfigurationStringWithFallBack(String module, String configurationKey, String defaultFallbackValue);
}
