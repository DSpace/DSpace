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

import org.datadryad.api.DryadDataFile;
import org.datadryad.api.DryadJournalConcept;

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


/**
 * DashStats retrieves detailed statistics about a data package,
 * in a format usable by the Dash counter-processor.
 *
 * Some statistics are calculated based on the files that are contained in the
 * data package, and from information in SOLR. Extra processing time is required
 * to process this metadata, so this report takes some time to run. 
 *
 * The task succeeds if it was able to locate all required stats, otherwise it fails.
 * Originally based on the DataPackageStats task.
 *
 * Input: a single data package OR a collection that contains data packages
 * Output: tab-separated file with appropriate stats 
 * @author Ryan Scherle
 */
@Suspendable
public class DashStats extends AbstractCurationTask {

    private static Logger log = Logger.getLogger(DashStats.class);
    private static boolean headersReported = false;
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
	log.info("performing DashStats task " + total++ );
	
	String handle = "\"[no handle found]\"";
	String packageDOI = "\"[no package DOI found]\"";
        String packageTitle = "\"[no package title found]\"";
        String packageAuthors = "";
	String articleDOI = "\"[no article DOI found]\"";
	String journal = "[no journal found]"; // don't add quotes here, because journal is always quoted when output below
	String numberOfFiles = "\"[no numberOfFiles found]\"";
	long packageSize = 0;
	String embargoType = "none";
	String embargoDate = "";
	String numberOfDownloads = "\"[unknown]\"";
	String manuscriptNum = null;
	String dateAccessioned = "\"[unknown]\"";
        String publicationYear = "\"[unknown]\"";

	
	try {
	    context = new Context();
        } catch (SQLException e) {
	    log.fatal("Unable to open database connection", e);
	    return Curator.CURATE_FAIL;
	}
	
