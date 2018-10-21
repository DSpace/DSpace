package org.dspace.workflow;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.rest.models.Manuscript;
import org.dspace.JournalUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: Daisie Huang
 *
 * An executable class which determines if a review item is old enough to be purged.
 * Old items are returned to the submitter's workspace.
 *
 */
public class AutoReturnReviewItem {
    private static Date olderThanDate;
    private static Integer olderThan = ConfigurationManager.getIntProperty("autoreturnreview.olderthan");
    private static boolean testMode = false;

    private static final Logger log = Logger.getLogger(AutoReturnReviewItem.class);

    static {
        setOlderThanDate();
    }

    public static void main(String[] args) throws ApproveRejectReviewItemException, ParseException {
        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("i", "wfitemid", true, "The workflow item id");
        options.addOption("a", "all", false, "Process all review items");
        options.addOption("t", "test", false, "test mode");
        options.addOption("h", "help", false, "help");

        setOlderThanDate();

        CommandLine line = parser.parse(options, args);
        if (line.hasOption('t')) {
            testMode = true;
            System.out.println("-----------------------------------\n" +
                               "Test Mode: no items will be purged.\n" +
                               "-----------------------------------");
        }
        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("dspace purge-review-item\n", options);
        } else if(line.hasOption('i')) {
            // get a WorkflowItem using a workflow ID
            Integer workflowItemId = Integer.parseInt(line.getOptionValue('i'));
            Context c = null;
            try {
                c = new Context();
                WorkflowItem wfi = WorkflowItem.find(c, workflowItemId);
                purgeOldItem(c, wfi);
            } catch (SQLException ex) {
                throw new ApproveRejectReviewItemException(ex);
            } catch (AuthorizeException ex) {
                throw new ApproveRejectReviewItemException(ex);
            } catch (IOException ex) {
                throw new ApproveRejectReviewItemException(ex);
            } finally {
                if (c != null) {
                    try {
                        c.complete();
                    } catch (SQLException ex) {
                        // Swallow it
                    } finally {
                        c = null;
                    }
                }
            }
        } else if (line.hasOption('a')) {
            System.out.println("*** Purging all items older than " + olderThanDate.toString());
            purgeOldItems();
        } else {
            System.out.println("No option was provided.");
            System.exit(1);
        }
    }

    private static void purgeOldItems() {
        Context c = null;
        try {
            c = new Context();
            List<ClaimedTask> claimedTasks = ClaimedTask.findAllInStep(c, "reviewStep");
            for (ClaimedTask task : claimedTasks) {
                int wfiID = task.getWorkflowItemID();
                WorkflowItem wfi = WorkflowItem.find(c, wfiID);
                purgeOldItem(c, wfi);
            }
            c.complete();
        } catch (Exception ex) {
            log.error("Couldn't purge old items");
            if (c != null) {
                c.abort();
            }
        }
    }

    private static void purgeOldItem(Context context, WorkflowItem wfi) {
        try {
            if (wfi != null) {
                //Check for a valid task
                Item item = wfi.getItem();
                DryadDataPackage dryadDataPackage = DryadDataPackage.findByWorkflowItemId(context, wfi.getID());
                if (!DryadWorkflowUtils.isItemInReview(context, wfi)) {
                    log.debug("Item " + item.getID() + " not found or not in review");
                } else {
                    // make sure that this item is updated according to the ApproveReject mechanism:
                    if (!testMode) {
                        log.info("check to see if item " + item.getID() + " is approved or rejected");
                        Manuscript databaseManuscript = JournalUtils.getStoredManuscriptForWorkflowItem(context, dryadDataPackage);
                        if (databaseManuscript != null && databaseManuscript.isAccepted()) {
                            ApproveRejectReviewItem.processWorkflowItemUsingManuscript(context, wfi, databaseManuscript);
                        }
                    }
                    if (itemIsOldItemInReview(item)) {
                        if (testMode) {
                            log.info("TEST: return item " + item.getID());
                        } else {
                            log.info("returning item " + item.getID());
                            context.turnOffAuthorisationSystem();
                            String reason = "Since this submission has been in review for more than " + olderThan + " years, we assume it is no longer needed. Feel free to delete it from your workspace.";
                            EPerson ePerson = EPerson.findByEmail(context, ConfigurationManager.getProperty("system.curator.account"));
                            //Also return all the data files
                            Item[] dataFiles = DryadWorkflowUtils.getDataFiles(context, item);
                            for (Item dataFile : dataFiles) {
                                try {
                                    WorkflowManager.rejectWorkflowItem(context, WorkflowItem.findByItemId(context, dataFile.getID()), ePerson, null, reason, false);
                                } catch (Exception e) {
                                    throw new IOException(e);
                                }
                            }
                            WorkflowManager.rejectWorkflowItem(context, wfi, ePerson, null, reason, true);

                            context.restoreAuthSystemState();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("couldn't purge review workflowitem " + wfi.getID());
        }
    }

    private static boolean itemIsOldItemInReview(Item item) {
        DCValue[] provenanceValues = item.getMetadata("dc.description.provenance");
        if (provenanceValues != null && provenanceValues.length > 0) {
            for (DCValue provenanceValue : provenanceValues) {
                //Submitted by Ricardo Rodríguez (ricardo_eyre@yahoo.es) on 2014-01-30T12:35:00Z workflow start=Step: requiresReviewStep - action:noUserSelectionAction\r
                String provenance = provenanceValue.value;
                Pattern pattern = Pattern.compile(".* on (.+?)Z.+requiresReviewStep.*");
                Matcher matcher = pattern.matcher(provenance);
                if (matcher.find()) {
                    String dateString = matcher.group(1);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    Date reviewDate = null;
                    try {
                        reviewDate = sdf.parse(dateString);
                        log.info("item " + item.getID() + " entered review on " + reviewDate.toString());
                        return reviewDate.before(olderThanDate);
                    } catch (Exception e) {
                        log.error("couldn't find date in provenance for item " + item.getID() + ": " + dateString);
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private static void setOlderThanDate() {
        if (olderThan < 1) {
            olderThan = 2;
        }
        Calendar calendar = new GregorianCalendar();
        calendar.roll(Calendar.YEAR, -olderThan);

        olderThanDate = calendar.getTime();
    }
}
