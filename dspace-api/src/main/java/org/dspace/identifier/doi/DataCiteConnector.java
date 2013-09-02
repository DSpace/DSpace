/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.DOI;
import org.dspace.identifier.IdentifierException;
import org.dspace.services.ConfigurationService;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 * @author Pascal-Nicolas Becker
 */
public class DataCiteConnector
implements DOIConnector
{

    private static final Logger log = LoggerFactory.getLogger(DataCiteConnector.class);
    
    // Configuration property names
    static final String CFG_USER = "identifier.doi.user";
    static final String CFG_PASSWORD = "identifier.doi.password";
    
    /**
     * Stores the scheme used to connect to the DataCite server. It will be set
     * by spring dependency injection.
     */
    private String SCHEME;
    /**
     * Stores the hostname of the DataCite server. Set by spring dependency
     * injection.
     */
    private String HOST;
    
    /**
     * Path on the DataCite server used to generate DOIs. Set by spring
     * dependency injection.
     */
    protected String DOI_PATH;
    /**
     * Path on the DataCite server used to register metadata. Set by spring
     * dependency injection.
     */
    protected String METADATA_PATH;
    /**
     * Name of crosswalk to convert metadata into DataCite Metadata Scheme. Set 
     * by spring dependency injection.
     */
    protected String CROSSWALK_NAME;
    /** 
     * DisseminationCrosswalk to map local metadata into DataCite metadata.
     * The name of the crosswalk is set by spring dependency injection using
     * {@link setDisseminationCrosswalk(String) setDisseminationCrosswalk} which
     * instantiates the crosswalk.
     */
    protected DisseminationCrosswalk xwalk;
    
    protected ConfigurationService configurationService;
    
    protected String USERNAME;
    protected String PASSWORD;
    
    /*
     * While registering/reserving a DOI we have several requests to check if
     * a DOI is reserved or registered. Some of those request check if a DOI is
     * reserved/registered for a specific dso, others check if a DOI is
     * reserved/registered in general. As opening a http connection to the
     * datacite server can take some time, we want to cache those informations.
     * Using {@link relax()} the cache can be cleaned.
     * The following maps use DOIs as keys. As values, null or an array of ints
     * with length 2 will be used. Null means the specific DOI is not reserved
     * or registered while an array of ints contains the type and id of the dso it is
     * registered/reserved for.
     */
    private Map<String, int[]> reserved;
    private Map<String, int[]> registered;
    private long cacheCreationTime;
    private long cacheTimeout;
    
    public DataCiteConnector()
    {
        this.xwalk = null;
        this.USERNAME = null;
        this.PASSWORD = null;
        this.registered = new HashMap<String, int[]>();
        this.reserved = new HashMap<String, int[]>();
        this.cacheCreationTime = -1l;
        // default value
        this.cacheTimeout = 120l;
    }
    
    /**
     * Used to set the scheme to connect the DataCite server. Used by spring
     * dependency injection.
     * @param DATACITE_SCHEME Probably https or http.
     */
    @Required
    public void setDATACITE_SCHEME(String DATACITE_SCHEME) {
        this.SCHEME = DATACITE_SCHEME;
    }

    /**
     * Set the hostname of the DataCite server. Used by spring dependency
     * injection.
     * @param DATACITE_HOST Hostname to connect to register DOIs (f.e. test.datacite.org).
     */
    @Required
    public void setDATACITE_HOST(String DATACITE_HOST) {
        this.HOST = DATACITE_HOST;
    }
    
    /**
     * To be able to set the cache timeout by spring dependency ingestion.
     * Default value set in {@link #DataCiteConnector()}.
     * 
     * @param CACHE_TIMEOUT time to hold cached information about reserved and 
     *                      registered DOIs in seconds.
     */
    public void setCACHE_TIMEOUT(long CACHE_TIMEOUT) {
        this.cacheTimeout = CACHE_TIMEOUT;
    }
    
    /**
     * Set the path on the DataCite server to register DOIs. Used by spring
     * dependency injection.
     * @param DATACITE_DOI_PATH Path to register DOIs, f.e. /doi.
     */
    @Required
    public void setDATACITE_DOI_PATH(String DATACITE_DOI_PATH) {
        if (!DATACITE_DOI_PATH.startsWith("/"))
            DATACITE_DOI_PATH = "/" + DATACITE_DOI_PATH;
        if (!DATACITE_DOI_PATH.endsWith("/"))
            DATACITE_DOI_PATH = DATACITE_DOI_PATH + "/";
        
        this.DOI_PATH = DATACITE_DOI_PATH;
    }
    
    /**
     * Set the path to register metadata on DataCite server. Used by spring
     * dependency injection.
     * @param DATACITE_METADATA_PATH Path to register metadata, f.e. /mds.
     */
    @Required
    public void setDATACITE_METADATA_PATH(String DATACITE_METADATA_PATH) {
        if (!DATACITE_METADATA_PATH.startsWith("/"))
            DATACITE_METADATA_PATH = "/" + DATACITE_METADATA_PATH;
        if (!DATACITE_METADATA_PATH.endsWith("/"))
            DATACITE_METADATA_PATH = DATACITE_METADATA_PATH + "/";
        
        this.METADATA_PATH = DATACITE_METADATA_PATH;
    }
    
    
    @Autowired
    @Required
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }
    
    /**
     * Set the name of the dissemination crosswalk used to convert the metadata
     * into DataCite Metadata Schema. This method tries to initialize the named
     * crosswalk. Used by spring dependency injection.
     * @param CROSSWALK_NAME The name of the dissemination crosswalk to use. This
     *                       crosswalk must be configured in dspace.cfg.
     */
    @Required
    public void setDisseminationCrosswalkName(String CROSSWALK_NAME) {
        this.CROSSWALK_NAME = CROSSWALK_NAME;
    }
    
    private void prepareXwalk()
    {
        if (null != this.xwalk)
            return;
        
        this.xwalk = (DisseminationCrosswalk) PluginManager.getNamedPlugin(
                DisseminationCrosswalk.class, this.CROSSWALK_NAME);
        
        if (this.xwalk == null)
        {
            throw new RuntimeException("Can't find crosswalk '"
                    + CROSSWALK_NAME + "'!");
        }
    }
    
    private String getUsername()
    {
        if (null == this.USERNAME)
        {
            this.USERNAME = this.configurationService.getProperty(CFG_USER);
            if (null == this.USERNAME)
            {
                throw new RuntimeException("Unable to load username from "
                        + "configuration. Cannot find property " +
                        CFG_USER + ".");
            }
        }
        return this.USERNAME;
    }
    
    private String getPassword()
    {
        if (null == this.PASSWORD)
        {
            this.PASSWORD = this.configurationService.getProperty(CFG_PASSWORD);
            if (null == this.PASSWORD)
            {
                throw new RuntimeException("Unable to load password from "
                        + "configuration. Cannot find property " +
                        CFG_PASSWORD + ".");
            }
        }
        return this.PASSWORD;
    }

    
    public boolean isDOIReserved(Context context, String doi)
            throws IdentifierException
    {
        if (!this.checkCache())
        {
            // do we have information about the doi in our cache?
            if (this.reserved.containsKey(doi))
            {
                // is it reserved (value in map is not null) or not?
                return (null != this.reserved.get(doi));
            }
        }
        
        return isDOIReserved(context, null, doi);
    }
    
    @Override
    public boolean isDOIReserved(Context context, DSpaceObject dso, String doi)
            throws IdentifierException
    {
        // get mds/metadata/<doi>
        
        // do we have cached information?
        if (!this.checkCache())
        {
            if (this.reserved.containsKey(doi))
            {
                // do we know that the doi is not reserved?
                if (null == this.reserved.get(doi))
                {
                    return false;
                }
                // we know that the doi is reserved.
                // Have we been asked if it is reserved for a specific dso?
                if (null == dso)
                {
                    return true;
                }
                else
                {
                    int[] ids = this.reserved.get(doi);
                    return (dso.getType() == ids[0] && dso.getID() == ids[1]);
                }
            }
        }
        
        DataCiteResponse resp = this.sendMetadataGetRequest(doi);
    
        switch (resp.getStatusCode())
        {
            // 200 -> reserved
            // if (200 && dso != null) -> compare url (out of response-content) with dso
            case (200) :
            {
                String handle = null;
                try
                {
                    handle = extractAlternateIdentifier(context, resp.getContent());
                }
                catch (SQLException e)
                {
                    if (null == dso)
                    {
                        return true;
                    }
                    throw new RuntimeException(e);
                }
                
                if (null == handle)
                {
                    // we were unable to find a handle belonging to our repository
                    // were we looking if a doi is reserved for a specific dso?
                    if (null != dso)
                    {
                        return false;
                    }
                    return true;
                }
                
                // add information to our cache
                int[] ids = new int[2];
                try
                {
                    DSpaceObject handleHolder = HandleManager.resolveToObject(context, handle);
                    ids[0] = handleHolder.getType();
                    ids[1] = handleHolder.getID();
                }
                catch (SQLException e)
                {
                    log.error("Error in database connection: " + e.getMessage());
                    throw new RuntimeException(e);
                }
                this.addToCache(doi, true, false, ids);
                
                if (null == dso)
                {
                    return true;
                }
                
                return (dso.getType() == ids[0] && dso.getID() == ids[1]);
            }
                
            // 404 "Not Found" means DOI is neither reserved nor registered.
            case (404) :
            {
                this.addToCache(doi, true, true, null);
                return false;
            }
                
            // 410 GONE -> DOI is set inactive
            // that means metadata has been deleted
            // it is unclear if the doi has been registered before or only reserved.
            // it is unclear for which item it has been reserved
            // we will handle this as if it reserved for an unknown object.
            case (410) :
            {
                if (null == dso)
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }
                
            // Catch all other http status code in case we forgot one.
            default :
            {
                log.warn("While checking if the DOI {} is registered, we got a "
                        + "http status code {} and the message \"{}\".",
                        new String[]
                        {
                            doi, Integer.toString(resp.statusCode),
                            resp.getContent()
                        });
                throw new IdentifierException("Unable to parse an answer from "
                        + "DataCite API. Please have a look into DSpace logs.");
            }
        }
    }
    
    @Override
    public boolean isDOIRegistered(Context context, String doi)
            throws IdentifierException
    {
        // Do we have information about the DOI in our cache?
        if (!this.checkCache())
        {
            if (this.registered.containsKey(doi))
            {
                // if the doi is reserved the value is not null.
                return (null != this.registered.get(doi));
            }
        }
        
        return isDOIRegistered(context, null, doi);
    }
    
    @Override
    public boolean isDOIRegistered(Context context, DSpaceObject dso, String doi)
            throws IdentifierException
    {
        // do we have information about the DOI in our cache?
        if (!this.checkCache())
        {
            if (this.registered.containsKey(doi))
            {
                int[] ids = this.registered.get(doi);
                if (null == ids)
                {
                    // doi is not reserved
                    return false;
                }
                if (null == dso)
                {
                    // don't check if it is registered for a specific dso, just
                    // wether it is registered or not
                    return true;
                }
                return (ids[0] == dso.getType() && ids[1] == dso.getID());
            }
        }
        
        DataCiteResponse response = sendDOIGetRequest(doi);
        
        switch (response.getStatusCode())
        {
            // status code 200 means the doi is reserved and registered
            // if we want to know if it is registered for a special dso we could
            // compare the metadata. But it's easier and should be sufficient to
            // compare the URLs.
            case (200) :
            {
                String url = response.getContent();
                if (null == url)
                {
                    log.error("Received a status code 200 without a response content. DOI: {}.", doi);
                    throw new IdentifierException("Received a http status code 200 without a response content.");
                }
                
                String handle = null;
                try
                {
                    handle = HandleManager.resolveUrlToHandle(context, url);
                }
                catch (SQLException e)
                {
                    log.error("Error in database connection: " + e.getMessage());
                    
                    if (null == dso)
                        return true;
                    
                    throw new RuntimeException(e);
                }
                
                // null == handle means it couldn't be extracted out of the url
                // or was not found in our local databse.
                if (null == handle && null == dso)
                    return true;
                // it is registered, but we cannot say for which object.
                if (null == handle)
                    return false;
                
                // resolve dso the handle is registered for
                int[] ids = new int[2];
                try
                {
                    DSpaceObject handleHolder = HandleManager.resolveToObject(context, handle);
                    ids[0] = handleHolder.getType();
                    ids[1] = handleHolder.getID();
                }
                catch (SQLException e)
                {
                    log.error("Error in database connection: " + e.getMessage());
                    throw new RuntimeException(e);
                }
                
                // safe this information in our cache
                // if it is registered, it must be reserverd too.
                this.addToCache(doi, true, true, ids);
                
                return (dso.getType() == ids[0] && dso.getID() == ids[1]);
            }
            // Status Code 204 "No Content" stands for a known DOI without URL.
            // A DOI that is known but does not have any associated URL is
            // reserved but not registered yet.
            case (204) :
            {
                // we know it is reserved, but we do not know for which object.
                // won't add this to the cache.
                return false;
            }
            // 404 "Not Found" means DOI is neither reserved nor registered.
            case (404) :
            {
                this.addToCache(doi, true, true, null);
                return false;
            }
            // Catch all other http status code in case we forgot one.
            default :
            {
                log.warn("While checking if the DOI {} is registered, we got a "
                        + "http status code {} and the message \"{}\".",
                        new String[] {doi, Integer.toString(response.statusCode), response.getContent()});
                throw new IdentifierException("Unable to parse an answer from "
                        + "DataCite API. Please have a look into DSpace logs.");
            }
        }
    }

    
    @Override
    public boolean deleteDOI(Context context, String doi)
            throws IdentifierException
    {
        if (!isDOIReserved(context, doi))
            return true;
        
        // delete mds/metadata/<doi>
        DataCiteResponse resp = this.sendMetadataDeleteRequest(doi);
        switch(resp.getStatusCode())
        {
            //ok
            case (200) :
            {
                return true;
            }
            // 404 "Not Found" means DOI is neither reserved nor registered.
            case (404) :
            {
                log.error("DOI {} is at least reserved, but a delete request "
                        + "told us that it is unknown!", doi);
                return true;
            }
            // Catch all other http status code in case we forgot one.
            default :
            {
                log.warn("While deleting metadata of DOI {}, we got a "
                        + "http status code {} and the message \"{}\".",
                        new String[] {doi, Integer.toString(resp.statusCode), resp.getContent()});
                throw new IdentifierException("Unable to parse an answer from "
                        + "DataCite API. Please have a look into DSpace logs.");
            }
        }
    }

    @Override
    public boolean reserveDOI(Context context, DSpaceObject dso, String doi)
            throws IdentifierException
    {
        this.prepareXwalk();
        
        if (!this.xwalk.canDisseminate(dso))
        {
            log.error("Crosswalk " + this.CROSSWALK_NAME 
                    + " cannot disseminate DSO with type " + dso.getType() 
                    + " and ID " + dso.getID() + ". Giving up reserving the DOI "
                    + doi + ".");
            log.warn("Please fix the crosswalk " + this.CROSSWALK_NAME + ".");
            return false;
        }
        
        Element root = null;
        try
        {
            root = xwalk.disseminateElement(dso);
        }
        catch (AuthorizeException ae)
        {
            log.error("Caught an Authorize Exception while disseminating DSO "
                    + "with type " + dso.getType() + " and ID " + dso.getID()
                    + ". Giving up to reserve DOI " + doi + ".");
            log.warn("AuthorizeExceptionMessage: " + ae.getMessage());
            return false;
        }
        catch (CrosswalkException ce)
        {
            log.error("Caught an CrosswalkException while reserving a DOI ("
                    + doi + ") for DSO with type " + dso.getType() + " and ID " 
                    + dso.getID() + ". Won't reserve the doi.");
            log.warn("Please fix the Crosswalk " + this.CROSSWALK_NAME + "!");
            return false;
        }
        catch (IOException ioe)
        {
            throw new RuntimeException(ioe);
        }
        catch (SQLException se)
        {
            throw new RuntimeException(se);
        }
        
        String metadataDOI = extractDOI(root);
        if (null == metadataDOI)
        {
            // The DOI will be saved as metadata of dso after successful
            // registration. To register a doi it has to be part of the metadata
            // sent to DataCite. So we add it to the XML we'll send to DataCite
            // and we'll add it to the DSO after successful registration.
            root = addDOI(doi, root);
        }
        else if (!metadataDOI.equals(doi.substring(DOI.SCHEME.length())))
        {
            throw new IdentifierException("DSO with type " + dso.getTypeText()
                    + " and id " + dso.getID() + " already has DOI "
                    + metadataDOI + ". Won't reserve DOI " + doi + " for it.");
        }
        
        // send metadata as post to mds/metadata
        DataCiteResponse resp = this.sendMetadataPostRequest(doi, root);
        
        switch (resp.getStatusCode())
        {
            // 201 -> created / ok
            case (201) :
            {
                addToCache(doi, true, false, new int[] {dso.getType(), dso.getID()});
                log.debug("Reserved DOI {}.", doi);
                return true;
            }
            // 400 -> invalid XML
            case (400) :
            {
                log.warn("DataCite was unable to understand the XML we send.");
                log.warn("DataCite Metadata API returned a http status code "
                        +"400: " + resp.getContent());
                Format format = Format.getCompactFormat();
                format.setEncoding("UTF-8");
                XMLOutputter xout = new XMLOutputter(format);
                log.info("We send the following XML:\n" + xout.outputString(root));
                throw new IdentifierException("Unable to reserve DOI " + doi 
                        + ". Please inform your administrator or take a look "
                        +" into the log files.");
            }
            // Catch all other http status code in case we forgot one.
            default :
            {
                log.warn("While reserving the DOI {}, we got a http status code "
                        + "{} and the message \"{}\".", new String[]
                        {doi, Integer.toString(resp.statusCode), resp.getContent()});
                throw new IdentifierException("Unable to parse an answer from "
                        + "DataCite API. Please have a look into DSpace logs.");
            }
        }
    }
    
    @Override
    public boolean registerDOI(Context context, DSpaceObject dso, String doi)
            throws IdentifierException
    {
        log.debug("Want to register DOI {}!", doi);

        // send doi=<doi>\nurl=<url> to mds/doi
        DataCiteResponse resp = null;
        try
        {
            resp = this.sendDOIPostRequest(doi, 
                    HandleManager.resolveToURL(context, dso.getHandle()));
        }
        catch (SQLException e)
        {
            log.error("Caught SQL-Exception while resolving handle to URL: "
                    + e.getMessage());
            throw new RuntimeException(e);
        }
        
        switch(resp.statusCode)
        {
            // 201 -> created/updated -> okay
            case (201) :
            {
                addToCache(doi, true, true, new int[] {dso.getType(), dso.getID()});
                return true;
            }
            // 400 -> wrong domain, wrong prefix, wrong request body
            case (400) :
            {
                log.warn("We send an irregular request to DataCite. While "
                        + "registering a DOI they told us: " + resp.getContent());
                throw new IdentifierException("Currently we cannot register "
                        + "DOIs. Please inform the administrator or take a look "
                        + " in the DSpace log file.");
            }
            // 412 Precondition failed: DOI was not reserved before registration!
            case (412) :
            {
                log.error("We tried to register a DOI {} that was not reserved "
                        + "before! The registration agency told us: {}.", doi,
                        resp.getContent());
                throw new IdentifierException("There was an error in handling "
                        + "of DOIs. The DOI we wanted to register had not been "
                        + "reserved in advance. Please contact the administrator "
                        + "or take a look in DSpace log file.");
            }
            // Catch all other http status code in case we forgot one.
            default :
            {
                log.warn("While registration of DOI {}, we got a http status code "
                        + "{} and the message \"{}\".", new String[]
                        {doi, Integer.toString(resp.statusCode), resp.getContent()});
                throw new IdentifierException("Unable to parse an answer from "
                        + "DataCite API. Please have a look into DSpace logs.");
            }
        }
    }
    
    /**
     * Purges cached information reserved and registered DOIs.
     * The DataCiteConnector caches information about reserved and registered 
     * DOIs. It caches these information as the DOIIdentifierProvider calls the
     * isDOIResereved and isDOIRegistered methods several times when it
     * reserves or registers new DOIs. This methods purges the cached
     * information. The cache will be purged automatically {@link #cacheTimeout}
     * seconds after first information was added to the cache.
     */
    public void purgeCache()
    {
        this.registered.clear();
        this.reserved.clear();
        this.cacheCreationTime = -1l;
    }
    
    private void addToCache(String doi, boolean reserved, boolean registered, int ids[])
    {
        Date date = new Date();
        if (this.cacheCreationTime < 0)
        {
            this.cacheCreationTime = date.getTime();
        }
        if (reserved)
        {
            this.reserved.put(doi, ids);
        }
        if (registered)
        {
            this.registered.put(doi, ids);
        }
    }
    
    private boolean checkCache()
    {
        Date date = new Date();
        if (date.getTime() > this.cacheTimeout + this.cacheCreationTime)
        {
            this.purgeCache();
            return true;
        }
        return false;
    }
    
    private DataCiteResponse sendDOIPostRequest(String doi, String url)
            throws IdentifierException
    {
        // post mds/doi/
        // body must contaion "doi=<doi>\nurl=<url>}n"
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme("https").setHost(HOST).setPath(DOI_PATH);
        
        HttpPost httppost = null;
        try
        {
            httppost = new HttpPost(uribuilder.build());
        }
        catch (URISyntaxException e)
        {
            log.error("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!");
            log.error("The URL was {}.", "https://" + HOST +
                    DOI_PATH + "/" + doi.substring(DOI.SCHEME.length()));
            throw new RuntimeException("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!", e);
        }
        
        // assemble request content:
        HttpEntity reqEntity = null;
        try
        {
            String req = "doi=" + doi.substring(DOI.SCHEME.length()) + "\n" + "url=" + url + "\n";
            ContentType contentType = ContentType.create("text/plain", "UTF-8");
            reqEntity = new StringEntity(req, contentType);
            httppost.setEntity(reqEntity);
            
            return sendHttpRequest(httppost, doi);
        }
        finally
        {
            // release ressources
            try
            {
                EntityUtils.consume(reqEntity);
            }
            catch (IOException ioe)
            {
               log.info("Caught an IOException while releasing a HTTPEntity:"
                       + ioe.getMessage());
            }
        }
    }
    
    
    private DataCiteResponse sendMetadataDeleteRequest(String doi)
            throws IdentifierException
    {
        // delete mds/metadata/<doi>
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme("https").setHost(HOST).setPath(METADATA_PATH
                + doi.substring(DOI.SCHEME.length()));
        
        HttpDelete httpdelete = null;
        try
        {
            httpdelete = new HttpDelete(uribuilder.build());
        }
        catch (URISyntaxException e)
        {
            log.error("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!");
            log.error("The URL was {}.", "https://" + HOST +
                    DOI_PATH + "/" + doi.substring(DOI.SCHEME.length()));
            throw new RuntimeException("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!", e);
        }
        return sendHttpRequest(httpdelete, doi);
    }
    
    private DataCiteResponse sendDOIGetRequest(String doi)
            throws IdentifierException
    {
        return sendGetRequest(doi, DOI_PATH);
    }
    
    private DataCiteResponse sendMetadataGetRequest(String doi)
            throws IdentifierException
    {
        return sendGetRequest(doi, METADATA_PATH);
    }
    
    private DataCiteResponse sendGetRequest(String doi, String path)
            throws IdentifierException
    {
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme("https").setHost(HOST).setPath(path
                + doi.substring(DOI.SCHEME.length()));
        
        HttpGet httpget = null;
        try
        {
            httpget = new HttpGet(uribuilder.build());
        }
        catch (URISyntaxException e)
        {
            log.error("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!");
            log.error("The URL was {}.", "https://" + HOST +
                    DOI_PATH + "/" + doi.substring(DOI.SCHEME.length()));
            throw new RuntimeException("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!", e);
        }
        return sendHttpRequest(httpget, doi);
    }
    
    private DataCiteResponse sendMetadataPostRequest(String doi, Element metadataRoot)
            throws IdentifierException
    {
        Format format = Format.getCompactFormat();
        format.setEncoding("UTF-8");
        XMLOutputter xout = new XMLOutputter(format);
        return sendMetadataPostRequest(doi, xout.outputString(new Document(metadataRoot)));
    }
    
    private DataCiteResponse sendMetadataPostRequest(String doi, Document metadata)
            throws IdentifierException
    {
        Format format = Format.getCompactFormat();
        format.setEncoding("UTF-8");
        XMLOutputter xout = new XMLOutputter(format);
        return sendMetadataPostRequest(doi, xout.outputString(metadata));
    }
    
    private DataCiteResponse sendMetadataPostRequest(String doi, String metadata)
            throws IdentifierException
    {
        // post mds/metadata/
        // body must contain metadata in DataCite-XML.
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme("https").setHost(HOST).setPath(METADATA_PATH);
        
        HttpPost httppost = null;
        try
        {
            httppost = new HttpPost(uribuilder.build());
        }
        catch (URISyntaxException e)
        {
            log.error("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!");
            log.error("The URL was {}.", "https://" + HOST +
                    DOI_PATH + "/" + doi.substring(DOI.SCHEME.length()));
            throw new RuntimeException("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!", e);
        }
        
        // assemble request content:
        HttpEntity reqEntity = null;
        try
        {
            ContentType contentType = ContentType.create("application/xml", "UTF-8");
            reqEntity = new StringEntity(metadata, contentType);
            httppost.setEntity(reqEntity);
            
            return sendHttpRequest(httppost, doi);
        }
        finally
        {
            // release ressources
            try
            {
                EntityUtils.consume(reqEntity);
            }
            catch (IOException ioe)
            {
               log.info("Caught an IOException while releasing an HTTPEntity:"
                       + ioe.getMessage());
            }
        }
    }
    
    /**
     * 
     * @param req
     * @param doi
     * @return
     * @throws IdentifierException 
     */
    private DataCiteResponse sendHttpRequest(HttpUriRequest req, String doi)
            throws IdentifierException
    {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.getCredentialsProvider().setCredentials(
                new AuthScope(HOST, 443),
                new UsernamePasswordCredentials(this.getUsername(), this.getPassword()));
        
        HttpEntity entity = null;
        try
        {
            HttpResponse response = httpclient.execute(req);
            
            StatusLine status = response.getStatusLine();
            int statusCode = status.getStatusCode();
            
            String content = null;
            entity = response.getEntity();
            if (null != entity)
            {
                content = EntityUtils.toString(entity, "UTF-8");
            }

            /* While debugging it can be useful to see whitch requests are send:
             *
             * log.debug("Going to send HTTP request of type " + req.getMethod() + ".");
             * log.debug("Will be send to " + req.getURI().toString() + ".");
             * if (req instanceof HttpEntityEnclosingRequestBase)
             * {
             *     log.debug("Request contains entity!");
             *     HttpEntityEnclosingRequestBase reqee = (HttpEntityEnclosingRequestBase) req;
             *     if (reqee.getEntity() instanceof StringEntity)
             *     {
             *         StringEntity se = (StringEntity) reqee.getEntity();
             *         try {
             *             BufferedReader br = new BufferedReader(new InputStreamReader(se.getContent()));
             *             String line = null;
             *             while ((line = br.readLine()) != null)
             *             {
             *                 log.debug(line);
             *             }
             *             log.info("----");
             *         } catch (IOException ex) {
             *             
             *         }
             *     }
             * } else {
             *     log.debug("Request contains no entity!");
             * }
             * log.debug("The request got http status code {}.", Integer.toString(statusCode));
             * if (null == content)
             * {
             *     log.debug("The response did not contain any answer.");
             * } else {
             *     log.debug("DataCite says: {}", content);
             * }
             * 
             */

            switch (statusCode)
            {
                // we get a 401 if we forgot to send credentials or if the username
                // and password did not match.
                case (401) :
                {
                    log.info("We were unable to authenticate against the DOI registry agency.");
                    log.info("The response was: {}", content);
                    throw new IdentifierException("Cannot authenticate at the "
                            + "DOI registry agency. Please check if username "
                            + "and password are set correctly.");
                }

                // We get a 403 Forbidden if we are managing a DOI that belongs to
                // another party or if there is a login problem.
                case (403) :
                {
                    log.info("Managing a DOI ({}) was prohibited by the DOI "
                            + "registration agency: {}", doi, content);
                    throw new IdentifierException("We can check, register or "
                            + "reserve DOIs that belong to us only.");
                }


                // 500 is documented and signals an internal server error
                case (500) :
                {
                    log.warn("Caught an http status code 500 while managing DOI "
                            +"{}. Message was: " + content);
                    throw new IdentifierException("DataCite API has an internal error. "
                            + "It is temporarily impossible to manage DOIs. "
                            + "Further information can be found in DSpace log file.");
                }
            }
            

            return new DataCiteResponse(statusCode, content);
        }
        catch (IOException e)
        {
            log.warn("Caught an IOException: " + e.getMessage());
            throw new RuntimeException(e);
        }
        finally
        {
            try
            {
                // Release any ressources used by HTTP-Request.
                if (null != entity)
                {
                    EntityUtils.consume(entity);
                }
            }
            catch (IOException e)
            {
                log.warn("Can't release HTTP-Entity: " + e.getMessage());
            }
        }
    }

    // returns null or handle
    private String extractAlternateIdentifier(Context context, String content)
    throws SQLException, IdentifierException
    {
        if (content == null)
        {
            return null;
        }
        
        // parse the XML
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = null;
        try
        {
            doc = saxBuilder.build(new ByteArrayInputStream(content.getBytes("UTF-8")));
        }
        catch (IOException ioe)
        {
            throw new RuntimeException("Got an IOException while reading from a string?!", ioe);
        }
        catch (JDOMException jde)
        {
            throw new IdentifierException("Got a JDOMException while parsing a response from the DataCite API.", jde);
        }
        
        String handle = null;
        
        Iterator<Element> it = doc.getDescendants(new ElementFilter("alternativeIdentifier"));
        while (handle == null && it.hasNext())
        {
            Element alternateIdentifier = it.next();
            try
            {
                handle = HandleManager.resolveUrlToHandle(context,
                        alternateIdentifier.getText());
            }
            catch (SQLException e)
            {
                throw e;
            }
        }

        return handle;
    }
    
    private String extractDOI(Element root) {
        Element doi = root.getChild("identifier", root.getNamespace());
        return (null == doi) ? null : doi.getTextTrim();
    }

    private Element addDOI(String doi, Element root) {
        if (null != extractDOI(root))
        {
            return root;
        }
        Element identifier = new Element("identifier", "http://datacite.org/schema/kernel-2.2");
        identifier.setAttribute("identifierType", "DOI");
        identifier.addContent(doi.substring(DOI.SCHEME.length()));
        return root.addContent(0, identifier);
    }

    private class DataCiteResponse
    {
        private final int statusCode;
        private final String content;

        protected DataCiteResponse(int statusCode, String content)
        {
            this.statusCode = statusCode;
            this.content = content;
        }
        
        protected int getStatusCode()
        {
            return this.statusCode;
        }
        
        protected String getContent()
        {
            return this.content;
        }
    }
}
