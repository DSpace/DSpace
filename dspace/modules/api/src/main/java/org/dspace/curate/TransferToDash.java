/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.log4j.Logger;

import org.dspace.JournalUtils;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.identifier.IdentifierService;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.utils.DSpace;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


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
 * Output: CSV file with some quick stats
 * Side Effects: Data package is transferred to DASH-based Dryad, Data Package is updated
 *               with metadata indicating date of successfull transfer.
 *
 * @author Ryan Scherle
 */

@Suspendable
public class TransferToDash extends AbstractCurationTask {
    
    private static Logger log = Logger.getLogger(TransferToDash.class);
    private IdentifierService identifierService = null;
    private DocumentBuilderFactory dbf = null;
    private DocumentBuilder docb = null;
    private static long total = 0;
    private Context context;
    private String dashServer;
    private String oauthToken;

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
        
        // init oauth connection with DASH
        dashServer = ConfigurationManager.getProperty("dash.server");
        String dashAppID = ConfigurationManager.getProperty("dash.application_id");
        String dashAppSecret = ConfigurationManager.getProperty("dash.application_secret");
        
        oauthToken = getOAUTHtoken(dashServer, dashAppID, dashAppSecret);
    }
    
    private String getOAUTHtoken(String dashServer, String dashAppID, String dashAppSecret) {
        
        String url = dashServer + "/oauth/token";
        String auth = dashAppID + ":" + dashAppSecret;
        String authentication = Base64.getEncoder().encodeToString(auth.getBytes());
        Pattern pat = Pattern.compile(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*");
        String token = "";

        try {
            URL urlObj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Authorization", "Basic " + authentication);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            con.setRequestProperty("Accept", "application/json");
            
            PrintStream os = new PrintStream(con.getOutputStream());
            os.print("grant_type=client_credentials");
            os.close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line = null;
            StringWriter out = new StringWriter(con.getContentLength() > 0 ? con.getContentLength() : 2048);
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            String response = out.toString();
            Matcher matcher = pat.matcher(response);
            if (matcher.matches() && matcher.groupCount() > 0) {
                token = matcher.group(1);
            }

            log.info("got OAuth token " + token);

        } catch (Exception e) {
            log.fatal("Unable to obtain OAuth token", e);
        }

        return token;
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
	String dateAccessioned = "\"[unknown]\"";
	
	try {
	    context = new Context();
        } catch (SQLException e) {
	    log.fatal("Unable to open database connection", e);
	    return Curator.CURATE_FAIL;
	}
        
	if (dso.getType() == Constants.COLLECTION) {
	    // output generic report header for the CSV file that will be created by processing all items in this collection
	    report("handle, packageDOI, articleDOI, journal, numberOfFiles, packageSize");
	} else if (dso.getType() == Constants.ITEM) {

            Item item = (Item)dso;

            // dataset is the JSON dataset object that will be deposited into New Dryad
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode dataset = objectMapper.createObjectNode();

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
                dataset.put("identifier", packageDOI);

                // title
                vals = item.getMetadata("dc.title");
                if (vals.length > 0) {
                    String title = vals[0].value;
                    log.debug("title = " + title);
                    dataset.put("title", title);
                }

                // abstract
                vals = item.getMetadata("dc.description");
                if (vals.length > 0) {
                    String packageAbstract = vals[0].value;
                    dataset.put("abstract", packageAbstract);
                }
                       
                // TODO -- fix this field! Need a real lookup!
                dataset.put("userID", 1);

		// article DOI
		vals = item.getMetadata("dc.relation.isreferencedby");
		if (vals.length == 0) {
		    log.debug("Object has no articleDOI (dc.relation.isreferencedby) " + handle);
		} else {
		    articleDOI = vals[0].value;
		}
		log.debug("articleDOI = " + articleDOI);
                ObjectNode article = objectMapper.createObjectNode();
                article.put("relationship", "IsCitedBy");
                article.put("identifierType", "DOI");
                article.put("identifier", articleDOI.substring("doi:".length()));
                ArrayNode relatedWorks = objectMapper.createArrayNode();
                relatedWorks.add(article);
                dataset.put("relatedWorks", relatedWorks);

                ArrayNode allAuthors = objectMapper.createArrayNode();
		vals = item.getMetadata("dc.contributor");
                for(int i = 0; i < vals.length; i++) {
                    String authorName = vals[i].value;
                    ObjectNode authorObj = objectMapper.createObjectNode();
                    int comma = authorName.indexOf(",");
                    authorObj.put("firstName", authorName.substring(comma+2));
                    authorObj.put("lastName", authorName.substring(0, comma));
                    // TODO add affiliation
                    allAuthors.add(authorObj);
                }
                vals = item.getMetadata("dc.contributor.author");
                for(int i = 0; i < vals.length; i++) {
                    String authorName = vals[i].value;
                    ObjectNode authorObj = objectMapper.createObjectNode();
                    int comma = authorName.indexOf(",");
                    authorObj.put("firstName", authorName.substring(comma+2));
                    authorObj.put("lastName", authorName.substring(0, comma));
                    // TODO add affiliation
                    allAuthors.add(authorObj);
                }
                dataset.put("authors", allAuthors);

		log.info(handle + " done.");
	    } catch (Exception e) {
		log.fatal("Skipping -- Exception in processing " + handle, e);
		setResult("Object has a fatal error: " + handle + "\n" + e.getMessage());
		report("Object has a fatal error: " + handle + "\n" + e.getMessage());
		
		context.abort();
		return Curator.CURATE_SKIP;
	    }

            // write this item to json
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File("/tmp/transferToDash.json"), dataset);

            // send this item to the server
            sendToDash(objectMapper.writeValueAsString(dataset));
            
            // provide output for the console
            setResult("Last processed item = " + handle + " -- " + packageDOI);        
            report(handle + ", " + packageDOI + ", " + articleDOI + ", \"" + journal + "\", " +
                   numberOfFiles + ", " + packageSize);

	} else {
	    log.info("Skipping -- non-item DSpace object");
	    setResult("Object skipped (not an item)");
	    context.abort();
	    return Curator.CURATE_SKIP;
        }

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

    private void sendToDash(String jsonString) {
        BufferedReader reader = null;
        try {
            URL url = new URL(dashServer + "/api/datasets");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            //connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + oauthToken);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            OutputStreamWriter wr= new OutputStreamWriter(connection.getOutputStream());
            wr.write(jsonString);
            wr.close();

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = null;
            StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            String response = out.toString();
            System.out.println("##### " + response);
        } catch (Exception e) {
            log.fatal("Unable to send item to DASH", e);
        }
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
