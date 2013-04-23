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
import org.dspace.identifier.IdentifierService;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.utils.DSpace;

import org.apache.log4j.Logger;

/**
 * DataPackageStats retrieves statistics based on an input data package.
 *
 * WARNING: This file is deprecated! Its original purpose was to generate
 * statistics for the 2011 Dryad NSF grant proposal. We are now breaking these stats out into their
 * own curation tasks, which makes it easier to update when we need to make changes. For summary stats,
 * see PackageSimpleStats. For a brief listing of fields from a particular package, see DataPackageInfo.
 *
 * The task succeeds if it was able to locate all required stats, otherwise it fails.
 * Originally based on the RequiredMetadata task by Richard Rodgers.
 *
 * Input: a single data package (the curation framework can iterate over a collection of packages)
 * Output: CSV file with appropriate stats 
 * @author Ryan Scherle
 */
@Suspendable
public class DataPackageStats extends AbstractCurationTask {

    private static Logger log = Logger.getLogger(DataPackageStats.class);
    private IdentifierService identifierService = null;
    DocumentBuilderFactory dbf = null;
    DocumentBuilder docb = null;
    static long total = 0;
    
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
	log.info("performing DataPackageStats task " + total++ );
	
	String handle = "[no handle found]";
	String packageDOI = "[no package DOI found]";
	String articleDOI = "[no article DOI found]";
	String journal = "[no journal found]";
	String numKeywords = "[no numKeywords found]";
	String numKeywordsJournal = "[unknown]";
	String numberOfFiles = "[no numberOfFiles found]";
	int packageSize = 0;
	String embargoType = "[unknown]";
	String embargoDate = "[unknown]";
	int maxDownloads = 0;
	String numberOfDownloads = "[unknown]";
	String manuscriptNum = null;
	String dateAccessioned = "[unknown]";
	
