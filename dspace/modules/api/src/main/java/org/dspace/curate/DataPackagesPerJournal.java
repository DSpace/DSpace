/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.text.DateFormat;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.text.ParseException;
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
 * DataPackagesPerJournal generates a list of journals and the number of data packages associated with them. This
 * statistic can be calculated for any timeframe by adjusting the static dates in this class.
 *
 * The task succeeds if it was able to calculate the correct result.
 *
 * Originally adapted from the ProfileFormats curation task by Richard Rodgers.
 *
 * Input: a collection of data packages
 * Output: a CSV indicating journal names and the number of data packages associated with each
 * @author Ryan Scherle
 */
@Distributive
public class DataPackagesPerJournal extends AbstractCurationTask {

    private static final String START_DATE_STRING = "2012-01-01T00:00:00Z";
    private static final String END_DATE_STRING = "2013-01-01T00:00:00Z";
    private static Date START_DATE = null;
    private static Date END_DATE = null;
    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    
    private static Logger log = Logger.getLogger(DataPackagesPerJournal.class);

    // map of journal names to counts of data packages
    private Map<String, Integer> journalTable = new HashMap<String, Integer>();

    /**
       Distribute the process across all items in the collection, then report the results.
     **/
    @Override
    public int perform(DSpaceObject dso) throws IOException {
	try {
	    START_DATE = df.parse(START_DATE_STRING);
	    END_DATE = df.parse(END_DATE_STRING);
	    report("Distribution of archived items, " + START_DATE_STRING + " to " + END_DATE_STRING);
	} catch (ParseException ex) {
	    log.fatal("Unable to parse start or end date", ex);
	    return Curator.CURATE_FAIL;
	}

	journalTable.clear();
        distribute(dso);
        formatResults();
        return Curator.CURATE_SUCCESS;
    }

    /**
       For a single item, increment the counter for the associated journal.
     **/
    @Override
    protected void performItem(Item item) throws SQLException, IOException {
	String journal = "UNKNOWN";

	// determine whether this item falls in our target date range
	// TODO: change this to an external selector based on a query
	String accDate = null;
	DCValue[] accDates = item.getMetadata("dc.date.accessioned");
	if (accDates.length == 0) {
	    log.error("Object has no dc.date.accessioned, " + item.getHandle());
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

	if(itemDate.before(START_DATE) || itemDate.after(END_DATE)) {
	    log.debug("skipping item with accession date out of range: " + accDate);
	} else {
	    // get journal name
	    DCValue[] vals = item.getMetadata("prism.publicationName");
	    if (vals.length == 0) {
		log.error("Object has no prism.publicationName available " + item.getHandle());
	    } else {
		journal = vals[0].value;
	    }
	    log.debug("journal = " + journal + ", counting item " + item.getHandle());
	    
	    // increment counter for this journal
	    Integer count = journalTable.get(journal);
	    if (count == null || count == 0) {
		count = 1;
	    } else {
		count += 1;
	    }
	    journalTable.put(journal, count);
	}
	    
	// clean up the DSpace cache so we don't use excessive memory
	item.decache();
    }           

    
    private void formatResults() throws IOException {
        try {
            Context c = new Context();
            StringBuilder sb = new StringBuilder();
            for (String journal : journalTable.keySet()) {
                sb.append(journal).append(", ").
		    append(journalTable.get(journal)).append("\n");
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
