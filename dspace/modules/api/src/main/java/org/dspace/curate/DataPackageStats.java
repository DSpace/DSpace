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
import java.util.ArrayList;
import java.util.HashMap;
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

import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.core.Constants;

import org.apache.log4j.Logger;

/**
 * DataPackageStats retrieves statistics based on an input data package.
 * WARNING: This file is a complete hack! Its original purpose was to generate
 * statistics for the 2011 Dryad NSF grant proposal. It is being modified
 * to generate statistics for conference presentations.
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

    DocumentBuilderFactory dbf = null;
    DocumentBuilder docb = null;
    HashMap<String, String> handleToID = null;
    
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

	// init handle-to-id lookup
	handleToID = new HashMap<String, String>();
	BufferedReader rdr = new BufferedReader(
		                 new InputStreamReader(
			             new FileInputStream(new File("/temp/handleToID.csv"))));
	String aLine = rdr.readLine();
        while(aLine != null) {
	    int comma = aLine.indexOf(",");
	    String handle = aLine.substring(0, comma).trim();
	    String itemID = aLine.substring(comma + 1).trim();
	    handleToID.put(handle, itemID);
            aLine = rdr.readLine();
        }
        rdr.close();

    }
    
    /**
     * Perform the curation task upon passed DSO
     *
     * @param dso the DSpace object
     * @throws IOException
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException {
	log.info("performing DataPackageStats task");
	
	String handle = "[no handle found]";
	String doi = "[no DOI found]";
	String journal = "[no journal found]";
	String numKeywords = "[no numKeywords found]";
	String numKeywordsJournal = "";
	String numberOfFiles = "[no numberOfFiles found]";
	int packageSize = 0;
	String embargoType = "none";
	String embargoDate = null;
	int maxDownloads = 0;
	String numberOfDownloads = "0";
	String manuscriptNum = null;
	String dateAccessioned = "";
	
	if (dso.getType() == Constants.COLLECTION) {
	    // output headers for the CSV file that will be created by processing all items in this collection
	    report("handle, doi, journal, numKeywords, numKeywordsJournal, numberOfFiles, packageSize, " +
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

		// DOI
		DCValue[] vals = item.getMetadata("dc.identifier");
		if (vals.length == 0) {
		    setResult("Object has no dc.identifier available " + handle);
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
		    return Curator.CURATE_FAIL;
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
			String journalDir="/opt/dryad/submission/journalMetadata/";
			manuscriptNum = manuscriptNum.substring(firstdash + 1);
			int lastdash = manuscriptNum.lastIndexOf("-");
			manuscriptNum = manuscriptNum.substring(0, lastdash);		    
			File journalFile = new File(journalDir + journalAbbrev + "/" + manuscriptNum + ".xml");
			
			//get keywords from the file and count them
			if(journalFile.exists()) {
			    Document journaldoc = docb.parse(new FileInputStream(journalFile));
			    NodeList nl = journaldoc.getElementsByTagName("keyword");
			    numKeywordsJournal = "" + nl.getLength();
			} else {
			    report("Unable to find journal file " + journalFile);
			    throw new Exception("Unable to find journal file " + journalFile);
			}
		    }
		}
		log.debug("numKeywordsJournal = " + numKeywordsJournal);


		
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
			
			// ensure fileID is a handle, so we can query OAI with it
			String shortHandle = "";
			if(fileID.startsWith("http://hdl.handle.net/10255/")) {
			    shortHandle = fileID.substring("http://hdl.handle.net/".length());
			} else if (fileID.startsWith("http://datadryad.org/handle/")) {
			    shortHandle = fileID.substring("http://datadryad.org/handle/".length());
			} else if (fileID.startsWith("doi:10.5061/")) {
			    URL doiLookupURL = new URL("http://datadryad.org/doi?lookup=" + fileID);
			    String lookupHandle = (new BufferedReader(new InputStreamReader(doiLookupURL.openStream()))).readLine();
			    shortHandle = lookupHandle.substring("http://datadryad.org/handle/".length());
			} else {
			    shortHandle = "###### UNEXPECTED PARTOF FORMAT!!! " + fileID;
			}
			log.debug("data file handle = " + shortHandle);
			

			/////////////////////////////////////////////////////////////////////////////////////////
			// TODO: instead of the crude mess below (OAI calls, pulling the METS, querying SOLR),
			// get the actual data file object through the DSpace API and obtain the information from it
			/////////////////////////////////////////////////////////////////////////////////////////

			
			// total package size
			// get the file metadata via OAI, since it reports on the file sizes, even for embargoed items
			URL oaiAccessURL = new URL("http://www.datadryad.org/oai/request?verb=GetRecord&identifier=oai:datadryad.org:" + shortHandle + "&metadataPrefix=mets");
			log.debug("requesting " + oaiAccessURL);
			Document oaidoc = docb.parse(oaiAccessURL.openStream());
			NodeList nl = oaidoc.getElementsByTagName("mods:titleInfo");
			if(nl.getLength() < 1) {
			    throw new MetadataValidationException("item has no title");
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

			// embargo setting (of last file processed)
			// need to get embargo from mets metadata since oai doesn't have it
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
		       			    			
			// number of downlaods for most downloaded file
			// must translate between the shortHandle and the DSpace internal database ID
			// (since the solr stats system is based on the DSpace database ID)
			log.debug("####### DSPACE OBJECT HAS INTERNAL ID: " + dso.getID());
			String itemID = handleToID.get(shortHandle);
			URL downloadStatURL = new URL("http://datadryad.org/solr/statistics/select/?indent=on&q=owningItem:" + itemID);
			log.debug("fetching " + downloadStatURL);
			Document statsdoc = docb.parse(downloadStatURL.openStream());
			nl = statsdoc.getElementsByTagName("result");
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
	report(handle + ", " + doi + ", \"" + journal + "\", " + numKeywords + ", " +
	       numKeywordsJournal + ", " + numberOfFiles + ", " + packageSize + ", " +
	       embargoType + ", " + numberOfDownloads + ", " + manuscriptNum + ", " + dateAccessioned);
	log.debug("DataPackageStats complete");
	return Curator.CURATE_SUCCESS;
    }

    /**
       An XML utility method that returns the text content of a node.
    **/
    private String getNodeText(Node aNode) {
	return aNode.getChildNodes().item(0).getNodeValue();
    }

}
