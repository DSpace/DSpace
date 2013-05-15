package org.dspace.doi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

@SuppressWarnings("deprecation")
public class Minter implements org.springframework.beans.factory.InitializingBean {
    
    private static Logger log = Logger.getLogger(Minter.class);
    
    private boolean myDataCiteConnectionIsLive;
    private CDLDataCiteService myDoiService;
    
    private DOIDatabase myLocalDatabase;
    private String myDataPkgColl;
    private String myDataFileColl;
    private String myHdlPrefix;
    private String myHostname;

    private ConfigurationService configurationService=null;

	/**
	 * Initialized Minter used when called from script or JUnit test.
	 */
	public Minter() {}

	public DOI register(DOI aDOI, String target, Map<String, String> metadata) throws IOException {
	    log.debug("Entering register(DOI) method");
	    
	    try {
                if(target == null) {
                    target = aDOI.getTargetURL().toString();
                }

		String doi = aDOI.toID();
		log.debug("doi to register: " + doi + ", target: " + target);

		// open connection to the DOI URL
		URL url = aDOI.toURL();
		HttpURLConnection http = (HttpURLConnection) url.openConnection();
		http.connect();

		// if the URL issues a redirect, the DOI is already registered, so try to update it
		if (http.getResponseCode() == 303) {
		    if (!http.getHeaderField("Location").equals(target)) {
			if (myDataCiteConnectionIsLive) {
			    String response = myDoiService.update(doi, target, metadata);
			    log.debug("Response from DOI service: " + response);
			} else {
			    log.info("Dryad URL updated: " + aDOI + " = " + target);
			}
		    } else {
			log.debug("Ignored: URL " + target + " already registered");
		    }
		} else {
		    // DOI is not registered, create a new registration
		    if (myDataCiteConnectionIsLive) {
			String response = myDoiService.registerDOI(doi, target, metadata);
			response = response.toUpperCase();
			
			if(!response.contains("OK") &&
			   !response.contains("CREATED") &&
			   !response.contains("SUCCESS")) {
			    log.error("DOI Service reports problem: " + response);
			    myDoiService.emailException(response, doi, "registration");
			} else {
			    log.debug("DOI registered: " + response);
			}
		    } else {
			log.info("DOI service not connected, registration skipped.");
		    }
		}

		http.disconnect();
	    } catch (MalformedURLException details) {
		log.error("Malformed URL", details);
		throw new RuntimeException(details);
	    }
	
	    return aDOI;
	}

    /**
     * Creates a DOI from the supplied DSpace URL string
     *
     * @param aDOI
     * @return
     */
    public void mintDOI(DOI aDOI){
	log.debug("Checking to see if " + aDOI.toString() + " is in the DOI database");
	
        DOI doi = myLocalDatabase.getByDOI(aDOI.toString());
	
        if(doi==null || !doi.getInternalIdentifier().equals(aDOI.getInternalIdentifier())) {
	    log.debug(aDOI + " wasn't found or it is pointing to a different URL, assigning to: " + aDOI.getTargetURL().toString());
	    
            if (!myLocalDatabase.put(aDOI)){
                throw new RuntimeException("Should be able to put if db doesn't contain DOI");
            }
        } else {
	    log.debug(aDOI + " is in local database");
	}
    }

	/**
	 * Returns an existing (e.g. known) DOI or null if the supplied DOI string
	 * isn't known.
	 *
	 * @param aDOIString
	 * @return
	 */
	public DOI getKnownDOI(String aDOIString) {
        if(myLocalDatabase==null) myLocalDatabase = DOIDatabase.getInstance();
		return myLocalDatabase.getByDOI(aDOIString);
	}

    public Set<DOI> getKnownDOIByURL(String url) {
		return myLocalDatabase.getByURL(url);
	}

