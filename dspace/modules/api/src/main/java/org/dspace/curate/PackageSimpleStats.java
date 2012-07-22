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
public class PackageSimpleStats extends AbstractCurationTask {

   private static Logger log = Logger.getLogger(PackageSimpleStats.class);

    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    // map of embargo types to counts of items with those types
    private Map<String, Integer> embargoTable = new HashMap<String, Integer>();

    /**
       Distribute the process across all items in the collection, then report the results.
     **/
    @Override
    public int perform(DSpaceObject dso) throws IOException {
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
	
	// get embargo type
	DCValue[] vals = item.getMetadata("dc.type.embargo");
	if (vals.length == 0) {
	    // assume no embargo chosen ("publish immediately")
	    // increment counter for "none"
	    Integer count = embargoTable.get("none");
	    if (count == null || count == 0) {
		count = 1;
	    } else {
		count += 1;
	    }
	    embargoTable.put("none", count);
	} else {
	    // increment counter for this embargo type
	    String emType = vals[0].value;
	    Integer count = embargoTable.get(emType);
	    if (count == null || count == 0) {
		count = 1;
	    } else {
		count += 1;
	    }
	    embargoTable.put(emType, count);
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
            for (String emType : embargoTable.keySet()) {
                sb.append(emType).append(", ").
		    append(embargoTable.get(emType)).append("\n");
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
