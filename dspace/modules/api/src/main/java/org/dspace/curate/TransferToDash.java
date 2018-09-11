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

import org.apache.log4j.Logger;
import org.datadryad.api.DryadJournalConcept;

/**
 * TransferToDash processes a data package and sends it to a DASH-based Dryad system.
 *
 * The task succeeds if it was able to process all required metadata and stats,
 * otherwise it fails. If the transfer was successful, results are recorded in the
 * metadata field dryad.dashTransferDate
 *
 * Originally based on the DataPackageStats curation task.
 *
 * Input: a single data package OR a collection that contains data packages
 * Output: CSV file with appropriate stats
 * Side Effects: Data package is transferred to DASH-based Dryad, Data Package is updated
 *               with metadata indicating date of successfull transfer.
 *
 * @author Ryan Scherle
 */

@Suspendable
public class TransferToDash extends AbstractCurationTask {

    private static Logger log = Logger.getLogger(TransferToDash.class);
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
	log.info("performing TransferToDash task " + total++ );
	
	String handle = "\"[no handle found]\"";
	String packageDOI = "\"[no package DOI found]\"";
	String articleDOI = "\"[no article DOI found]\"";
	String journal = "[no journal found]"; // don't add quotes here, because journal is always quoted when output below
	String numberOfFiles = "\"[no numberOfFiles found]\"";
	long packageSize = 0;
	String embargoType = "none";
	String embargoDate = "";
	int maxDownloads = 0;
	String numberOfDownloads = "\"[unknown]\"";
	String manuscriptNum = null;
	int numReadmes = 0;
	String dateAccessioned = "\"[unknown]\"";

	
	try {
	    context = new Context();
        } catch (SQLException e) {
	    log.fatal("Unable to open database connection", e);
	    return Curator.CURATE_FAIL;
	}
	
	if (dso.getType() == Constants.COLLECTION) {
	    // output generic report header for the CSV file that will be created by processing all items in this collection
	    report("handle, packageDOI, articleDOI, journal, numberOfFiles, packageSize, " +
		   "embargoType, embargoDate, numberOfDownloads, manuscriptNum, numReadmes, dateAccessioned");
	} else if (dso.getType() == Constants.ITEM) {
            Item item = (Item)dso;

	    try {
		handle = item.getHandle();
		log.info("handle = " + handle);
		
		if (handle == null) {
		    // this item is still in workflow - no handle assigned
		    handle = "in workflow";
		}
		
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

		// article DOI
		vals = item.getMetadata("dc.relation.isreferencedby");
		if (vals.length == 0) {
		    log.debug("Object has no articleDOI (dc.relation.isreferencedby) " + handle);
		} else {
		    articleDOI = vals[0].value;
		}
		log.debug("articleDOI = " + articleDOI);

		
		// journal
	 	vals = item.getMetadata("prism.publicationName");
		if (vals.length == 0) {
		    setResult("Object has no prism.publicationName available " + handle);
		    log.error("Skipping -- Object has no prism.publicationName available " + handle);
		    context.abort();
		    return Curator.CURATE_SKIP;
		} else {
		    journal = vals[0].value;
		}
		log.debug("journal = " + journal);

		// accession date
		vals = item.getMetadata("dc.date.accessioned");
		if (vals.length == 0) {
		    setResult("Object has no dc.date.accessioned available " + handle);
		    log.error("Skipping -- Object has no dc.date.accessioned available " + handle);
		    context.abort();
		    return Curator.CURATE_SKIP;
		} else {
		    dateAccessioned = vals[0].value;
		}
		log.debug("dateAccessioned = " + dateAccessioned);

		// manuscript number
		DCValue[] manuvals = item.getMetadata("dc.identifier.manuscriptNumber");
		manuscriptNum = null;
		if(manuvals.length > 0) {
		    manuscriptNum = manuvals[0].value;
		}
		if(manuscriptNum != null && manuscriptNum.trim().length() > 0) {
		    log.debug("has a real manuscriptNum = " + manuscriptNum);

		}
		
		// count the files, and compute statistics that depend on the files
		log.debug("getting data file info");
		DCValue[] dataFiles = item.getMetadata("dc.relation.haspart");
		if (dataFiles.length == 0) {
		    setResult("Object has no dc.relation.haspart available " + handle);
		    log.error("Skipping -- Object has no dc.relation.haspart available " + handle);
		    context.abort();
		    return Curator.CURATE_SKIP;
		} else {
		    numberOfFiles = "" + dataFiles.length;
		    packageSize = 0;
		    
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
			
			// total package size
			// add total size of the bitstreams in this data file 
			// to the cumulative total for the package
			// (includes metadata, readme, and textual conversions for indexing)
			for (Bundle bn : fileItem.getBundles()) {
			    for (Bitstream bs : bn.getBitstreams()) {
				packageSize = packageSize + bs.getSize();
			    }
			}
			log.debug("total package size (as of file " + fileID + ") = " + packageSize);

			// Readmes
			// Check for at least one readme bitstream. There may be more, due to indexing and cases
			// where the file itself is named readme. We only count one readme per datafile.
			boolean readmeFound = false;
			for (Bundle bn : fileItem.getBundles()) {
			    for (Bitstream bs : bn.getBitstreams()) {
				String name = bs.getName().trim().toLowerCase();
				if(name.startsWith("readme")) {
				    readmeFound = true;
				}
			    }
			}
			if(readmeFound) {
			    numReadmes++;
			}
			log.debug("total readmes (as of file " + fileID + ") = " + numReadmes);

			
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
			
		       			    			
			// number of downlaods for most downloaded file
			// must use the DSpace item ID, since the solr stats system is based on this ID
			// The SOLR address is hardcoded to the production system here, because even when we run on test servers,
			// it's easiest to use the real stats --the test servers typically don't have useful stats available
			URL downloadStatURL = new URL("http://datadryad.org/solr/statistics/select/?indent=on&q=owningItem:" + fileItem.getID());
			log.debug("fetching " + downloadStatURL);
			Document statsdoc = docb.parse(downloadStatURL.openStream());
			NodeList nl = statsdoc.getElementsByTagName("result");
			String downloadsAtt = nl.item(0).getAttributes().getNamedItem("numFound").getTextContent();
			int currDownloads = Integer.parseInt(downloadsAtt);
			if(currDownloads > maxDownloads) {
			    maxDownloads = currDownloads;
			    // rather than converting maxDownloads back to a string, just use the string we parsed above
			    numberOfDownloads = downloadsAtt;
			}
			log.debug("max downloads (as of file " + fileID + ") = " + numberOfDownloads);
			
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

	setResult("Last processed item = " + handle + " -- " + packageDOI);
	report(handle + ", " + packageDOI + ", " + articleDOI + ", \"" + journal + "\", " +
	       numberOfFiles + ", " + packageSize + ", " +
	       embargoType + ", " + embargoDate + ", " + numberOfDownloads + ", " + manuscriptNum + ", " +
	       numReadmes + ", " + dateAccessioned);

	// slow this down a bit so we don't overwhelm the production SOLR server with requests
	try {
	    Thread.sleep(20);
	} catch(InterruptedException e) {
	    // ignore it
	}

	log.debug("TransferToDash complete");

	try { 
	    context.complete();
        } catch (SQLException e) {
	    log.fatal("Unable to close database connection", e);
	}
	return Curator.CURATE_SUCCESS;
    }

    /**
       An XML utility method that returns the text content of a node.
    **/
    private String getNodeText(Node aNode) {
	return aNode.getChildNodes().item(0).getNodeValue();
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
    
}
