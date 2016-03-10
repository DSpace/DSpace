/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dspace.JournalUtils;
import org.dspace.content.authority.Concept;
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
import org.dspace.content.BitstreamFormat;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.identifier.IdentifierService;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.utils.DSpace;

import org.apache.log4j.Logger;

import org.datadryad.api.DryadDataFile;
import org.datadryad.api.DryadJournalConcept;

/**
 * DataFileStats retrieves detailed statistics about a data file.
 *
 * The task succeeds if it was able to locate all required stats, otherwise it fails.
 * Originally based on the RequiredMetadata task by Richard Rodgers.
 *
 * Input: a single data file OR a collection that contains data files
 * Output: CSV file with appropriate stats 
 * @author Ryan Scherle
 */
@Suspendable
public class DataFileStats extends AbstractCurationTask {

    private static Logger log = Logger.getLogger(DataFileStats.class);
    private IdentifierService identifierService = null;
    DocumentBuilderFactory dbf = null;
    DocumentBuilder docb = null;
    static long total = 0;
    private Context context;
    private static List<String> journalsThatAllowReview = new ArrayList<String>();
    private static List<String> integratedJournals = new ArrayList<String>();
    private static List<String> integratedJournalsThatAllowEmbargo = new ArrayList<String>();
    
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

		DryadJournalConcept[] journalConcepts = JournalUtils.getAllJournalConcepts();
		for (DryadJournalConcept journalConcept : journalConcepts) {
			String journalDisplay = journalConcept.getFullName();
			if (journalConcept.getIntegrated()) {
				integratedJournals.add(journalDisplay);
			}
			if (journalConcept.getAllowEmbargo()) {
				integratedJournalsThatAllowEmbargo.add(journalDisplay);
			}
			if (journalConcept.getAllowReviewWorkflow()) {
				journalsThatAllowReview.add(journalDisplay);
			}
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
	log.info("performing DataFileStats task " + total++ );
	
	String handle = "\"[no handle found]\"";
	String fileDOI = "\"[no file DOI found]\"";
	String packageDOI = "\"[no package DOI found]\"";
	String articleDOI = "\"[no article DOI found]\"";
	String journal = "[no journal found]"; // don't add quotes here, because journal is always quoted when output below
	boolean journalAllowsEmbargo = false;
	boolean journalAllowsReview = false;
	String numKeywords = "\"[no numKeywords found]\"";
	String numKeywordsJournal = "\"[unknown]\"";
	long fileSize = 0;
	String embargoType = "none";
	String embargoDate = "";
	int maxDownloads = 0;
	String numberOfDownloads = "\"[unknown]\"";
	int numReadmes = 0;
	boolean wentThroughReview = false;
	String dateAccessioned = "\"[unknown]\"";
        String fileName = "\"[unknown]\"";
        String mimeType = "\"[unknown]\"";

	
	try {
	    context = new Context();
        } catch (SQLException e) {
	    log.fatal("Unable to open database connection", e);
	    return Curator.CURATE_FAIL;
	}
	
	if (dso.getType() == Constants.COLLECTION) {
	    // output headers for the CSV file that will be created by processing all items in this collection
	    report("handle, fileDOI, articleDOI, journal, journalAllowsEmbargo, journalAllowsReview, numKeywords, fileSize, " +
		   "embargoType, embargoDate, numberOfDownloads, numReadmes, wentThroughReview, dateAccessioned, fileName, mimeType");
	} else if (dso.getType() == Constants.ITEM) {
            Item item = (Item)dso;

	    if(!item.isArchived()) {
		    context.abort();
            return Curator.CURATE_SKIP;
	    }
	    
	    try {
		handle = item.getHandle();
		log.info("handle = " + handle);
		
		if (handle == null) {
		    // this item is still in workflow - no handle assigned
		    handle = "in workflow";
		}
		
		// file DOI
		DCValue[] vals = item.getMetadata("dc.identifier");
		if (vals.length == 0) {
		    setResult("Object has no dc.identifier available " + handle);
		    log.error("Skipping -- no dc.identifier available for " + handle);
		    context.abort(); 
		    return Curator.CURATE_SKIP;
		} else {
		    for(int i = 0; i < vals.length; i++) {
			if (vals[i].value.startsWith("doi:")) {
			    fileDOI = vals[i].value;
			}
		    }
		}
		log.debug("fileDOI = " + fileDOI);


		// locate the containing data package
		Item packageItem = null;
		vals = item.getMetadata("dc.relation.ispartof");
		if (vals.length == 0) {
		    log.debug("Object has no packageDOI (dc.relation.ispartof) " + handle);
		} else {
		    packageDOI = vals[0].value;
		    packageItem = getDSpaceItem(packageDOI);
		}
		log.debug("packageDOI = " + packageDOI);

		
		// article DOI -- from package
		vals = packageItem.getMetadata("dc.relation.isreferencedby");
		if (vals.length == 0) {
		    log.debug("Object has no articleDOI (dc.relation.isreferencedby) " + handle);
		} else {
		    articleDOI = vals[0].value;
		}
		log.debug("articleDOI = " + articleDOI);

		
		// journal -- from package
	 	vals = packageItem.getMetadata("prism.publicationName");
		if (vals.length == 0) {
		    setResult("Object has no prism.publicationName available " + handle);
		    log.error("Skipping -- Object has no prism.publicationName available " + handle);
		    context.abort();
		    return Curator.CURATE_SKIP;
		} else {
		    journal = vals[0].value;
		}
		log.debug("journal = " + journal);

		// journalAllowsEmbargo
		// embargoes are allowed for all non-integrated journals
		// embargoes are also allowed for integrated journals that have set the embargoesAllowed option
		if(!integratedJournals.contains(journal) || integratedJournalsThatAllowEmbargo.contains(journal)) {
		    journalAllowsEmbargo = true;
		} 

		// journalAllowsReview
		if(journalsThatAllowReview.contains(journal)) {
		    journalAllowsReview = true;
		}
				
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

		// wentThroughReview
		vals = item.getMetadata("dc.description.provenance");
		if (vals.length == 0) {
		    log.warn("That's strange -- Object has no provenance data available " + handle);
		} else {
		    for(DCValue aVal : vals) {
			if(aVal.value != null && aVal.value.contains("requiresReviewStep")) {
			    wentThroughReview = true;
			}
		    }
		}
		log.debug("wentThroughReview = " + wentThroughReview);

		
		// number of keywords
		int intNumKeywords = item.getMetadata("dc.subject").length +
		    item.getMetadata("dwc.ScientificName").length +
		    item.getMetadata("dc.coverage.temporal").length +
		    item.getMetadata("dc.coverage.spatial").length;

		numKeywords = "" + intNumKeywords; //convert integer to string by appending
		log.debug("numKeywords = " + numKeywords);

		
		// total file size
		// add total size of the bitstreams in this data file 
		// (includes metadata, readme, and textual conversions for indexing)
		for (Bundle bn : item.getBundles()) {
		    for (Bitstream bs : bn.getBitstreams()) {
			fileSize = fileSize + bs.getSize();
		    }
		}
		log.debug("total file size = " + fileSize);

		// embargo setting 
		vals = item.getMetadata("dc.type.embargo");
		if (vals.length > 0) {
		    embargoType = vals[0].value;
		    log.debug("EMBARGO vals " + vals.length + " type " + embargoType);
		}
		vals = item.getMetadata("dc.date.embargoedUntil");
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
			
		       			    			
		// number of downloads
		// must use the DSpace item ID, since the solr stats system is based on this ID
		// The SOLR address is hardcoded to the production system here, because even when we run on test servers,
		// it's easiest to use the real stats --the test servers typically don't have useful stats available
		URL downloadStatURL = new URL("http://datadryad.org/solr/statistics/select/?indent=on&q=owningItem:" + item.getID());
		log.debug("fetching " + downloadStatURL);
		Document statsdoc = docb.parse(downloadStatURL.openStream());
		NodeList nl = statsdoc.getElementsByTagName("result");
		numberOfDownloads = nl.item(0).getAttributes().getNamedItem("numFound").getTextContent();

                // file name (of first bitstream)
                DryadDataFile df = new DryadDataFile(item);
                Bitstream bitstream = df.getFirstBitstream();
                fileName = bitstream.getName();
                    
                // file type (of first bitstream)
                BitstreamFormat bf = bitstream.getFormat();
                mimeType = bf.getMIMEType();

                // report output
		report(handle + ", " + fileDOI + ", " + articleDOI + ", \"" + journal + "\", " +
		       journalAllowsEmbargo + ", " + journalAllowsReview + ", " + numKeywords + ", " +
		       fileSize + ", " + embargoType + ", " + embargoDate + ", " + numberOfDownloads + ", " +
		       numReadmes + ", " + wentThroughReview + ", " + dateAccessioned + ", " + fileName + ", " + mimeType);
		
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

	// slow this down a bit so we don't overwhelm the production SOLR server with requests
	try {
	    Thread.sleep(20);
	} catch(InterruptedException e) {
	    // ignore it
	}

	log.debug("DataPackageStats complete");

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
