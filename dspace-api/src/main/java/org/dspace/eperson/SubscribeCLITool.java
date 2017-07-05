/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.*;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.search.Harvest;
import org.dspace.search.HarvestedItemInfo;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * CLI tool used for sending new item e-mail alerts to users
 *
 * @author Robert Tansley
 * @version $Revision$
 */
public class SubscribeCLITool {

    private static final Logger log = Logger.getLogger(SubscribeCLITool.class);

    private static HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    private static ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private static SubscribeService subscribeService = EPersonServiceFactory.getInstance().getSubscribeService();

    /**
     * Process subscriptions. This must be invoked only once a day. Messages are
     * only sent out when a collection has actually received new items, so that
     * people's mailboxes are not clogged with many "no new items" mails.
     * <p>
     * Yesterday's newly available items are included. If this is run at for
     * example midday, any items that have been made available during the
     * current day will not be included, but will be included in the next day's
     * run.
     * <p>
     * For example, if today's date is 2002-10-10 (in UTC) items made available
     * during 2002-10-09 (UTC) will be included.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param test
     *     If true, do a "dry run", i.e. don't actually send email, just log the attempt
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     */
    public static void processDaily(Context context, boolean test) throws SQLException,
            IOException {
        // Grab the subscriptions

        List<Subscription> subscriptions = subscribeService.findAll(context);

        EPerson currentEPerson = null;
        List<Collection> collections = null; // List of Collections

        // Go through the list collating subscriptions for each e-person
        for (Subscription subscription : subscriptions) {
            // Does this row relate to the same e-person as the last?
            if ((currentEPerson == null)
                    || (!subscription.getePerson().getID().equals(currentEPerson
                                            .getID()))) {
                // New e-person. Send mail for previous e-person
                if (currentEPerson != null) {

                    try {
                        sendEmail(context, currentEPerson, collections, test);
                    } catch (MessagingException me) {
                        log.error("Failed to send subscription to eperson_id="
                                + currentEPerson.getID());
                        log.error(me);
                    }
                }

                currentEPerson = subscription.getePerson();
                collections = new ArrayList<>();
            }

            collections.add(subscription.getCollection());
        }

        // Process the last person
        if (currentEPerson != null) {
            try {
                sendEmail(context, currentEPerson, collections, test);
            } catch (MessagingException me) {
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
     * @param context     DSpace context object
     * @param eperson     eperson to send to
     * @param collections List of collection IDs (Integers)
     * @param test
     *     If true, do a "dry run", i.e. don't actually send email, just log the attempt
     * @throws IOException
     *     A general class of exceptions produced by failed or interrupted I/O operations.
     * @throws MessagingException
     *     A general class of exceptions for sending email.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public static void sendEmail(Context context, EPerson eperson,
                                 List<Collection> collections, boolean test) throws IOException, MessagingException,
            SQLException {
        // Get a resource bundle according to the eperson language preferences
        Locale supportedLocale = I18nUtil.getEPersonLocale(eperson);
        ResourceBundle labels = ResourceBundle.getBundle("Messages", supportedLocale);

        // Get the start and end dates for yesterday

        // The date should reflect the timezone as well. Otherwise we stand to lose that information
        // in truncation and roll to an earlier date than intended.
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.setTime(new Date());

        // What we actually want to pass to Harvest is "Midnight of yesterday in my current timezone"
        // Truncation will actually pass in "Midnight of yesterday in UTC", which will be,
        // at least in CDT, "7pm, the day before yesterday, in my current timezone".
        cal.add(Calendar.HOUR, -24);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date midnightYesterday = cal.getTime();


        // FIXME: text of email should be more configurable from an
        // i18n viewpoint
        StringBuffer emailText = new StringBuffer();
        boolean isFirst = true;

        for (int i = 0; i < collections.size(); i++) {
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

                if (ConfigurationManager.getBooleanProperty("eperson.subscription.onlynew", false)) {
                    // get only the items archived yesterday
                    itemInfos = filterOutModified(itemInfos);
                } else {
                    // strip out the item archived today or
                    // not archived yesterday and modified today
                    itemInfos = filterOutToday(itemInfos);
                }

                // Only add to buffer if there are new items
                if (itemInfos.size() > 0) {
                    if (!isFirst) {
                        emailText
                                .append("\n---------------------------------------\n");
                    } else {
                        isFirst = false;
                    }

                    emailText.append(labels.getString("org.dspace.eperson.Subscribe.new-items")).append(" ").append(
                            c.getName()).append(": ").append(
                            itemInfos.size()).append("\n\n");

                    for (int j = 0; j < itemInfos.size(); j++) {
                        HarvestedItemInfo hii = (HarvestedItemInfo) itemInfos
                                .get(j);

                        String title = hii.item.getName();
                        emailText.append("      ").append(labels.getString("org.dspace.eperson.Subscribe.title")).append(" ");

                        if (StringUtils.isNotBlank(title)) {
                            emailText.append(title);
                        } else {
                            emailText.append(labels.getString("org.dspace.eperson.Subscribe.untitled"));
                        }

                        List<MetadataValue> authors = itemService.getMetadata(hii.item, MetadataSchema.DC_SCHEMA, "contributor", Item.ANY, Item.ANY);

                        if (authors.size() > 0) {
                            emailText.append("\n    ").append(labels.getString("org.dspace.eperson.Subscribe.authors")).append(" ").append(
                                    authors.get(0).getValue());

                            for (int k = 1; k < authors.size(); k++) {
                                emailText.append("\n             ").append(
                                        authors.get(k).getValue());
                            }
                        }

                        emailText.append("\n         ").append(labels.getString("org.dspace.eperson.Subscribe.id")).append(" ").append(
                                handleService.getCanonicalForm(hii.handle)).append(
                                "\n\n");
                    }
                }
            } catch (ParseException pe) {
                // This should never get thrown as the Dates are auto-generated
            }
        }

        // Send an e-mail if there were any new items
        if (emailText.length() > 0) {

            if (test) {
                log.info(LogManager.getHeader(context, "subscription:", "eperson=" + eperson.getEmail()));
                log.info(LogManager.getHeader(context, "subscription:", "text=" + emailText.toString()));

            } else {

                Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "subscription"));
                email.addRecipient(eperson.getEmail());
                email.addArgument(emailText.toString());
                email.send();

                log.info(LogManager.getHeader(context, "sent_subscription", "eperson_id=" + eperson.getID()));

            }


        }
    }

