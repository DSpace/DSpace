/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;
import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.orcid.jaxb.activities.Educations;
import org.dspace.authority.orcid.jaxb.activities.Employments;
import org.dspace.authority.orcid.jaxb.activities.Fundings;
import org.dspace.authority.orcid.jaxb.activities.WorkGroup;
import org.dspace.authority.orcid.jaxb.activities.Works;
import org.dspace.authority.orcid.jaxb.address.Address;
import org.dspace.authority.orcid.jaxb.address.Addresses;
import org.dspace.authority.orcid.jaxb.bulk.Bulk;
import org.dspace.authority.orcid.jaxb.common.SourceType;
import org.dspace.authority.orcid.jaxb.education.Education;
import org.dspace.authority.orcid.jaxb.education.EducationSummary;
import org.dspace.authority.orcid.jaxb.email.Emails;
import org.dspace.authority.orcid.jaxb.employment.Employment;
import org.dspace.authority.orcid.jaxb.employment.EmploymentSummary;
import org.dspace.authority.orcid.jaxb.funding.Funding;
import org.dspace.authority.orcid.jaxb.keyword.Keyword;
import org.dspace.authority.orcid.jaxb.keyword.Keywords;
import org.dspace.authority.orcid.jaxb.othername.OtherName;
import org.dspace.authority.orcid.jaxb.othername.OtherNames;
import org.dspace.authority.orcid.jaxb.person.Person;
import org.dspace.authority.orcid.jaxb.person.externalidentifier.ExternalIdentifier;
import org.dspace.authority.orcid.jaxb.person.externalidentifier.ExternalIdentifiers;
import org.dspace.authority.orcid.jaxb.personaldetails.PersonalDetails;
import org.dspace.authority.orcid.jaxb.researcherurl.ResearcherUrl;
import org.dspace.authority.orcid.jaxb.researcherurl.ResearcherUrls;
import org.dspace.authority.orcid.jaxb.work.Work;
import org.dspace.authority.orcid.jaxb.work.WorkSummary;
import org.dspace.authority.rest.RestSource;
import org.dspace.content.DCPersonName;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.utils.DSpace;
import org.glassfish.jersey.message.internal.MessageBodyProviderNotFoundException;
import org.orcid.ns.record.Record;
import org.orcid.ns.search.Result;
import org.orcid.ns.search.Search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Based on works provided by TomDemeranville
 * 
 * https://github.com/TomDemeranville/orcid-java-client
 * 
 * @author l.pascarelli
 *
 */
public class OrcidService extends RestSource
{

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(OrcidService.class);

    public static final Integer CONSTANT_PART_OF_RESEARCHER_TYPE = 9;
    
    public static final String CONSTANT_OTHERNAME_UUID = "OTHERNAME";
    public static final String CONSTANT_RESEARCHERURL_UUID = "RESEARCHERURL";
    public static final String CONSTANT_EXTERNALIDENTIFIER_UUID = "EXTERNALIDENTIFIER";
    public static final String CONSTANT_ADDRESS_UUID = "ADDRESS";
    public static final String CONSTANT_KEYWORD_UUID = "KEYWORD";
    public static final String CONSTANT_EMPLOYMENT_UUID = "EMPLOYMENT";
    public static final String CONSTANT_EDUCATION_UUID = "EDUCATION";
    
    public static final String RECORD_ENDPOINT = "/record";

    public static final String ACTIVITIES_ENDPOINT = "/activities";

    public static final String ADDRESS_ENDPOINT = "/address";

    public static final String BIOGRAPHY_ENDPOINT = "/biography";

    public static final String EDUCATIONS_ENDPOINT = "/educations";

    public static final String EDUCATION_ENDPOINT = "/education";

    public static final String EDUCATION_SUMMARY_ENDPOINT = "/education/summary";

    public static final String EMAIL_ENDPOINT = "/email";

    public static final String EMPLOYMENTS_ENDPOINT = "/employments";

    public static final String EMPLOYMENT_ENDPOINT = "/employment";

    public static final String EMPLOYMENT_SUMMARY_ENDPOINT = "/employment/summary";

    public static final String EXTERNAL_IDENTIFIERS_ENDPOINT = "/external-identifiers";

    public static final String FUNDING_ENDPOINT = "/funding";

    public static final String FUNDING_SUMMARY_ENDPOINT = "/funding/summary";

    public static final String FUNDINGS_ENDPOINT = "/fundings";

    public static final String KEYWORDS_ENDPOINT = "/keywords";

    public static final String OTHER_NAMES_ENDPOINT = "/other-names";

    public static final String PEERREVIEW_ENDPOINT = "/peer-review";

    public static final String PEERREVIEW_SUMMARY_ENDPOINT = "/peer-review/summary";

    public static final String PERSON_ENDPOINT = "/person";

