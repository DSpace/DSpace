/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
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
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.integration.BindItemToRP;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.RelationPreferenceService;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

/**
 * Script to search and bind potential matches (Items) for all the ResearcherPage (or single) 
 * 
 * @author Luigi Andrea Pascarelli
 *
 */
public class ScriptBindItemToRP
{
    
    private static final DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    
    /** log4j logger */
    private static Logger log = Logger.getLogger(ScriptBindItemToRP.class);

    /**
     * Batch script to find potential matches between DSpace items and RP. See
     * the technical documentation for further details.
     */
    public static void main(String[] args) throws ParseException
    {

        log.info("#### START Script bind item to researcher page: -----"
                + new Date() + " ----- ####");

        DSpace dspace = new DSpace();
  
        SearchService searchService = dspace.getSingletonService(SearchService.class);
        RelationPreferenceService relationPreferenceService = dspace.getServiceManager().getServiceByName(
                "org.dspace.app.cris.service.RelationPreferenceService", RelationPreferenceService.class);
        ApplicationService applicationService = dspace.getServiceManager().getServiceByName(
                "applicationService", ApplicationService.class);
        
        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption("h", "help", false, "help");
        options.addOption("a", "all_researcher", false,
                "Work on all researchers pages");
        options.addOption("s", "single_researcher", true,
                "Work on single researcher");
        options.addOption("d", "MODE_DATE", true,
                "Script work only on RP names modified after this date");
        options.addOption("D", "MODE_HOUR", true,
                "Script work only on RP names modified in this hours range");

        CommandLine line = parser.parse(options, args);

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ScriptBindItemToRP \n", options);
            System.out
                    .println("\n\nUSAGE:\n ScriptBindItemToRP [-a (-d|-D <date>)|-s <researcher_identifier>] \n");

            System.exit(0);
        }

        if (line.hasOption('a') && line.hasOption('s'))
        {
            System.out
                    .println("\n\nUSAGE:\n ScriptBindItemToRP [-a (-d|-D <date>)|-s <researcher_identifier>] \n");
            System.out.println("Insert either a or s like parameters");
            log.error("Either a or s like parameters");
            System.exit(1);
        }

        List<ResearcherPage> rps = null;
        if (line.hasOption('a'))
        {
            log
                    .info("Script launched with -a parameter...it will work on all researcher...");
            // get list of name
            if (line.hasOption('d') || line.hasOption('D'))
            {

                try
                {
                    Date nameTimestampLastModified;
                    String date_string = line.getOptionValue("d");
                    if (line.hasOption('D'))
                    {
                        date_string = line.getOptionValue("D");
                        long hour = Long.parseLong(date_string);
                        Date now = new Date();
                        nameTimestampLastModified = new Date(now.getTime()
                                - (hour * 60 * 60000));
                        log.info("...it will work on RP modified between "
                                + nameTimestampLastModified + " and " + now);
                    }
                    else
                    {
                        nameTimestampLastModified = dateFormat.parse(date_string);
                        log.info("...it will work on RP modified after ..."
                                + date_string);
                    }

                    rps = applicationService
                            .getResearchersPageByNamesTimestampLastModified(nameTimestampLastModified);
                }
                catch (java.text.ParseException e)
                {
                    log
                            .error("Error parsing the date", e);
                    System.exit(1);
                }
            }
            else
            {
                log.info("...it will work on all researcher...");
                SolrQuery query = new SolrQuery("*:*");
                query.addFilterQuery("{!field f=search.resourcetype}" + CrisConstants.RP_TYPE_ID);
                query.setFields("search.resourceid", "search.resourcetype");
                query.setRows(Integer.MAX_VALUE);
                rps = new ArrayList<ResearcherPage>();
                try
                {
                    QueryResponse response = searchService.search(query);
                    SolrDocumentList docList = response.getResults();
                    Iterator<SolrDocument> solrDoc = docList.iterator();
                    while (solrDoc.hasNext())
                    {
                        SolrDocument doc = solrDoc.next();
                        Integer rpId = (Integer) doc
                                .getFirstValue("search.resourceid");
                        rps.add(applicationService.get(ResearcherPage.class, rpId));
                    }
                }
                catch (SearchServiceException e)
                {
                    log
                    .error("Error retrieving documents", e);
                }
            }
            BindItemToRP.work(rps, relationPreferenceService);
        }
        else
        {
            if (line.hasOption('s'))
            {
                // get researcher by parameter
                String rp = line.getOptionValue("s");
                if (rp == null || rp.isEmpty())
                {
                    System.out
                            .println("\n\nUSAGE:\n ScriptBindItemToRP [-a|-s <researcher_identifier>] \n");
                    System.out
                            .println("Researcher id parameter is needed after option -s");
                    log
                            .error("Researcher id parameter is needed after option -s");
                    System.exit(1);
                }

                log
                        .info("Script launched with -s parameter...it will work on researcher with rp identifier "
                                + rp);
                rps = new LinkedList<ResearcherPage>();
                ResearcherPage researcher = applicationService
                        .getResearcherByAuthorityKey(rp);
                rps.add(researcher);
                BindItemToRP.work(rps, relationPreferenceService);
            }
            else
            {
                System.out
                        .println("\n\nUSAGE:\n ScriptBindItemToRP [-a|-s <researcher_identifier>] \n");
                System.out.println("Option a or s is needed");
                log.error("Option a or s is needed");
                System.exit(1);
            }
        }

        log.info("#### END: -----" + new Date() + " ----- ####");
        System.exit(0);
    }

}
