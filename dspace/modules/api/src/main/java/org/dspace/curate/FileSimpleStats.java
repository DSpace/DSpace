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

import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Context;

import org.apache.log4j.Logger;

/**
 * FileSimpleStats generates a list of journals and the first date on which Dryad archived a submission from that journal.
 * It computes stats both for the repository as a whole, and for a specific time window.
 *
 * The task succeeds if it was able to calculate the correct result.
 *
 * Input: a collection of data files
 * Output: a CSV indicating simple statistics about data file objects
 *
 * @author Ryan Scherle
 */
@Distributive
public class FileSimpleStats extends AbstractCurationTask {

    private static Logger log = Logger.getLogger(FileSimpleStats.class);
    
    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    // dates for the specific time window to analyze
    private static final String START_DATE_STRING = "2012-01-01T00:00:00Z";
    private static final String END_DATE_STRING = "2012-07-01T00:00:00Z";
    private static Date START_DATE = null;
    private static Date END_DATE = null;

    // map of embargo types to counts of items with those types
    private Map<String, Integer> embargoTable = new HashMap<String, Integer>();
    private Map<String, Integer> windowEmbargoTable = new HashMap<String, Integer>();

    /**
       Distribute the process across all items in the collection, then report the results.
     **/
    @Override
    public int perform(DSpaceObject dso) throws IOException {
	try {
	    START_DATE = df.parse(START_DATE_STRING);
	    END_DATE = df.parse(END_DATE_STRING);
	} catch (ParseException ex) {
	    log.fatal("Unable to parse start or end date", ex);
	    return Curator.CURATE_FAIL;
	}

	windowEmbargoTable.clear();
        embargoTable.clear();
        distribute(dso);
        formatResults();
        return Curator.CURATE_SUCCESS;
    }

    /**
       For a single item, if the item has an earlier deposit date than currently known for the journal, update the date.
     **/
    @Override
    protected void performItem(Item item) throws SQLException, IOException {
	String journal = "UNKNOWN";

	// determine whether this item falls in our target date range
	// TODO: change this to an external selector based on a query
	String accDate = null;
	DCValue[] accDates = item.getMetadata("dc.date.available");
	if (accDates.length == 0) {
	    log.error("Object has no dc.date.available, " + item.getHandle());
	    return;
	} else {
	    accDate = accDates[0].value;
	}

	Date itemDate = null;
	try {
	    itemDate = df.parse(accDate);
	} catch (ParseException ex) {
	    log.error("Unable to parse date " + accDate + " in item " + item.getHandle());
	    return;
	}
	
	// get embargo type
	String emType = "none";
	DCValue[] vals = item.getMetadata("dc.type.embargo");
	if (vals.length == 0) {
	    // there is no type set; check if a date was set. If a date is set, the embargo was "oneyear" and was deleted.
	    DCValue[] emDateVals = item.getMetadata("dc.date.embargoedUntil");
	    if(emDateVals.length != 0) {
		String emDate = emDateVals[0].value;
		if(emDate != null && !emDate.equals("")) {
		    emType = "oneyear";
		}
	    }
	} else {
	    // there is a type set, so use it
	    emType = vals[0].value;
	}
	
	// increment counter for this embargo type
	Integer count = embargoTable.get(emType);
	if (count == null || count == 0) {
	    count = 1;
	} else {
	    count += 1;
	}
	embargoTable.put(emType, count);
	
	// only increment counts in the window table if the item's accession date is within the window
	if(itemDate.after(START_DATE) && itemDate.before(END_DATE)) {
	    Integer wcount = windowEmbargoTable.get(emType);
	    if (wcount == null || wcount == 0) {
		wcount = 1;
	    } else {
		wcount += 1;
	    }
	    windowEmbargoTable.put(emType, wcount);
	}
	
	// clean up the DSpace cache so we don't use excessive memory
	item.decache();
    }           

    /**
       Formats the results into a CSV list.
    **/
    private void formatResults() throws IOException {
        try {
            Context c = new Context();
            StringBuilder sb = new StringBuilder();
	    sb.append("File stats for entire repository: \n");
            for (String emType : embargoTable.keySet()) {
                sb.append(emType).append(", ").
		    append(embargoTable.get(emType)).append("\n");
            }

	    sb.append("File stats for time " + START_DATE_STRING + " to " + END_DATE_STRING + " \n");
            for (String wemType : windowEmbargoTable.keySet()) {
                sb.append(wemType).append(", ").
		    append(windowEmbargoTable.get(wemType)).append("\n");
            }
            report(sb.toString());
            setResult(sb.toString());
            c.complete();
        }
        catch (SQLException sqlE)
        {
	    log.fatal("Unable to process results", sqlE);
            throw new IOException(sqlE.getMessage(), sqlE);
        }
    }
}
