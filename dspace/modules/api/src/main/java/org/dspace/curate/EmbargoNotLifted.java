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
public class EmbargoNotLifted extends AbstractCurationTask {

	private static final String EMBARGO_TYPE = "untilArticleAppears";
    private static Logger log = Logger.getLogger(EmbargoNotLifted.class);

    /**
       Distribute the process across all items in the collection, then report the results.
     **/
    @Override
    public int perform(DSpaceObject dso) throws IOException {
	try {
	    report("Embargo Check");
	} catch (ParseException ex) {
	    log.fatal("Unable to parse start or end date", ex);
	    return Curator.CURATE_FAIL;
	}

        distribute(dso);
        return Curator.CURATE_SUCCESS;
    }

    /**
       For a single item, increment the counter for the associated journal.
     **/
    @Override
    protected void performItem(Item item) throws SQLException, IOException {


// ******** DF
	String eType = null;
	DCValue[] eTypes = item.getMetadata("dc.type.embargo");
	if (eTypes.length > 0) {
		eType = eTypes[0].value;

		if (eType.equals("untilArticleAppears")) {
			String eDate = null;
			DCValue[] eDates = item.getMetadata("dc.date.embargoedUntil");
			if (eDates.length == 0) {
				report("Object has no dc.date.embargoedUntil: " + item.getHandle());
			} else {
				eDate = eDates[0].value;
				if (futureDate (eDate)){
					String citation = null;
					DCValue[] citations = item.getMetadata("dc.identifier.citation");
					if (citations.length > 0) {
						report("Object has citation but embargoes not lifted: " + item.getHandle());
	    				return;
	    			}
				}			
			}
	    }
	} else {
	    return;
	}
	
	
// ******** DF	
	
/*	
			// embargo setting (of last file processed)
			vals = fileItem.getMetadata("dc.type.embargo");
			if (vals.length > 0) {
			    embargoType = vals[0].value;
			    log.debug("EMBARGO vals " + vals.length + " type " + embargoType);
			}
			vals = fileItem.getMetadata("dc.date.embargoedUntil");
			if (vals.length > 0) {
			    embargoDate = vals[0].value;
			}
			if((embargoType == null || embargoType.equals("") || embargoType.equals("none")) &&
			   (embargoDate != null && !embargoDate.equals(""))) {
			    // correctly encode embago type to "oneyear" if there is a date set, but the type is blank or none
			    embargoType = "oneyear";
			}
			log.debug("embargoType = " + embargoType);
			log.debug("embargoDate = " + embargoDate);	
	
	
	
*/	
	
	
	
	
	
/*
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
*/
	// clean up the DSpace cache so we don't use excessive memory
	item.decache();
    }           


    
    /** returns true if the date given is after today's date and false if it is not */
	public static boolean futureDate(String someDate) {
	
        boolean future = FALSE;

        if (new SimpleDateFormat("yyyy-MM-dd").parse(someDate).after(new Date())) {
        	after = TRUE;
        }

        return future;
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
