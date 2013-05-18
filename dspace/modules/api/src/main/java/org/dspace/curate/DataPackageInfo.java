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
import java.util.List;
import java.net.URL;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.core.Context;
import org.dspace.core.Constants;

import org.apache.log4j.Logger;

/**
 * DataPackageInfo retrieves basic information about an input data package.
 *
 * This report is intended for quick and simple information. It does not
 * iterate over the data files that belong to a data package. For more
 * detailed statistics, see DataPackageStats.
 *
 * The task succeeds if it was able to locate all required stats, otherwise it fails.
 * Originally based on the RequiredMetadata task by Richard Rodgers.
 *
 * Input: a single data package OR a collection that contains data packages
 * Output: CSV file with appropriate stats 
 * @author Ryan Scherle
 */
@Suspendable
public class DataPackageInfo extends AbstractCurationTask {

    private static Logger log = Logger.getLogger(DataPackageStats.class);

    DocumentBuilderFactory dbf = null;
    DocumentBuilder docb = null;
    static long total = 0;
    
    @Override 
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);

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
	log.info("performing DataPackageStats task " + total++ );


	String handle = "[no handle found]";
	String doi = "[no DOI found]";
	String journal = "[no journal found]";
	String dateAccessioned = "";
	
	if (dso.getType() == Constants.COLLECTION) {
	    // output headers for the CSV file that will be created by processing all items in this collection
	    report("handle, doi, journal, dateAccessioned");
	} else if (dso.getType() == Constants.ITEM) {
            Item item = (Item)dso;
	    
	    try {
		// handle
		handle = item.getHandle();
		if (handle == null) {
		    // we are still in workflow - no handle assigned
		    handle = "in workflow";
		}
		log.info("handle = " + handle);

		// DOI
		DCValue[] vals = item.getMetadata("dc.identifier");
		if (vals.length == 0) {
		    setResult("Object has no dc.identifier available " + handle);
		    log.error("Object has no dc.identifier available " + handle);
		    return Curator.CURATE_FAIL;
		} else {
		    for(int i = 0; i < vals.length; i++) {
			if (vals[i].value.startsWith("doi:")) {
			    doi = vals[i].value;
			}
		    }
		}
		log.debug("DOI = " + doi);


		// journal
		vals = item.getMetadata("prism.publicationName");
		if (vals.length == 0) {
		    setResult("Object has no prism.publicationName available " + handle);
		    log.error("Object has no prism.publicationName available " + handle);
		    return Curator.CURATE_SKIP;
		} else {
		    journal = vals[0].value;
		}
		log.debug("journal = " + journal);

		// accession date
		vals = item.getMetadata("dc.date.accessioned");
		if (vals.length == 0) {
		    setResult("Object has no dc.date.accessioned available " + handle);
		    log.error("Object has no dc.date.accessioned available " + handle);
		    return Curator.CURATE_FAIL;
		} else {
		    dateAccessioned = vals[0].value;
		}
		log.debug("dateAccessioned = " + dateAccessioned);
		
	    } catch (Exception e) {
		log.fatal("Exception in processing", e);
		setResult("Object has a fatal error: " + handle + "\n" + e.getMessage());
		report("Object has a fatal error: " + handle + "\n" + e.getMessage());
		return Curator.CURATE_FAIL;
	    }
	} else {
	    log.info("skipping non-item DSpace object");
	    setResult("Object skipped (not an item)");
	    return Curator.CURATE_SKIP;
        }

	setResult("Last processed item = " + handle + " -- " + doi);
	report(handle + ", " + doi + ", \"" + journal + "\", " + dateAccessioned);

	
	log.debug("DataPackageStats complete");
	return Curator.CURATE_SUCCESS;
    }

    /**
       An XML utility method that returns the text content of a node.
    **/
    private String getNodeText(Node aNode) {
	return aNode.getChildNodes().item(0).getNodeValue();
    }

    private Item getDSpaceItem(String shortHandle) {
	Item dspaceItem = null;
	try {
	    Context context = new Context();
	    dspaceItem = (Item)HandleManager.resolveToObject(context, shortHandle);
	    context.complete();
        } catch (SQLException e) {
	    log.fatal("Unable to get DSpace Item for " + shortHandle, e);
        }

	return dspaceItem;
    }
    
}
