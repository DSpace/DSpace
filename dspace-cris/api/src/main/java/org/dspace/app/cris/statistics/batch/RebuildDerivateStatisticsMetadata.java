/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics.batch;


import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.statistics.SolrLogger;
import org.dspace.statistics.SolrStatsIndexPlugin;
import org.dspace.utils.DSpace;

public class RebuildDerivateStatisticsMetadata
{

    /**
     * @param args
     * @throws SolrServerException
     * @throws IOException
     * @throws SQLException
     */
    public static void main(String[] args) throws SolrServerException,
            IOException, SQLException
    {
        DSpace dspace = new DSpace();

        SolrLogger indexer = dspace.getServiceManager().getServiceByName(SolrLogger.class.getName(),SolrLogger.class);

        
        SolrDocumentList sdl = indexer.getRawData(Constants.ITEM);
        System.out.println("Found " + sdl.getNumFound()
                + " access in the statistics core");
        HttpSolrServer solr = indexer.getSolr();
        indexer.deleteByType(Constants.ITEM);
        solr.commit();
        System.out.println("Remove old data");
        Context context = new Context();
        context.turnOffAuthorisationSystem();
        int i = 0;
        for (SolrDocument sd : sdl)
        {
            i++;
            System.out.println("Processed access #" + i + " of "
                    + sdl.getNumFound());
            SolrInputDocument sdi = ClientUtils.toSolrInputDocument(sd);
            Integer id = (Integer) sd.getFieldValue("id");
            Integer type = (Integer) sd.getFieldValue("type");

            DSpaceObject dso = DSpaceObject.find(context, type, id);
            
            // Do any additional indexing, depends on the plugins
            List<SolrStatsIndexPlugin> solrServiceIndexPlugins = new DSpace()
                    .getServiceManager().getServicesByType(
                            SolrStatsIndexPlugin.class);
            for (SolrStatsIndexPlugin solrServiceIndexPlugin : solrServiceIndexPlugins)
            {
                solrServiceIndexPlugin.additionalIndex(null, dso,
                        sdi);
            }

            
            context.removeCached(dso, id);
            solr.add(sdi);
        }
        solr.commit();
        solr.optimize();

        sdl = indexer.getRawData(CrisConstants.RP_TYPE_ID);
        System.out.println("Found " + sdl.getNumFound()
                + " access in the RP statistics core");
        HttpSolrServer rpsolr = indexer.getSolr();
        indexer.deleteByType(CrisConstants.RP_TYPE_ID);
        rpsolr.commit();

        System.out.println("Remove old data");
        
        ApplicationService as = dspace.getServiceManager().getServiceByName(
                "applicationService", ApplicationService.class);
        i = 0;
        for (SolrDocument sd : sdl)
        {
            i++;
            System.out.println("Processed RP access #" + i + " of "
                    + sdl.getNumFound());
            SolrInputDocument sdi = ClientUtils.toSolrInputDocument(sd);
            Integer id = (Integer) sd.getFieldValue("id");

            ResearcherPage rp = as.get(ResearcherPage.class, id);
            if (rp == null)
                continue;

            // Do any additional indexing, depends on the plugins
            List<SolrStatsIndexPlugin> solrServiceIndexPlugins = new DSpace()
                    .getServiceManager().getServicesByType(
                            SolrStatsIndexPlugin.class);
            for (SolrStatsIndexPlugin solrServiceIndexPlugin : solrServiceIndexPlugins)
            {
                solrServiceIndexPlugin.additionalIndex(null, rp,
                        sdi);
            }
           
            rpsolr.add(sdi);
        }
        rpsolr.commit();
        rpsolr.optimize();
    }
}
