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
 * PackageSimpleStats generates a list of data packages with useful statistical information.
 *
 * The task succeeds if it was able to calculate the correct result.
 *
 * Originally adapted from the ProfileFormats curation task by Richard Rodgers.
 *
 * Input: a collection of data packages
 * Output: 
 *
 * @author Ryan Scherle
 */
@Distributive
public class PackageSimpleStats extends AbstractCurationTask {

   private static Logger log = Logger.getLogger(PackageSimpleStats.class);

    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    // dates for the specific time window to analyze
    private static final String START_DATE_STRING = "2012-01-01T00:00:00Z";
    private static final String END_DATE_STRING = "2012-07-01T00:00:00Z";
    private static Date START_DATE = null;
    private static Date END_DATE = null;
    
    // accumulators for counting integrated submissions
    private int numIntegrated = 0;
    private int windowNumIntegrated = 0;
    
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

	distribute(dso);
        formatResults();
        return Curator.CURATE_SUCCESS;
    }

    /**
       Process a single item.
     **/
    @Override
    protected void performItem(Item item) throws SQLException, IOException {
	
	DCValue[] vals = item.getMetadata("dc.identifier.manuscriptNumber");
	if (vals.length > 0) {
	    numIntegrated = numIntegrated + 1;
	}


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
	// only increment counts in the window accumulator if the item's accession date is within the window
	if(itemDate.after(START_DATE) && itemDate.before(END_DATE) && vals.length > 0) {
	    windowNumIntegrated = windowNumIntegrated + 1;
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
	    sb.append("Total integrated submissions: " + numIntegrated + "\n");
	    sb.append("Integrated submissions within date window: " + windowNumIntegrated + "\n");
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
