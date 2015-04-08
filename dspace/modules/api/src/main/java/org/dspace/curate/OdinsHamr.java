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
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.identifier.IdentifierService;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.utils.DSpace;

import org.apache.log4j.Logger;

/**
 * OdinsHamr helps users reconcile author metadata with metadata stored in ORCID.
 *
 * Input: a single DSpace item OR a collection
 * Output: CSV file that maps author names in the DSpace item to author names in ORCID.
 * @author Ryan Scherle
 */
@Suspendable
public class OdinsHamr extends AbstractCurationTask {

    private static final String ORCID_QUERY_BASE = "http://pub.orcid.org/search/orcid-bio?q=digital-object-ids:";
    private static final double MATCH_THRESHHOLD = 0.5;
    
    private static Logger log = Logger.getLogger(OdinsHamr.class);
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
     *	input: an item
     *  output: several lines of authors, can show all or only identified matches
     *
     * @param dso the DSpace object
     * @throws IOException
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException {
        log.info("performing ODIN's Hamr task " + total++ );

	String handle = "\"[no handle found]\"";
	String itemDOI = "\"[no item DOI found]\"";
	String articleDOI = "";
	
	try {
	    context = new Context();
        } catch (SQLException e) {
	    log.fatal("Unable to open database connection", e);
	    return Curator.CURATE_FAIL;
	}
	
	if (dso.getType() == Constants.COLLECTION) {
	    // output headers for the CSV file that will be created by processing all items in this collection
	    report("itemDOI, articleDOI, orcidID, orcidName, dspaceORCID, dspaceName");
	} else if (dso.getType() == Constants.ITEM) {
            Item item = (Item)dso;

	    try {
		handle = item.getHandle();
		log.info("handle = " + handle);
		
		if (handle == null) {
		    // this item is still in workflow - no handle assigned
		    context.abort(); 
		    return Curator.CURATE_SKIP;
		}
		
		// item DOI
		DCValue[] vals = item.getMetadata("dc.identifier");
		if (vals.length == 0) {
		    setResult("Object has no dc.identifier available " + handle);
		    log.error("Skipping -- no dc.identifier available for " + handle);
		    context.abort(); 
		    return Curator.CURATE_SKIP;
		} else {
		    for(int i = 0; i < vals.length; i++) {
			if (vals[i].value.startsWith("doi:")) {
			    itemDOI = vals[i].value;
			}
		    }
		}
		log.debug("itemDOI = " + itemDOI);

		// article DOI
		vals = item.getMetadata("dc.relation.isreferencedby");
		if (vals.length == 0) {
		    log.debug("Object has no articleDOI (dc.relation.isreferencedby) " + handle);
		    articleDOI = "";
		} else {
		    articleDOI = vals[0].value;
		}
		log.debug("articleDOI = " + articleDOI);


		// DSpace names
		List<OrcidName> dspaceNames = new ArrayList<OrcidName>();
		vals = item.getMetadata("dc.contributor.author");
		if (vals.length == 0) {
		    log.error("Object has no dc.contributor.author available in DSpace" + handle);
		} else {
		    for(int i = 0; i < vals.length; i++) {
			dspaceNames.add(new OrcidName("",vals[i].value));
		    }
		}

		// ORCID names
		List<OrcidName> orcidNames = new ArrayList<OrcidName>();
		orcidNames.addAll(retrieveOrcidNames(itemDOI));
		
		if(articleDOI != null && articleDOI.length() > 0) {
		    List<OrcidName> orcidNamesArticle = retrieveOrcidNames(articleDOI);
		    for(OrcidName orcidName:orcidNamesArticle) {
			if(!containsName(orcidNames, orcidName.getName())) {
			    orcidNames.add(orcidName);
			}
		    }
		}
		
		// reconcile names between DSpace and ORCID
		HashMap<OrcidName,OrcidName> mappedNames = doHamrMatch(dspaceNames, orcidNames);
		
		// output the resultant mappings
		Iterator nameIt = mappedNames.entrySet().iterator();
		while(nameIt.hasNext()) {
		    Map.Entry pairs = (Map.Entry)nameIt.next();
		    OrcidName mappedOrcidEntry = (OrcidName)pairs.getKey();
		    OrcidName mappedDSpaceEntry = (OrcidName)pairs.getValue();
		    report(itemDOI + ", " + articleDOI + ", " + mappedOrcidEntry.getOrcid() + ", \"" + mappedOrcidEntry.getName() + "\", " +
			   mappedDSpaceEntry.getOrcid() + ", \"" + mappedDSpaceEntry.getName() + "\", " + hamrScore(mappedDSpaceEntry,mappedOrcidEntry));
		
		    setResult("Last processed item = " + handle + " -- " + itemDOI);
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

	try { 
	    context.complete();
        } catch (SQLException e) {
	    log.fatal("Unable to close database connection", e);
	}

        log.info("ODIN's Hamr complete");
        return Curator.CURATE_SUCCESS;
	}

    /**
       Computes a minimum between three integers.
    **/
    private static int minimum(int a, int b, int c) {
	    return Math.min(Math.min(a, b), c);
    }

    /**
       Levenshtein distance algorithm, borrowed from
       http://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance
    **/
    public static int computeLevenshteinDistance(CharSequence str1, CharSequence str2) {
        int[][] distance = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++)
            distance[i][0] = i;
        for (int j = 1; j <= str2.length(); j++)
            distance[0][j] = j;

        for (int i = 1; i <= str1.length(); i++)
            for (int j = 1; j <= str2.length(); j++)
            distance[i][j] = minimum(
                         distance[i - 1][j] + 1,
                         distance[i][j - 1] + 1,
                         distance[i - 1][j - 1] + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1));

        return distance[str1.length()][str2.length()];
    }
    
    /**
       Scores the correspondence between two author names.
    **/
    private double hamrScore(OrcidName name1, OrcidName name2) {
	// if the orcid ID's match, the names are a perfect match
	if(name1.getOrcid() != null && name1.getOrcid().equals(name2.getOrcid())) {
	    return 1.0;
	}

	int maxlen = Math.max(name1.getName().length(), name2.getName().length());
	int editlen = computeLevenshteinDistance(name1.getName(), name2.getName());

	return (double)(maxlen-editlen)/(double)maxlen;	
    }
    
    /**
       Matches two lists of OrcidNames using the Hamr algorithm.
    **/
    private HashMap<OrcidName,OrcidName> doHamrMatch(List<OrcidName>dspaceNames, List<OrcidName>orcidNames) {
	HashMap<OrcidName,OrcidName> matchedNames = new HashMap<OrcidName,OrcidName>();

	// for each dspaceName, find the best possible match in the orcidName list, provided the score is over the threshhold
	for(OrcidName dspaceName:dspaceNames) {
	    double currentScore = 0.0;
	    OrcidName currentMatch = null;
	    for(OrcidName orcidName:orcidNames) {
		double strength = hamrScore(dspaceName, orcidName);

		if(strength > currentScore && strength > MATCH_THRESHHOLD) {
		    currentScore = strength;
		    currentMatch = orcidName;
		}
	    }
	    if(currentScore > 0.0) {
		matchedNames.put(dspaceName, currentMatch);
	    }
	}

	return matchedNames;
    }
    
    /**
       Reports whether a given targetName appears in a list of OrcidNames.
    **/
    private boolean containsName(List<OrcidName> theList, String targetName) {
	if(theList != null) {
	    for(OrcidName aName:theList) {
		if(aName.getName().equals(targetName)) {
		    return true;
		}
	    }
	}
	return false;
    }

    /**
       Retrieve the text of a named sub-element of the given node.
    **/
    private String retrieveXMLChild(Node aNode, String elemName) {
	NodeList subNodes = aNode.getChildNodes();
	for(int i = 0; i < subNodes.getLength(); i++) {
	    Node subNode = subNodes.item(i);
	    String subNodeName = subNode.getNodeName();
	    if(subNodeName != null && subNodeName.equals(elemName)) {
		return subNode.getTextContent();
	    } else {
		String subResult = retrieveXMLChild(subNode, elemName);
		if(subResult != null) {
		    return subResult;
		}
		// if subResult is null, the for loop continues to other nodes
	    }
	}
	return null;
    }

    /**
       Retrieve a list of names associated with this DOI in ORCID.
       The DOI may represent a Dryad item, or any other work.
    **/
    private List<OrcidName> retrieveOrcidNames(String aDOI) {
	List<OrcidName> orcidNames = new ArrayList<OrcidName>();

	if(aDOI.startsWith("doi:")) {
	    aDOI = aDOI.substring("doi:".length());
	}
	try {
	    URL orcidQuery = new URL(ORCID_QUERY_BASE + "%22" + aDOI + "%22");
	    Document orcidDoc = docb.parse(orcidQuery.openStream());
	    NodeList nl = orcidDoc.getElementsByTagName("orcid-profile");
	    // for each returned ORCID profile...
	    for(int i = 0; i < nl.getLength(); i++) {
		Node profile = nl.item(i);
		String theOrcid = retrieveXMLChild(profile, "orcid");
		String givenName = retrieveXMLChild(profile, "given-names");
		String familyName = retrieveXMLChild(profile, "family-name");
		orcidNames.add(new OrcidName(theOrcid, familyName +  ", " + givenName));
	    }
	} catch (MalformedURLException e) {
	    log.error("cannot make a valid URL for aDOI="  + aDOI, e);
	} catch (IOException e) {
	    log.error("IO problem for aDOI="  + aDOI, e);
	} catch (SAXException e) {
	    log.error("error processing XML for aDOI="  + aDOI, e);
	}

        return orcidBios;
    }

    
    class OrcidName extends Object {
	private String orcid = "";
	private String name = "";
	
	public OrcidName(String orcid, String name) {
	    this.orcid = orcid;
	    this.name = name;
	}

	public String getName() {
	    return name;
	}

	public String getOrcid() {
	    return orcid;
	}

	public String toString() {
	    return name;
	}
    }
}
