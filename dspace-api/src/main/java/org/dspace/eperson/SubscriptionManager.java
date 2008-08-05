/*
 * Subscribe.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
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
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
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
package org.dspace.eperson;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.dao.EPersonDAO;
import org.dspace.eperson.dao.EPersonDAOFactory;
import org.dspace.eperson.dao.SubscriptionDAO;
import org.dspace.eperson.dao.SubscriptionDAOFactory;
import org.dspace.search.Harvest;
import org.dspace.search.HarvestedItemInfo;
import org.dspace.uri.IdentifierService;
import org.dspace.uri.ObjectIdentifier;

import javax.mail.MessagingException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Class defining methods for sending new item e-mail alerts to users
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class SubscriptionManager
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(SubscriptionManager.class);

    /**
     * Subscribe an e-person to a collection. An e-mail will be sent every day a
     * new item appears in the collection.
     * 
     * @param context
     *            DSpace context
     * @param eperson
     *            EPerson to subscribe
     * @param collection
     *            Collection to subscribe to
     */
    public static void subscribe(Context context, EPerson eperson,
            Collection collection) throws AuthorizeException
    {
        // Check authorisation. Must be administrator, or the eperson.
        if (AuthorizeManager.isAdmin(context)
                || ((context.getCurrentUser() != null) && (context
                        .getCurrentUser().getID() == eperson.getID())))
        {
            if (!isSubscribed(context, eperson, collection))
            {
                SubscriptionDAO dao =
                    SubscriptionDAOFactory.getInstance(context);

                Subscription sub = dao.create();
                sub.setEPersonID(eperson.getID());
                sub.setCollectionID(collection.getID());
                dao.update(sub);

                log.info(LogManager.getHeader(context, "subscribe",
                        "eperson_id=" + eperson.getID() + ",collection_id="
                                + collection.getID()));
            }
        }
        else
        {
            throw new AuthorizeException(
                    "Only admin or e-person themselves can subscribe");
        }
    }

    /**
     * Unsubscribe an e-person to a collection. Passing in <code>null</code>
     * for the collection unsubscribes the e-person from all collections they
     * are subscribed to.
     * 
     * @param context
     *            DSpace context
     * @param eperson
     *            EPerson to unsubscribe
     * @param collection
     *            Collection to unsubscribe from
     */
    public static void unsubscribe(Context context, EPerson eperson,
            Collection collection) throws AuthorizeException
    {
        // Check authorisation. Must be administrator, or the eperson.
        if (AuthorizeManager.isAdmin(context)
                || ((context.getCurrentUser() != null) && (context
                        .getCurrentUser().getID() == eperson.getID())))
        {
            SubscriptionDAO dao = SubscriptionDAOFactory.getInstance(context);

            if (collection == null)
            {
                for (Subscription sub : dao.getSubscriptions(eperson))
                {
                    log.info(LogManager.getHeader(context, "unsubscribe",
                            "eperson_id=" + sub.getEPersonID() +
                            ",collection_id=" + sub.getCollectionID()));

                    dao.delete(sub.getID());
                }
            }
            else
            {
                for (Subscription sub : dao.getSubscriptions(eperson))
                {
                    if (collection.getID() == sub.getCollectionID())
                    {
                        log.info(LogManager.getHeader(context, "unsubscribe",
                                "eperson_id=" + sub.getEPersonID() +
                                ",collection_id=" + sub.getCollectionID()));

                        dao.delete(sub.getID());
                        break;
                    }
                }
            }
        }
        else
        {
            throw new AuthorizeException(
                    "Only admin or e-person themselves can unsubscribe");
        }
    }

    /**
     * Find out which collections an e-person is subscribed to
     * 
     * @param context
     *            DSpace context
     * @param eperson
     *            EPerson
     * @return array of collections e-person is subscribed to
     */
    public static Collection[] getSubscriptions(Context context, EPerson eperson)
    {
        SubscriptionDAO dao = SubscriptionDAOFactory.getInstance(context);
        CollectionDAO collectionDAO = CollectionDAOFactory.getInstance(context);

        List<Subscription> subscriptions = dao.getSubscriptions(eperson);
        List<Collection> collections = new ArrayList<Collection>();

        for (Subscription sub : subscriptions)
        {
            collections.add(collectionDAO.retrieve(sub.getCollectionID()));
        }
        
        return (Collection[]) collections.toArray(new Collection[0]);
    }

    /**
     * Is that e-person subscribed to that collection?
     * 
     * @param context
     *            DSpace context
     * @param eperson
     *            find out if this e-person is subscribed
     * @param collection
     *            find out if subscribed to this collection
     * @return <code>true</code> if they are subscribed
     */
    public static boolean isSubscribed(Context context, EPerson eperson,
            Collection collection)
    {
        SubscriptionDAO dao = SubscriptionDAOFactory.getInstance(context);
        return dao.isSubscribed(eperson, collection);
    }

    /**
     * Process subscriptions. This must be invoked only once a day. Messages are
     * only sent out when a collection has actually received new items, so that
     * people's mailboxes are not clogged with many "no new items" mails.
     * <P>
     * Yesterday's newly available items are included. If this is run at for
     * example midday, any items that have been made available during the
     * current day will not be included, but will be included in the next day's
     * run.
     * <P>
     * For example, if today's date is 2002-10-10 (in UTC) items made available
     * during 2002-10-09 (UTC) will be included.
     * 
     * @param context
     *            DSpace context object
     */
    public static void processDaily(Context context, boolean test) throws IOException
    {
        // Grab the subscriptions
        SubscriptionDAO dao = SubscriptionDAOFactory.getInstance(context);
        CollectionDAO collectionDAO = CollectionDAOFactory.getInstance(context);
        EPersonDAO epersonDAO = EPersonDAOFactory.getInstance(context);

        List<Subscription> subscriptions = dao.getSubscriptions();

        EPerson currentEPerson = null;
        List<Collection> collections = null; // List of Collections

        // Go through the list collating subscriptions for each e-person
        for (Subscription sub : subscriptions)
        {
            // Does this row relate to the same e-person as the last?
            if ((currentEPerson == null)
                    || (sub.getEPersonID() != currentEPerson.getID()))
            {
                // New e-person. Send mail for previous e-person
                if (currentEPerson != null)
                {
                    try
                    {
                        sendEmail(context, currentEPerson, collections, test);
                    }
                    catch (MessagingException me)
                    {
                        log.error("Failed to send subscription to eperson_id="
                                + currentEPerson.getID());
                        log.error(me);
                    }
                }

                currentEPerson = epersonDAO.retrieve(sub.getEPersonID());
                collections = new ArrayList<Collection>();
            }

            collections.add(collectionDAO.retrieve(sub.getCollectionID()));
        }
        
        // Process the last person
        if (currentEPerson != null)
        {
            try
            {
                sendEmail(context, currentEPerson, collections, test);
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
     * given collections, items that appeared yesterday. No e-mail is sent if
     * there aren't any new items in any of the collections.
     * 
     * @param context
     *            DSpace context object
     * @param eperson
     *            eperson to send to
     * @param collections
     *            List of collection IDs (Integers)
     */
    public static void sendEmail(Context context, EPerson eperson,
            List<Collection> collections, boolean test)
        throws IOException, MessagingException
    {
        ObjectIdentifier identifier = null;

        // Get a resource bundle according to the eperson language preferences
        Locale supportedLocale = I18nUtil.getEPersonLocale(eperson);
        ResourceBundle labels = ResourceBundle.getBundle("Messages", supportedLocale);
        
        // Get the start and end dates for yesterday
        Date thisTimeYesterday = new Date(System.currentTimeMillis()
                - (24 * 60 * 60 * 1000));

        DCDate dcDateYesterday = new DCDate(thisTimeYesterday);

        // this time yesterday in ISO 8601, stripping the time
        String isoDateYesterday = dcDateYesterday.toString().substring(0, 10);

        String startDate = isoDateYesterday;
        String endDate = isoDateYesterday + "T23:59:59Z";

        // FIXME: text of email should be more configurable from an
        // i18n viewpoint
        StringBuffer emailText = new StringBuffer();
        boolean isFirst = true;

        for (Collection c : collections)
        {
            try
            {
                // Limit and offset zero, get everything
                List itemInfos = Harvest.harvest(context, c, startDate,
                        endDate, 0, 
                        0, true, // Need item objects
                        false, // But not containers
                        false); // Or withdrawals
    
                // Only add to buffer if there are new items
                if (itemInfos.size() > 0)
                {
                    if (!isFirst)
                    {
                        emailText.append("\n---------------------------------------\n");
                    }
                    else
                    {
                        isFirst = false;
                    }
    
                    emailText.append(labels.getString(
                                "org.dspace.eperson.Subscribe.new-items")).append(
                                c.getMetadata("name")).append(": ").append(
                                itemInfos.size()).append("\n\n");
    
                    for (int j = 0; j < itemInfos.size(); j++)
                    {
                        HarvestedItemInfo hii =
                            (HarvestedItemInfo) itemInfos.get(j);
    
                        DCValue[] titles = hii.item.getDC("title", null, Item.ANY);
                        emailText.append("      ").append(labels.getString(
                                    "org.dspace.eperson.Subscribe.title")).append(" ");
    
                        if (titles.length > 0)
                        {
                            emailText.append(titles[0].value);
                        }
                        else
                        {
                            emailText.append(labels.getString(
                                        "org.dspace.eperson.Subscribe.untitled"));
                        }
    
                        DCValue[] authors = hii.item.getDC("contributor", Item.ANY,
                                Item.ANY);
    
                        if (authors.length > 0)
                        {
                            emailText.append("\n    ").append(labels.getString(
                                        "org.dspace.eperson.Subscribe.authors"))
                                .append(authors[0].value);
    
                            for (int k = 1; k < authors.length; k++)
                            {
                                emailText.append("\n             ").append(
                                        authors[k].value);
                            }
                        }

                        // FIXME: what's going on here?  This is not consistent with other Identifier Lore.
                        identifier = hii.identifier;

                        emailText.append("\n         ").append(labels.getString(
                                    "org.dspace.eperson.Subscribe.id")).append(
                                    IdentifierService.getURL(identifier).toString()).append(
                                    "\n\n");
                    }
                }
            }
            catch (ParseException pe)
            {
                // This should never get thrown as the Dates are auto-generated
            }
        }

        // Send an e-mail if there were any new items
        if (emailText.length() > 0)
        {
            
            if(test)
            {
                log.info(LogManager.getHeader(context, "subscription:", "eperson=" + eperson.getEmail() ));
                log.info(LogManager.getHeader(context, "subscription:", "text=" + emailText.toString() ));

            } else {
                
                Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(supportedLocale, "subscription"));
                email.addRecipient(eperson.getEmail());
                email.addArgument(emailText.toString());
                email.send();
                
                log.info(LogManager.getHeader(context, "sent_subscription", "eperson_id=" + eperson.getID() ));
                
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
        Options options = new Options();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine line = null;

        options.addOption(
                OptionBuilder.isRequired(false).withDescription(
                "Run test session").withLongOpt("test").create("t"));
        
        boolean test = options.hasOption("t");

        if(test)
            log.setLevel(Level.DEBUG);
        
        Context context = null;

        try
        {
            context = new Context();
            processDaily(context, test);
            context.complete();
        }
        catch( Exception e )
        {
            log.fatal(e);
        }
        finally
        {
            if( context != null && context.isValid() )
            {
                // Nothing is actually written
                context.abort();
            }
        }
    }

    private static List filterOutModified(List completeList)
    {
        log.debug("Filtering out all modified to leave new items list size="+completeList.size());
        List filteredList = new ArrayList();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        for (Iterator iter = completeList.iterator(); iter.hasNext();)
        {
            HarvestedItemInfo infoObject = (HarvestedItemInfo) iter.next();
            DCValue[] dateAccArr = infoObject.item.getMetadata("dc", "date", "accessioned", Item.ANY);
            String lastModded = sdf.format(infoObject.datestamp);
            if (dateAccArr != null && dateAccArr.length > 0)
            {
                for(DCValue date : dateAccArr)
                {
                    if(date != null && date.value != null)
                    {
                        // if it's never been changed
                        if (date.value.startsWith(lastModded))
                        {
                            filteredList.add(infoObject);
                            log.debug("adding : " + dateAccArr[0].value +" : " + lastModded + " : " + infoObject.identifier.getCanonicalForm());
                        }
                        else
                        {
                            log.debug("ignoring : " + dateAccArr[0].value +" : " + lastModded + " : " + infoObject.identifier.getCanonicalForm());
                        }
                    }
                }
                

                
            }
            else
            {
                log.debug("no date accessioned, adding  : " + infoObject.identifier.getCanonicalForm());
                filteredList.add(infoObject);
            }
        }

        return filteredList;
    }
}
