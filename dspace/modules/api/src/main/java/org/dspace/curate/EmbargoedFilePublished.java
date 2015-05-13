/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.text.ParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dspace.JournalUtils;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.dspace.handle.HandleManager;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Bundle;
import org.dspace.content.Bitstream;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.identifier.IdentifierService;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.utils.DSpace;

import java.util.Date;
import java.text.DateFormat;
import java.util.Locale;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

/**
 * EmbargoedFilePublished reports items that have a citation in the 
 * metadata (dc.identifier.citation) yet still have an embargo in place
 * with a future embargo release date.
 *
 * EmbargoedFilePublished can be run from the command line within an up and
 * running VM to generate output showing the package DOI, article DOI, 
 * embargo type and embargo date for each item.
 *
 * For example, to run EmbargoedFilePublished from the command line within an
 * up and running VM and create a .csv output file, enter the following:
 * /opt/dryad/bin/dspace curate -v -t embargoedfilepublished -i 10255/3 -r - >~/temp/embargoedfilepublished.csv
 * To view the resulting output file:
 * cat ~/temp/embargoedfilepublished.csv
 *
 * The task succeeds if it was able to calculate the correct result.
 *
 * Input: a collection (any collection)
 * Output: a CSV indicating simple information about the data packages that are in review
 *
 * @author Debra Fagan
 * public class EmbargoedFilePublished
 */
@Suspendable
public class EmbargoedFilePublished extends AbstractCurationTask {

    private static Logger log = Logger.getLogger(DataPackageStats.class);
    private IdentifierService identifierService = null;
    DocumentBuilderFactory dbf = null;
    DocumentBuilder docb = null;
    static long total = 0;
    private Context context;
    

    @Override 
    public void init(Curator curator, String taskId) throws IOException {
    
    	super.init(curator, taskId);
	
		identifierService = new DSpace().getSingletonService(IdentifierService.class);            
	
		// init xml processing
		try {
	    	dbf = DocumentBuilderFactory.newInstance();
	    	docb = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
	  	  throw new IOException("unable to initiate xml processor", e);
		}
    }
    
