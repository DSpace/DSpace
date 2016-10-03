/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.app.cris.integration.RPAuthority;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowserScope;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

public class ScriptDeleteRP
{
    private static final String plugInBrowserIndex = ConfigurationManager
            .getProperty(CrisConstants.CFG_MODULE, "researcherpage.browseindex");

    /** log4j logger */
    private static Logger log = Logger.getLogger(ScriptDeleteRP.class);

    private static final String QUESTION_ONE = "Are you sure to continue? (yes/no)";

    private static final String MESSAGE_ONE = "WARNING: this operation can't be undone! Removing the Researcher Page will remove permanently any relations with the items in the HUB. The items will be NOT removed";

    /**
     * Batch script to delete a RP. See the technical documentation for further
     * details.
     */
    public static void main(String[] args)
    {
        log.info("#### START DELETE: -----" + new Date() + " ----- ####");
        Context dspaceContext = null;
        ApplicationContext context = null;
        try
        {
            dspaceContext = new Context();
            dspaceContext.turnOffAuthorisationSystem();

            DSpace dspace = new DSpace();
            ApplicationService applicationService = dspace.getServiceManager().getServiceByName(
                    "applicationService", ApplicationService.class);

            CommandLineParser parser = new PosixParser();

            Options options = new Options();
            options.addOption("h", "help", false, "help");

            options.addOption("r", "researcher", true, "RP id to delete");

            options.addOption("s", "silent", false, "no interactive mode");

            CommandLine line = parser.parse(options, args);

            if (line.hasOption('h'))
            {
                HelpFormatter myhelp = new HelpFormatter();
                myhelp.printHelp("ScriptHKURPDelete \n", options);
                System.out.println("\n\nUSAGE:\n ScriptHKURPDelete -r <id> \n");
                System.out
                        .println("Please note: add -s for no interactive mode");
                System.exit(0);
            }

            Integer rpId = null;
            boolean delete = false;
            boolean silent = line.hasOption('s');
            Item[] items = null;
            if (line.hasOption('r'))
            {
                rpId = ResearcherPageUtils.getRealPersistentIdentifier(line.getOptionValue("r"),ResearcherPage.class);
                ResearcherPage rp = applicationService.get(
                        ResearcherPage.class, rpId);

                if (rp == null)
                {
                    if (!silent)
                    {
                        System.out.println("RP not exist...exit");
                    }
                    log.info("RP not exist...exit");
                    System.exit(0);
                }

                log.info("Use browse indexing");

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
                    
                String authKey = ResearcherPageUtils.getPersistentIdentifier(rp);

                // set up a BrowseScope and start loading the values into it
                BrowserScope scope = new BrowserScope(dspaceContext);
                scope.setUserLocale(dspaceContext.getCurrentLocale().getLanguage());
                scope.setBrowseIndex(bi);
                // scope.setOrder(order);
                scope.setFilterValue(authKey);
                scope.setAuthorityValue(authKey);
                scope.setResultsPerPage(Integer.MAX_VALUE);
                scope.setBrowseLevel(1);

                // now start up a browse engine and get it to do the work for us
                BrowseEngine be = new BrowseEngine(dspaceContext, isMultilanguage? 
                        scope.getUserLocale():null);
                BrowseInfo binfo = be.browse(scope);
                log.debug("Find " + binfo.getResultCount()
                        + "item(s) for the reseracher " + authKey);
                items = binfo.getItemResults(dspaceContext);
                
                if (!silent && rp != null)
                {
                    System.out.println(MESSAGE_ONE);
                    
                    // interactive mode
                    System.out.println("Attempting to remove Researcher Page:");
                    System.out.println("StaffNo:" + rp.getSourceID());
                    System.out.println("FullName:" + rp.getFullName());
                    System.out.println("the researcher has " + items.length + " relation(s) with item(s) in the HUB");
                    System.out.println();

                    System.out.println(QUESTION_ONE);
                    InputStreamReader isr = new InputStreamReader(System.in);
                    BufferedReader reader = new BufferedReader(isr);
                    String answer = reader.readLine();
                    if (answer.equals("yes"))
                    {
                        delete = true;
                    }
                    else
                    {
                        System.out.println("Exit without delete");
                        log.info("Exit without delete");
                        System.exit(0);
                    }

                }
                else
                {
                    delete = true;
                }
            }
            else
            {
                System.out
                        .println("\n\nUSAGE:\n ScriptHKURPDelete <-v> -r <RPid> \n");
                System.out.println("-r option is mandatory");
                log.error("-r option is mandatory");
                System.exit(1);
            }

            if (delete)
            {
                if (!silent)
                {
                    System.out.println("Deleting...");
                }
                log.info("Deleting...");
                cleanAuthority(dspaceContext, items, rpId);
                applicationService.delete(ResearcherPage.class, rpId);
                dspaceContext.complete();
            }

            if (!silent)
            {
                System.out.println("Ok...Bye");
            }
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        finally
        {
            if (dspaceContext != null && dspaceContext.isValid())
            {
                dspaceContext.abort();
            }
            if (context != null)
            {
                context.publishEvent(new ContextClosedEvent(context));
            }
        }
        log.info("#### END: -----" + new Date() + " ----- ####");
        System.exit(0);
    }

    public static void cleanAuthority(Context dspaceContext, Item[] items, Integer rpId) throws SQLException, AuthorizeException
    {
        //find all metadata with authority support
        MetadataField[] fields = MetadataField.findAll(dspaceContext);
        List<MetadataField> fieldsWithAuthoritySupport = new LinkedList<MetadataField>();
        for (MetadataField field : fields)
        {
            String schema = (MetadataSchema.find(dspaceContext, field
                    .getSchemaID())).getName();
            String mdstring = schema
                    + "."
                    + field.getElement()
                    + (field.getQualifier() == null ? "" : "."
                            + field.getQualifier());
            String choicesPlugin = ConfigurationManager
                    .getProperty("choices.plugin." + mdstring);
            
            if (choicesPlugin != null)
            {
                choicesPlugin = choicesPlugin.trim();
            }
            if ((RPAuthority.RP_AUTHORITY_NAME.equals(choicesPlugin)))
            {
                fieldsWithAuthoritySupport.add(field);
            }
        }

        String authorityKey = ResearcherPageUtils.getPersistentIdentifier(rpId, ResearcherPage.class);
        for (Item item : items)
        {
            Metadatum[] values = null;
            for (MetadataField md : fieldsWithAuthoritySupport)
            {
                String schema = (MetadataSchema.find(dspaceContext, md
                        .getSchemaID())).getName();

                values = item.getMetadata(schema, md.getElement(), md.getQualifier(), Item.ANY);
                item.clearMetadata(schema, md.getElement(), md.getQualifier(),
                        Item.ANY);
                for (Metadatum value : values)
                {
                    if (authorityKey.equals(value.authority))
                    {
                        log.debug("remove authority_key " + authorityKey + " in item_id: "
                                    + item.getID());
                        value.confidence = Choices.CF_UNSET;
                        value.authority = null;
                    }
                    
                    item.addMetadata(value.schema, value.element,
                            value.qualifier, value.language,
                            value.value, value.authority,
                            value.confidence);
                }
            }
            item.update();
        }

    }

}
