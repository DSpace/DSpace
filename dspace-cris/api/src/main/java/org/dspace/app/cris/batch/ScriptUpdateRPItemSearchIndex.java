/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;


import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowserScope;
import org.dspace.browse.IndexBrowse;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;

public class ScriptUpdateRPItemSearchIndex
{
    private static final DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    
    /** log4j logger */
    private static Logger log = Logger
            .getLogger(ScriptUpdateRPItemSearchIndex.class);

    private static final String plugInBrowserIndex = ConfigurationManager
            .getProperty(CrisConstants.CFG_MODULE, "researcherpage.browseindex");

    /**
     * Batch script to update DSpace browse and search indexes with the last
     * changes in the RP. See the technical documentation for further details.
     */
    public static void main(String[] args) throws ParseException, SQLException, BrowseException, IOException
    {

        log
                .info("#### START Script update researcher page's items search index: -----"
                        + new Date() + " ----- ####");

        DSpace dspace = new DSpace();
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
                "Script work only on RP names modified since the date");
        options.addOption("D", "MODE_HOUR", true,
                "Script work only on RP names modified in the last n hours");

        CommandLine line = parser.parse(options, args);

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ScriptUpdateRPItemSearchIndex \n", options);
            System.out
                    .println("\n\nUSAGE:\n ScriptUpdateRPItemSearchIndex [-a (-d|-D <date>)|-s <researcher_identifier>] \n");

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
                    log.error("Error parsing the date", e);
                    System.exit(1);
                }
            }
            else
            {
                log.info("...it will work on all researcher...");
                rps = applicationService.getList(ResearcherPage.class);
            }
            reIndexItems(rps, applicationService);
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
                reIndexItems(rps, applicationService);
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

    public static void reIndexItems(List<ResearcherPage> rps,
            ApplicationService applicationService) throws SQLException,
            BrowseException, IOException
    {
        log.info("Create DSpace context and use browse indexing");
        Context context = null;
        try
        {
            context = new Context();
            context.setIgnoreAuthorization(true);
            BrowseIndex bi = BrowseIndex.getBrowseIndex(plugInBrowserIndex);
            
            boolean isMultilanguage = new DSpace().getConfigurationService()
                    .getPropertyAsType(
                            "discovery.browse.authority.multilanguage."
                                    + bi.getName(),
                            new DSpace().getConfigurationService()
                                    .getPropertyAsType(
                                            "discovery.browse.authority.multilanguage",
                                            new Boolean(false)),
                            false);
            
            // we need to assure that the right names will be present in the browse
            IndexBrowse ib = new IndexBrowse(context);
            int count = 1;
            for (ResearcherPage rp : rps)
            {
                String authKey = ResearcherPageUtils
                        .getPersistentIdentifier(rp);
                log.debug("work on " + rp.getFullName() + "[staffno "
                        + rp.getSourceID() + "] with identifier " + authKey
                        + " (" + count + " of " + rps.size() + ")");
                // set up a BrowseScope and start loading the values into it
                BrowserScope scope = new BrowserScope(context);
                scope.setUserLocale(context.getCurrentLocale().getLanguage());
                scope.setBrowseIndex(bi);
                // scope.setOrder(order);
                scope.setFilterValue(authKey);
                scope.setAuthorityValue(authKey);
                scope.setResultsPerPage(Integer.MAX_VALUE);
                scope.setBrowseLevel(1);

                // now start up a browse engine and get it to do the work for us
                BrowseEngine be = new BrowseEngine(context, isMultilanguage? 
                        scope.getUserLocale():null);
                BrowseInfo binfo = be.browse(scope);
                log.debug("Find " + binfo.getResultCount()
                        + "item(s) for the reseracher " + authKey);
                Item[] items = binfo.getItemResults(context);
                for (Item item : items)
                {
                    context.addEvent(new Event(Event.MODIFY_METADATA, Constants.ITEM, item.getID(), null));
                }
            }
            context.commit();
            context.clearCache();
        }
        finally
        {
            if (context != null && context.isValid())
                context.abort();
        }
    }
}
