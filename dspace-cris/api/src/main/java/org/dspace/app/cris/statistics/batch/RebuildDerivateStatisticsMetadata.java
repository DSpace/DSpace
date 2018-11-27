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
    public static void main(String[] args)
            throws SolrServerException, IOException, SQLException
    {

        String usage = "org.dspace.app.cris.statistics.batch.RebuildDerivateStatisticsMetadata [y <year> | c <crisentity> | o <community_collection_gotocrisentity>]";
        Options options = new Options();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine line = null;

        options.addOption("y", "year", true, "Year");        
        options.addOption("q", "query", true, "by query");
        options.addOption("c", "crisentity", true, "Cris entity");
        options.addOption("o", "other", true, "Other DSpace type");

        try
        {
            line = new PosixParser().parse(options, args);
        }
        catch (Exception e)
        {
            // automatically generate the help statement
            formatter.printHelp(usage, e.getMessage(), options, "");
            System.exit(1);
        }

        if (line.hasOption("h"))
        {
            // automatically generate the help statement
            formatter.printHelp(usage, options);
            System.exit(1);
        }

        DSpace dspace = new DSpace();

        SolrLogger indexer = dspace.getServiceManager()
                .getServiceByName(SolrLogger.class.getName(), SolrLogger.class);

        SolrDocumentList sdl = null;

        boolean gotoCris = false;
        int dspaceType = Constants.ITEM;
        if (line.hasOption("o"))
        {
            dspaceType = Integer.parseInt(line.getOptionValue("o"));
            if (dspaceType >= 9)
            {
                gotoCris = true;
            }
        }
        
        int failed = 0;
        int total = 0;
        StringBuilder errorsBuffer = new StringBuilder();
        
        boolean year = line.hasOption("y");
        int yearFound = -1;
        if(year) {
            yearFound = Integer.parseInt(line.getOptionValue("y"));
        }

        boolean byQuery = line.hasOption("q");
        String query = null;
        if(byQuery) {
            query = line.getOptionValue("q");
        }
        if (!gotoCris)
        {
            if (year)
            {
                System.out.println("YEAR");
                sdl = indexer.getRawData(dspaceType, yearFound);
            }
            else if(byQuery)
            {
                System.out.println("BYQUERY");
                sdl = indexer.getRawData(dspaceType, query);
            }
            else
            {
                System.out.println("ALL");
                sdl = indexer.getRawData(dspaceType);
            }

            System.out.println("Found " + sdl.getNumFound()
                    + " access in the statistics core");
            HttpSolrServer solr = indexer.getSolr();
            if (year)
            {
                indexer.deleteByTypeAndYear(dspaceType, yearFound);
            }
            else if(byQuery)
            {
                indexer.deleteByTypeAndQuery(dspaceType, query);
            }
            else
            {
                indexer.deleteByType(dspaceType);
            }
            solr.commit();
            System.out.println("Remove old data");
            Context context = new Context();
            context.turnOffAuthorisationSystem();
            int i = 0;
            one: for (SolrDocument sd : sdl)
            {
                i++;
                System.out.println(
                        "Processed access #" + i + " of " + sdl.getNumFound());
                SolrInputDocument sdi = ClientUtils.toSolrInputDocument(sd);
                Integer id = (Integer) sd.getFieldValue("id");
                Integer type = (Integer) sd.getFieldValue("type");

                DSpaceObject dso = null;
                try
                {
                    dso = DSpaceObject.find(context, type, id);
                }
                catch (Exception ex)
                {
                    failed++;
                    System.out.println("SolrDoc:"
                            + (sdi == null ? "null" : "OK") + " - DSO:"
                            + (dso == null ? "null" : "OK")
                            + " search.uniqueid " + type + "-" + id
                            + " Error:" + ex.getMessage());
                    errorsBuffer.append(sdi);
                    continue one;
                }
                // Do any additional indexing, depends on the plugins
                List<SolrStatsIndexPlugin> solrServiceIndexPlugins = new DSpace()
                        .getServiceManager()
                        .getServicesByType(SolrStatsIndexPlugin.class);
                two: for (SolrStatsIndexPlugin solrServiceIndexPlugin : solrServiceIndexPlugins)
                {
                    try
                    {
                        solrServiceIndexPlugin.additionalIndex(null, dso, sdi);
                    }
                    catch (Exception ex)
                    {
                        failed++;                        
                        System.out.println("SolrDoc:"
                                + (sdi == null ? "null" : "OK") + " - DSO:"
                                + (dso == null ? "null" : "OK")
                                + " search.uniqueid " + type + "-" + id
                                + " Error:" + ex.getMessage());
                        errorsBuffer.append(sdi);
                        continue one;
                    }
                }

                context.removeCached(dso, id);
                solr.add(sdi);
            }
            total += i;
            solr.commit();
            solr.optimize();
        }
        if (gotoCris || line.hasOption("c"))
        {
            Integer crisType = Integer.parseInt(line.getOptionValue("c"));
            if (year)
            {
                System.out.println("YEAR CRIS " + crisType);
                sdl = indexer.getRawData(crisType, yearFound);
            }
            else if(byQuery)
            {
                System.out.println("BYQUERY " + crisType);
                sdl = indexer.getRawData(crisType, query);
            }
            else
            {
                System.out.println("ALL CRIS " + crisType);
                sdl = indexer.getRawData(crisType);
            }

            System.out.println("Found " + sdl.getNumFound()
                    + " access in the RP statistics core");
            HttpSolrServer rpsolr = indexer.getSolr();
            if (year)
            {
                indexer.deleteByTypeAndYear(crisType, yearFound);
            }
            else if(byQuery)
            {
                indexer.deleteByTypeAndQuery(crisType, query);
            }
            else
            {
                indexer.deleteByType(crisType);
            }

            rpsolr.commit();

            System.out.println("Remove old data");

            ApplicationService as = dspace.getServiceManager().getServiceByName(
                    "applicationService", ApplicationService.class);
            int i = 0;
            one: for (SolrDocument sd : sdl)
            {
                i++;
                System.out.println("Processed RP access #" + i + " of "
                        + sdl.getNumFound());
                SolrInputDocument sdi = ClientUtils.toSolrInputDocument(sd);
                Integer id = (Integer) sd.getFieldValue("id");

                ACrisObject rp = null;
                try
                {
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
                }
                catch (Exception ex)
                {
                    failed++;
                    System.out.println("SolrDoc:"
                            + (sdi == null ? "null" : "OK") + " - DSO:"
                            + (rp == null ? "null" : "OK")
                            + " search.uniqueid " + crisType + "-" + id
                            + " Error:" + ex.getMessage());
                    System.out.println(sdi);
                    errorsBuffer.append(sdi);
                    continue one;
                }
                
                if (rp == null) {
                    failed++;
                    System.out.println("SolrDoc:"
                            + (sdi == null ? "null" : "OK") + " - DSO:"
                            + (rp == null ? "null" : "OK")
                            + " search.uniqueid " + crisType + "-" + id
                            );
                    System.out.println(sdi);
                    errorsBuffer.append(sdi);
                    continue one;
                }

                // Do any additional indexing, depends on the plugins
                List<SolrStatsIndexPlugin> solrServiceIndexPlugins = new DSpace()
                        .getServiceManager()
                        .getServicesByType(SolrStatsIndexPlugin.class);
                for (SolrStatsIndexPlugin solrServiceIndexPlugin : solrServiceIndexPlugins)
                {
                    try
                    {
                        solrServiceIndexPlugin.additionalIndex(null, rp, sdi);
                    }
                    catch (Exception ex)
                    {
                        failed++;                        
                        System.out.println("SolrDoc:"
                                + (sdi == null ? "null" : "OK") + " - DSO:"
                                + (rp == null ? "null" : "OK")
                                + " search.uniqueid " + crisType + "-" + id
                                + " Error:" + ex.getMessage());
                        errorsBuffer.append(sdi);
                        continue one;
                    }
                }

                rpsolr.add(sdi);
            }
            
            total += i;
            rpsolr.commit();
            rpsolr.optimize();
        }
        
        System.out.println("TOTAL:" + total);
        System.out.println("FAILED:" + failed);
        System.out.println(errorsBuffer.toString());
    }
}
