/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpDelete;
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
import org.dspace.content.crosswalk.ParameterizedDisseminationCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.DOI;
import org.dspace.services.ConfigurationService;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
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
    static final String CFG_PREFIX
            = "identifier.doi.prefix";
    static final String CFG_PUBLISHER
            = "crosswalk.dissemination.DataCite.publisher";
    static final String CFG_DATAMANAGER
            = "crosswalk.dissemination.DataCite.dataManager";
    static final String CFG_HOSTINGINSTITUTION
            = "crosswalk.dissemination.DataCite.hostingInstitution";
    static final String CFG_NAMESPACE
            = "crosswalk.dissemination.DataCite.namespace";

    /**
     * Stores the scheme used to connect to the DataCite server. It will be set
     * by spring dependency injection.
     */
    protected String SCHEME;
    /**
     * Stores the hostname of the DataCite server. Set by spring dependency
     * injection.
     */
    protected String HOST;
    
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
     * {@link #setDisseminationCrosswalkName(String) setDisseminationCrosswalkName} which
     * instantiates the crosswalk.
     */
    protected ParameterizedDisseminationCrosswalk xwalk;
    
    protected ConfigurationService configurationService;
    
    protected String USERNAME;
    protected String PASSWORD;
    @Autowired
    protected HandleService handleService;
    
    public DataCiteConnector()
    {
        this.xwalk = null;
        this.USERNAME = null;
        this.PASSWORD = null;
    }
    
    /**
     * Used to set the scheme to connect the DataCite server. Used by spring
     * dependency injection.
     * @param DATACITE_SCHEME Probably https or http.
     */
    @Required
    public void setDATACITE_SCHEME(String DATACITE_SCHEME)
    {
        this.SCHEME = DATACITE_SCHEME;
    }

    /**
     * Set the hostname of the DataCite server. Used by spring dependency
     * injection.
     * @param DATACITE_HOST Hostname to connect to register DOIs (f.e. test.datacite.org).
     */
    @Required
    public void setDATACITE_HOST(String DATACITE_HOST)
    {
        this.HOST = DATACITE_HOST;
    }
    
    /**
     * Set the path on the DataCite server to register DOIs. Used by spring
     * dependency injection.
     * @param DATACITE_DOI_PATH Path to register DOIs, f.e. /doi.
     */
    @Required
    public void setDATACITE_DOI_PATH(String DATACITE_DOI_PATH)
    {
        if (!DATACITE_DOI_PATH.startsWith("/"))
        {
            DATACITE_DOI_PATH = "/" + DATACITE_DOI_PATH;
        }
        if (!DATACITE_DOI_PATH.endsWith("/"))
        {
            DATACITE_DOI_PATH = DATACITE_DOI_PATH + "/";
        }
        
        this.DOI_PATH = DATACITE_DOI_PATH;
    }
    
    /**
     * Set the path to register metadata on DataCite server. Used by spring
     * dependency injection.
     * @param DATACITE_METADATA_PATH Path to register metadata, f.e. /mds.
     */
    @Required
    public void setDATACITE_METADATA_PATH(String DATACITE_METADATA_PATH)
    {
        if (!DATACITE_METADATA_PATH.startsWith("/"))
        {
            DATACITE_METADATA_PATH = "/" + DATACITE_METADATA_PATH;
        }
        if (!DATACITE_METADATA_PATH.endsWith("/"))
        {
            DATACITE_METADATA_PATH = DATACITE_METADATA_PATH + "/";
        }
        
        this.METADATA_PATH = DATACITE_METADATA_PATH;
    }
    
    
    @Autowired
    @Required
    public void setConfigurationService(ConfigurationService configurationService)
    {
        this.configurationService = configurationService;
    }
    
    /**
     * Set the name of the dissemination crosswalk used to convert the metadata
     * into DataCite Metadata Schema. Used by spring dependency injection.
     * @param CROSSWALK_NAME The name of the dissemination crosswalk to use. This
     *                       crosswalk must be configured in dspace.cfg.
     */
    @Required
    public void setDisseminationCrosswalkName(String CROSSWALK_NAME) {
        this.CROSSWALK_NAME = CROSSWALK_NAME;
    }
    
    protected void prepareXwalk()
    {
        if (null != this.xwalk)
            return;
        
        this.xwalk = (ParameterizedDisseminationCrosswalk) CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(
                DisseminationCrosswalk.class, this.CROSSWALK_NAME);
        
        if (this.xwalk == null)
        {
            throw new RuntimeException("Can't find crosswalk '"
                    + CROSSWALK_NAME + "'!");
        }
    }
    
    protected String getUsername()
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
    
    protected String getPassword()
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

    
    @Override
    public boolean isDOIReserved(Context context, String doi)
            throws DOIIdentifierException
    {
        // get mds/metadata/<doi>
        DataCiteResponse resp = this.sendMetadataGetRequest(doi);
    
        switch (resp.getStatusCode())
        {
            // 200 -> reserved
            // if (200 && dso != null) -> compare url (out of response-content) with dso
            case (200) :
            {
                return true;
            }
                
            // 404 "Not Found" means DOI is neither reserved nor registered.
            case (404) :
            {
                return false;
            }
                
            // 410 GONE -> DOI is set inactive
            // that means metadata has been deleted
            // it is unclear if the doi has been registered before or only reserved.
            // it is unclear for which item it has been reserved
            // we will handle this as if it reserved for an unknown object.
            case (410) :
            {
                return true;
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
                throw new DOIIdentifierException("Unable to parse an answer from "
                        + "DataCite API. Please have a look into DSpace logs.",
                        DOIIdentifierException.BAD_ANSWER);
            }
        }
    }
    
    @Override
    public boolean isDOIRegistered(Context context, String doi)
            throws DOIIdentifierException
    {
        DataCiteResponse response = sendDOIGetRequest(doi);
        
        switch (response.getStatusCode())
        {
            // status code 200 means the doi is reserved and registered
            case (200) :
            {
                return true;
            }
            // Status Code 204 "No Content" stands for a known DOI without URL.
            // A DOI that is known but does not have any associated URL is
            // reserved but not registered yet.
            case (204) :
            {
                // we know it is reserved, but we do not know for which object.
                return false;
            }
            // 404 "Not Found" means DOI is neither reserved nor registered.
            case (404) :
            {
                return false;
            }
            // Catch all other http status code in case we forgot one.
            default :
            {
                log.warn("While checking if the DOI {} is registered, we got a "
                        + "http status code {} and the message \"{}\".",
                        new String[] {doi, Integer.toString(response.statusCode), response.getContent()});
                throw new DOIIdentifierException("Unable to parse an answer from "
                        + "DataCite API. Please have a look into DSpace logs.",
                        DOIIdentifierException.BAD_ANSWER);
            }
        }
    }

    
    @Override
    public void deleteDOI(Context context, String doi)
            throws DOIIdentifierException
    {
        if (!isDOIReserved(context, doi))
            return;
        
        // delete mds/metadata/<doi>
        DataCiteResponse resp = this.sendMetadataDeleteRequest(doi);
        switch(resp.getStatusCode())
        {
            //ok
            case (200) :
            {
                return;
            }
            // 404 "Not Found" means DOI is neither reserved nor registered.
            case (404) :
            {
                log.error("DOI {} is at least reserved, but a delete request "
                        + "told us that it is unknown!", doi);
                return;
            }
            // Catch all other http status code in case we forgot one.
            default :
            {
                log.warn("While deleting metadata of DOI {}, we got a "
                        + "http status code {} and the message \"{}\".",
                        new String[] {doi, Integer.toString(resp.statusCode), resp.getContent()});
                throw new DOIIdentifierException("Unable to parse an answer from "
                        + "DataCite API. Please have a look into DSpace logs.",
                        DOIIdentifierException.BAD_ANSWER);
            }
        }
    }

    @Override
    public void reserveDOI(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException
    {   
        this.prepareXwalk();

        DSpaceObjectService<DSpaceObject> dSpaceObjectService = ContentServiceFactory.getInstance().getDSpaceObjectService(dso);

        if (!this.xwalk.canDisseminate(dso))
        {
            log.error("Crosswalk " + this.CROSSWALK_NAME 
                    + " cannot disseminate DSO with type " + dso.getType() 
                    + " and ID " + dso.getID() + ". Giving up reserving the DOI "
                    + doi + ".");
            throw new DOIIdentifierException("Cannot disseminate "
                    + dSpaceObjectService.getTypeText(dso) + "/" + dso.getID()
                    + " using crosswalk " + this.CROSSWALK_NAME + ".",
                    DOIIdentifierException.CONVERSION_ERROR);
        }

        // Set the transform's parameters.
        // XXX Should the actual list be configurable?
        Map<String, String> parameters = new HashMap<>();
        if (configurationService.hasProperty(CFG_PREFIX))
            parameters.put("prefix",
                    configurationService.getProperty(CFG_PREFIX));
        if (configurationService.hasProperty(CFG_PUBLISHER))
            parameters.put("publisher",
                    configurationService.getProperty(CFG_PUBLISHER));
        if (configurationService.hasProperty(CFG_DATAMANAGER))
            parameters.put("datamanager",
                    configurationService.getProperty(CFG_DATAMANAGER));
        if (configurationService.hasProperty(CFG_HOSTINGINSTITUTION))
            parameters.put("hostinginstitution",
                    configurationService.getProperty(CFG_HOSTINGINSTITUTION));

        Element root = null;
        try
        {
            root = xwalk.disseminateElement(context, dso, parameters);
        }
        catch (AuthorizeException ae)
        {
            log.error("Caught an AuthorizeException while disseminating DSO "
                    + "with type " + dso.getType() + " and ID " + dso.getID()
                    + ". Giving up to reserve DOI " + doi + ".", ae);
            throw new DOIIdentifierException("AuthorizeException occured while "
                    + "converting " + dSpaceObjectService.getTypeText(dso) + "/" + dso.getID()
                    + " using crosswalk " + this.CROSSWALK_NAME + ".", ae,
                    DOIIdentifierException.CONVERSION_ERROR);
        }
        catch (CrosswalkException ce)
        {
            log.error("Caught an CrosswalkException while reserving a DOI ("
                    + doi + ") for DSO with type " + dso.getType() + " and ID " 
                    + dso.getID() + ". Won't reserve the doi.", ce);
            throw new DOIIdentifierException("CrosswalkException occured while "
                    + "converting " + dSpaceObjectService.getTypeText(dso) + "/" + dso.getID()
                    + " using crosswalk " + this.CROSSWALK_NAME + ".", ce,
                    DOIIdentifierException.CONVERSION_ERROR);
        }
        catch (IOException | SQLException ex)
        {
            throw new RuntimeException(ex);
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
            log.error("While reserving a DOI, the "
                    + "crosswalk to generate the metadata used another DOI than "
                    + "the DOI we're reserving. Cannot reserve DOI " + doi
                    + " for " + dSpaceObjectService.getTypeText(dso) + " " 
                    + dso.getID() + ".");
            throw new IllegalStateException("An internal error occured while "
                    + "generating the metadata. Unable to reserve doi, see logs "
                    + "for further information.");
        }
        
        // send metadata as post to mds/metadata
        DataCiteResponse resp = this.sendMetadataPostRequest(doi, root);
        
        switch (resp.getStatusCode())
        {
            // 201 -> created / ok
            case (201) :
            {
                return;
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
                throw new DOIIdentifierException("Unable to reserve DOI " + doi 
                        + ". Please inform your administrator or take a look "
                        +" into the log files.", DOIIdentifierException.BAD_REQUEST);
            }
            // Catch all other http status code in case we forgot one.
            default :
            {
                log.warn("While reserving the DOI {}, we got a http status code "
                        + "{} and the message \"{}\".", new String[]
                        {doi, Integer.toString(resp.statusCode), resp.getContent()});
                throw new DOIIdentifierException("Unable to parse an answer from "
                        + "DataCite API. Please have a look into DSpace logs.",
                        DOIIdentifierException.BAD_ANSWER);
            }
        }
    }
    
    @Override
    public void registerDOI(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException
    {
        // DataCite wants us to reserve a DOI before we can register it
        if (!this.isDOIReserved(context, doi))
        {
            // the DOIIdentifierProvider should catch and handle this
            throw new DOIIdentifierException("You need to reserve a DOI "
                    + "before you can register it.",
                    DOIIdentifierException.RESERVE_FIRST);
        }

        // send doi=<doi>\nurl=<url> to mds/doi
        DataCiteResponse resp = null;
        try
        {
            resp = this.sendDOIPostRequest(doi, 
                    handleService.resolveToURL(context, dso.getHandle()));
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
                return;
            }
            // 400 -> wrong domain, wrong prefix, wrong request body
            case (400) :
            {
                log.warn("We send an irregular request to DataCite. While "
                        + "registering a DOI they told us: " + resp.getContent());
                throw new DOIIdentifierException("Currently we cannot register "
                        + "DOIs. Please inform the administrator or take a look "
                        + " in the DSpace log file.",
                        DOIIdentifierException.BAD_REQUEST);
            }
            // 412 Precondition failed: DOI was not reserved before registration!
            case (412) :
            {
                log.error("We tried to register a DOI {} that has not been reserved "
                        + "before! The registration agency told us: {}.", doi,
                        resp.getContent());
                throw new DOIIdentifierException("There was an error in handling "
                        + "of DOIs. The DOI we wanted to register had not been "
                        + "reserved in advance. Please contact the administrator "
                        + "or take a look in DSpace log file.",
                        DOIIdentifierException.RESERVE_FIRST);
            }
            // Catch all other http status code in case we forgot one.
            default :
            {
                log.warn("While registration of DOI {}, we got a http status code "
                        + "{} and the message \"{}\".", new String[]
                        {doi, Integer.toString(resp.statusCode), resp.getContent()});
                throw new DOIIdentifierException("Unable to parse an answer from "
                        + "DataCite API. Please have a look into DSpace logs.",
                        DOIIdentifierException.BAD_ANSWER);
            }
        }
    }
    
    @Override
    public void updateMetadata(Context context, DSpaceObject dso, String doi) 
            throws DOIIdentifierException
    { 
        // We can use reserveDOI to update metadata. DataCite API uses the same
        // request for reservation as for updating metadata.
        this.reserveDOI(context, dso, doi);
    }
    
    protected DataCiteResponse sendDOIPostRequest(String doi, String url)
            throws DOIIdentifierException
    {
        // post mds/doi/
        // body must contaion "doi=<doi>\nurl=<url>}n"
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme(SCHEME).setHost(HOST).setPath(DOI_PATH);
        
        HttpPost httppost = null;
        try
        {
            httppost = new HttpPost(uribuilder.build());
        }
        catch (URISyntaxException e)
        {
            log.error("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!");
            log.error("The URL was {}.", SCHEME + "://" + HOST +
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
            // release resources
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
    
    
    protected DataCiteResponse sendMetadataDeleteRequest(String doi)
            throws DOIIdentifierException
    {
        // delete mds/metadata/<doi>
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme(SCHEME).setHost(HOST).setPath(METADATA_PATH
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
            log.error("The URL was {}.", SCHEME + "://" + HOST +
                    DOI_PATH + "/" + doi.substring(DOI.SCHEME.length()));
            throw new RuntimeException("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!", e);
        }
        return sendHttpRequest(httpdelete, doi);
    }
    
    protected DataCiteResponse sendDOIGetRequest(String doi)
            throws DOIIdentifierException
    {
        return sendGetRequest(doi, DOI_PATH);
    }
    
    protected DataCiteResponse sendMetadataGetRequest(String doi)
            throws DOIIdentifierException
    {
        return sendGetRequest(doi, METADATA_PATH);
    }
    
    protected DataCiteResponse sendGetRequest(String doi, String path)
            throws DOIIdentifierException
    {
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme(SCHEME).setHost(HOST).setPath(path
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
            log.error("The URL was {}.", SCHEME + "://" + HOST +
                    DOI_PATH + "/" + doi.substring(DOI.SCHEME.length()));
            throw new RuntimeException("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!", e);
        }
        return sendHttpRequest(httpget, doi);
    }
    
    protected DataCiteResponse sendMetadataPostRequest(String doi, Element metadataRoot)
            throws DOIIdentifierException
    {
        Format format = Format.getCompactFormat();
        format.setEncoding("UTF-8");
        XMLOutputter xout = new XMLOutputter(format);
        return sendMetadataPostRequest(doi, xout.outputString(new Document(metadataRoot)));
    }
    
    protected DataCiteResponse sendMetadataPostRequest(String doi, String metadata)
            throws DOIIdentifierException
    {
        // post mds/metadata/
        // body must contain metadata in DataCite-XML.
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme(SCHEME).setHost(HOST).setPath(METADATA_PATH);
        
        HttpPost httppost = null;
        try
        {
            httppost = new HttpPost(uribuilder.build());
        }
        catch (URISyntaxException e)
        {
            log.error("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!");
            log.error("The URL was {}.", SCHEME + "://" + HOST +
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
            // release resources
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
     * @return response from DataCite
     * @throws DOIIdentifierException if DOI error
     */
    protected DataCiteResponse sendHttpRequest(HttpUriRequest req, String doi)
            throws DOIIdentifierException
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

            /* While debugging it can be useful to see which requests are sent:
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

            // We can handle some status codes here, others have to be handled above
            switch (statusCode)
            {
                // we get a 401 if we forgot to send credentials or if the username
                // and password did not match.
                case (401) :
                {
                    log.info("We were unable to authenticate against the DOI registry agency.");
                    log.info("The response was: {}", content);
                    throw new DOIIdentifierException("Cannot authenticate at the "
                            + "DOI registry agency. Please check if username "
                            + "and password are set correctly.",
                            DOIIdentifierException.AUTHENTICATION_ERROR);
                }

                // We get a 403 Forbidden if we are managing a DOI that belongs to
                // another party or if there is a login problem.
                case (403) :
                {
                    log.info("Managing a DOI ({}) was prohibited by the DOI "
                            + "registration agency: {}", doi, content);
                    throw new DOIIdentifierException("We can check, register or "
                            + "reserve DOIs that belong to us only.",
                            DOIIdentifierException.FOREIGN_DOI);
                }


                // 500 is documented and signals an internal server error
                case (500) :
                {
                    log.warn("Caught an http status code 500 while managing DOI "
                            +"{}. Message was: " + content);
                    throw new DOIIdentifierException("DataCite API has an internal error. "
                            + "It is temporarily impossible to manage DOIs. "
                            + "Further information can be found in DSpace log file.",
                            DOIIdentifierException.INTERNAL_ERROR);
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
                // Release any resources used by HTTP-Request.
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
    protected String extractAlternateIdentifier(Context context, String content)
    throws SQLException, DOIIdentifierException
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
            throw new DOIIdentifierException("Got a JDOMException while parsing "
                    + "a response from the DataCite API.", jde,
                    DOIIdentifierException.BAD_ANSWER);
        }
        
        String handle = null;
        
        Iterator<Element> it = doc.getDescendants(new ElementFilter("alternateIdentifier"));
        while (handle == null && it.hasNext())
        {
            Element alternateIdentifier = it.next();
            try
            {
                handle = handleService.resolveUrlToHandle(context,
                        alternateIdentifier.getText());
            }
            catch (SQLException e)
            {
                throw e;
            }
        }

        return handle;
    }
    
    protected String extractDOI(Element root) {
        Element doi = root.getChild("identifier", root.getNamespace());
        return (null == doi) ? null : doi.getTextTrim();
    }

    protected Element addDOI(String doi, Element root) {
        if (null != extractDOI(root))
        {
            return root;
        }
        Element identifier = new Element("identifier",
                    configurationService.getProperty(CFG_NAMESPACE,
                        "http://datacite.org/schema/kernel-3"));
        identifier.setAttribute("identifierType", "DOI");
        identifier.addContent(doi.substring(DOI.SCHEME.length()));
        return root.addContent(0, identifier);
    }

    protected class DataCiteResponse
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