	if (dso.getType() == Constants.COLLECTION) {
	    // output headers for the CSV file that will be created by processing all items in this collection
	    report("handle, packageDOI, articleDOI, journal, numKeywords, numKeywordsJournal, numberOfFiles, packageSize, " +
		   "embargoType, numberOfDownloads, manuscriptNum, dateAccessioned");
	} else if (dso.getType() == Constants.ITEM) {
            Item item = (Item)dso;
	    
	    try {
		handle = item.getHandle();
		log.info("handle = " + handle);
		
		if (handle == null) {
		    // we are still in workflow - no handle assigned
		    handle = "in workflow";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Item: ").append(handle);

		// package DOI
		DCValue[] vals = item.getMetadata("dc.identifier");
		if (vals.length == 0) {
		    setResult("Object has no dc.identifier available " + handle);
		    return Curator.CURATE_FAIL;
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
		    setResult("Object has no dc.relation.isreferencedby available " + handle);
		    log.error("Object has no dc.relation.isreferencedby available " + handle);
		    return Curator.CURATE_SKIP;
		} else {
		    articleDOI = vals[0].value;
		}
		log.debug("articleDOI = " + articleDOI);

		
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
		    return Curator.CURATE_FAIL;
		} else {
		    dateAccessioned = vals[0].value;
		}
		log.debug("dateAccessioned = " + dateAccessioned);
				
		// number of keywords
		int intNumKeywords = item.getMetadata("dc.subject").length +
		    item.getMetadata("dwc.ScientificName").length +
		    item.getMetadata("dc.coverage.temporal").length +
		    item.getMetadata("dc.coverage.spatial").length;

		numKeywords = "" + intNumKeywords; //convert integer to string by appending
		log.debug("numKeywords = " + numKeywords);

		// number of keywords in journal email

		/* **************************
		   COMMENTED OUT THIS SECTION -- we don't need the manuscript numbers right now, so removed until the error is fixed
		   **************************
		   
		   //TODO: fix this for the new style of manuscript numbers -- it fails on anything deposited in 2012 and later
		
		DCValue[] manuvals = item.getMetadata("dc.identifier.manuscriptNumber");
		manuscriptNum = null;
		if(manuvals.length > 0) {
		    manuscriptNum = manuvals[0].value;
		}
		if(manuscriptNum != null && manuscriptNum.trim().length() > 0) {
		    log.debug("has a real manuscriptNum = " + manuscriptNum);
		    //find file for manu
		    int firstdash = manuscriptNum.indexOf("-");
		    String journalAbbrev = manuscriptNum.substring(0, firstdash);
		    if(journalAbbrev.startsWith("0") ||
		       journalAbbrev.startsWith("1") ||
		       journalAbbrev.startsWith("2") ||
		       journalAbbrev.startsWith("3") ||
		       journalAbbrev.startsWith("4") ||
		       journalAbbrev.startsWith("5") ||
		       journalAbbrev.startsWith("6") ||
		       journalAbbrev.startsWith("7") ||
		       journalAbbrev.startsWith("8") ||
		       journalAbbrev.startsWith("9")) {
			//handle older manuscript numbers from amnat
			journalAbbrev = "amNat";
			manuscriptNum = "amNat-" + manuscriptNum;
			firstdash = 5;
		    }
		    if(!journalAbbrev.equals("new")) {
			numKeywordsJournal = "0";
			String journalDir="/opt/dryad/submission/journalMetadata/";
			manuscriptNum = manuscriptNum.substring(firstdash + 1);
			int lastdash = manuscriptNum.lastIndexOf("-");
			if(lastdash >= 0) {
			    manuscriptNum = manuscriptNum.substring(0, lastdash);		    
			    File journalFile = new File(journalDir + journalAbbrev + "/" + manuscriptNum + ".xml");
			    
			    //get keywords from the file and count them
			    if(journalFile.exists()) {
				Document journaldoc = docb.parse(new FileInputStream(journalFile));
				NodeList nl = journaldoc.getElementsByTagName("keyword");
				numKeywordsJournal = "" + nl.getLength();
			    } else {
				report("Unable to find journal file " + journalFile);
				log.error("Unable to find journal file " + journalFile);
			    }
			} else {
			    report("Unable to parse manuscript number " +  manuvals[0].value);
			    log.error("Unable to parse manuscript number " +  manuvals[0].value);
			}
		    }
		}
		log.debug("numKeywordsJournal = " + numKeywordsJournal);

		*/
		
		// number of files
		log.debug("getting data file info");
		vals = item.getMetadata("dc.relation.haspart");
		if (vals.length == 0) {
		    setResult("Object has no dc.relation.haspart available " + handle);
		    return Curator.CURATE_FAIL;
		} else {
		    numberOfFiles = "" + vals.length;
		    packageSize = 0;
		    
		    // for each data file in the package

		    for(int i = 0; i < vals.length; i++) {
			String fileID = vals[i].value;
			log.debug(" ======= processing fileID = " + fileID);

			// get the DSpace Item for this fileID
			Item fileItem = getDSpaceItem(fileID);
			
			log.debug("file internalID = " + fileItem.getID());
			
			// total package size
			// get the file metadata via OAI, since it reports on the file sizes, even for embargoed items
			/*
			  Package size is currently commented out, because we don't want to use the handle or mess with OAI. We need to do this a better way.
			  
			URL oaiAccessURL = new URL("http://www.datadryad.org/oai/request?verb=GetRecord&identifier=oai:datadryad.org:" + shortHandle + "&metadataPrefix=mets");
			log.debug("requesting " + oaiAccessURL);
			Document oaidoc = docb.parse(oaiAccessURL.openStream());
			NodeList nl = oaidoc.getElementsByTagName("mods:titleInfo");
			if(nl.getLength() < 1) {
			    log.error("Object has no mods:titleInfo available " + handle);
			    return Curator.CURATE_SKIP;
			}
			String nodeText = nl.item(0).getTextContent();

			// add total size of bitstreams in this data file 
			// to the cumulative total for the package
			// (includes readme and textual conversions for indexing)
			nl = oaidoc.getElementsByTagName("file");
			
			for(int j = 0; j < nl.getLength(); j++) {
			    Node aNode = nl.item(j);
			    Node sizeAtt = aNode.getAttributes().getNamedItem("SIZE");
			    int bitstreamSize =  Integer.parseInt(sizeAtt.getTextContent());
			    packageSize = packageSize + bitstreamSize;
			}
			log.debug("total package size = " + packageSize);
			*/

			// embargo setting (of last file processed)
			// need to get embargo from mets metadata since oai doesn't have it
			/* Temorarily disable embargo settings -- we don't want to use the handles, and we should be able to get this without using the METS
			URL metsAccessURL = new URL("http://datadryad.org/metadata/handle/" + shortHandle + "/mets.xml");
			Document metsdoc = docb.parse(metsAccessURL.openStream());
			nl = metsdoc.getElementsByTagName("dim:field");
			for(int k = 0; k < nl.getLength(); k++) {
			    Node aNode = nl.item(k);
			    Node qualAtt = aNode.getAttributes().getNamedItem("qualifier");
			    if(qualAtt != null && qualAtt.getTextContent().equals("embargoedUntil")) {
				// extract embargo date
				embargoDate = aNode.getTextContent();
			    }
			    if(qualAtt != null && qualAtt.getTextContent().equals("embargo")) {
				// extract embargo type
				embargoType = aNode.getTextContent();
			    }
			}
			if((embargoType == null || embargoType.equals("") || embargoType.equals("none")) &&
			   (embargoDate != null && !embargoDate.equals(""))) {
				// correctly encode embago type to "oneyear" if there is a date set, but the type is blank or none
				embargoType = "oneyear";
			}
			log.debug("embargoType = " + embargoType);
			*/
		       			    			
			// number of downlaods for most downloaded file
			// must use the DSpace item ID, since the solr stats system is based on this ID
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
			log.debug("max downloads = " + numberOfDownloads);
			
		    }

		}
		log.info(handle + "done.");
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

	setResult("Last processed item = " + handle + " -- " + packageDOI);
	report(handle + ", " + packageDOI + ", " + articleDOI + ", \"" + journal + "\", " + numKeywords + ", " +
	       numKeywordsJournal + ", " + numberOfFiles + ", " + packageSize + ", " +
	       embargoType + ", " + numberOfDownloads + ", " + manuscriptNum + ", " + dateAccessioned);

	// don't overwhelm the production server with requests
	// TODO: remove this after the code above is rewritten to not use OAI or METS -- it will be much more
	// efficient once it is accessing the database directly
	try {
	    Thread.sleep(200);
	} catch(InterruptedException e) {
	    // ignore it
	}

	
	log.debug("DataPackageStats complete");
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
	    Context context = new Context();
	    dspaceItem = (Item)identifierService.resolve(context, itemID);  
	    context.complete();
        } catch (SQLException e) {
	    log.fatal("Unable to get DSpace Item for " + itemID, e);
        } catch (IdentifierNotFoundException e) {
	    log.fatal("Unable to get DSpace Item for " + itemID, e);
	} catch (IdentifierNotResolvableException e) {
	    log.fatal("Unable to get DSpace Item for " + itemID, e);
	}

	return dspaceItem;
    }
    
}
