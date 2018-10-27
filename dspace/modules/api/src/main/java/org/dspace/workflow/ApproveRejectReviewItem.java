package org.dspace.workflow;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.api.DryadJournalConcept;
import org.datadryad.rest.models.Manuscript;
import org.dspace.JournalUtils;
import org.dspace.core.Context;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 19-aug-2010
 * Time: 14:25:02
 *
 * An executable class which determines if an item is to be rejected or accepted,
 * when accepted the submission is sent to the next step
 *
 * Updated by dcl9 (dan.leehr@nescent.org) 04-Sep-2014 - exposing reviewItem as static methods
 */
public class ApproveRejectReviewItem {
    private static final String PUBLICATION_DOI = "dc.relation.isreferencedby";
    private static final String MANUSCRIPT = "dc.identifier.manuscriptNumber";
    private static final String ARTICLE_TITLE = "dc.title";
    private static final String ABSTRACT = "dc.description";
    private static final String PUBLICATION_DATE = "dc.date.issued";

    private static final Logger log = Logger.getLogger(ApproveRejectReviewItem.class);
    public static void main(String[] args) throws ApproveRejectReviewItemException, ParseException {
        // create an options object and populate it
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("m", "manuscriptnumber", true, "The manuscript number");
        options.addOption("i", "wfitemid", true, "The workflow item id");
        options.addOption("j", "journalcode", true, "The journal code");
        options.addOption("a", "approved", true, "Whether or not the the application will be approved, can either be true or false");
        options.addOption("h", "help", false, "help");


        CommandLine line = parser.parse(options, args);
        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("dspace review-item\n", options);
            System.exit(1);
        }
        Boolean approved;

        if(line.hasOption('a')){
            approved = Boolean.valueOf(line.getOptionValue('a'));
        }else{
            System.out.println("No result (approved true or false) was given");
            System.exit(1);
            return;
        }

        if(line.hasOption('m')){
            // get a WorkflowItem using a manuscript number
            String manuscriptNumber = line.getOptionValue('m');
            String journalCode = line.getOptionValue('j');
            if (journalCode == null) {
                System.out.println("No journal code was given. This is needed to match the manuscript number with an item in review.");
                System.exit(1);
            }
            DryadJournalConcept journalConcept = JournalUtils.getJournalConceptByJournalID(journalCode);
            String status = approved ? Manuscript.STATUS_ACCEPTED : Manuscript.STATUS_REJECTED;
            Manuscript manuscript = new Manuscript(journalConcept);
            manuscript.setManuscriptId(manuscriptNumber);
            manuscript.setStatus(status);
            processWorkflowItemsUsingManuscript(manuscript);
        } else if(line.hasOption('i')) {
            // get a WorkflowItem using a workflow ID
            Integer wfItemId = Integer.parseInt(line.getOptionValue('i'));
            Context c = null;
            try {
                c = new Context();
                DryadDataPackage dryadDataPackage = DryadDataPackage.findByWorkflowItemId(c, wfItemId);
                Manuscript manuscript = new Manuscript();
                manuscript.setStatus(approved ? Manuscript.STATUS_ACCEPTED : Manuscript.STATUS_REJECTED);
                processReviewPackageUsingManuscript(c, dryadDataPackage, manuscript);
            } catch (SQLException ex) {
                throw new ApproveRejectReviewItemException(ex);
            } finally {
                if(c != null) {
                    try {
                        c.complete();
                    } catch (SQLException ex) {
                        // Swallow it
                    } finally {
                        c = null;
                    }
                }
            }
        } else {
            System.out.println("No manuscript number or workflow ID was given. One of these must be provided to identify the correct item in the review stage.");
            System.exit(1);
        }
    }

    public static void processWorkflowItemsUsingManuscript(Manuscript manuscript) throws ApproveRejectReviewItemException {
        try {
            Context c = new Context();

            ArrayList<WorkflowItem> workflowItems = new ArrayList<WorkflowItem>();

            if (manuscript.getDryadDataDOI() != null) {
                try {
                    WorkflowItem wfi = WorkflowItem.findByDOI(c, manuscript.getDryadDataDOI());
                    if (wfi != null) {
                        workflowItems.add(wfi);
                    }
                } catch (ApproveRejectReviewItemException e) {
    //                LOGGER.debug ("no workflow items matched DOI " + manuscript.dryadDataDOI);
                }
            }

            workflowItems.addAll(WorkflowItem.findAllByManuscript(c, manuscript));
            for (WorkflowItem wfi : workflowItems) {
                try {
                    DryadDataPackage dryadDataPackage = DryadDataPackage.findByWorkflowItemId(c, wfi.getID());
                    processReviewPackageUsingManuscript(c, dryadDataPackage, manuscript);
                } catch (ApproveRejectReviewItemException e) {
                    throw new ApproveRejectReviewItemException("Exception caught while reviewing item " + wfi.getItem().getID() + ": " + e.getMessage(), e);
                }
            }
        } catch (SQLException ex) {
            throw new ApproveRejectReviewItemException(ex);
        }
    }

    public static void processReviewPackageUsingManuscript(Context c, DryadDataPackage dryadDataPackage, Manuscript manuscript) throws ApproveRejectReviewItemException {
        try {
            // update duplicate submission metadata for this item.
            dryadDataPackage.updateDuplicatePackages(c);
            if (dryadDataPackage.isPackageInReview(c)) {
                if (Manuscript.statusIsApproved(manuscript.getStatus())) { // approve
                    dryadDataPackage.approvePackageUsingManuscript(c, manuscript);
                } else { // reject
                    dryadDataPackage.rejectPackageUsingManuscript(c, manuscript);
                }
            }
        } catch (Exception ex) {
            throw new ApproveRejectReviewItemException(ex);
        }
    }
}
