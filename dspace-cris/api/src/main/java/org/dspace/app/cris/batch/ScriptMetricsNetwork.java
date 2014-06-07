/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;


import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.network.ConstantNetwork;
import org.dspace.app.cris.network.NetworkPlugin;
import org.dspace.app.cris.network.VisualizationGraphSolrService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.PluginManager;
import org.dspace.utils.DSpace;

public class ScriptMetricsNetwork
{

    /** log4j logger */
    private static Logger log = Logger.getLogger(ScriptMetricsNetwork.class);

    /**
     * Batch script to RP metrics.
     */
    public static void main(String[] args) throws ParseException
    {

        log.info("#### START Script index metrics on collaboration network: -----"
                + new Date() + " ----- ####");

        DSpace dspace = new DSpace();
        VisualizationGraphSolrService service = dspace.getServiceManager()
                .getServiceByName("visualNetworkSolrService",
                        VisualizationGraphSolrService.class);

        ApplicationService applicationService = dspace.getServiceManager()
                .getServiceByName("applicationService",
                        ApplicationService.class);

        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption("h", "help", false, "help");
        options.addOption("a", "all_connections", false,
                "Work on all connections read from configuration");
        options.addOption("s", "single_connection", true,
                "Work on single connection");

        CommandLine line = parser.parse(options, args);

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ScriptMetricsNetwork \n", options);
            System.out
                    .println("\n\nUSAGE:\n ScriptMetricsNetwork -a|-s <connection_name>] \n");

            System.exit(0);
        }

        if (line.hasOption('a') && line.hasOption('s'))
        {
            System.out
                    .println("\n\nUSAGE:\n ScriptMetricsNetwork -a|-s <connection_name>] \n");
            System.out.println("Insert either a or s like parameters");
            log.error("Either a or s like parameters");
            System.exit(1);
        }
        List<String> discardedConnection = new LinkedList<String>();
        List<String[]> discardedNode = new LinkedList<String[]>();
        List<String> connections = new LinkedList<String>();
        Integer importedNodes = 0;
        Boolean otherError = false;
        if (line.hasOption('a'))
        {

            String connectionsString = ConfigurationManager.getProperty(NetworkPlugin.CFG_MODULE, ConstantNetwork.CONFIG_CONNECTIONS);
            if (connectionsString == null || connectionsString.isEmpty())
            {
                System.out
                        .println("\n\nUSAGE:\n ScriptMetricsNetwork -a|-s <connection_name>] \n");
                System.out
                        .println("Error to get configuration values, check your dspace.cfg");
                log.error("Error to get configuration values, check your dspace.cfg");
                System.exit(1);
            }
            else
            {
                for (String connection : connectionsString.split(","))
                {
                    connections.add(connection);
                }
            }
        }
        else
        {
            if (line.hasOption('s'))
            {
                // get researcher by parameter
                String connection = line.getOptionValue("s");
                if (connection == null || connection.isEmpty())
                {
                    System.out
                            .println("\n\nUSAGE:\n ScriptMetricsNetwork -a|-s <connection_name>] \n");
                    System.out
                            .println("Connection name parameter is needed after option -s");
                    log.error("Connection name parameter is needed after option -s");
                    System.exit(1);
                }

                log.info("Script launched with -s parameter...it will work on connection with name "
                        + connection);
                connections.add(connection);
            }

        }

        external: for (String connection : connections)
        {
            boolean exit = false;
            // check rp configuration
            RPPropertiesDefinition rpPropertiesDefinition = null;
            rpPropertiesDefinition = applicationService
                    .findPropertiesDefinitionByShortName(
                            RPPropertiesDefinition.class,
                            ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_1
                                    + connection);
            if (rpPropertiesDefinition == null)
            {
                System.out
                        .println("\n\nMETADATA NOT FOUND:\n ScriptMetricsNetwork has relieved that "
                                + ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_1
                                + connection
                                + " missed on configuration, add its on RP properties definition \n");
                log.error("METADATA NOT FOUND:\n ScriptMetricsNetwork has relieved that "
                        + ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_1
                        + connection
                        + " missed on configuration, add its on RP properties definition");
                exit = true;
            }
            rpPropertiesDefinition = null;
            rpPropertiesDefinition = applicationService
                    .findPropertiesDefinitionByShortName(
                            RPPropertiesDefinition.class,
                            ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_2
                                    + connection);
            if (rpPropertiesDefinition == null)
            {
                System.out
                        .println("\n\nMETADATA NOT FOUND:\n ScriptMetricsNetwork has relieved that "
                                + ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_2
                                + connection
                                + " missed on configuration, add its on RP properties definition \n");
                log.error("METADATA NOT FOUND:\n ScriptMetricsNetwork has relieved that "
                        + ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_2
                        + connection
                        + " missed on configuration, add its on RP properties definition");
                exit = true;
            }
            rpPropertiesDefinition = null;
            rpPropertiesDefinition = applicationService
                    .findPropertiesDefinitionByShortName(
                            RPPropertiesDefinition.class,
                            ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_3
                                    + connection);
            if (rpPropertiesDefinition == null)
            {
                System.out
                        .println("\n\nMETADATA NOT FOUND:\n ScriptMetricsNetwork has relieved that "
                                + ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_3
                                + connection
                                + " missed on configuration, add its on RP properties definition \n");
                log.error("METADATA NOT FOUND:\n ScriptMetricsNetwork has relieved that "
                        + ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_3
                        + connection
                        + " missed on configuration, add its on RP properties definition");
                exit = true;
            }
            rpPropertiesDefinition = null;
            rpPropertiesDefinition = applicationService
                    .findPropertiesDefinitionByShortName(
                            RPPropertiesDefinition.class,
                            ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_4
                                    + connection);
            if (rpPropertiesDefinition == null)
            {
                System.out
                        .println("\n\nMETADATA NOT FOUND:\n ScriptMetricsNetwork has relieved that "
                                + ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_4
                                + connection
                                + " missed on configuration, add its on RP properties definition \n");
                log.error("METADATA NOT FOUND:\n ScriptMetricsNetwork has relieved that "
                        + ConstantNetwork.PREFIX_METADATA_BIBLIOMETRIC_4
                        + connection
                        + " missed on configuration, add its on RP properties definition");
                exit = true;
            }

            if(exit) {
                break external;
            }
            // get plugin
            try
            {
                NetworkPlugin plugin = (NetworkPlugin) PluginManager
                        .getNamedPlugin(NetworkPlugin.CFG_MODULE, NetworkPlugin.class, connection);
                // load data from connection
                plugin.loadMetrics(discardedNode, importedNodes, otherError); // load
                // all
                // node

            }
            catch (Exception e)
            {
                log.error(e.getMessage(), e);
                discardedConnection.add(connection);
                continue external;
            }

        }

        log.info("#### ----------- STATS --------- ####");
        log.info("Imported nodes: " + importedNodes);
        if (!discardedConnection.isEmpty())
        {
            log.info("Connection discarded (all nodes discarded):");
            for (String discC : discardedConnection)
            {
                log.info("# " + discC + "#");
            }
        }
        log.info("Node discarded:");
        for (String[] discC : discardedNode)
        {
            log.info("# Node: " + discC[1] + " in connection: " + discC[0]);
        }

        if (otherError)
        {
            log.info("Warning there are other nodes discarded,please see log");
        }
        log.info("#### ########################### ####");
        log.info("#### END: -----" + new Date() + " ----- ####");

        System.exit(0);
    }

}
