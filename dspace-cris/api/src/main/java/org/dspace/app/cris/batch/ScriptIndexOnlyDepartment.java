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
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.network.ConstantNetwork;
import org.dspace.app.cris.network.DepartmentNetworkPlugin;
import org.dspace.app.cris.network.NetworkPlugin;
import org.dspace.app.cris.network.VisualizationGraphSolrService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.PluginManager;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

public class ScriptIndexOnlyDepartment
{

    /** log4j logger */
    private static Logger log = Logger.getLogger(ScriptIndexOnlyDepartment.class);

    /**
     * Batch script to find potential matches between DSpace items and RP. See
     * the technical documentation for further details.
     */
    public static void main(String[] args) throws ParseException
    {

        log.info("#### START Script index collaboration network: -----"
                + new Date() + " ----- ####");

        DSpace dspace = new DSpace();
        VisualizationGraphSolrService service = dspace.getServiceManager()
                .getServiceByName("visualNetworkSolrService",
                        VisualizationGraphSolrService.class);

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
            myhelp.printHelp("ScriptIndexNetwork \n", options);
            System.out
                    .println("\n\nUSAGE:\n ScriptIndexNetwork -a|-s <connection_name>] \n");

            System.exit(0);
        }

        if (line.hasOption('a') && line.hasOption('s'))
        {
            System.out
                    .println("\n\nUSAGE:\n ScriptIndexNetwork -a|-s <connection_name>] \n");
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
                        .println("\n\nUSAGE:\n ScriptIndexNetwork -a|-s <connection_name>] \n");
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
                            .println("\n\nUSAGE:\n ScriptIndexNetwork -a|-s <connection_name>] \n");
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
            // get plugin
            try
            {
                VisualizationGraphSolrService.getSolr().deleteByQuery("type:\"" + connection + "\" AND entity:1");
                
        
            }
            catch (Exception e)
            {
                log.error(e.getMessage(), e);                
                continue external;
            }

        }

        // load network departmental data
        try
        {
      
                log.info("Work on department");
                DepartmentNetworkPlugin deptPlugin = (DepartmentNetworkPlugin) PluginManager
                        .getSinglePlugin(NetworkPlugin.CFG_MODULE, NetworkPlugin.class);
                deptPlugin.load(discardedNode, importedNodes, otherError,
                        connections);
            
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            discardedConnection.add("DEPT mode");
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

    private static boolean checkAvailableData(String connection,
            VisualizationGraphSolrService service)
            throws SearchServiceException
    {

        String query = "type:" + connection;

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setStart(0);
        solrQuery.setRows(0);
        QueryResponse rsp = service.search(solrQuery);
        SolrDocumentList docs = rsp.getResults();
        if (docs != null)
        {
            if (docs.getNumFound() > 0)
            {
                return true;
            }
        }
        return false;
    }

}