    /**
     * Method for invoking subscriptions via the command line
     *
     * @param argv the command line arguments given
     */
    public static void main(String[] argv) {
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

        try {
            line = new PosixParser().parse(options, argv);
        } catch (Exception e) {
            // automatically generate the help statement
            formatter.printHelp(usage, e.getMessage(), options, "");
            System.exit(1);
        }

        if (line.hasOption("h")) {
            // automatically generate the help statement
            formatter.printHelp(usage, options);
            System.exit(1);
        }

        boolean test = line.hasOption("t");

        if (test) {
            log.setLevel(Level.DEBUG);
        }

        Context context = null;

        try {
            context = new Context(Context.Mode.READ_ONLY);
            processDaily(context, test);
            context.complete();
        } catch (Exception e) {
            log.fatal(e);
        } finally {
            if (context != null && context.isValid()) {
                // Nothing is actually written
                context.abort();
            }
        }
    }

    private static List<HarvestedItemInfo> filterOutToday(List<HarvestedItemInfo> completeList) {
        log.debug("Filtering out all today item to leave new items list size="
                + completeList.size());
        List<HarvestedItemInfo> filteredList = new ArrayList<HarvestedItemInfo>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(new Date());
        // Get the start and end dates for yesterday
        Date thisTimeYesterday = new Date(System.currentTimeMillis()
                - (24 * 60 * 60 * 1000));
        String yesterday = sdf.format(thisTimeYesterday);

        for (HarvestedItemInfo infoObject : completeList) {
            Date lastUpdate = infoObject.item.getLastModified();
            String lastUpdateStr = sdf.format(lastUpdate);

            // has the item modified today?
            if (lastUpdateStr.equals(today)) {
                List<MetadataValue> dateAccArr = itemService.getMetadata(infoObject.item, "dc",
                        "date", "accessioned", Item.ANY);
                // we need only the item archived yesterday
                if (dateAccArr != null && dateAccArr.size() > 0) {
                    for (MetadataValue date : dateAccArr) {
                        if (date != null && date.getValue() != null) {
                            // if it hasn't been archived today
                            if (date.getValue().startsWith(yesterday)) {
                                filteredList.add(infoObject);
                                log.debug("adding : " + dateAccArr.get(0).getValue()
                                        + " : " + today + " : "
                                        + infoObject.handle);
                                break;
                            } else {
                                log.debug("ignoring : " + dateAccArr.get(0).getValue()
                                        + " : " + today + " : "
                                        + infoObject.handle);
                            }
                        }
                    }
                } else {
                    log.debug("no date accessioned, adding  : "
                            + infoObject.handle);
                    filteredList.add(infoObject);
                }
            } else {
                // the item has been modified yesterday...
                filteredList.add(infoObject);
            }
        }

        return filteredList;
    }

    private static List<HarvestedItemInfo> filterOutModified(List<HarvestedItemInfo> completeList) {
        log.debug("Filtering out all modified to leave new items list size=" + completeList.size());
        List<HarvestedItemInfo> filteredList = new ArrayList<HarvestedItemInfo>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        // Get the start and end dates for yesterday
        Date thisTimeYesterday = new Date(System.currentTimeMillis()
                - (24 * 60 * 60 * 1000));
        String yesterday = sdf.format(thisTimeYesterday);

        for (HarvestedItemInfo infoObject : completeList) {
            List<MetadataValue> dateAccArr = itemService.getMetadata(infoObject.item, "dc", "date", "accessioned", Item.ANY);

            if (dateAccArr != null && dateAccArr.size() > 0) {
                for (MetadataValue date : dateAccArr) {
                    if (date != null && date.getValue() != null) {
                        // if it has been archived yesterday
                        if (date.getValue().startsWith(yesterday)) {
                            filteredList.add(infoObject);
                            log.debug("adding : " + dateAccArr.get(0).getValue() + " : " + yesterday + " : " + infoObject.handle);
                            break;
                        } else {
                            log.debug("ignoring : " + dateAccArr.get(0).getValue() + " : " + yesterday + " : " + infoObject.handle);
                        }
                    }
                }


            } else {
                log.debug("no date accessioned, adding  : " + infoObject.handle);
                filteredList.add(infoObject);
            }
        }

        return filteredList;
    }
}