	if (dso.getType() == Constants.COLLECTION) {
            // do nothing
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

		// package title
		vals = item.getMetadata("dc.title");
		if (vals.length == 0) {
		    log.debug("Object has no dc.title" + handle);
		} else {
		    packageTitle = vals[0].value;
		}
		log.debug("packageTitle = " + packageTitle);

                // package authors
		vals = item.getMetadata("dc.contributor.author");
                packageAuthors = "";
                log.debug("found " + vals.length + " authors");
		if (vals.length == 0) {
		    log.debug("Object has no dc.contributor.author" + handle);
		} else {
                    for(int i = 0; i < vals.length; i++) {
                        log.debug(" -- adding " + vals[i].value);
                        packageAuthors = packageAuthors + vals[i].value;
                        if(vals.length > i + 1) {
                            packageAuthors = packageAuthors + "|";
                        }
                    } 
                    log.debug("authors are " + packageAuthors);
		}
		log.debug("packageTitle = " + packageTitle);
                
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

                // publication year
                publicationYear = dateAccessioned.substring(0,4);
                
		// manuscript number
		DCValue[] manuvals = item.getMetadata("dc.identifier.manuscriptNumber");
		manuscriptNum = null;
		if(manuvals.length > 0) {
		    manuscriptNum = manuvals[0].value;
		}
		if(manuscriptNum != null && manuscriptNum.trim().length() > 0) {
		    log.debug("has a real manuscriptNum = " + manuscriptNum);

		}

                // ensure the headers are output exactly once per run of the stats process
                if(!headersReported) {
                    report("#Fields: event_time" + "\t" +
                           "client_ip" + "\t" +
                           "session_cookie_id" + "\t" +
                           "user_cookie_id" + "\t" +
                           "user_id" + "\t" +
                           "request_url" + "\t" +
                           "identifier" + "\t" +
                           "filename" + "\t" +
                           "size" + "\t" +
                           "user-agent" + "\t" +
                           "title" + "\t" +
                           "publisher" + "\t" +
                           "publisher_id" + "\t" +
                           "authors" + "\t" +
                           "publication_date" + "\t" +
                           "version" + "\t" +
                           "other_id" + "\t" +
                           "target_url" + "\t" +
                           "publication_year"
                           );
                    headersReported = true;
                }
                
                URL pageviewsURL = new URL("http://datadryad.org/solr/statistics/select/?rows=10000000&q=id:" + item.getID());
                log.debug("fetching pageviews " + pageviewsURL);
                Document viewsdoc = docb.parse(pageviewsURL.openStream());
                NodeList nl = viewsdoc.getElementsByTagName("result");
                String viewsAtt = nl.item(0).getAttributes().getNamedItem("numFound").getTextContent();
                int currViews = Integer.parseInt(viewsAtt);
                log.debug("number of pageviews " + currViews);
                NodeList viewNodes = nl.item(0).getChildNodes();
                log.debug("number of nodes returned " + viewNodes.getLength());
                for(int i = 0; i < viewNodes.getLength(); i++) {
                    Node aView = viewNodes.item(i);
                    String eventTime = getNamedChildText(aView, "time");
                    String clientIP = getNamedChildText(aView, "ip");
                    String userAgent = getNamedChildText(aView, "userAgent");
                    report(eventTime + "\t" +
                           clientIP + "\t" +
                           "-\t" +  //session_cookie_id
                           "-\t" +  //user_cookie_id
                           "-\t" +  //user__id
                           "https://datadryad.org/resource/" + packageDOI + "\t" +
                           packageDOI + "\t" +
                           "-\t" +  //filename
                           "-\t" +  //size
                           userAgent + "\t" +
                           packageTitle + "\t" +
                           "Dryad Digital Repository\t" + //publisher
                           "grid.466587.e\t" + //publisher_id
                           packageAuthors + "\t" +
                           dateAccessioned + "\t" +
                           "1\t" +  //version
                           "-\t" +  //other_id
                           "https://datadryad.org/resource/" + packageDOI + "\t" +
                           publicationYear
                           );
                }
                
                // ================= START DATA FILES ================
                
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
			
                        // basic file metadata
                        DryadDataFile ddf = new DryadDataFile(fileItem);
                        Bitstream bitstream = ddf.getFirstBitstream();
                        String filename = bitstream.getName();
                        Long fileSize = bitstream.getSize();
                        String fileDOI = ddf.getIdentifier();
                                              
			// must use the DSpace item ID, since the solr stats system is based on this ID
			// The SOLR address is hardcoded to the production system here, because even when we run on test servers,
			// it's easiest to use the real stats --the test servers typically don't have useful stats available
			URL downloadStatURL = new URL("http://datadryad.org/solr/statistics/select/?rows=1000000&q=owningItem:" + fileItem.getID());
			log.debug("fetching " + downloadStatURL);
			Document downloadsdoc = docb.parse(downloadStatURL.openStream());
			NodeList dnl = downloadsdoc.getElementsByTagName("result");
			String downloadsAtt = dnl.item(0).getAttributes().getNamedItem("numFound").getTextContent();
			int currDownloads = Integer.parseInt(downloadsAtt);
                        log.debug("number of downloads " + currDownloads);
                        NodeList dlNodes = dnl.item(0).getChildNodes();
                        log.debug("number of nodes returned " + dlNodes.getLength());

                        for(int j = 0; j < dlNodes.getLength(); j++) {
                            Node aDownload = dlNodes.item(j);
                            String eventTime = getNamedChildText(aDownload, "time");
                            String clientIP = getNamedChildText(aDownload, "ip");
                            String userAgent = getNamedChildText(aDownload, "userAgent");
                            report(eventTime + "\t" +
                                   clientIP + "\t" +
                                   "- \t" +  //session_cookie_id
                                   "- \t" +  //user_cookie_id
                                   "- \t" +  //user__id
                                   "https://datadryad.org/resource/" + fileDOI + "\t" +
                                   packageDOI + "\t" +
                                   filename + "\t" +
                                   fileSize + "\t" +
                                   userAgent + "\t" +
                                   filename + "\t" +
                                   "Dryad Digital Repository \t" + //publisher
                                   packageAuthors + "\t" +
                                   dateAccessioned + "\t" +
                                   "1 \t" +  //version
                                   "- \t" +  //other_id
                                   "https://datadryad.org/resource/" + fileDOI + "\t" +
                                   publicationYear
                                   );
                        }
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

	// slow this down a bit so we don't overwhelm the production SOLR server with requests
	try {
	    Thread.sleep(20);
	} catch(InterruptedException e) {
	    // ignore it
	}

	log.debug("DashStats complete");

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

    /**
       Returns the text of a "named" child node. For example, if called on the structure
       below with the name of "color", this method would return "blue".

       <rootNode>
         <child name="size">4</child>
         <child name="color">blue</child>
         <child name="shape">square</child>
       </rootNode>
    **/
    private String getNamedChildText(Node aNode, String fieldName) {
        String result = null;
        try {
            NodeList childNodes = aNode.getChildNodes();
            for(int i = 0; i < childNodes.getLength(); i++) {
                Node aChild = childNodes.item(i);
                String name = aChild.getAttributes().getNamedItem("name").getTextContent();
                if(name.equals(fieldName)) {
                    result = aChild.getFirstChild().getTextContent();
                    break;
                }
            }
        } catch (Exception e) {
            // an exception processing one field shouldn't cause problems for the entire log entry,
            // so log it and move on.
            log.error("unable to get field " + fieldName + " out of node " + aNode, e);
        }
        return result;
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
