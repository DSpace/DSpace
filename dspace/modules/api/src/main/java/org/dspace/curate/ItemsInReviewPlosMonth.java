/**
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree and available
 * online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.util.Date;
import java.text.DateFormat;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.Constants;

import org.dspace.workflow.WorkflowItem;
import org.datadryad.api.DryadDataPackage;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

import org.apache.log4j.Logger;

/**
 * PlosItemsReviewMonth reports on items from PLOS publications that have been
 * in the review workflow longer than one month.
 *
 * The task succeeds if it was able to calculate the correct result.
 *
 * Input: a collection (any collection) Output: a CSV indicating simple
 * information about the PLOS data packages that have been in review longer than
 * one month
 *
 * @author Debra Fagan/Ryan Scherle
 */
@Distributive
public class ItemsInReviewPlosMonth extends AbstractCurationTask {

    private static Logger log = Logger.getLogger(FileSimpleStats.class);
    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    Context context;

    // Database objects
    static final String DB_MANUSCRIPT_TABLE = "manuscript";
    static final String DB_COLUMN_ID = "manuscript_id";
    static final String DB_COLUMN_ORGANIZATION_ID = "organization_id";
    static final String DB_COLUMN_MSID = "msid";
    static final String DB_COLUMN_VERSION = "version";
    // active is stored as String because DatabaseManager doesn't support Boolean
    static final String DB_COLUMN_ACTIVE = "active";
    static final String DB_COLUMN_JSON_DATA = "json_data";

    static final String DB_ACTIVE_TRUE = String.valueOf(true);
    static final String DB_ACTIVE_FALSE = String.valueOf(false);

    @Override
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);

        try {
            context = new Context();
        } catch (SQLException e) {
            log.fatal("Cannot initialize database connection", e);
        }
    }

    /**
     * returns data from the manuscript table based on the given Manuscript ID
     * and organization code
     */
    private String getManuscriptData(Context myContext, String msid, String organizationCode) throws SQLException, IOException {
        String query = "SELECT * FROM MANUSCRIPT WHERE msid = ? and active = ?";
        TableRow row = DatabaseManager.querySingleTable(myContext, DB_MANUSCRIPT_TABLE, query, msid, DB_ACTIVE_TRUE);

        String json_data = row.getStringColumn(DB_COLUMN_JSON_DATA);

        if (row != null) {
            String the_json_data = row.getStringColumn(DB_COLUMN_JSON_DATA);
            return the_json_data;
        } else {
            return null;
        }
    }

    /**
     * returns the number of days between today's date and anotherDateMS, which
     * is passed in
     */
    public static int numDaysSince(long anotherDateMS) {

        Date todayDate = new Date();
        long todayDateMS = todayDate.getTime();
        long timeBetweenDatesMS = todayDateMS - anotherDateMS;
        long timeInReview = timeBetweenDatesMS / (24 * 60 * 60 * 1000);
        int numDaysInReview = (int) timeInReview;
        return numDaysInReview;
    }

    /**
     * Perform 
     *
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException {
        try {

            if (dso.getType() == Constants.COLLECTION) {
                // output headers for the CSV file that will be created by processing all items in this collection
                report("itemID, publicationName, lastModificationDate");

                // Iterate over the workflow "collection", calling this perform method on each item.
                // This bypasses the normal functionality of the curation task system, since items in
                // workflow don't yet belong to a real collection.
                WorkflowItem[] wfis = WorkflowItem.findAll(context);
                for (int i = 0; i < wfis.length; i++) {
                    perform(wfis[i].getItem());
                }

            } else if (dso.getType() == Constants.ITEM) {
                // determine whether this item is in the review workflow
                // workflow stage is stored in taskowner table

                DryadDataPackage dataPackage = new DryadDataPackage((Item) dso);
                log.debug("processing " + dataPackage.getItem().getID());
                WorkflowItem wfi = dataPackage.getWorkflowItem(context);
                if (wfi != null) {
                    log.debug(" -- is in workflow");
                    int workflowID = wfi.getID();
                    TableRow tr = DatabaseManager.querySingleTable(context, "taskowner", "SELECT * FROM taskowner WHERE workflow_item_id= ?", workflowID);
                    if (tr != null && tr.getStringColumn("step_id").equals("reviewStep")) {
                        log.debug(" -- is in review");
                        // report on the item
                        int itemID = dataPackage.getItem().getID();

                        String publicationName = dataPackage.getPublicationName();

                        Date lastModificationDate = dataPackage.getItem().getLastModified();

                        // Select and write to file PLOS items that have been in review 30 days or more
                        int NUMBEROFDAYS = 30;
                        String PUBNAME = "plos";
                        String DRYADDOI = "doi:10.5061";
                        boolean notificationReceived = false;

                        int numDaysInReview = numDaysSince(lastModificationDate.getTime());

                        if ((publicationName.toLowerCase().contains(PUBNAME)) && (numDaysInReview >= NUMBEROFDAYS)) {
                        	// Check to see if we have a plos notification for the item:
                            //      1. Use doi to get manuscript number.
                            //      2. Use manuscript number to get manuscript from the Manuscript table
                            //      3. If manuscript's json_data contains "doi:10.5061" print info to xml file

                            String packageDOI = dataPackage.getIdentifier();
                            String packageManuscriptNumber = dataPackage.getManuscriptNumber();

                            String plosManuscriptData = getManuscriptData(context, packageManuscriptNumber, PUBNAME);

                            if (plosManuscriptData != null) {
                                if (plosManuscriptData.toLowerCase().contains(DRYADDOI)) {
                                    report(itemID + ", " + publicationName + ", " + lastModificationDate);
                                }

                            }

                        }

                    }
                }

                // clean up the DSpace cache so we don't use excessive memory
                ((Item) dso).decache();
            }
        } catch (SQLException e) {
            log.fatal("Problem with database access", e);
        } catch (AuthorizeException e) {
            log.fatal("Problem with authorization", e);
        }

        return Curator.CURATE_SUCCESS;
    }
}
