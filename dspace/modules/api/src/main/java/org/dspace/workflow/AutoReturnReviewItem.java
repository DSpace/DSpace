package org.dspace.workflow;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.rest.models.Manuscript;
import org.dspace.JournalUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.*;

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
                DryadDataPackage dryadDataPackage = DryadDataPackage.findByWorkflowItemId(c, workflowItemId);
                purgeOldItem(c, dryadDataPackage);
            } catch (SQLException ex) {
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
                DryadDataPackage dryadDataPackage = DryadDataPackage.findByWorkflowItemId(c, wfiID);
                purgeOldItem(c, dryadDataPackage);
            }
            c.complete();
        } catch (Exception ex) {
            log.error("Couldn't purge old items");
            if (c != null) {
                c.abort();
            }
        }
    }

    private static void purgeOldItem(Context context, DryadDataPackage dryadDataPackage) {
        try {
            if (!dryadDataPackage.isPackageInReview(context)) {
                log.debug("Package " + dryadDataPackage.getIdentifier() + " not found or not in review");
            } else {
                // make sure that this item is updated according to the ApproveReject mechanism:
                if (!testMode) {
                    log.info("check to see if package " + dryadDataPackage.getIdentifier() + " is approved or rejected");
                    Manuscript databaseManuscript = JournalUtils.getStoredManuscriptForPackage(context, dryadDataPackage);
                    if (databaseManuscript != null && databaseManuscript.isAccepted()) {
                        ApproveRejectReviewItem.processReviewPackageUsingManuscript(context, dryadDataPackage, databaseManuscript);
                    }
                }
                if (packageIsOldReviewPackage(dryadDataPackage)) {
                    if (testMode) {
                        log.info("TEST: return package " + dryadDataPackage.getIdentifier());
                    } else {
                        log.info("returning package " + dryadDataPackage.getIdentifier());
                        String reason = "Since this submission has been in review for more than " + olderThan + " years, we assume it is no longer needed. Feel free to delete it from your workspace.";
                        dryadDataPackage.rejectPackageUsingManuscript(context, null, reason);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("couldn't purge review package " + dryadDataPackage.getIdentifier());
        }
    }

    private static boolean packageIsOldReviewPackage(DryadDataPackage dryadDataPackage) {
        Date reviewDate = dryadDataPackage.getEnteredReviewDate();
        if (reviewDate == null) {
            return false;
        }
        return reviewDate.before(olderThanDate);
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