    public Set<DOI> getAllKnownDOI() {
		return myLocalDatabase.getALL();
	}


    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }


    /**
     * Breaks down the DSpace URL (e.g.,
     * http://dev.datadryad.org/handle/12345/dryad.620) into a "12345/dryad.620"
     * part and a "dryad.620" part.
     *
     * @param aDSpaceURL
     * @return Metadata about item gathered from the URL
     */
    public URLMetadata getURLMetadata(String aDSpaceURL) {
	int breakPoint = aDSpaceURL.lastIndexOf(myHdlPrefix + "/") + 1;
	int start = breakPoint + myHdlPrefix.length();
	
	if (start > myHdlPrefix.length()) {
	    String id = aDSpaceURL.substring(start, aDSpaceURL.length());
	    return new URLMetadata(myHdlPrefix + "/" + id, id);
	} else {
	    return new URLMetadata(myHdlPrefix + "/" + aDSpaceURL, aDSpaceURL);
	}
    }
    

    public void dump(OutputStream aOut) throws IOException {
	myLocalDatabase.dump(aOut);
    }
    
    public int count() throws IOException {
	return myLocalDatabase.size();
    }


    public boolean remove(DOI aDOI) {
        if (aDOI == null || aDOI.equals(""))
            throw new RuntimeException("Provide a good Identifier to remove. ");
	
	if (myLocalDatabase.contains(aDOI)) {
	    return myLocalDatabase.remove(aDOI);
	}
	return false;
    }


    public void close() {
	myLocalDatabase.close();
    }
    
    /**
     * Breaks down the DSpace handle-like string (e.g.,
     * http://dev.datadryad.org/handle/12345/dryad.620) into a "12345/dryad.620"
     * part and a "dryad.620" part (in that order).
     *
     * @param aHDL
     * @return
     */
    public String[] stripHandle(String aHDL) {
	int start = aHDL.lastIndexOf(myHdlPrefix + "/") + 1
	    + myHdlPrefix.length();
	String id;
	
	if (start > myHdlPrefix.length()) {
	    id = aHDL.substring(start, aHDL.length());
	    return new String[] { myHdlPrefix + "/" + id, id };
	} else {
	    return new String[] { myHdlPrefix + "/" + aHDL, aHDL };
	}
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
	// For testing...
	
        Minter minter = new DSpace().getSingletonService(Minter.class);
	
	// Do the fake optparse waltz... what does dspace use for this?
	if (args.length > 0) {
	    if (log.isDebugEnabled()) {
		log.debug("Initial arguments: " + Arrays.toString(args));
	    }
	    
	    switch (args[0].charAt(0)) {
	    case '-':
		switch (args[0].charAt(1)) {
		case 'h':
		    printUsage();
		    break;
		case 's':
		    if (args.length != 2) {
			printUsage();
		    } else {
			DOI doi = minter.getKnownDOI(args[1]);
			
			if (doi != null) {
			    System.out.println(doi.toString() + " "
					       + doi.getTargetURL());
			} else {
			    System.out.println(args[1] + " not found");
			}
		    }
		    break;
		case 'c':
		    System.out.println("Total DOIs stored in db: "
				       + minter.count());
		    break;
		case 'p':
		    if (args.length == 2) {
			FileOutputStream out = new FileOutputStream(args[1]);
			minter.dump(out);
		    } else {
			System.out.println("DOI Dump:");
			System.out.println();
			minter.dump(System.out);
		    }
		}
		break;
	    default:
		printUsage();
	    }
	} else {
	    printUsage();
	}
    }
    
    private static void printUsage() {
	System.out.println("Usage:");
	System.out.println("  ");
	System.out.println("  -h              Help... prints this usage information");
	System.out.println("  -s              Search for a known DOI and return it");
	System.out.println("  -p <FILE>       Prints the DOI database to an output stream");
	System.out.println("  -c              Outputs the number of DOIs in the database");
    }


    public void afterPropertiesSet() throws Exception {
        String doiDirString = configurationService.getProperty("doi.dir");
	
	if (doiDirString == null || doiDirString.equals("")) {
	    String message = "Failed to find ${doi.dir} in dspace.cfg";
	    log.fatal(message);
	    throw new RuntimeException(message);
	}
	
	File doiDir = new File(doiDirString);
	
	if (!doiDir.exists()) {
	    if (!doiDir.mkdir()) {
		String message = "Failed to create ${doi.dir}";
		log.fatal(message);
		throw new RuntimeException(message);
	    }
	}
	
	if (!doiDir.isDirectory() || !doiDir.canWrite()) {
	    String message = "Either $(doi.dir} isn't a dir or it isn't writeable";
	    log.fatal(message);
	    throw new RuntimeException(message);
	}
	
	String doiUsername = configurationService.getProperty("doi.username");
	String doiPassword = configurationService.getProperty("doi.password");
	String length = null;
	
	myHdlPrefix = configurationService.getProperty("handle.prefix");
	myDoiService = new CDLDataCiteService(doiUsername, doiPassword);
	myHostname = configurationService.getProperty("dryad.url");
	myDataCiteConnectionIsLive = configurationService.getPropertyAsType("doi.datacite.connected",boolean.class);
	myDataPkgColl = configurationService.getProperty("stats.datapkgs.coll");
	myDataFileColl = configurationService.getProperty("stats.datafiles.coll");
	
	if (myDataPkgColl == null || myDataPkgColl.equals("")) {
	    throw new RuntimeException("stats.datapkgs.coll in dspace.cfg not configured");
	}
	
	if (myDataFileColl == null || myDataFileColl.equals("")) {
	    throw new RuntimeException("stats.datafiles.coll in dspace.cfg not configured");
	}
	
	// TODO some error checking on these dspace.cfg values
	try {
	    length = configurationService.getProperty("doi.suffix.length");
	} catch (NumberFormatException details) {
	    log.warn("dspace.cfg error: " + length + " is not a valid number");
	}
	
    }
    
    private class URLMetadata {
	@SuppressWarnings("unused")
	    private String myItemHandle;
	@SuppressWarnings("unused")
	    private String myItemName;
	
	private URLMetadata(String aItemHandle, String aItemName) {
	    myItemHandle = aItemHandle;
	    myItemName = aItemName;
	}
    }
    
    public DOIDatabase getMyLocalDatabase() {
        return myLocalDatabase;
    }
    
    public void setMyLocalDatabase(DOIDatabase myLocalDatabase) {
        this.myLocalDatabase = myLocalDatabase;
    }
}
