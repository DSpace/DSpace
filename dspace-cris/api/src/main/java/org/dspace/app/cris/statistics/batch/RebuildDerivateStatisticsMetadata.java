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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearchObject;
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
        
        String usage = "org.dspace.app.cris.statistics.batch.RebuildDerivateStatisticsMetadata [-a|y <year>]";
        Options options = new Options();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine line = null;

    
        options.addOption("y", "year", true, "Year");
        options.addOption("c", "crisentity", true, "Cris entity");
        options.addOption("o", "other", true, "Other DSpace type");
        
        try {
            line = new PosixParser().parse(options, args);
        } catch (Exception e) {
            // automatically generate the help statement
            formatter.printHelp(usage, e.getMessage(), options, "");
            System.exit(1);
        }

        if (line.hasOption("h")) {
            // automatically generate the help statement
            formatter.printHelp(usage, options);
            System.exit(1);
        }
        
        
        DSpace dspace = new DSpace();

        SolrLogger indexer = dspace.getServiceManager().getServiceByName(SolrLogger.class.getName(),SolrLogger.class);

        
        SolrDocumentList sdl = null;
        
        int dspaceType = Constants.ITEM;
        if (line.hasOption("o")) {
            dspaceType = Integer.parseInt(line.getOptionValue("o"));
        }
        
        boolean year = line.hasOption("y");
        
        if (year) {
            System.out.println("YEAR");
            sdl = indexer.getRawData(dspaceType, Integer.parseInt(line.getOptionValue("y")));
        } else {
            System.out.println("ALL");
            sdl = indexer.getRawData(dspaceType);
        }
        
        System.out.println("Found " + sdl.getNumFound()
                + " access in the statistics core");
        HttpSolrServer solr = indexer.getSolr();
        if(year) {
            indexer.deleteByTypeAndYear(dspaceType, Integer.parseInt(line.getOptionValue("y")));
        }
        else {
            indexer.deleteByType(dspaceType);
        }
        solr.commit();
        System.out.println("Remove old data");
        Context context = new Context();
        context.turnOffAuthorisationSystem();
        int i = 0;
        one:for (SolrDocument sd : sdl)
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
            two:for (SolrStatsIndexPlugin solrServiceIndexPlugin : solrServiceIndexPlugins)
            {
                try {
                    solrServiceIndexPlugin.additionalIndex(null, dso,
                        sdi);
                }
                catch(Exception ex) {
                    System.out.println("SolrDoc:"+ (sdi==null?"null":"OK") + " - DSO:" + (dso==null?"null":"OK") + " search.uniqueid " + type+"-"+id + " Error:" + ex.getMessage());
                    continue one;                    
                }
            }
            
            context.removeCached(dso, id);
            solr.add(sdi);
        }
        solr.commit();
        solr.optimize();

        if(!year && line.hasOption("c")) {
            
            Integer crisType = Integer.parseInt(line.getOptionValue("c"));
            sdl = indexer.getRawData(crisType);
            System.out.println("Found " + sdl.getNumFound()
                    + " access in the RP statistics core");
            HttpSolrServer rpsolr = indexer.getSolr();
            indexer.deleteByType(crisType);
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
                
                ACrisObject rp = null;
                switch (crisType)
                {
                case 9:
                    rp = as.get(ResearcherPage.class, id);
                    break;
                case 10:
                    rp = as.get(Project.class, id);
                    break;
                case 11:
                    rp = as.get(OrganizationUnit.class, id);
                    break;
                default:
                    rp = as.get(ResearchObject.class, id);
                    break;
                }
                    
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
}
