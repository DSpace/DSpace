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
 *
 * /opt/dryad/bin/dspace curate -v -t embargonotlifted -i 10255/3 -r - >~/temp/embargonotlifted.csv
 * ~/temp/embargonotlifted.csv 
 *
 * Input: a collection of data packages
 * Output: a CSV indicating journal names and the number of data packages associated with each
 *
 * @author Debra Fagan
 */ 
 
@Distributive
public class EmbargoFilePublished extends AbstractCurationTask {

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
        
    
    /**
       Perform 
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
