/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.statistics.CrisSolrLogger;
import org.dspace.app.cris.statistics.plugin.StatsIndicatorsPlugin;
import org.dspace.app.cris.util.Researcher;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.statistics.SolrLogger;
import org.dspace.utils.DSpace;

public class ScriptStatsMetrics
{

    /** log4j logger */
    private static Logger log = Logger.getLogger(ScriptStatsMetrics.class);

    /**
     * Batch script to build cris stats indicators.
     */
    public static void main(String[] args) throws ParseException
    {

        log.info("#### START Script stats indicators: -----" + new Date()
                + " ----- ####");

        DSpace dspace = new DSpace();
        Researcher researcher = new Researcher();
        CrisSearchService searchService = (CrisSearchService) dspace
                .getSingletonService(SearchService.class);

        CrisSolrLogger statsService = (CrisSolrLogger) dspace
                .getSingletonService(SolrLogger.class);

        ApplicationService applicationService = researcher.getApplicationService();

        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption("h", "help", false, "help");
        options.addOption("s", "single_plugin", true, "Work on single plugin");
        options.addOption("l", "filter", true, "Filter by");

        CommandLine line = parser.parse(options, args);

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ScriptStatsIndicators \n", options);
            System.out.println(
                    "\n\nUSAGE:\n ScriptStatsIndicators <-s <connection_name>>] \n");

            System.exit(0);
        }

        String connection = "";
        if (line.hasOption('s'))
        {
            // get researcher by parameter
            connection = line.getOptionValue("s");
            if (connection == null || connection.isEmpty())
            {
                System.out.println(
                        "\n\nUSAGE:\n ScriptStatsIndicators -s <plugin_name>] \n");
                System.out.println(
                        "Plugin name parameter is needed after option -s");
                log.error("Plugin name parameter is needed after option -s");
                System.exit(1);
            }

            log.info(
                    "Script launched with -s parameter...it will work on connection with name "
                            + connection);
        }

        String level = null;
        if (line.hasOption('l')) {            
            level = line.getOptionValue("l");
            if (StringUtils.isBlank(level))
            {
                System.out.println(
                        "\n\nUSAGE:\n ScriptStatsIndicators -s <plugin_name>] -l <filter>\n");
                System.exit(1);
            }
        }
            
        List<StatsIndicatorsPlugin> plugins = new ArrayList<StatsIndicatorsPlugin>();
        if (StringUtils.isBlank(connection))
        {
            plugins = new DSpace().getServiceManager()
                    .getServicesByType(StatsIndicatorsPlugin.class);
        }
        else
        {
            StatsIndicatorsPlugin plugin = new DSpace().getServiceManager()
                    .getServiceByName(connection, StatsIndicatorsPlugin.class);
            plugins.add(plugin);
        }

        List<String> discardedConnection = new LinkedList<String>();
        List<String> successfullConnection = new LinkedList<String>();
        // get plugin
        Context context = null;
        try
        {
            context = new Context();
            String currentPlugin = "";
            plugin: for (StatsIndicatorsPlugin plugin : plugins)
            {
                currentPlugin = plugin.getName();
                try
                {
                    plugin.buildIndicator(context, applicationService, statsService,
                            searchService, level);
                    successfullConnection.add(currentPlugin);
                    context.commit();
                }
                catch (Exception e)
                {
                    log.error(e.getMessage(), e);
                    discardedConnection.add(currentPlugin);
                    continue plugin;
                }
            }
            context.complete();
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
        finally {
            if(context!=null && context.isValid()) {
                context.abort();
            }
        }

        log.info("#### ----------- SCRIPT RESULTS --------- ####");
        log.info("Plugin success:");
        if (!successfullConnection.isEmpty())
        {
            for (String discC : successfullConnection)
            {
                log.info("# " + discC + "#");
            }
        }
        else
        {
            log.info("#### NONE ####");
        }
        log.info("Plugin failed:");
        if (!discardedConnection.isEmpty())
        {

            for (String discC : discardedConnection)
            {
                log.info("# " + discC + "#");
            }
        }
        else
        {
            log.info("#### NONE ####");
        }
        log.info("#### ########################### ####");
        log.info("#### END: -----" + new Date() + " ----- ####");

        System.exit(0);
    }

}
