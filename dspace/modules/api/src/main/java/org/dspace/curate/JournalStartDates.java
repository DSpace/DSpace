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
 * JournalStartDates generates a list of journals and the first date on which Dryad archived a submission from that journal.
 *
 * The task succeeds if it was able to calculate the correct result.
 *
 * Originally adapted from the ProfileFormats curation task by Richard Rodgers.
 *
 * Input: a collection of data packages
 * Output: a CSV indicating journal names and the date on which Dryad first received a submission associated with the journal
 *
 * @author Ryan Scherle
 */
@Distributive
public class JournalStartDates extends AbstractCurationTask {

   private static Logger log = Logger.getLogger(JournalStartDates.class);

    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    // map of journal names to earliest accession date for an item in that journal
    private Map<String, Date> journalTable = new HashMap<String, Date>();

    /**
       Distribute the process across all items in the collection, then report the results.
     **/
    @Override
    public int perform(DSpaceObject dso) throws IOException {
        journalTable.clear();
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
	
	// get journal name
	DCValue[] vals = item.getMetadata("prism.publicationName");
	if (vals.length == 0) {
	    log.error("Object has no prism.publicationName available " + item.getHandle());
	} else {
	    journal = vals[0].value;
	}
	
	Date itemDate = getAccessionDate(item);
	log.debug("journal = " + journal + ", date = " + itemDate);

	
	// update the date setting for this journal
	if(itemDate != null) {
	    Date currJournalDate = journalTable.get(journal);
	    if (currJournalDate == null || currJournalDate.after(itemDate)) {
		journalTable.put(journal, itemDate);	    
	    }
	}

	// clean up the DSpace cache so we don't use excessive memory
	item.decache();
    }           

    /**
       Returns the accession date for the given item. If there is no accession date, returns null.
     **/
    private Date getAccessionDate(Item item) {
	String itemDateString = null;
	Date itemDate = null;

	DCValue[] accDates = item.getMetadata("dc.date.available");
	if (accDates.length == 0) {
	    log.error("Object has no dc.date.available, " + item.getHandle());
	} else {
	    itemDateString = accDates[0].value;

	    try {
		itemDate = df.parse(itemDateString);
	    } catch (ParseException ex) {
		log.error("Unable to parse date " + itemDateString + " in item " + item.getHandle());
	    }
	}
	return itemDate;
    }

    /**
       Formats the list of journals and dates into a CSV list.
    **/
    private void formatResults() throws IOException {
        try {
            Context c = new Context();
            StringBuilder sb = new StringBuilder();
            for (String journal : journalTable.keySet()) {
                sb.append(journal).append(", ").
		    append(df.format(journalTable.get(journal))).append("\n");
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