    public static final String PERSONAL_DETAILS_ENDPOINT = "/personal-details";

    public static final String RESEARCHER_URLS_ENDPOINT = "/researcher-urls";

    public static final String WORK_ENDPOINT = "/work";

    public static final String WORK_SUMMARY_ENDPOINT = "/work/summary";

    public static final String WORKS_ENDPOINT = "/works";

    private static final String SEARCH_ENDPOINT = "/search";

    private static final String READ_PUBLIC_SCOPE = "/read-public";

    private static final String READ_LIMITED_SCOPE = "/read-limited";

    private static final String PROFILE_CREATE_SCOPE = "/person/update";

    private static final String ACTIVITIES_CREATE_SCOPE = "/activities/update";

    public static final String SYSTEM_ORCID_TOKEN_PROFILE_CREATE_SCOPE = "system-orcid-token-person-update";

    public static final String SYSTEM_ORCID_TOKEN_ACTIVITIES_CREATE_SCOPE = "system-orcid-token-activities-update";

    public static final String ORCID_MODE_APPEND = "POST";

    public static final String ORCID_MODE_UPDATE = "PUT";

    public static final String ORCID_MODE_DELETE = "DELETE";

    private static OrcidService orcid;

    private static String sourceClientName;

    private final MediaType APPLICATION_ORCID_XML = new MediaType("application",
            "orcid+xml");

    private String clientID;

    private String clientSecretKey;

    private String tokenURL;

    private String baseURL;

    public static OrcidService getOrcid()
    {
        if (orcid == null)
        {
            orcid = new DSpace().getServiceManager()
                    .getServiceByName("OrcidSource", OrcidService.class);
            sourceClientName = ConfigurationManager.getProperty(
                    "authentication-oauth", "application-client-name");
        }
        return orcid;
    }

    private OrcidService(String url, String clientID, String clientSecretKey,
            String tokenURL) throws JAXBException
    {
        super(url);
        this.clientID = clientID;
        this.clientSecretKey = clientSecretKey;
        this.tokenURL = tokenURL;
    }

