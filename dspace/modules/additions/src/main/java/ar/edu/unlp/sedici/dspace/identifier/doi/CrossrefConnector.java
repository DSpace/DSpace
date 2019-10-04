/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package ar.edu.unlp.sedici.dspace.identifier.doi;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.DOI;
import org.dspace.identifier.doi.DOIConnector;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.dspace.services.ConfigurationService;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 * @author Pascal-Nicolas Becker
 */
public class CrossrefConnector
implements DOIConnector
{

    private static final Logger log = LoggerFactory.getLogger(CrossrefConnector.class);
    
    // Configuration property names
    static final String CFG_USER = "identifier.doi.user";
    static final String CFG_PASSWORD = "identifier.doi.password";

    private static final String XML_EXTENSION = ".xml";
    
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
     * Path on the Crossref Server used to deposit records metadata. Set by spring
     * dependency injection.
     */
    protected String DEPOSIT_PATH;
    /**
     * Prefix of the deposit filename used in "fname" parameter for the "doMDUpload" operation when deposit at Crossref API.
     * Set by spring dependency injection.
     */
    protected String DEPOSIT_PREFIX_FILENAME;
    /**
     * Path on the Crossref Server used to poll the results about deposits of records already made.
     * Set by spring dependency injection.
     */
    protected String SUBMISSION_PATH;
    /**
     * Path on the Crossref Server used to make queries for already registered content at Crossref.
     * Set by spring dependency injection.
     */
    protected String QUERY_PATH;
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
    
    private String PROCESSING_DATE;

    //CRossref PARAMETERS names & values
    public static String OPERATION_PARAM = "operation";
    //doMDUpload parameters, for endpoint "/deposit"
    public static String DOMD_UPLOAD = "doMDUpload";
    public static String DOMD_LOGIN = "login_id";
    public static String DOMD_PASSWD = "login_passwd";
    public static String DOMD_FNAME = "fname";
    public static String DOMD_SERVELT_PATH = "deposit";
    //Parameters used for "/submissionDownload" endpoint
    //https://support.crossref.org/hc/en-us/articles/217515926
    public static String SBMDW_TYPE_PARAM= "type";
    public static String SBMDW_TYPE_RESULT= "result";
    public static String SBMDW_FNAME= "file_name";
    public static String SBMDW_USERNAME= "usr";
    public static String SBMDW_PASSWORD= "pwd";
    //Doi Batch Diagnostics Schema constants
    //https://support.crossref.org/hc/en-us/articles/214337306-Interpreting-Submission-Logs
    public static String DBD_STATUS_UNKNOWN = "unknown_submission";
    public static String DBD_STATUS_COMPLETED = "completed";
    public static String DBD_RECORD_DIAG_FAIL = "failure";
    public static String DBD_RECORD_DIAG_SUCC = "success";
    //DOI-to-metadata query constants
    // https://support.crossref.org/hc/en-us/articles/213566986
    public static String QUERY_PID = "pid";
    /** Parameter where put the DOI to query. It has the format "id=&lt;username&gt;:&lt;password&gt;".*/
    public static String QUERY_DOI_FIELD = "id";
    public static String QUERY_FORMAT_PARAM = "format";
    /** UNIXREF Query Output Format 
     * https://support.crossref.org/hc/en-us/articles/214936283-UNIXREF-query-output-format 
     */
    public static String CROSSREF_UNIXREF_FORMAT = "unixref";
    
    
    public CrossrefConnector()
    {
        this.xwalk = null;
        this.USERNAME = null;
        this.PASSWORD = null;
        this.PROCESSING_DATE = null;
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
     * Set the path on the Crossref server to deposit DOIs records. Used by spring
     * dependency injection.
     * @param CROSSREF_DEPOSIT_PATH Path to register DOIs, f.e. /doi.
     */
    @Required
    public void setCROSSREF_DEPOSIT_PATH(String CROSSREF_DEPOSIT_PATH)
    {
        if (!CROSSREF_DEPOSIT_PATH.startsWith("/"))
        {
            CROSSREF_DEPOSIT_PATH = "/" + CROSSREF_DEPOSIT_PATH;
        }
        
        this.DEPOSIT_PATH = CROSSREF_DEPOSIT_PATH;
    }
    
    /**
     * Set the path to register metadata on DataCite server. Used by spring
     * dependency injection.
     * @param CROSSREF_SUBMISSION_PATH Path to register metadata, f.e. /mds.
     */
    @Required
    public void setCROSSREF_SUBMISSION_PATH(String CROSSREF_SUBMISSION_PATH)
    {
        if (!CROSSREF_SUBMISSION_PATH.startsWith("/"))
        {
            CROSSREF_SUBMISSION_PATH = "/" + CROSSREF_SUBMISSION_PATH;
        }
        
        this.SUBMISSION_PATH = CROSSREF_SUBMISSION_PATH;
    }

    /**
     * Set the path to register metadata on DataCite server. Used by spring
     * dependency injection.
     * @param CROSSREF_QUERY_PATH Path to register metadata, f.e. /mds.
     */
    @Required
    public void setCROSSREF_QUERY_PATH(String CROSSREF_QUERY_PATH)
    {
        if (!CROSSREF_QUERY_PATH.startsWith("/"))
        {
            CROSSREF_QUERY_PATH = "/" + CROSSREF_QUERY_PATH;
        }
        
        this.QUERY_PATH = CROSSREF_QUERY_PATH;
    }
    
    public void setDEPOSIT_PREFIX_FILENAME(String DEPOSIT_PREFIX_FNAME) {
        this.DEPOSIT_PREFIX_FILENAME = DEPOSIT_PREFIX_FNAME;
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
        
        this.xwalk = (DisseminationCrosswalk) PluginManager.getNamedPlugin(
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
    
    /**
     * Return the name of the XML file send to Crossref to DEPOSIT endpoint.
     * @param doi
     * @return From a doi, it must return an unique filename with ".xml" extension. This filename
     * must be unique because the poll of deposit result (at "/submissionDownload") is based on
     * the name of the deposited file at "/deposit".
     */
    private String getDepositFileName(String doi) {
        return DEPOSIT_PREFIX_FILENAME + "_" + doi.substring(DOI.SCHEME.length()).replace("/", "_") + "_" + PROCESSING_DATE  + XML_EXTENSION;
    }
    
    /**
     * Set the exact time for the process of this connector.
     */
    private void initProcessingDate() {
        DateFormat df = new SimpleDateFormat("yy-MM-dd_HHmmss.SSS");
        PROCESSING_DATE = df.format(new Date());
    }

    /**
     * In Crossref context, there is no exists the concept of reservation. So always return @true.
     */
    public boolean isDOIReserved(Context context, String doi)
            throws DOIIdentifierException
    {
        return isDOIReserved(context, null, doi);
    }
    

    /**
     * In Crossref context, there is no exists the concept of reservation. So always return @true.
     */
    @Override
    public boolean isDOIReserved(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException
    {
        return true;
    }
    
    @Override
    public boolean isDOIRegistered(Context context, String doi)
            throws DOIIdentifierException
    {
        return isDOIRegistered(context, null, doi);
    }
    
    
    /**
     * Check if an specific doi is registered at registration agency.
     * 
     * @param doi   The doi for check at registration agency.
     * @param dso   (!)This parameter is no longer used, for now...
     * @return true     if DOI is registered at registration agency.
     */
    @Override
    public boolean isDOIRegistered(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException
    {
        DataCiteResponse response = sendDOIGetRequest(doi);
        switch (response.getStatusCode())
        {
            // status code 200 means the doi is reserved and registered
            case (200) :
            {
                Document queryResult = null;
                try {
                    queryResult = parseXMLContent(response.getContent());
                    if(queryResult == null) {
                        throw new DOIIdentifierException("Unable to obtain Crossref DOI-To-Query results ('/query') "
                                + "for DOI=" + doi + ". The response is empty...", DOIIdentifierException.INTERNAL_ERROR);
                    }
                } catch (JDOMException e) {
                    throw new DOIIdentifierException("Got a JDOMException while parsing "
                            + "a response from the Crossref DOI-To-Query results ('/query') endpoint,"
                            + "DOI=" + doi, e,
                            DOIIdentifierException.BAD_ANSWER);
                }
                // Do we check if doi is reserved generally or for a specified dso?
                if (null == dso)
                {
                            Element queryRoot = queryResult.getRootElement();
                            Element errorTagLookup = getElementFromPath(queryRoot, "/doi_records/doi_record/crossref/error", "", queryRoot.getNamespaceURI());
                            //If there is no <error> tag in the XML response, then there exists a records in Crossref for specified DOI...
                            return (errorTagLookup == null)? true : false;
                }
                
                // In our case of use, it is strange that an specific DOI change the URL to the object
                // where is pointing. Therefore, the verification for DSO URL is disabled.
                // In case check for DSO Url must be enabled in the future, this code remains here as help...
                
//                Element queryRoot = queryResult.getRootElement();
//                Element resourceURL = getElementFromPath(queryRoot, "/doi_records/doi_record/crossref//doi_data/resource", "", queryRoot.getNamespaceURI());
//                String doiUrl = (null == resourceURL || resourceURL.getTextTrim().isEmpty())? null : resourceURL.getTextTrim();
//                if (null == doiUrl)
//                {
//                    log.error("A DOI is registered at Crossref, but no resource URL was saved. DOI: {}.", doi);
//                    throw new DOIIdentifierException("A DOI is registered at Crossref, but no resource URL was saved.",
//                            DOIIdentifierException.BAD_ANSWER);
//                }
//                
//                String dsoUrl = null;
//                try
//                {
//                    dsoUrl = HandleManager.resolveToURL(context, dso.getHandle());
//                }
//                catch (SQLException e)
//                {
//                    log.error("Error in database connection: " + e.getMessage());
//                    throw new RuntimeException(e);
//                }
//                
//                if (null == dsoUrl)
//                {
//                    // the handle of the dso was not found in our db?!
//                    log.error("The HandleManager was unable to find the handle "
//                            + "of a DSpaceObject in the database!?! "
//                            + "Type: {} ID: {}", dso.getTypeText(), dso.getID());
//                    throw new RuntimeException("The HandleManager was unable to "
//                            + "find the handle of a DSpaceObject in the database!");
//                }
//                
//                return (dsoUrl.equals(doiUrl));
            }
            case (401) :
            {
                log.info("We were anauthorized to query against the DOI registry agency.");
                log.info("The response was: {}", response.getContent());
                throw new DOIIdentifierException("Cannot authenticate at the DOI registry agency. "
                        + "Please check if username and password are set correctly.",DOIIdentifierException.AUTHENTICATION_ERROR);
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
  //TODO adaptar a Crossref, leer https://support.crossref.org/hc/en-us/articles/213022326-Changing-or-deleting-DOIs
    public void deleteDOI(Context context, String doi)
            throws DOIIdentifierException
    {
        throw new UnsupportedOperationException("Deletion of DOI " + doi + " must be requested manually");
//        if (!isDOIReserved(context, doi))
//            return;
//        
//        // delete mds/metadata/<doi>
//        DataCiteResponse resp = this.sendMetadataDeleteRequest(doi);
//        switch(resp.getStatusCode())
//        {
//            //ok
//            case (200) :
//            {
//                return;
//            }
//            // 404 "Not Found" means DOI is neither reserved nor registered.
//            case (404) :
//            {
//                log.error("DOI {} is at least reserved, but a delete request "
//                        + "told us that it is unknown!", doi);
//                return;
//            }
//            // Catch all other http status code in case we forgot one.
//            default :
//            {
//                log.warn("While deleting metadata of DOI {}, we got a "
//                        + "http status code {} and the message \"{}\".",
//                        new String[] {doi, Integer.toString(resp.statusCode), resp.getContent()});
//                throw new DOIIdentifierException("Unable to parse an answer from "
//                        + "DataCite API. Please have a look into DSpace logs.",
//                        DOIIdentifierException.BAD_ANSWER);
//            }
//        }
    }

    /**
     * In Crossref context, there is no exists the concept of reservation. 
     * Crossref does not make DOI reservations, but directly post the DOIs to register
     *  along with respective metadata.
     */
    @Override
    public void reserveDOI(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException
    {   
        //Do nothing for Crossref...
    }
    
    @Override
    public void registerDOI(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException
    {
        // check if the DOI is already registered online
        if (this.isDOIRegistered(context, doi))
        {
            //TODO este bloque de código se activará cuando se complete toda la funcionalidad del método isDOIRegistered(context, dso, doi)
            // if it is registered for another object we should notify an admin
//            if (!this.isDOIRegistered(context, dso, doi))
//            {
//                // DOI is reserved for another object
//                log.warn("DOI {} is registered for another object already.", doi);
//                throw new DOIIdentifierException(DOIIdentifierException.DOI_ALREADY_EXISTS);
//            }
            // doi is registered for this object, we're done
            
            //FIXME hay un error en el flujo de DSpace, ya que si retorno en este punto sin lanzar excepción, entonces me genera dos veces el metadato dc.identifier.uri con el DOI.
            log.warn("DOI {} is already registered at Crossref. Will not be registered again.", doi);
            throw new DOIIdentifierException("DOI " + doi +" is already registered at Crossref. "
                    + "Will not be registered again.", DOIIdentifierException.DOI_ALREADY_EXISTS);
        }

        initProcessingDate();

        Element root = prepareDSOForDisseminate(dso, doi);
        
        checkCrossrefErrors(root, dso);

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
            // FIXME: that's not an error. If at all, it is worth logging it.
            throw new DOIIdentifierException("DSO with type " + dso.getTypeText()
                    + " and id " + dso.getID() + " already has DOI "
                    + metadataDOI + ". Won't register DOI " + doi + " for it.");
        }
        
        // send metadata FILE as a POST to "/deposit"
        DataCiteResponse resp = this.sendMetadataPostRequest(doi, root);
        
        switch(resp.statusCode)
        {
            // 200 -> Submission of Crossref file was successful.
            case (200) :
            {
                log.info("The submission file for DOI={} to Crossref was succesfully send! Proceeding to inspect result...", doi);
                break;
            }
            case (503) :
            {
                //Further info: https://support.crossref.org/hc/en-us/articles/214960123
                log.info("The submission file for DOI={} cannot be processed because the "
                        + "submission queue at Crossref is full. Retry deposit sending later...", doi);
                throw new DOIIdentifierException("Unable to submit file at Crossref for DOI= " + doi 
                        + ". The submission queue is full. Please retry later.",
                        DOIIdentifierException.INTERNAL_ERROR);
            }
            default :
            {
                log.warn("While registration of DOI {}, we got a http status code "
                        + "{} and the message \"{}\".", new String[]
                        {doi, Integer.toString(resp.statusCode), resp.getContent()});
                throw new DOIIdentifierException("Unable to parse an answer from "
                        + "Crossref API. Please have a look into DSpace logs.",
                        DOIIdentifierException.BAD_ANSWER);
            }
        }
        // Check at "submissionDownload" the result of metadata send to "/deposit" in the previous step
        //FIXME (Ver ticket#5914) Por ahora, se elimina la etapa de revisión del envío, hasta encontrar una solución factible...
        //checkSubmissionProcess(doi);
        
    }

    /**
     * Return DOM model for the XML file for submission at Crossref API.
     * @param dso   the DSpace object to disseminate.
     * @param doi   the DOI currently being processed.
     * @return  the DOM model result of the dissemination process.
     * @throws DOIIdentifierException   if an error occurs when disseminate.
     */
    private Element prepareDSOForDisseminate(DSpaceObject dso, String doi) throws DOIIdentifierException {
        this.prepareXwalk();
        
        if (!this.xwalk.canDisseminate(dso))
        {
            log.error("Crosswalk " + this.CROSSWALK_NAME 
                    + " cannot disseminate DSO with type " + dso.getType() 
                    + " and ID " + dso.getID() + ". Giving up reserving the DOI "
                    + doi + ".");
            throw new DOIIdentifierException("Cannot disseminate "
                    + dso.getTypeText() + "/" + dso.getID()
                    + " using crosswalk " + this.CROSSWALK_NAME + ".",
                    DOIIdentifierException.CONVERSION_ERROR);
        }
        
        Element root = null;
        try
        {
            root = xwalk.disseminateElement(dso);
        }
        catch (AuthorizeException ae)
        {
            log.error("Caught an AuthorizeException while disseminating DSO "
                    + "with type " + dso.getType() + " and ID " + dso.getID()
                    + ". Giving up to reserve DOI " + doi + ".", ae);
            throw new DOIIdentifierException("AuthorizeException occurred while "
                    + "converting " + dso.getTypeText() + "/" + dso.getID()
                    + " using crosswalk " + this.CROSSWALK_NAME + ".", ae,
                    DOIIdentifierException.CONVERSION_ERROR);
        }
        catch (CrosswalkException ce)
        {
            log.error("Caught an CrosswalkException while reserving a DOI ("
                    + doi + ") for DSO with type " + dso.getType() + " and ID " 
                    + dso.getID() + ". Won't reserve the doi.", ce);
            throw new DOIIdentifierException("CrosswalkException occurred while "
                    + "converting " + dso.getTypeText() + "/" + dso.getID()
                    + " using crosswalk " + this.CROSSWALK_NAME + ".", ce,
                    DOIIdentifierException.CONVERSION_ERROR);
        }
        catch (IOException ioe)
        {
            throw new RuntimeException(ioe);
        }
        catch (SQLException se)
        {
            throw new RuntimeException(se);
        }
        return root;
    }

    /**
     * Searches for any error in the XML generated by the crosswalk.
     * An error is represented by an <error> element inside the XML.
     * @param dso the DSpaceObject being processed.
     * @throws DOIIdentifierException if XML contains any error.
     */
    private void checkCrossrefErrors(Element root, DSpaceObject dso) throws DOIIdentifierException {
        String xpath_expression = "//dspaceCrswalk:error";
        List<Element> errorElements = getElementsFromPath(root, xpath_expression, "dspaceCrswalk",
                "http://www.dspace.org/xmlns/dspace/crosswalk");
        if (errorElements != null) {
            StringBuffer errors = new StringBuffer();
            for (Element errorElement : errorElements) {
                errors.append(errorElement.getValue());
                errors.append(". ");
                log.error("Crossref crosswalk error: "
                        + errorElement.getValue()
                        + ". For item "
                        + dso.getID());
            }
            throw new DOIIdentifierException("CrosswalkException occurred while "
                    + "converting " + dso.getTypeText() + " with id " + dso.getID()
                    + " using crosswalk " + this.CROSSWALK_NAME + ". "
                    + errors,
                    DOIIdentifierException.CONVERSION_ERROR);
        }
    }

    /**
     * Check at "/submissionDownload" the result of metadata send to "/deposit" related to the "doi" parameter value.
     * Raise an exception only if the submission was not processed correctly or another problem exists.
     * @param doi   the doi to check its submission state at Crossref.
     * @throws DOIIdentifierException if the submission for doi specified does not exists in Crossref, if the deposit
     *              was made with errors, and if the XML submitted was invalid.
     */
    private void checkSubmissionProcess(String doi) throws DOIIdentifierException {
        Document submissionResultDoc = null;
        String submissionResultResponse = null;
        String depositFilename = getDepositFileName(doi);
        try {
            submissionResultResponse = pollResultsForSentMetadata(doi).getContent();
            submissionResultDoc = parseXMLContent(submissionResultResponse);
            if(submissionResultDoc == null) {
                throw new DOIIdentifierException("Unable to obtain Crossref Submission result ('/submissionDownload') "
                        + "for filename=" + depositFilename + ". The response is empty...", DOIIdentifierException.INTERNAL_ERROR);
            } else {
                Element submissionRoot = submissionResultDoc.getRootElement();
                Attribute doi_batch_status = getAttributeFromPath(submissionRoot, "/doi_batch_diagnostic/@status");
                if(doi_batch_status != null && doi_batch_status.getValue().equalsIgnoreCase(DBD_STATUS_UNKNOWN)) {
                    log.warn("Crossref Submission for filename='{}' no exists at crossref submission queue.", depositFilename);
                    throw new DOIIdentifierException("Unable to obtain Crossref Submission result ('/submissionDownload'). "
                            + "There is no exists any submission for filename=" + depositFilename, DOIIdentifierException.BAD_REQUEST);
                } else if(doi_batch_status != null && doi_batch_status.getValue().equalsIgnoreCase(DBD_STATUS_COMPLETED)) {
                    // Checking if DOI was processed successfully...
                    // More info at https://support.crossref.org/hc/en-us/articles/214337306-Interpreting-Submission-Logs#log1
                    Element doi_batch_record_count = getElementFromPath(submissionRoot, "/doi_batch_diagnostic/batch_data/record_count");
                    Element doi_batch_success_count = getElementFromPath(submissionRoot, "/doi_batch_diagnostic/batch_data/success_count");
                    Element doi_batch_failure_count = getElementFromPath(submissionRoot, "/doi_batch_diagnostic/batch_data/failure_count");
                    Element doi_batch_warning_count = getElementFromPath(submissionRoot, "/doi_batch_diagnostic/batch_data/warning_count");
                    if(doi_batch_record_count == null || doi_batch_success_count == null 
                            || doi_batch_failure_count == null || doi_batch_warning_count == null) {
                        throw new JDOMException("batch_data element at doi_batch_diagnostic has no the data structure required! Response was \n" + submissionResultResponse);
                    } else {
                        int record_count = Integer.valueOf(doi_batch_record_count.getTextTrim());
                        int success_count = Integer.valueOf(doi_batch_success_count.getTextTrim());
                        int failure_count = Integer.valueOf(doi_batch_failure_count.getTextTrim());
                        int warning_count = Integer.valueOf(doi_batch_warning_count.getTextTrim());
                        //TODO completar y poner mas información todos los casos de respuesta...
                        if( success_count < record_count) {
                            throw new DOIIdentifierException("Crossref Submission Log results with deposit errors (success_count="
                                    + String.valueOf(success_count) + " < record_count=" + String.valueOf(record_count) + ").\n"
                                            + "Response was " + submissionResultResponse);
                        }
                        if( record_count == 1 && failure_count == 1) {
                            throw new DOIIdentifierException("Crossref Submission Log with XML validation error.");
                        }
                        if(warning_count > 0 && failure_count == 0 
                                && (warning_count + success_count) == record_count ) {
                            log.warn("Crossref Submission Log with warnings. The submitted DOIs where sucessfully procesed but some are in conflict. \n "
                                    + "Response was " + submissionResultResponse);
                            return;
                        }
                        if( failure_count == 0 && success_count == record_count) {
                            //Registration submission was successful!
                            return;
                        }
                    }
                }
            }
        } catch (JDOMException e) {
            throw new DOIIdentifierException("Got a JDOMException while parsing "
                    + "a response from the Crossref Submission results ('/submissionDownload') endpoint,"
                    + "filename=" + depositFilename, e,
                    DOIIdentifierException.BAD_ANSWER);
        }
    }
    

    /**
     * Get the status of a Deposit through "/deposit" endpoint. Further information at https://support.crossref.org/hc/en-us/articles/217515926.
     * @param doi
     * @return
     * @throws DOIIdentifierException 
     */
    protected DataCiteResponse pollResultsForSentMetadata(String doi) throws DOIIdentifierException {
        List<NameValuePair> submissionParams = new ArrayList<NameValuePair>();
        submissionParams.add(new BasicNameValuePair(SBMDW_TYPE_PARAM, SBMDW_TYPE_RESULT));
        submissionParams.add(new BasicNameValuePair(SBMDW_FNAME, this.getDepositFileName(doi)));
        submissionParams.add(new BasicNameValuePair(SBMDW_USERNAME, this.getUsername()));
        submissionParams.add(new BasicNameValuePair(SBMDW_PASSWORD, this.getPassword()));
        DataCiteResponse submissionResp = this.sendGetRequest(doi, SUBMISSION_PATH, submissionParams);
        switch (submissionResp.getStatusCode()) {
            case(200):
                //all OK, continue...
                break;
            default:
                log.warn("While polling Crossref submission result for FILENAME {}, we got a http status code "
                        + "{} and the message \"{}\".", new String[]
                        {getDepositFileName(doi), Integer.toString(submissionResp.getStatusCode()), submissionResp.getContent()});
                throw new DOIIdentifierException("The query to Crossref Subsmission endpoint (path='" + SCHEME + HOST + SUBMISSION_PATH +"') was not successful "
                        + "(status_code = " + submissionResp.getStatusCode() + "). Please have a look into DSpace logs.",
                        DOIIdentifierException.BAD_ANSWER);
        }
        return submissionResp;
    }

    @Override
    public void updateMetadata(Context context, DSpaceObject dso, String doi) 
            throws DOIIdentifierException
    { 
        // check if doi is registered
        if (!this.isDOIRegistered(context, doi))
        {
            log.warn("Trying to update metadata for DOI {}, that is not registered!", doi);
            throw new DOIIdentifierException("Trying to update metadata for DOI=" + doi + 
                    ", that is not registered at Crossref!", DOIIdentifierException.DOI_DOES_NOT_EXIST);
        }
        initProcessingDate();

        Element root = prepareDSOForDisseminate(dso, doi);
        
        checkCrossrefErrors(root, dso);

        String metadataDOI = extractDOI(root);
        if (null == metadataDOI)
        {
            //Add DOI for update, only if no other DOI exists. It the last happens, there must be some errors at item metadata.
            root = addDOI(doi, root);
        }
        else if (!metadataDOI.equals(doi.substring(DOI.SCHEME.length())))
        {
            log.warn("Crossref UPDATE cancelled for DSO with handle="+ dso.getHandle() +". Cannot update a record with 2 DOIs! Please check your metadata. "
                    + "(Crossref_DOI="+ doi.substring(DOI.SCHEME.length()) +", other_DOI="+ metadataDOI +").");
            throw new DOIIdentifierException("Cancelling UPDATE process for Crossref record. There exists another DOI for this object! "
                    + "(DOI_at_Crossref=" + doi.substring(DOI.SCHEME.length()) +", another_DOI=" +metadataDOI + ")");
        }
        
        // send metadata FILE as a POST to "/deposit"
        DataCiteResponse resp = this.sendMetadataPostRequest(doi, root);
        
        switch(resp.statusCode)
        {
            // 200 -> Submission UPDATE file at Crossref API was successful.
            case (200) :
            {
                log.info("The submission UPDATE file for record with DOI={} at Crossref was succesfully send!", doi);
                break;
            }
            case (503) :
            {
                //Further info: https://support.crossref.org/hc/en-us/articles/214960123
                log.info("The submission UPDATE file for DOI={} cannot be processed because the "
                        + "submission queue at Crossref is full. Retry deposit sending later...", doi);
                throw new DOIIdentifierException("Unable to submit UPDATE file at Crossref for DOI= " + doi 
                        + ". The submission queue is full. Please retry later.",
                        DOIIdentifierException.INTERNAL_ERROR);
            }
            default :
            {
                log.warn("While UPDATE process of DOI {}, we got a http status code "
                        + "{} and the message \"{}\".", new String[]
                        {doi, Integer.toString(resp.statusCode), resp.getContent()});
                throw new DOIIdentifierException("Unable to parse an answer from "
                        + "Crossref API. Please have a look into DSpace logs.",
                        DOIIdentifierException.BAD_ANSWER);
            }
        }
    }
    
    //TODO adaptar a Crossref/fijarse si se sigue usando....
    protected DataCiteResponse sendMetadataDeleteRequest(String doi)
            throws DOIIdentifierException
    {
        // delete mds/metadata/<doi>
        URIBuilder uribuilder = new URIBuilder();
        //FIXME corregir RUTAS...
        uribuilder.setScheme(SCHEME).setHost(HOST).setPath(SUBMISSION_PATH
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
                    SUBMISSION_PATH + "/" + doi.substring(DOI.SCHEME.length()));
            throw new RuntimeException("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!", e);
        }
        return sendHttpRequest(httpdelete, doi);
    }
    
    /**
     * Check if specified DOI is already registered at Crossref.
     * @param doi   the DOI to check if registered.
     * @return
     * @throws DOIIdentifierException
     */
    protected DataCiteResponse sendDOIGetRequest(String doi)
            throws DOIIdentifierException
    {
        List<NameValuePair> doiToQueryParams = new ArrayList<NameValuePair>();
        doiToQueryParams.add(new BasicNameValuePair(QUERY_PID, this.getUsername() + ":" + this.getPassword()));
        doiToQueryParams.add(new BasicNameValuePair(QUERY_DOI_FIELD, doi.substring(DOI.SCHEME.length())));
        doiToQueryParams.add(new BasicNameValuePair(QUERY_FORMAT_PARAM, CROSSREF_UNIXREF_FORMAT));
        return this.sendGetRequest(doi, QUERY_PATH, doiToQueryParams);
    }
  
  //TODO adaptar a Crossref/fijarse si se sigue usando....
    protected DataCiteResponse sendMetadataGetRequest(String doi)
            throws DOIIdentifierException
    {
        return sendGetRequest(doi, SUBMISSION_PATH, null);
    }
    
    protected DataCiteResponse sendGetRequest(String doi, String path, List<NameValuePair> params)
            throws DOIIdentifierException
    {
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme(SCHEME).setHost(HOST).setPath(path);
        if(params != null && !params.isEmpty())
        {
            uribuilder.addParameters(params);
        }
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
                    path + "/" + doi.substring(DOI.SCHEME.length()));
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
        URIBuilder uribuilder = new URIBuilder();
        uribuilder.setScheme(SCHEME).setHost(HOST).setPath(DEPOSIT_PATH);
        
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
                    DEPOSIT_PATH + "/" + doi.substring(DOI.SCHEME.length()));
            throw new RuntimeException("The URL we constructed to check a DOI "
                    + "produced a URISyntaxException. Please check the configuration parameters!", e);
        }
        
        // assemble request content:
        HttpEntity reqEntity = null;
        File tmp = null;
        try
        {
            String filename = this.getDepositFileName(doi);
            tmp = File.createTempFile(filename.substring(0, filename.indexOf(XML_EXTENSION)), XML_EXTENSION);
            xout.output(metadataRoot, new FileWriter(tmp));
            MultipartEntityBuilder builder = 
                    MultipartEntityBuilder.create().setMode(HttpMultipartMode.STRICT)
                        .addTextBody(OPERATION_PARAM, DOMD_UPLOAD)
                        .addTextBody(DOMD_LOGIN, this.getUsername())
                        .addTextBody(DOMD_PASSWD, this.getPassword())
                        .addBinaryBody(DOMD_FNAME, tmp, ContentType.TEXT_XML, this.getDepositFileName(doi));
            HttpEntity entity = builder.build();
            httppost.setEntity(entity);
            
            return sendHttpRequest(httppost, doi);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create temporary XML file for Crossref metadata when posting to HTTPS API.", e);
        }
        finally
        {
            // release ressources
            if (tmp != null) 
                tmp.delete();
            try
            {
                EntityUtils.consume(reqEntity);
            }
            catch (IOException ioe)
            {
               log.error("Caught an IOException while releasing an HTTPEntity:"
                       + ioe.getMessage());
            }
        }
    }
    
    /**
     * 
     * @param req
     * @param doi
     * @return
     * @throws DOIIdentifierException 
     */
    protected DataCiteResponse sendHttpRequest(HttpUriRequest req, String doi)
            throws DOIIdentifierException
    {
        HttpClient httpclient = HttpClientBuilder.create().build();
        //There is no credentials required, these goes in POST payload. No HTTP basic authentication is used.
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

            // We can handle some status codes here, others have to be handled above
            switch (statusCode)
            {
                // we get a 401 if we forgot to send credentials or if the username
                // and password did not match.
                case (401) :
                {
                    log.info("We were anauthorized to post against the DOI registry agency.");
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
                    log.error("Unsuccesful submission for (doi={}, user=" + this.getUsername() +") : {}", doi, content);
                    throw new DOIIdentifierException("We can register DOIs that belong to us only.",
                            DOIIdentifierException.BAD_REQUEST);
                }


                // 500 is documented and signals an internal server error
                case (500) :
                {
                    log.warn("Caught an http status code 500 while managing DOI "
                            +"{}. Message was: " + content);
                    throw new DOIIdentifierException("Crossref HTTPS API has an internal error. "
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
                // Release any ressources used by HTTP-Request.
                if (null != entity)
                {
                    EntityUtils.consume(entity);
                }
            }
            catch (IOException e)
            {
                log.warn("Can't release HTTP-Entity: " + e.getMessage(), e);
            }
        }
    }

  //TODO adaptar a formato XML de Crossref. Fijarse si se sigue usando...
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
            //TODO hacer un log en este punto
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
    
    /**
     * Search for a <doi-data>/<doi> element inside the disseminated XML Crossref file.
     * @param root
     * @return null o the value of the DOI if this exists.
     */
    protected String extractDOI(Element root){
        String xpath_expression = "//crossref:doi_data/crossref:doi";
        Element doiElement = getElementFromPath(root, xpath_expression, "crossref", root.getNamespaceURI());
        return (null == doiElement || doiElement.getTextTrim().isEmpty()) ? null : doiElement.getTextTrim();
    }

    /**
     * Add a <doi> text value in Crossref XML file.
     * @param doi   the DOI to add to file
     * @param root  the root node, <doi_batch> node.
     * @param dso   the dso vinculated to the specified DOI.
     * @return the root element
     * @throws DOIIdentifierException 
     */
    protected Element addDOI(String doi, Element root) throws DOIIdentifierException {
        if (null != extractDOI(root))
        {
            return root;
        }
        //Get first child of <body> (considering that only one child exists at this moment), i.e., journal, dissertation, book, etc.
        String xpath_expression = "//crossref:doi_data/crossref:doi";
        Element doiTag = getElementFromPath(root, xpath_expression, "crossref", root.getNamespaceURI());
        if(doiTag == null) {
            throw new DOIIdentifierException("Cannot add DOI for disseminated XML Crossref. Please check your XSL crosswalk.");
        }
        doiTag.addContent(doi.substring(DOI.SCHEME.length()));
        return root;
    }

    /**
     * Please use the {@code getElementsFromPath} instead this, unless necessary.
     * Get all nodes objects within XML data tied to the specified XPATH expression.
     * @return a list of all nodes objects tied to XPATH or @null if the XPATH does not match.
     */
    private List<?> getNodesFromPath(Element root, String xpath_expression, String ns_prefix, String ns_uri) {
        List<?> targetNodes = null;
        XPath xpathDOI;
        Document doc;
        try {
            xpathDOI = XPath.newInstance(xpath_expression);
            xpathDOI.addNamespace(ns_prefix, ns_uri);
            if(root.getDocument() != null) {
                doc = root.getDocument();
            } else {
                doc = new Document(root);
            }
            targetNodes = xpathDOI.selectNodes(doc);
        } catch (JDOMException e) {
            log.error("Incorrect XPATH expression!! Please check it. (XPATH =" + xpath_expression  + ")",  e.getMessage());
            //continue the normal code and return @null if this excepcion is raised...
        }
        return targetNodes;
    }

    /**
     * Get an Element node object within XML data tied to the specified XPATH expression.
     */
    private List<Element> getElementsFromPath(Element root, String xpath_expression, String ns_prefix, String ns_uri) {
        List<?> nodes = getNodesFromPath(root, xpath_expression, ns_prefix, ns_uri);
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        List<Element> elements = new ArrayList<Element>();
        for (Object node : nodes) {
            elements.add((Element) node);
        }
        return elements;
    }

    /**
     * Get an Element node object within XML data tied to the specified XPATH expression.
     */
    private Element getElementFromPath(Element root, String xpath_expression, String ns_prefix, String ns_uri) {
        Element element = null;
        List<?> nodes = getNodesFromPath(root, xpath_expression, ns_prefix, ns_uri);
        if(nodes != null && !nodes.isEmpty()) {
            element = (Element)(nodes.get(0));
        }
        return element;
    }

    /**
     * Get an Attribute node object within XML data tied to the specified XPATH expression.
     */
    private Attribute getAttributeFromPath(Element root, String xpath_expression, String ns_prefix, String ns_uri) {
        Attribute attribute = null;
        List<?> nodes = getNodesFromPath(root, xpath_expression, ns_prefix, ns_uri);
        if(nodes != null && !nodes.isEmpty()) {
            attribute = (Attribute)(nodes.get(0));
        }
        return attribute;
    }
    
    /**
     * Get an Element within XML data tied to the specified XPATH expression. The namespace URI and prefix are considered empty.
     * @return the Element node object tied to XPATH or @null if the XPATH does not match.
     */
    private Element getElementFromPath(Element root, String xpath_expression) {
        return getElementFromPath(root, xpath_expression, "", "");
    }
    /**
     * Get an Attribute within XML data tied to the specified XPATH expression. The namespace URI and prefix are considered empty.
     * @return the Attribute node object tied to XPATH or @null if the XPATH does not match.
     */
    private Attribute getAttributeFromPath(Element root, String xpath_expression) {
        return getAttributeFromPath(root, xpath_expression, "", "");
    }
    

    /**
     * Convert a target String in XML format to an JDom Documento object.
     * @param XMLString     the String to convert
     * @return a Document based on XMLString parameter content. Return null in case of empty or null for XMLString.
     * @throws JDOMException     when cannot parse XMLString
     */
    private Document parseXMLContent(String XMLString) throws JDOMException {
        if (XMLString == null){
            return null;
        }
        try {
            SAXBuilder builder = new SAXBuilder();
            InputStream stream = new ByteArrayInputStream(XMLString.getBytes("UTF-8"));
            return builder.build(stream);
        } catch (IOException e) {
            throw new RuntimeException("Got an IOException while reading from a string?!", e);
        }
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
