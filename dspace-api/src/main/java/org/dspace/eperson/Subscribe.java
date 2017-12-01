/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.mail.MessagingException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;
import org.dspace.search.Harvest;
import org.dspace.search.HarvestedItemInfo;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Class defining methods for sending new item e-mail alerts to users
 *
 * @author Robert Tansley
 * @version $Revision$
 */
public class Subscribe
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(Subscribe.class);

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
            Collection collection) throws SQLException, AuthorizeException
    {
        // Check authorisation. Must be administrator, or the eperson.
        if (AuthorizeManager.isAdmin(context)
                || ((context.getCurrentUser() != null) && (context
                        .getCurrentUser().getID() == eperson.getID())))
        {
            // already subscribed?
            TableRowIterator r = DatabaseManager.query(context,
                    "SELECT * FROM subscription WHERE eperson_id= ? " +
                    " AND collection_id= ? ",
                    eperson.getID(),collection.getID());

            try
            {
                if (!r.hasNext())
                {
                    // Not subscribed, so add them
                    TableRow row = DatabaseManager.row("subscription");
                    row.setColumn("eperson_id", eperson.getID());
                    row.setColumn("collection_id", collection.getID());
                    DatabaseManager.insert(context, row);

                    log.info(LogManager.getHeader(context, "subscribe",
                            "eperson_id=" + eperson.getID() + ",collection_id="
                                    + collection.getID()));
                }
            }
            finally
            {
                // close the TableRowIterator to free up resources
                if (r != null)
                {
                    r.close();
                }
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
            Collection collection) throws SQLException, AuthorizeException
    {
        // Check authorisation. Must be administrator, or the eperson.
        if (AuthorizeManager.isAdmin(context)
                || ((context.getCurrentUser() != null) && (context
                        .getCurrentUser().getID() == eperson.getID())))
        {
            if (collection == null)
            {
                // Unsubscribe from all
                DatabaseManager.updateQuery(context,
                        "DELETE FROM subscription WHERE eperson_id= ? ",
                        eperson.getID());
            }
            else
            {
                DatabaseManager.updateQuery(context,
                        "DELETE FROM subscription WHERE eperson_id= ? " +
                        "AND collection_id= ? ",
                        eperson.getID(),collection.getID());

                log.info(LogManager.getHeader(context, "unsubscribe",
                        "eperson_id=" + eperson.getID() + ",collection_id="
                                + collection.getID()));
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
            throws SQLException
    {
        TableRowIterator tri = DatabaseManager.query(context,
                "SELECT collection_id FROM subscription WHERE eperson_id= ? ",
                eperson.getID());

        List<Collection> collections = new ArrayList<Collection>();

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                collections.add(Collection.find(context, row
                        .getIntColumn("collection_id")));
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        Collection[] collArray = new Collection[collections.size()];

        return (Collection[]) collections.toArray(collArray);
    }

    /**
     * Find out which collections the currently logged in e-person can subscribe to
     *
     * @param context
     *            DSpace context
     * @param eperson
     *            EPerson
     * @return array of collections the currently logged in e-person can subscribe to
     */
    public static Collection[] getAvailableSubscriptions(Context context)
            throws SQLException
    {
        return getAvailableSubscriptions(context, null);
    }
    
    /**
     * Find out which collections an e-person can subscribe to
     *
     * @param context
     *            DSpace context
     * @param eperson
     *            EPerson
     * @return array of collections e-person can subscribe to
     */
    public static Collection[] getAvailableSubscriptions(Context context, EPerson eperson)
            throws SQLException
    {
        Collection[] collections;
        
        if (eperson != null)
        {
            context.setCurrentUser(eperson);
        }
        
        Site site = (Site) Site.find(context, 0);
        
        collections = Collection.findAuthorized(context, null, Constants.ADD);

        return collections;
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
            Collection collection) throws SQLException
    {
    	TableRowIterator tri = DatabaseManager.query(context,
                "SELECT * FROM subscription WHERE eperson_id= ? " +
                "AND collection_id= ? ",
                eperson.getID(),collection.getID());

        try
        {
            return tri.hasNext();
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }
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
     * @param test
     */
    public static void processDaily(Context context, boolean test) throws SQLException,
            IOException
    {
        // Grab the subscriptions
        TableRowIterator tri = DatabaseManager.query(context,
                "SELECT * FROM subscription ORDER BY eperson_id");

        EPerson currentEPerson = null;
        List<Collection> collections = null; // List of Collections

        try
        {
            // Go through the list collating subscriptions for each e-person
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // Does this row relate to the same e-person as the last?
                if ((currentEPerson == null)
                        || (row.getIntColumn("eperson_id") != currentEPerson
                                .getID()))
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

                    currentEPerson = EPerson.find(context, row
                            .getIntColumn("eperson_id"));
                    collections = new ArrayList<Collection>();
                }

                collections.add(Collection.find(context, row
                        .getIntColumn("collection_id")));
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
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
     * @param test
     */
    public static void sendEmail(Context context, EPerson eperson,
            List<Collection> collections, boolean test) throws IOException, MessagingException,
            SQLException
    {
        // Get a resource bundle according to the eperson language preferences
        Locale supportedLocale = I18nUtil.getEPersonLocale(eperson);
        ResourceBundle labels =  ResourceBundle.getBundle("Messages", supportedLocale);

        // Get the start and end dates for yesterday

        // The date should reflect the timezone as well. Otherwise we stand to lose that information
        // in truncation and roll to an earlier date than intended.
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.setTime(new Date());

        // What we actually want to pass to Harvest is "Midnight of yesterday in my current timezone"
        // Truncation will actually pass in "Midnight of yesterday in UTC", which will be,
        // at least in CDT, "7pm, the day before yesterday, in my current timezone".
        cal.add(Calendar.HOUR, -24);
        Date thisTimeYesterday = cal.getTime();

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date midnightYesterday = cal.getTime();


        // FIXME: text of email should be more configurable from an
        // i18n viewpoint
        StringBuffer emailText = new StringBuffer();
        boolean isFirst = true;

        for (int i = 0; i < collections.size(); i++)
        {
            Collection c = collections.get(i);

            try {
                boolean includeAll = ConfigurationManager.getBooleanProperty("harvest.includerestricted.subscription", true);

                // we harvest all the changed item from yesterday until now
                List<HarvestedItemInfo> itemInfos = Harvest.harvest(context, c, new DCDate(midnightYesterday).toString(), null, 0, // Limit
                                                                                    // and
                                                                                    // offset
                                                                                    // zero,
                                                                                    // get
                                                                                    // everything
                        0, true, // Need item objects
                        false, // But not containers
                        false, // Or withdrawals
                        includeAll);

                if (ConfigurationManager.getBooleanProperty("eperson.subscription.onlynew", false))
                {
                    // get only the items archived yesterday
                    itemInfos = filterOutModified(itemInfos);
                }
                else
                {
                    // strip out the item archived today or
                    // not archived yesterday and modified today
                    itemInfos = filterOutToday(itemInfos);
                }

                // Only add to buffer if there are new items
                if (itemInfos.size() > 0)
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

                    emailText.append(labels.getString("org.dspace.eperson.Subscribe.new-items")).append(" ").append(
                            c.getMetadata("name")).append(": ").append(
                            itemInfos.size()).append("\n\n");

                    for (int j = 0; j < itemInfos.size(); j++)
                    {
                        HarvestedItemInfo hii = (HarvestedItemInfo) itemInfos
                                .get(j);

                        DCValue[] titles = hii.item.getDC("title", null, Item.ANY);
                        emailText.append("      ").append(labels.getString("org.dspace.eperson.Subscribe.title")).append(" ");

                        if (titles.length > 0)
                        {
                            emailText.append(titles[0].value);
                        }
                        else
                        {
                            emailText.append(labels.getString("org.dspace.eperson.Subscribe.untitled"));
                        }

                        DCValue[] authors = hii.item.getDC("contributor", Item.ANY,
                                Item.ANY);

                        if (authors.length > 0)
                        {
                            emailText.append("\n    ").append(labels.getString("org.dspace.eperson.Subscribe.authors")).append(" ").append(
                                    authors[0].value);

                            for (int k = 1; k < authors.length; k++)
                            {
                                emailText.append("\n             ").append(
                                        authors[k].value);
                            }
                        }

                        emailText.append("\n         ").append(labels.getString("org.dspace.eperson.Subscribe.id")).append(" ").append(
                                HandleManager.getCanonicalForm(hii.handle)).append(
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

                Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "subscription"));
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
        String usage = "org.dspace.eperson.Subscribe [-t] or nothing to send out subscriptions.";

        Options options = new Options();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine line = null;

        {
            Option opt = new Option("t", "test", false, "Run test session");
            opt.setRequired(false);
            options.addOption(opt);
        }

        {
            Option opt = new Option("h", "help", false, "Print this help message");
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

        if(test)
        {
            log.setLevel(Level.DEBUG);
        }

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

    private static List<HarvestedItemInfo> filterOutToday(List<HarvestedItemInfo> completeList)
    {
        log.debug("Filtering out all today item to leave new items list size="
                + completeList.size());
        List<HarvestedItemInfo> filteredList = new ArrayList<HarvestedItemInfo>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(new Date());
        // Get the start and end dates for yesterday
        Date thisTimeYesterday = new Date(System.currentTimeMillis()
                - (24 * 60 * 60 * 1000));
        String yesterday = sdf.format(thisTimeYesterday);

        for (HarvestedItemInfo infoObject : completeList)
        {
            Date lastUpdate = infoObject.item.getLastModified();
            String lastUpdateStr = sdf.format(lastUpdate);

            // has the item modified today?
            if (lastUpdateStr.equals(today))
            {
                DCValue[] dateAccArr = infoObject.item.getMetadata("dc",
                        "date", "accessioned", Item.ANY);
                // we need only the item archived yesterday
                if (dateAccArr != null && dateAccArr.length > 0)
                {
                    for (DCValue date : dateAccArr)
                    {
                        if (date != null && date.value != null)
                        {
                            // if it hasn't been archived today
                            if (date.value.startsWith(yesterday))
                            {
                                filteredList.add(infoObject);
                                log.debug("adding : " + dateAccArr[0].value
                                        + " : " + today + " : "
                                        + infoObject.handle);
                                break;
                            }
                            else
                            {
                                log.debug("ignoring : " + dateAccArr[0].value
                                        + " : " + today + " : "
                                        + infoObject.handle);
                            }
                        }
                    }
                }
                else
                {
                    log.debug("no date accessioned, adding  : "
                            + infoObject.handle);
                    filteredList.add(infoObject);
                }
            }
            else
            {
                // the item has been modified yesterday...
                filteredList.add(infoObject);
            }
        }

        return filteredList;
    }

    private static List<HarvestedItemInfo> filterOutModified(List<HarvestedItemInfo> completeList)
    {
        log.debug("Filtering out all modified to leave new items list size="+completeList.size());
        List<HarvestedItemInfo> filteredList = new ArrayList<HarvestedItemInfo>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        // Get the start and end dates for yesterday
        Date thisTimeYesterday = new Date(System.currentTimeMillis()
                - (24 * 60 * 60 * 1000));
        String yesterday = sdf.format(thisTimeYesterday);

        for (HarvestedItemInfo infoObject : completeList)
        {
            DCValue[] dateAccArr = infoObject.item.getMetadata("dc", "date", "accessioned", Item.ANY);

            if (dateAccArr != null && dateAccArr.length > 0)
            {
                for(DCValue date : dateAccArr)
                {
                    if(date != null && date.value != null)
                    {
                        // if it has been archived yesterday
                        if (date.value.startsWith(yesterday))
                        {
                            filteredList.add(infoObject);
                            log.debug("adding : " + dateAccArr[0].value +" : " + yesterday + " : " + infoObject.handle);
                            break;
                        }
                        else
                        {
                            log.debug("ignoring : " + dateAccArr[0].value +" : " + yesterday + " : " + infoObject.handle);
                        }
                    }
                }



            }
            else
            {
                log.debug("no date accessioned, adding  : " + infoObject.handle);
                filteredList.add(infoObject);
            }
        }

        return filteredList;
    }
}
