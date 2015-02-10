/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
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
 * ItemsInReview reports on the status of items in the review workflow.
 *
 * The task succeeds if it was able to calculate the correct result.
 *
 * Input: a collection (any collection)
 * Output: a CSV indicating simple information about the data packages that are in review
 *
 * @author Ryan Scherle
 */
@Distributive
public class ItemsInReview extends AbstractCurationTask {

    private static Logger log = Logger.getLogger(FileSimpleStats.class);
    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    Context context;

    @Override 
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);

        try {
            context = new Context();
        } catch (SQLException e) {
            log.fatal("Cannot initialize database connection", e);
        }
    }
    
    
    
    /** returns the number of days between today's date and anotherDateMS, which is passed in */
	public static int numDaysSinceModified(long anotherDateMS) {

   		
        Date todayDate = new Date();
        long todayDateMS = todayDate.getTime();

        long timeBetweenDatesMS = todayDateMS - anotherDateMS;
        long timeInReview = timeBetweenDatesMS / (24 * 60 * 60 * 1000);
        int numDaysInReview = (int) timeInReview;

        return numDaysInReview;
}
        
    
    /**
       Perform 
     **/
    @Override
    public int perform(DSpaceObject dso) throws IOException {
	try {
			// added following one line by DF 20150210

			report("itemID, publicationName, lastModificationDate, notificationReceived");
			
            if (dso.getType() == Constants.COLLECTION) {
                // output headers for the CSV file that will be created by processing all items in this collection
                report("itemID, publicationName, lastModificationDate, notificationReceived");

                // Iterate over the workflow "collection", calling this perform method on each item.
                // This bypasses the normal functionality of the curation task system, since items in
                // workflow don't yet belong to a real collection.
                WorkflowItem[] wfis = WorkflowItem.findAll(context);
                for(int i = 0; i < wfis.length; i++) {
                    perform(wfis[i].getItem());
                }

            } else if (dso.getType() == Constants.ITEM) {
                // determine whether this item is in the review workflow
                // workflow stage is stored in taskowner table
                
                DryadDataPackage dataPackage = new DryadDataPackage((Item)dso);
                log.debug("processing " + dataPackage.getItem().getID());
                WorkflowItem wfi = dataPackage.getWorkflowItem(context);
                if(wfi != null) {
                    log.debug(" -- is in workflow");
                    int workflowID = wfi.getID();
                    TableRow tr = DatabaseManager.querySingleTable(context,"taskowner", "SELECT * FROM taskowner WHERE workflow_item_id= ?", workflowID);
                    if(tr != null && tr.getStringColumn("step_id").equals("reviewStep")) {
                        log.debug(" -- is in review");
                        // report on the item
                        int itemID = dataPackage.getItem().getID();
                        
                        // Where is getPublicationName method located???  - *DF*
                        String publicationName = dataPackage.getPublicationName();

                        Date lastModificationDate = dataPackage.getItem().getLastModified();

                        
                        
                        
                        // Select and write to file PLOS items that have been in review 30 days or more - *DF*
						int numberOfDays = 31;
                        String pubName = "plos";
                        
                        String publicationNameLowerCase = publicationName.toLowerCase();

                        int numDaysInReview = numDaysSinceModified(lastModificationDate.getTime());
                    
                        if ( (publicationNameLowerCase.contains(pubName)) && (numDaysInReview >= numberOfDays) ) {
                        	// report whether we have a plos notification for the item
                        	//     1. search for the data package DOI in the manuscript table, json_data field. 
                        	//        It should appear in the dataReviewURL of the json, but may appear in the dataAvailabilityStatement
                        	//     2. select json_data from manuscript where json_data like '%resource/doi:10.5061/dryad.p5hd0%';
                        	
                        	//boolean notificationReceived = dataPackage.getPublicationName(); String doi = manuscript.dryadDataDOI; 
                        	                        
                        	// get DOI and manuscript number - *DF*
                        	String packageDOI = dataPackage.getIdentifier();
                        	String packageManuscriptNumber = dataPackage.getManuscriptNumber();
                        	
                        	// get manuscript based on manuscript number - *DF*
                        	Manuscript packageManuscript = new Manuscript(packageManuscriptNumber, STATUS_SUBMITTED);
                        	// getDataReviewURL does not currently exist - *DF*
                        	String packageDataReviewURL = packageManuscript.getDataReviewURL();
                        	
                        	boolean notificationReceived = false;
                        	if(packageDataReviewURL != null && !packageDataReviewURL.isEmpty()) {
                        		notificationReceived = true;
                        	}
                        	
                        	if (notificationReceived) {
                        		report(itemID + ", " + publicationName + ", " + lastModificationDate+ ", " + "Y");
                        	} else {
                        		report(itemID + ", " + publicationName + ", " + lastModificationDate+ ", " + "N");
                        	}
                        }
                    }
                }
                
                // clean up the DSpace cache so we don't use excessive memory
                ((Item)dso).decache();
            }
        } catch (SQLException e) {
	    log.fatal("Problem with database access", e);
	} catch (AuthorizeException e) {
            log.fatal("Problem with authorization", e);
        }
        
        return Curator.CURATE_SUCCESS;
    }    
}
 