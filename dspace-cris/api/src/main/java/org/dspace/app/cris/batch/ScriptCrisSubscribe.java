/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
/*
 * Subscribe.java
 *
 * Version: $Revision: 3762 $
 *
 * Date: $Date: 2009-05-07 06:36:47 +0200 (gio, 07 mag 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.cris.batch;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.configuration.RelationConfiguration;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.discovery.OwnerRPAuthorityIndexer;
import org.dspace.app.cris.integration.CrisComponentsService;
import org.dspace.app.cris.integration.ICRISComponent;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.CrisSubscription;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.CrisSubscribeService;
import org.dspace.app.cris.util.Researcher;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscribe;
import org.dspace.handle.HandleManager;

/**
 * Class defining methods for sending new item e-mail alerts to users. Based on
 * {@link Subscribe} written by Robert Tansley
 * 
 * @author Luigi Andrea Pascarelli
 */
public class ScriptCrisSubscribe
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(ScriptCrisSubscribe.class);
    
    private static Map<Integer, List<String>> mapRelationFields = new HashMap<Integer, List<String>>();

    /**
     * Process subscriptions. This must be invoked only once a day. Messages are
     * only sent out when a rp has actually received new items, so that people's
     * mailboxes are not clogged with many "no new items" mails.
     * <P>
     * Yesterday's newly available items are included. If this is run at for
     * example midday, any items that have been made available during the
     * current day will not be included, but will be included in the next day's
     * run.
     * <P>
     * For example, if today's date is 2002-10-10 (in UTC) items made available
     * during 2002-10-09 (UTC) will be included.
     * 
     * @param applicationService
     * 
     * @param context
     *            DSpace context object
     * @param test
     * @throws SearchServiceException
     */
    public static void processDaily(Researcher researcher,
            ApplicationService applicationService, Context context, boolean test)
            throws SQLException, IOException, SearchServiceException
    {

        List<CrisSubscription> rpSubscriptions = applicationService
                .getList(CrisSubscription.class);
        EPerson currentEPerson = null;
        List<String> rpkeys = null; // List of rp keys
        List<String> relationField = new LinkedList<String>();
        for (CrisSubscription rpSubscription : rpSubscriptions)
        {
            // Does this row relate to the same e-person as the last?
            if ((currentEPerson == null)
                    || (rpSubscription.getEpersonID() != currentEPerson.getID()))
            {
                // New e-person. Send mail for previous e-person
                if (currentEPerson != null)
                {
                    try
                    {
                        relationField = mapRelationFields.get(rpSubscription.getTypeDef());
                        sendEmail(researcher, context, currentEPerson, rpkeys,
                                test, relationField);
                    }
                    catch (MessagingException me)
                    {
                        log.error("Failed to send subscription to eperson_id="
                                + currentEPerson.getID());
                        log.error(me);
                    }
                }

                currentEPerson = EPerson.find(context,
                        rpSubscription.getEpersonID());
                rpkeys = new ArrayList<String>();
            }
            rpkeys.add(ResearcherPageUtils
                    .getPersistentIdentifier(applicationService
                            .getEntityByUUID(rpSubscription.getUuid())));
        }
        // Process the last person
        if (currentEPerson != null)
        {
            try
            {
                sendEmail(researcher, context, currentEPerson, rpkeys, test,
                        relationField);
            }
            catch (MessagingException me)
            {
                log.error("Failed to send subscription to eperson_id="
                        + currentEPerson.getID());
                log.error(me);
            }
        }
    }

    /**
     * Sends an email to the given e-person with details of new items in the
     * given dspace object (MUST be a community or a collection), items that
     * appeared yesterday. No e-mail is sent if there aren't any new items in
     * any of the dspace objects.
     * 
     * @param context
     *            DSpace context object
     * @param eperson
     *            eperson to send to
     * @param rpkeys
     *            List of DSpace Objects
     * @param test
     * @throws SearchServiceException
     */
    public static void sendEmail(Researcher researcher, Context context,
            EPerson eperson, List<String> rpkeys, boolean test,
            List<String> relationFields) throws IOException,
            MessagingException, SQLException, SearchServiceException
    {

        CrisSearchService searchService = researcher.getCrisSearchService();

        // Get a resource bundle according to the eperson language preferences
        Locale supportedLocale = I18nUtil.getEPersonLocale(eperson);

        StringBuffer emailText = new StringBuffer();
        boolean isFirst = true;

        for (String rpkey : rpkeys)
        {
            SolrQuery query = new SolrQuery();
            query.setFields("search.resourceid");
            query.addFilterQuery("{!field f=search.resourcetype}"
                    + Constants.ITEM, "{!field f=inarchive}true");

            for (String tmpRelations : relationFields)
            {
                String fq = "{!field f=" + tmpRelations + "}" + rpkey;
                query.addFilterQuery(fq);
            }

            query.setRows(Integer.MAX_VALUE);

            if (ConfigurationManager.getBooleanProperty(
                    "eperson.subscription.onlynew", false))
            {
                // get only the items archived yesterday
				query.setQuery("dateaccessioned_dt:[NOW/DAY-1DAY TO NOW/DAY]");
            }
            else
            {
                // get all item modified yesterday but not published the day
                // before
                // and all the item modified today and archived yesterday
				query.setQuery("(itemLastModified_dt:[NOW/DAY-1DAY TO NOW/DAY] AND dateaccessioned_dt:[NOW/DAY-1DAY TO NOW/DAY]) OR ((itemLastModified_dt:[NOW/DAY TO NOW] AND dateaccessioned_dt:[NOW/DAY-1DAY TO NOW/DAY]))");
            }

            QueryResponse qResponse = searchService.search(query);
            SolrDocumentList results = qResponse.getResults();

            // Only add to buffer if there are new items
            if (results.getNumFound() > 0)

            {
                if (!isFirst)
                {
                    emailText
                            .append("\n---------------------------------------\n");
                }
                else
                {
                    isFirst = false;
                }

                emailText
                        .append(I18nUtil.getMessage(
                                "org.dspace.eperson.Subscribe.new-items",
                                supportedLocale)).append(" ").append(rpkey)
                        .append(": ").append(results.getNumFound())
                        .append("\n\n");

                for (SolrDocument solrDoc : results)

                {
                    Item item = Item.find(context, (Integer) solrDoc
                            .getFieldValue("search.resourceid"));

                    Metadatum[] titles = item.getDC("title", null, Item.ANY);
                    emailText
                            .append("      ")
                            .append(I18nUtil.getMessage(
                                    "org.dspace.eperson.Subscribe.title",
                                    supportedLocale)).append(" ");

                    if (titles.length > 0)
                    {
                        emailText.append(titles[0].value);
                    }
                    else
                    {
                        emailText.append(I18nUtil.getMessage(
                                "org.dspace.eperson.Subscribe.untitled",
                                supportedLocale));
                    }

                    Metadatum[] authors = item.getDC("contributor", Item.ANY,
                            Item.ANY);

                    if (authors.length > 0)
                    {
                        emailText
                                .append("\n    ")
                                .append(I18nUtil.getMessage(
                                        "org.dspace.eperson.Subscribe.authors",
                                        supportedLocale)).append(" ")
                                .append(authors[0].value);

                        for (int k = 1; k < authors.length; k++)
                        {
                            emailText.append("\n             ").append(
                                    authors[k].value);
                        }
                    }

                    emailText
                            .append("\n         ")
                            .append(I18nUtil.getMessage(
                                    "org.dspace.eperson.Subscribe.id",
                                    supportedLocale))
                            .append(" ")
                            .append(HandleManager.getCanonicalForm(item
                                    .getHandle())).append("\n\n");
                    context.removeCached(item, item.getID());
                }
            }
        }

        // Send an e-mail if there were any new items
        if (emailText.length() > 0)
        {

            if (test)
            {
                log.info(LogManager.getHeader(context, "subscription:",
                        "eperson=" + eperson.getEmail()));
                log.info(LogManager.getHeader(context, "subscription:", "text="
                        + emailText.toString()));

            }
            else
            {

                Email email = Email.getEmail(I18nUtil
                        .getEmailFilename(supportedLocale, "subscription"));
                email.addRecipient(eperson.getEmail());
                email.addArgument(emailText.toString());
                email.send();

                log.info(LogManager.getHeader(context, "sent_subscription",
                        "eperson_id=" + eperson.getID()));

            }
        }
    }

    /**
     * Method for invoking subscriptions via the command line
     * 
     * @param argv
     *            command-line arguments, none used yet
     */
    public static void main(String[] argv)
    {
        log.info("#### START PROCESS DAILY: -----" + new Date() + " ----- ####");
        String usage = "org.dspace.app.cris.batch.ScriptCrisSubscribe [-t|-s] or nothing to send out subscriptions.";

        Options options = new Options();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine line = null;

        {
            Option opt = new Option("t", "test", false, "Run test session");
            opt.setRequired(false);
            options.addOption(opt);
        }

        {
            Option opt = new Option("h", "help", false,
                    "Print this help message");
            opt.setRequired(false);
            options.addOption(opt);
        }
        
        {
        	Option opt = new Option("s", "subscribe", false,
        			"First to process Daily Notification, try to subscribe all owner of the CRIS entity to the daily content");
            opt.setRequired(false);
            options.addOption(opt);        	
        }

        try
        {
            line = new PosixParser().parse(options, argv);
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

        boolean test = line.hasOption("t");

        if (test) {
            log.setLevel(Level.DEBUG);
        }
        
        Context context = null;
        try
        {
            context = new Context();
            Researcher researcher = new Researcher();
            ApplicationService applicationService = researcher
                    .getApplicationService();
            
            if(line.hasOption("s")) {
        
            	CrisSearchService searchService = researcher.getCrisSearchService();
            	
                SolrQuery query = new SolrQuery();
                query.setFields("cris-uuid", OwnerRPAuthorityIndexer.OWNER_I);
                query.addFilterQuery("{!field f=search.resourcetype}"
                        + CrisConstants.RP_TYPE_ID);
                query.setRows(Integer.MAX_VALUE);
   				query.setQuery("*:*");

                QueryResponse qResponse = searchService.search(query);
                SolrDocumentList results = qResponse.getResults();

                if (results.getNumFound() > 0)
                {
                    for (SolrDocument solrDoc : results)
                    {
                    	String uuid = (String) solrDoc.getFieldValue("cris-uuid");
                    	Integer oo = (Integer) solrDoc.getFieldValue(OwnerRPAuthorityIndexer.OWNER_I);
                    	
                    	if(oo!=null) {
	                    	CrisSubscribeService crisSubscribeService = researcher.getCrisSubscribeService();
	                    	EPerson eperson = EPerson.find(context, oo);
	                    	if(eperson!=null && StringUtils.isNotBlank(uuid)) {
	                    		crisSubscribeService.subscribe(eperson, uuid);
	                    	}
                    	}
                    }
                }
            	
            }

            List<CrisComponentsService> serviceComponent = researcher
                    .getAllCrisComponents();
            for (CrisComponentsService service : serviceComponent)
            {
                for (ICRISComponent component : service.getComponents()
                        .values())
                {
                    RelationConfiguration relationConfiguration = component
                            .getRelationConfiguration();
                    if (Item.class.isAssignableFrom(relationConfiguration.getRelationClass()))
                    {
                        Integer key = CrisConstants.getEntityType(component.getTarget());
                        String query = relationConfiguration.getQuery();
                        if(!mapRelationFields.containsKey(key)) {
                            List<String> rels = new LinkedList<String>();
                            rels.add(query);
                            mapRelationFields.put(key, rels);
                        }
                        else {
                            mapRelationFields.get(key).add(query);
                        }
                    }
                }
            }
            processDaily(researcher, applicationService, context, test);
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                // Nothing is actually written
                context.abort();
            }
        }
        log.info("#### END: -----" + new Date() + " ----- ####");
        System.exit(0);
    }
}