    /**
     * Returns the fields and "works" research activities that are set as
     * "Public" in the ORCID Record for the scholar represented by the specified
     * orcid_id. When used with the Member API, limited-access data is also
     * returned if permissions were grant by the user.
     * 
     * Public API
     * 
     * @param id
     * @return
     */
    public Record getRecord(String id)
    {

        OrcidAccessToken token = null;
        try
        {
            token = getMemberSearchToken();
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e.getMessage(), e);
        }
        String access_token = null;
        if (token != null)
        {
            access_token = token.getAccess_token();
        }
        return getRecord(id, access_token);
    }

    /**
     * Returns the fields and "works" research activities that are set as
     * "Public" in the ORCID Record for the scholar represented by the specified
     * orcid_id. When used with an access token and the Member API,
     * limited-access data is also returned.
     * 
     * Member API
     * 
     * @param id
     * @param token
     * @return
     */
    public Record getRecord(String id, String token)
    {

        String endpoint = id + RECORD_ENDPOINT;
        return get(endpoint, token, null).readEntity(Record.class);

    }

    public Works getWorks(String id, String token)
    {

        String endpoint = id + WORKS_ENDPOINT;
        Works message = null;
        try
        {
            message = get(endpoint, token, null).readEntity(Works.class);
        }
        catch (Exception e)
        {
            log.error(e);
        }
        return message;
    }

    public WorkSummary getWorkSummary(String id, String token, String putCode)
    {

        String endpoint = id + WORK_SUMMARY_ENDPOINT;
        WorkSummary message = null;
        try
        {
            message = get(endpoint, token, putCode)
                    .readEntity(WorkSummary.class);
        }
        catch (Exception e)
        {
            log.error(e);
        }
        return message;
    }

    public Work getWork(String id, String token, String putCode)
    {

        String endpoint = id + WORK_ENDPOINT;
        Work message = null;
        try
        {
            message = get(endpoint, token, putCode).readEntity(Work.class);
        }
        catch (Exception e)
        {
            log.error(e);
        }
        return message;
    }

    public Fundings getFundings(String id, String token)
    {

        String endpoint = id + FUNDINGS_ENDPOINT;
        Fundings message = null;
        try
        {
            message = get(endpoint, token, null).readEntity(Fundings.class);
        }
        catch (Exception e)
        {
            log.error(e);
        }
        return message;
    }

    public Emails getEmails(String id, String token)
    {

        String endpoint = id + EMAIL_ENDPOINT;
        Emails message = null;
        try
        {
            message = get(endpoint, token, null).readEntity(Emails.class);
        }
        catch (Exception e)
        {
            log.error(e);
        }
        return message;
    }

    public PersonalDetails getPersonalDetails(String id, String token)
    {

        String endpoint = id + PERSONAL_DETAILS_ENDPOINT;
        PersonalDetails message = null;
        try
        {
            message = get(endpoint, token, null)
                    .readEntity(PersonalDetails.class);
        }
        catch (Exception e)
        {
            log.error(e);
        }
        return message;
    }

    public Person getPerson(String id, String token)
    {

        String endpoint = id + PERSON_ENDPOINT;
        Person message = null;
        try
        {
            message = get(endpoint, token, null).readEntity(Person.class);
        }
        catch (Exception e)
        {
            log.error(e);
        }
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * Internally call getProfile - will be use with the Public API
     * 
     * @see
     * org.dspace.authority.rest.RestSource#queryAuthorityID(java.lang.String)
     */
    @Override
    public AuthorityValue queryAuthorityID(String id)
    {
        Record bio = getRecord(id);
        return OrcidAuthorityValue.create(bio);
    }

    /**
     * Used to retrieve Orcid Profile
     * 
     * @param text
     * @param start
     * @param max
     * @return
     * @throws IOException
     */
    public List<AuthorityValue> queryOrcidBioByFamilyNameAndGivenName(
            String text, int start, int max) throws IOException
    {
        DCPersonName tmpPersonName = new DCPersonName(text);

        String query = "";
        if (StringUtils.isNotBlank(tmpPersonName.getLastName()))
        {
            query += "family-name:(" + tmpPersonName.getLastName().trim()
                    + (StringUtils.isNotBlank(tmpPersonName.getFirstNames())
                            ? "" : "*")
                    + ")";
        }

        if (StringUtils.isNotBlank(tmpPersonName.getFirstNames()))
        {
            query += (query.length() > 0 ? " AND given-names:("
                    : "given-names:(") + tmpPersonName.getFirstNames().trim()
                    + "*)";
        }

        query += " OR other-names:(" + text + ")";

        List<Result> results = search(query, start, max);

        return getAuthorityValuesFromOrcidResults(results);
    }

    /**
     * 
     * Perform a search against the Public API or Member API
     * 
     * @param query
     * @param page
     * @param pagesize
     * @return
     * @throws IOException
     */
    public List<Result> search(String query, int page, int pagesize)
            throws IOException
    {
        if (query == null || query.isEmpty())
        {
            throw new IllegalArgumentException();
        }

        WebTarget target = restConnector.getClientRest(SEARCH_ENDPOINT);
        target = target.queryParam("q", query);
        if (pagesize >= 0)
        {
            target = target.queryParam("rows", Integer.toString(pagesize));
        }
        if (page >= 0)
        {
            target = target.queryParam("start", Integer.toString(page));
        }

        Builder builder = target.request().accept(APPLICATION_ORCID_XML);
        List<Result> reader = null;
        try
        {
            reader = builder.get().readEntity(Search.class).getResult();
        }
        catch (ForbiddenException | MessageBodyProviderNotFoundException e1)
        {
            builder = builder.header(HttpHeaders.AUTHORIZATION,
                    "Bearer " + getMemberSearchToken().getAccess_token());
            reader = builder.get().readEntity(Search.class).getResult();
        }
        catch (Exception e2)
        {
            log.info("Problem unmarshalling return value " + e2);
            throw new IOException(e2);
        }
        return reader;
    }

    /**
     * 
     * Allows an ORCID client to obtain an OAuth Access Token to make Public API
     * calls using the Member API (and its service level agreement).
     * 
     * @return
     * @throws IOException
     */
    public OrcidAccessToken getMemberSearchToken() throws IOException
    {
        String code = READ_PUBLIC_SCOPE;
        return getAccessToken(code, "scope", "client_credentials");
    }

    /**
     * 
     * Allows an ORCID client to obtain an OAuth Access Token to create new
     * ORCID iDs and Records
     * 
     * @return
     * @throws IOException
     */
    public OrcidAccessToken getMemberProfileCreateToken() throws IOException
    {
        String code = PROFILE_CREATE_SCOPE;
        return getAccessToken(code, "scope", "client_credentials");
    }

    /**
     * Allows an ORCID member client to exchange an OAuth Authorization Code for
     * an OAuth Access Token.
     * 
     * @return
     * @throws IOException
     */
    public OrcidAccessToken getAuthorizationAccessToken(String code)
            throws IOException
    {
        return getAccessToken(code, "code", "authorization_code");
    }

    /**
     * Allows an ORCID client to exchange an OAuth Authorization Code for an
     * OAuth Access Token for a specific access scope.
     * 
     * @param code
     * @return
     * @throws IOException
     * @throws JsonProcessingException
     */
    private OrcidAccessToken getAccessToken(String code, String codeOrScope,
            String grantType) throws IOException, JsonProcessingException
    {
        if (StringUtils.isBlank(clientID)
                || StringUtils.isBlank(clientSecretKey))
        {
            return null;
        }

        Client client = ClientBuilder
                .newClient(restConnector.getClientConfig());
        WebTarget target = client.target(tokenURL);
        Form form = new Form();
        form.param("client_id", clientID);
        form.param("client_secret", clientSecretKey);
        form.param("grant_type", grantType);
        form.param(codeOrScope, code);
        Builder builder = target.request().accept(MediaType.APPLICATION_JSON);
        String response = builder.post(Entity.form(form), String.class);
        return new ObjectMapper().reader(OrcidAccessToken.class)
                .readValue(response);
    }

    /*
     * (non-Javadoc)
     * 
     * Internal call "search" method, this will be use by Public API and Member
     * API
     * 
     * @see
     * org.dspace.authority.rest.RestSource#queryAuthorities(java.lang.String,
     * java.lang.String, int, int)
     */
    @Override
    public List<AuthorityValue> queryAuthorities(String field, String text,
            int start, int max) throws IOException
    {
        List<Result> results = search(field + ":" + URLEncoder.encode(text),
                start, max);

        return getAuthorityValuesFromOrcidResults(results);
    }

    /**
     * From JAXB OrcidSearchResults to AuthorityValue
     * 
     * @param results
     * @return
     */
    private List<AuthorityValue> getAuthorityValuesFromOrcidResults(
            List<Result> results)
    {
        List<AuthorityValue> authorities = new ArrayList<AuthorityValue>();
        for (Result result : results)
        {
            authorities.add(OrcidAuthorityValue.create(
                    get(result.getOrcidIdentifier().getUriPath(), null, null)
                            .readEntity(Record.class)));
        }
        return authorities;
    }

    /*
     * (non-Javadoc)
     * 
     * Internal call "search" method, this will be use by Public API and Member
     * API
     * 
     * @see
     * org.dspace.authority.rest.RestSource#queryAuthorities(java.lang.String,
     * int)
     */
    @Override
    public List<AuthorityValue> queryAuthorities(String text, int max)
            throws IOException
    {
        List<Result> results = search(URLEncoder.encode(text), 0, max);

        return getAuthorityValuesFromOrcidResults(results);
    }

    public String getBaseURL()
    {
        return baseURL;
    }

    public void setBaseURL(String baseURL)
    {
        this.baseURL = baseURL;
    }

    public static String getSourceClientName()
    {
        if (sourceClientName == null)
        {
            sourceClientName = ConfigurationManager.getProperty(
                    "authentication-oauth", "application-client-name");
        }
        return sourceClientName;
    }

    // Method to read and update sections on an individual basis

    // WORKS

    /**
     * Adds work
     * 
     * Member API, require '/activities/update' scope.
     * 
     * 
     * @param id
     * @param token
     * @param work
     * @throws IOException
     * @throws JAXBException
     */
    public String appendWork(String id, String token, Work work)
            throws IOException, JAXBException
    {
        String endpoint = id + WORK_ENDPOINT;
        Response response = null;
        try
        {
            response = post(endpoint, token,
                    Entity.entity(work, MediaType.APPLICATION_XML_TYPE));
            return retrievePutCode(response);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    /* Bulks */
    public Bulk appendWorks(String id, String token, Bulk bulk)
    {
        String endpoint = id + WORKS_ENDPOINT;
        Response response = null;
        try
        {
            response = post(endpoint, token,
                    Entity.entity(bulk, MediaType.APPLICATION_XML_TYPE));
            return response.readEntity(Bulk.class);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    /**
     * Update work
     * 
     * Member API, require '/activities/update' scope.
     * 
     * 
     * @param id
     * @param token
     * @param work
     * @throws IOException
     * @throws JAXBException
     */
    public StatusType putWork(String id, String token, String putCode,
            Work work) throws IOException, JAXBException
    {
        String endpoint = id + WORK_ENDPOINT;
        Response response = null;
        try
        {
            response = put(endpoint, token, putCode,
                    Entity.entity(work, MediaType.APPLICATION_XML_TYPE));
            return response.getStatusInfo();
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public void deleteWork(String id, String token, String putCode)
            throws IOException, JAXBException
    {
        String endpoint = id + WORK_ENDPOINT;
        Response response = null;
        try
        {
            response = delete(endpoint, token, putCode);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }
    
    // Fundings
    /**
     * Add funding
     * 
     * Member API, require '/activities/update' scope.
     * 
     * 
     * @param id
     * @param token
     * @param work
     * @throws IOException
     * @throws JAXBException
     */
    public String appendFunding(String id, String token, Funding funding)
            throws IOException, JAXBException
    {
        String endpoint = id + FUNDING_ENDPOINT;
        Response response = null;
        try
        {
            response = post(endpoint, token,
                    Entity.entity(funding, MediaType.APPLICATION_XML_TYPE));
            return retrievePutCode(response);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }
    
    public void deleteFunding(String id, String token, String putCode)
            throws IOException, JAXBException
    {
        String endpoint = id + FUNDING_ENDPOINT;
        Response response = null;
        try
        {
            response = delete(endpoint, token, putCode);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    /**
     * Update funding
     * 
     * Member API, require '/activities/update' scope.
     * 
     * 
     * @param id
     * @param token
     * @param work
     * @throws IOException
     * @throws JAXBException
     */
    public StatusType putFunding(String id, String token, String putCode,
            Funding funding) throws IOException, JAXBException
    {
        String endpoint = id + FUNDING_ENDPOINT;
        Response response = null;
        try
        {
            response = put(endpoint, token, putCode,
                    Entity.entity(funding, MediaType.APPLICATION_XML_TYPE));
            return response.getStatusInfo();
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public Employments getEmployments(String id, final String token)
    {
        String endpoint = id + EMPLOYMENTS_ENDPOINT;
        Response response = null;
        try
        {
            response = get(endpoint, token, null);

            return response.readEntity(Employments.class);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public Employment getEmployment(String id, final String token, final String putCode)
    {
        String endpoint = id + EMPLOYMENT_ENDPOINT;
        Response response = null;
        try
        {
            response = get(endpoint, token, putCode);

            return response.readEntity(Employment.class);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public EmploymentSummary getEmploymentSummary(String id, final String token,
            final String putCode)
    {
        String endpoint = id + EMPLOYMENT_SUMMARY_ENDPOINT;
        Response response = null;
        try
        {
            response = get(endpoint, token, putCode);

            return response.readEntity(EmploymentSummary.class);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    /**
     * 
     * Employment Update
     * 
     * Member API
     * 
     * @param id
     * @param token
     * @param profile
     * @return
     * @throws IOException
     * @throws JAXBException
     */
    public StatusType putEmployment(String id, String token, String putCode,
            Employment employment) throws IOException, JAXBException
    {
        String endpoint = id + EMPLOYMENT_ENDPOINT;
        Response response = null;
        try
        {
            response = put(endpoint, token, putCode,
                    Entity.entity(employment, MediaType.APPLICATION_XML_TYPE));
            return response.getStatusInfo();
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public void deleteEmployment(String id, String token, String putCode)
            throws IOException, JAXBException
    {
        String endpoint = id + EMPLOYMENT_ENDPOINT;
        Response response = null;
        try
        {
            response = delete(endpoint, token, putCode);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public String appendEmployment(String id, String token,
            Employment employment) throws IOException, JAXBException
    {
        String endpoint = id + EMPLOYMENT_ENDPOINT;
        Response response = null;
        try
        {
            response = post(endpoint, token,
                    Entity.entity(employment, MediaType.APPLICATION_XML_TYPE));
            return retrievePutCode(response);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }        
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public Educations getEducations(String id, final String token)
    {
        String endpoint = id + EDUCATIONS_ENDPOINT;
        Response response = null;
        try
        {
            response = get(endpoint, token, null);

            return response.readEntity(Educations.class);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public Education getEducation(String id, final String token, final String putCode)
    {
        String endpoint = id + EDUCATION_ENDPOINT;
        Response response = null;
        try
        {
            response = get(endpoint, token, putCode);

            return response.readEntity(Education.class);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public EducationSummary getEducationSummary(String id, final String token,
            final String putCode)
    {
        String endpoint = id + EDUCATION_SUMMARY_ENDPOINT;
        Response response = null;
        try
        {
            response = get(endpoint, token, putCode);

            return response.readEntity(EducationSummary.class);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public StatusType putEducation(String id, String token, String putCode,
            Education education) throws IOException, JAXBException
    {
        String endpoint = id + EDUCATION_ENDPOINT;
        Response response = null;
        try
        {
            response = put(endpoint, token, putCode,
                    Entity.entity(education, MediaType.APPLICATION_XML_TYPE));
            return response.getStatusInfo();
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public void deleteEducation(String id, String token, String putCode)
            throws IOException, JAXBException
    {
        String endpoint = id + EDUCATION_ENDPOINT;
        Response response = null;
        try
        {
            response = delete(endpoint, token, putCode);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public String appendEducation(String id, String token, Education education)
            throws IOException, JAXBException
    {
        String endpoint = id + EDUCATION_ENDPOINT;
        Response response = null;
        try
        {
            response = post(endpoint, token,
                    Entity.entity(education, MediaType.APPLICATION_XML_TYPE));
            return retrievePutCode(response);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }        
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public OtherNames getOtherNames(String id, final String token)
    {
        String endpoint = id + OTHER_NAMES_ENDPOINT;
        Response response = null;
        try
        {
            response = get(endpoint, token, null);

            return response.readEntity(OtherNames.class);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public OtherName getOtherName(String id, final String token, final String putCode)
    {
        String endpoint = id + OTHER_NAMES_ENDPOINT;
        Response response = null;
        try
        {
            response = get(endpoint, token, putCode);

            return response.readEntity(OtherName.class);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    
    public StatusType putOtherName(String id, String token, String putCode,
            OtherName otherName) throws IOException, JAXBException
    {
        String endpoint = id + OTHER_NAMES_ENDPOINT;
        Response response = null;
        try
        {
            response = put(endpoint, token, putCode,
                    Entity.entity(otherName, MediaType.APPLICATION_XML_TYPE));
            return response.getStatusInfo();
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public void deleteOtherName(String id, String token, String putCode)
            throws IOException, JAXBException
    {
        String endpoint = id + OTHER_NAMES_ENDPOINT;
        Response response = null;
        try
        {
            response = delete(endpoint, token, putCode);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public String appendOtherName(String id, String token, OtherName otherName)
            throws IOException, JAXBException
    {
        String endpoint = id + OTHER_NAMES_ENDPOINT;
        Response response = null;
        try
        {
            response = post(endpoint, token,
                    Entity.entity(otherName, MediaType.APPLICATION_XML_TYPE));
            return retrievePutCode(response);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }        
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }
    
    //EXTIDS
    public ExternalIdentifiers getExternalIdentifiers(String id, final String token)
    {
        String endpoint = id + EXTERNAL_IDENTIFIERS_ENDPOINT;
        Response response = null;
        try
        {
            response = get(endpoint, token, null);

            return response.readEntity(ExternalIdentifiers.class);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public ExternalIdentifier getExternalIdentifier(String id, final String token, final String putCode)
    {
        String endpoint = id + EXTERNAL_IDENTIFIERS_ENDPOINT;
        Response response = null;
        try
        {
            response = get(endpoint, token, putCode);

            return response.readEntity(ExternalIdentifier.class);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    
    public StatusType putExternalIdentifier(String id, String token, String putCode,
            ExternalIdentifier externalIdentifier) throws IOException, JAXBException
    {
        String endpoint = id + EXTERNAL_IDENTIFIERS_ENDPOINT;
        Response response = null;
        try
        {
            response = put(endpoint, token, putCode,
                    Entity.entity(externalIdentifier, MediaType.APPLICATION_XML_TYPE));
            return response.getStatusInfo();
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public void deleteExternalIdentifier(String id, String token, String putCode)
            throws IOException, JAXBException
    {
        String endpoint = id + EXTERNAL_IDENTIFIERS_ENDPOINT;
        Response response = null;
        try
        {
            response = delete(endpoint, token, putCode);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public String appendExternalIdentifier(String id, String token, ExternalIdentifier externalIdentifier)
            throws IOException, JAXBException
    {
        String endpoint = id + EXTERNAL_IDENTIFIERS_ENDPOINT;
        Response response = null;
        try
        {
            response = post(endpoint, token,
                    Entity.entity(externalIdentifier, MediaType.APPLICATION_XML_TYPE));
            return retrievePutCode(response);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }        
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }
    
    //RESEARCHER URL    
    public ResearcherUrls getResearcherUrls(String id, final String token)
    {
        String endpoint = id + RESEARCHER_URLS_ENDPOINT;
        Response response = null;
        try
        {
            response = get(endpoint, token, null);

            return response.readEntity(ResearcherUrls.class);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public ResearcherUrl getResearcherUrl(String id, final String token, final String putCode)
    {
        String endpoint = id + RESEARCHER_URLS_ENDPOINT;
        Response response = null;
        try
        {
            response = get(endpoint, token, putCode);

            return response.readEntity(ResearcherUrl.class);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    
    public StatusType putResearcherUrl(String id, String token, String putCode,
            ResearcherUrl researcherUrl) throws IOException, JAXBException
    {
        String endpoint = id + RESEARCHER_URLS_ENDPOINT;
        Response response = null;
        try
        {
            response = put(endpoint, token, putCode,
                    Entity.entity(researcherUrl, MediaType.APPLICATION_XML_TYPE));
            return response.getStatusInfo();
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public void deleteResearcherUrl(String id, String token, String putCode)
            throws IOException, JAXBException
    {
        String endpoint = id + RESEARCHER_URLS_ENDPOINT;
        Response response = null;
        try
        {
            response = delete(endpoint, token, putCode);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public String appendResearcherUrl(String id, String token, ResearcherUrl researcherUrl)
            throws IOException, JAXBException
    {
        String endpoint = id + RESEARCHER_URLS_ENDPOINT;
        Response response = null;
        try
        {
            response = post(endpoint, token,
                    Entity.entity(researcherUrl, MediaType.APPLICATION_XML_TYPE));
            return retrievePutCode(response);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }        
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }
    
    //ADDRESS
    public Addresses getAddresses(String id, final String token)
    {
        String endpoint = id + ADDRESS_ENDPOINT;
        Response response = null;
        try
        {
            response = get(endpoint, token, null);

            return response.readEntity(Addresses.class);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public Address getAddress(String id, final String token, final String putCode)
    {
        String endpoint = id + ADDRESS_ENDPOINT;
        Response response = null;
        try
        {
            response = get(endpoint, token, putCode);

            return response.readEntity(Address.class);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    
    public StatusType putAddress(String id, String token, String putCode,
            Address address) throws IOException, JAXBException
    {
        String endpoint = id + ADDRESS_ENDPOINT;
        Response response = null;
        try
        {
            response = put(endpoint, token, putCode,
                    Entity.entity(address, MediaType.APPLICATION_XML_TYPE));
            return response.getStatusInfo();
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public void deleteAddress(String id, String token, String putCode)
            throws IOException, JAXBException
    {
        String endpoint = id + ADDRESS_ENDPOINT;
        Response response = null;
        try
        {
            response = delete(endpoint, token, putCode);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public String appendAddress(String id, String token, Address address)
            throws IOException, JAXBException
    {
        String endpoint = id + ADDRESS_ENDPOINT;
        Response response = null;
        try
        {
            response = post(endpoint, token,
                    Entity.entity(address, MediaType.APPLICATION_XML_TYPE));
            return retrievePutCode(response);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    } 

    // KEYWORD
    public Keywords getKeywords(String id, final String token)
    {
        String endpoint = id + KEYWORDS_ENDPOINT;
        Response response = null;
        try
        {
            response = get(endpoint, token, null);

            return response.readEntity(Keywords.class);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public Keyword getKeyword(String id, final String token, final String putCode)
    {
        String endpoint = id + KEYWORDS_ENDPOINT;
        Response response = null;
        try
        {
            response = get(endpoint, token, putCode);

            return response.readEntity(Keyword.class);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    
    public StatusType putKeyword(String id, String token, String putCode,
            Keyword keyword) throws IOException, JAXBException
    {
        String endpoint = id + KEYWORDS_ENDPOINT;
        Response response = null;
        try
        {
            response = put(endpoint, token, putCode,
                    Entity.entity(keyword, MediaType.APPLICATION_XML_TYPE));
            return response.getStatusInfo();
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public void deleteKeyword(String id, String token, String putCode)
            throws IOException, JAXBException
    {
        String endpoint = id + KEYWORDS_ENDPOINT;
        Response response = null;
        try
        {
            response = delete(endpoint, token, putCode);
        }
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }

    public String appendKeyword(String id, String token, Keyword keyword)
            throws IOException, JAXBException
    {
        String endpoint = id + KEYWORDS_ENDPOINT;
        Response response = null;
        try
        {
            response = post(endpoint, token,
                    Entity.entity(keyword, MediaType.APPLICATION_XML_TYPE));
            return retrievePutCode(response);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }        
        finally
        {
            if (response != null)
            {
                response.close();
            }
        }
    }    
    
    // Utility call
    public int higherDisplayIndex(WorkGroup orcidGroup)
    {
        int higher = 0;
        for (WorkSummary orcidSummary : orcidGroup.getWorkSummary())
        {
            if (StringUtils.isNotBlank(orcidSummary.getDisplayIndex()))
            {
                int current = Integer.parseInt(orcidSummary.getDisplayIndex());
                if (current > higher)
                {
                    higher = current;
                }
            }
        }
        return higher;
    }

    // Higher level call
    /**
     * HTTP GET method using to read resources from WS-REST
     * 
     * @param endpoint
     * @param token
     * @param putCode
     * @return
     */
    private <T> Response get(String endpoint, final String token,
            final String putCode)
    {
        Response response = null;
        if (StringUtils.isNotBlank(putCode))
        {
            endpoint = endpoint + "/" + putCode;
        }
        WebTarget target = restConnector.getClientRest(endpoint);

        if (token != null)
        {
            response = target.request()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(APPLICATION_ORCID_XML).acceptEncoding("UTF-8")
                    .get();
        }
        else
        {
            try
            {
                response = target.request().header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + getAccessToken(READ_PUBLIC_SCOPE, "scope",
                                "client_credentials").getAccess_token())
                        .accept(APPLICATION_ORCID_XML).acceptEncoding("UTF-8")
                        .get();
            }
            catch (IOException e)
            {

            }
        }

        log.debug("[GET] " + response.getStatus());
        log.debug("[GET] " + response.getStatusInfo().getReasonPhrase());

        return response;
    }

    /**
     * HTTP POST method used to add resource
     * 
     * @param endpoint
     * @param token
     * @param entity
     * @return
     */
    private <T> Response post(String endpoint, final String token,
            final Entity<T> entity)
    {
        Response response = null;
        WebTarget target = restConnector.getClientRest(endpoint);
        Builder builder = target.request().accept(APPLICATION_ORCID_XML)
                .acceptEncoding("UTF-8")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        response = builder.post(entity);
        log.debug("[POST] " + response.getStatus());
        log.debug("[POST] " + response.getStatusInfo().getReasonPhrase());

        return response;
    }

    /**
     * HTTP PUT method used to update resource by putCode
     * 
     * @param endpoint
     * @param token
     * @param putCode
     * @param entity
     * @return
     */
    private <T> Response put(String endpoint, final String token,
            final String putCode, final Entity<T> entity)
    {
        Response response;
        WebTarget target = restConnector
                .getClientRest(endpoint + "/" + putCode);
        Builder builder = target.request().accept(APPLICATION_ORCID_XML)
                .acceptEncoding("UTF-8")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        response = builder.put(entity);
        log.debug("[PUT] " + response.getStatus());
        log.debug("[PUT] " + response.getStatusInfo().getReasonPhrase());

        return response;
    }

    /**
     * HTTP DELETE method used to delete resource by putCode
     * 
     * @param endpoint
     * @param token
     * @param putCode
     * @return
     */
    private <T> Response delete(String endpoint, final String token,
            final String putCode)
    {
        Response response;
        WebTarget target = restConnector.getClientRest(endpoint
                + (StringUtils.isNotBlank(putCode) ? ("/" + putCode) : ""));
        Builder builder = target.request().accept(APPLICATION_ORCID_XML)
                .acceptEncoding("UTF-8")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        response = builder.delete();
        log.debug("[DELETE] " + response.getStatus());
        log.debug("[DELETE] " + response.getStatusInfo().getReasonPhrase());

        return response;
    }

    public String retrievePutCode(Response response) throws Exception
    {
        StatusType status = response.getStatusInfo();
        if (status != null)
        {
            if (!Family.SUCCESSFUL.equals(status.getFamily()))
            {
                log.error("[REASON]" + status.getStatusCode() + ":"
                        + status.getReasonPhrase());
                throw new Exception(status.getStatusCode() + ":"
                        + status.getReasonPhrase());
            }
        }
        String location = response.getLocation().toString();
        if (location != null && !"".equals(location))
        {
            String putCode = location.substring(location.lastIndexOf("/") + 1,
                    location.length());
            if (putCode.matches("\\d+"))
                return putCode;
        }

        return null;
    }

    public static boolean isValid(final String orcid)
    {
        return orcid.matches(
                "([0-9]{4})-([0-9]{4})-([0-9]{4})-([0-9]{3})(?:[0-9X]{1})");
    }

    public static void main(String[] args)
            throws JAXBException, IOException, ParseException
    {
        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption("h", "help", false, "help");
        options.addOption("i", "clientid", true, "Client iD");
        options.addOption("s", "clientsecret", true, "Client Secret");

        CommandLine line = parser.parse(options, args);

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("OrcidService \n", options);
            System.out.println(
                    "\n\nUSAGE:\n OrcidService -i <yourclientid> -s <yourclientsecret>\n");
            System.out.println(
                    "\n\nEXAMPLE:\n OrcidService -i XACASCASCSACASAC -s d0adf2-3232-3223-3232\n");
            System.exit(0);
        }

        if (line.hasOption('i') && line.hasOption('s'))
        {
            String clientid = line.getOptionValue("i");
            String secretid = line.getOptionValue("s");
            System.out.println(
                    "Try to validate against ORCID MEMBER API 'https://api.orcid.org/v2.0' and ORCID MEMBER TOKEN URL 'https://orcid.org/oauth/token'");
            System.out.println("Your Production Credentials:");
            System.out.println("Client iD:'" + clientid + "'");
            System.out.println("Client Secret:'" + secretid + "'");

            OrcidService orcidService = new OrcidService(
                    "https://api.orcid.org/v2.0", clientid, secretid,
                    "https://orcid.org/oauth/token");
            try
            {
                orcidService.search("test", 1, 1);
                System.out.println("OK!");
            }
            catch (Exception ex)
            {
                System.out.println("ERROR MESSAGE:" + ex.getMessage());
                System.out.println("FAILED!");
            }
        }
        else
        {
            System.out.println(
                    "\n\nUSAGE:\n OrcidService -i <yourclientid> -s <yourclientsecret>\n");
            System.out.println("Insert i and s parameters");
            System.out.println("use -h for help");
            System.exit(1);
        }

    }

}