    /**
     * Perform the curation task upon passed DSO
     *
     * @param dso the DSpace object
     * @throws IOException
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException {
	log.info("performing EmbargoedFilePublished task " + total++ );
	
	String handle = "\"[no handle found]\"";
	String packageDOI = "\"[no package DOI found]\"";
	String embargoedFileTitle = "\"[no embargoed file title found]\"";
	String articleCitation = "\"[no article citation found]\"";
	boolean articleCitationFound = false;
	boolean reportItem = false;
	boolean futureEmbargoDate = false;
	String embargoType = "none";
	String embargoDate = "";

	
	try {
	    context = new Context();
        } catch (SQLException e) {
	    log.fatal("Unable to open database connection", e);
	    return Curator.CURATE_FAIL;
	}
	
	if (dso.getType() == Constants.COLLECTION) {
	    // output headers for the CSV file that will be created by processing all items in this collection
	    report("packageDOI,embargoedFileTitle,embargoType,embargoDate");
	} else if (dso.getType() == Constants.ITEM) {
            Item item = (Item)dso;

	    try {
		
		// package DOI
		DCValue[] vals = item.getMetadata("dc.identifier");
		if (vals.length == 0) {
		    setResult("Object has no dc.identifier available " + handle);
		    log.error("Skipping -- no dc.identifier available for " + handle);
		    context.abort(); 
		    return Curator.CURATE_SKIP;
		} else {
		    for(int i = 0; i < vals.length; i++) {
			if (vals[i].value.startsWith("doi:")) {
			    packageDOI = vals[i].value;
			}
		    }
		}
		log.debug("packageDOI = " + packageDOI);		

		

		// article citation
		vals = item.getMetadata("dc.identifier.citation");
		if (vals.length == 0) {
		    log.debug("Object has no citation (dc.identifier.citation) " + handle);
		} else {
		    articleCitation = vals[0].value;
		    articleCitationFound = true;
		}
		log.debug("articleCitation = " + articleCitation);

		
		// process data files in packages
		log.debug("getting data file info");
		DCValue[] dataFiles = item.getMetadata("dc.relation.haspart");
		if (dataFiles.length == 0) {
		    setResult("Object has no dc.relation.haspart available " + handle);
		    log.error("Skipping -- Object has no dc.relation.haspart available " + handle);
		    context.abort();
		    return Curator.CURATE_SKIP;
		} else {
				    
		    // for each data file in the package

		    for(int i = 0; i < dataFiles.length; i++) {
			String fileID = dataFiles[i].value;
			log.debug(" ======= processing fileID = " + fileID);

			// get the DSpace Item for this fileID
			Item fileItem = getDSpaceItem(fileID);

			if(fileItem == null) {
			    log.error("Skipping data file -- it's null");
			    break;
			}
			log.debug("file internalID = " + fileItem.getID());
			
			// file title (of last file processed)
			vals = fileItem.getMetadata("dc.title");
			if (vals.length > 0) {
			    embargoedFileTitle = vals[0].value;
			    log.debug("File title: " + vals.length + " type " + embargoedFileTitle);
			}


			// embargo setting (of last file processed)
			vals = fileItem.getMetadata("dc.type.embargo");
			if (vals.length > 0) {
			    embargoType = vals[0].value;
			    log.debug("Embargo type: " + vals.length + " type " + embargoType);
			}
			vals = fileItem.getMetadata("dc.date.embargoedUntil");
			if (vals.length > 0) {
			    embargoDate = vals[0].value;
				futureEmbargoDate = futureDate(embargoDate);
			}

			if((embargoType.equals("untilArticleAppears"))) {
			    if (((embargoDate == null) || (embargoDate.equals(""))) || ((embargoDate != null) && (!(embargoDate.equals(""))) && (futureEmbargoDate))){

			    	if (articleCitationFound) {
			    		reportItem = true;
			    	}
				}
			}
			log.debug("embargoType = " + embargoType);
			log.debug("embargoDate = " + embargoDate);

		    }

		}
		log.info(handle + " done.");
	    } catch (Exception e) {
		log.fatal("Skipping -- Exception in processing " + handle, e);
		setResult("Object has a fatal error: " + handle + "\n" + e.getMessage());
		report("Object has a fatal error: " + handle + "\n" + e.getMessage());
		
		context.abort();
		return Curator.CURATE_SKIP;
	    }
	} else {
	    log.info("Skipping -- non-item DSpace object");
	    setResult("Object skipped (not an item)");
	    context.abort();
	    return Curator.CURATE_SKIP;
        }
        
	if (reportItem) {
		report(packageDOI + "," + embargoedFileTitle + "," + embargoType + "," + embargoDate);
	}

	log.debug("EmbargoedFilePublished complete");

	try { 
	    context.complete();
        } catch (SQLException e) {
	    log.fatal("Unable to close database connection", e);
	}
	return Curator.CURATE_SUCCESS;
    }



    private Item getDSpaceItem(String itemID) {
	Item dspaceItem = null;
	try {
	    dspaceItem = (Item)identifierService.resolve(context, itemID);  
        } catch (IdentifierNotFoundException e) {
	    log.fatal("Unable to get DSpace Item for " + itemID, e);
	} catch (IdentifierNotResolvableException e) {
	    log.fatal("Unable to get DSpace Item for " + itemID, e);
	}

	return dspaceItem;
    }


    
    /** returns true if the date given is after today's date and false if it is not */
	public static boolean futureDate(String someDate) {
	
        boolean future = false;

      	if ((someDate != null) && (someDate != "")) {
      		try {
        		if (new SimpleDateFormat("yyyy-MM-dd").parse(someDate).after(new Date())) {
        			future = true;
        		}
      		} catch (ParseException e) {}
		} 
		
        return future;
	} 
    
    
    
}

