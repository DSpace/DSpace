/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.orcid.jaxb.Funding;
import org.dspace.authority.orcid.jaxb.FundingExternalIdentifier;
import org.dspace.authority.orcid.jaxb.FundingExternalIdentifiers;
import org.dspace.authority.orcid.jaxb.FundingList;
import org.dspace.authority.orcid.jaxb.OrcidActivities;
import org.dspace.authority.orcid.jaxb.OrcidBio;
import org.dspace.authority.orcid.jaxb.OrcidMessage;
import org.dspace.authority.orcid.jaxb.OrcidProfile;
import org.dspace.authority.orcid.jaxb.OrcidSearchResult;
import org.dspace.authority.orcid.jaxb.OrcidSearchResults;
import org.dspace.authority.orcid.jaxb.OrcidWork;
import org.dspace.authority.orcid.jaxb.OrcidWorks;
import org.dspace.authority.orcid.jaxb.WorkExternalIdentifier;
import org.dspace.authority.orcid.jaxb.WorkExternalIdentifiers;
import org.dspace.authority.rest.RestSource;
import org.dspace.content.DCPersonName;
import org.dspace.core.ConfigurationManager;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

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
public class OrcidService extends RestSource {

	private static final boolean testMode = false;
	
	public static final String ORCID_MODE_UPDATE = "PUT";
	public static final String ORCID_MODE_APPEND = "POST";

	/**
	 * log4j logger
	 */
	private static Logger log = Logger.getLogger(OrcidService.class);

	private static final String WORK_CREATE_ENDPOINT = "/orcid-works";
	private static final String PROFILE_CREATE_ENDPOINT = "/orcid-profile";
	private static final String FUNDING_CREATE_ENDPOINT = "/funding";
	
	private static final String READ_PROFILE_ENDPOINT = "/orcid-profile";
	private static final String READ_WORKS_ENDPOINT = "/orcid-works";
	private static final String READ_FUNDING_ENDPOINT = "/funding";
	private static final String READ_BIO_ENDPOINT = "/orcid-bio";
	
	private static final String BIO_UPDATE_ENDPOINT = "/orcid-bio";
	private static final String SEARCH_ENDPOINT = "search/orcid-bio/";	

	private static final String READ_PUBLIC_SCOPE = "/read-public";
	private static final String PROFILE_CREATE_SCOPE = "/orcid-profile/create";

	public static final String MESSAGE_VERSION = "1.2";

	private static OrcidService orcid;
	
	private static String sourceClientName;

	private final JAXBContext orcidMessageContext;

	private final MediaType APPLICATION_ORCID_XML = new MediaType("application", "orcid+xml");
	private final MediaType APPLICATION_VDN_ORCID_XML = new MediaType("application", "vdn.orcid+xml");

	private String clientID;

	private String clientSecretKey;

	private String tokenURL;

	private String baseURL;

	public static OrcidService getOrcid() {
		if (orcid == null) {
			orcid = new DSpace().getServiceManager().getServiceByName("OrcidSource", OrcidService.class);
            sourceClientName = ConfigurationManager.getProperty(
                    "authentication-oauth", "application-client-name");
		}
		return orcid;
	}

	private OrcidService(String url, String clientID, String clientSecretKey, String tokenURL) throws JAXBException {
		super(url);
		this.clientID = clientID;
		this.clientSecretKey = clientSecretKey;
		this.tokenURL = tokenURL;
		orcidMessageContext = JAXBContext.newInstance(OrcidMessage.class);
	}

	/**
	 * Returns the fields and "works" research activities that are set as
	 * "Public" in the ORCID Record for the scholar represented by the specified
	 * orcid_id. When used with  the Member API,
	 * limited-access data is also returned if permissions were grant by the user.
	 * 
	 * Public API
	 * 
	 * @param id
	 * @return
	 */
	public OrcidProfile getProfile(String id) {

		OrcidAccessToken token = null;
		try {
			token = getMemberSearchToken();
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		String access_token = null;
		if(token != null) {
		    access_token = token.getAccess_token();
		}
        return getProfile(id, access_token);
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
    public OrcidProfile getProfile(String id, String token)
    {

        WebTarget target = restConnector
                .getClientRest(id + READ_PROFILE_ENDPOINT);
        String response;
        if (token != null)
        {
            response = target.request()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_XML).acceptEncoding("UTF-8")
                    .get(String.class);
        }
        else
        {
            response = target.request().accept(MediaType.APPLICATION_XML)
                    .acceptEncoding("UTF-8").get(String.class);
        }
        OrcidMessage message = null;
        try
        {
            StringReader sr = new StringReader(response);
            message = (OrcidMessage) orcidMessageContext.createUnmarshaller()
                    .unmarshal(sr);
        }
        catch (JAXBException e)
        {
            log.error(e);
        }
        return message.getOrcidProfile();
    }

	public OrcidWorks getWorks(String id, String token) {

		WebTarget target = restConnector.getClientRest(id + READ_WORKS_ENDPOINT);
		String response = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.accept(MediaType.APPLICATION_XML).acceptEncoding("UTF-8").get(String.class);
		OrcidMessage message = null;
		try {
			StringReader sr = new StringReader(response);
			message = (OrcidMessage) orcidMessageContext.createUnmarshaller().unmarshal(sr);
		} catch (JAXBException e) {
			log.error(e);
		}
		return message.getOrcidProfile().getOrcidActivities().getOrcidWorks();
	}

    public FundingList getFundings(String id, String token)
    {

        WebTarget target = restConnector
                .getClientRest(id + READ_FUNDING_ENDPOINT);
        String response = target.request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .accept(MediaType.APPLICATION_XML).acceptEncoding("UTF-8")
                .get(String.class);
        OrcidMessage message = null;
        try
        {
            StringReader sr = new StringReader(response);
            message = (OrcidMessage) orcidMessageContext.createUnmarshaller()
                    .unmarshal(sr);
        }
        catch (JAXBException e)
        {
            log.error(e);
        }
        return message.getOrcidProfile().getOrcidActivities().getFundingList();
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
	public AuthorityValue queryAuthorityID(String id) {
		OrcidProfile bio = getProfile(id);
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
	public List<AuthorityValue> queryOrcidBioByFamilyNameAndGivenName(String text, int start, int max)
			throws IOException {
		DCPersonName tmpPersonName = new DCPersonName(text);

		String query = "";
		if (StringUtils.isNotBlank(tmpPersonName.getLastName())) {
			query += "family-name:(" + tmpPersonName.getLastName().trim()
					+ (StringUtils.isNotBlank(tmpPersonName.getFirstNames()) ? "" : "*")+")";
		}

		if (StringUtils.isNotBlank(tmpPersonName.getFirstNames())) {
			query += (query.length() > 0 ? " AND given-names:(" : "given-names:(") + tmpPersonName.getFirstNames().trim()
					+ "*)";
		}

		query += " OR other-names:(" + text + ")";

		OrcidSearchResults results = search(query, start, max);

		return getAuthorityValuesFromOrcidResults(results);
	}

	/**
	 * Creates new ORCID iDs and Records and notifies each scholar that the
	 * record has been created. The scholar has 10 days to decline the
	 * invitation before the iD is activated and information in the Record
	 * become accessible (according to the privacy model). The scholar may claim
	 * (start managing) or deactivate the ORCID Record at any time after it has
	 * been created. Member API (Creator license required).
	 *
	 * @param profile
	 * @return orcid iD
	 * @throws IOException
	 * @throws JAXBException
	 */
	public String buildProfile(OrcidProfile profile) throws IOException, JAXBException {
		WebTarget target = restConnector.getClientRest(PROFILE_CREATE_ENDPOINT);

		StringWriter sw = new StringWriter();
		Marshaller marshaller = orcidMessageContext.createMarshaller();

//		validate(marshaller);

		marshaller.marshal(wrapProfile(profile), sw);

		if(testMode) {
			return "test-orcid-profile-xxxx";
		}
		
		Builder builder = target.request().accept(MediaType.APPLICATION_XML).acceptEncoding("UTF-8")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + getMemberProfileCreateToken().getAccess_token());
		Entity<String> entity = Entity.entity(sw.toString(), APPLICATION_VDN_ORCID_XML);

		Response response = builder.post(entity);
		if (response.getStatus() != HttpStatus.SC_OK && response.getStatus() != HttpStatus.SC_CREATED) {
			if (response.hasEntity()) {
				try {
					Unmarshaller um = orcidMessageContext.createUnmarshaller();
					OrcidMessage message = (OrcidMessage) um.unmarshal((InputStream) response.getEntity());
					log.error(message.getErrorDesc().getContent());
				} catch (JAXBException e) {
					log.info("Problem unmarshalling return value " + e);
					throw new IOException(e.getMessage(),
							new RuntimeException("Failed : HTTP error code : " + response.getStatus()));
				}
			}
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}
		String result = null;
		if (response.getLocation() != null) {
			result = response.getLocation().getPath();
			Pattern pattern = Pattern.compile("/(.*)/orcid-profile");
			Matcher matcher = pattern.matcher(result);
			if (matcher.matches()) {
				result = matcher.group(1);
			}
		}
		return result;

	}

	/**
	 * 
	 * Completely replaces all "works" research activities from a given
	 * work-source in the ORCID Record for the scholar represented by the
	 * specified orcid_id. (You can only update works that your client
	 * application has added)
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
	public OrcidMessage updateBio(String id, String token, OrcidProfile profile) throws IOException, JAXBException {
		WebTarget target = restConnector.getClientRest(id + BIO_UPDATE_ENDPOINT);

		StringWriter sw = new StringWriter();
		Marshaller marshaller = orcidMessageContext.createMarshaller();

		validate(marshaller);

		marshaller.marshal(wrapProfile(profile), sw);

		Builder builder = target.request().accept(MediaType.APPLICATION_XML).acceptEncoding("UTF-8")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		Entity<String> entity = Entity.entity(sw.toString(), APPLICATION_VDN_ORCID_XML);
		if(testMode) {
			return new OrcidMessage();
		}
		Response response = builder.put(entity);
		return getOrcidMessage(response);
	}

	private void validate(Marshaller marshaller) throws FileNotFoundException {
		SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = null;
		try {
			InputStream inputStream = OrcidService.class
					.getResourceAsStream("orcid-message-" + MESSAGE_VERSION + ".xsd");
			if (inputStream == null) {
				throw new FileNotFoundException();
			}
			Source source = new StreamSource(inputStream);
			schema = sf.newSchema(source);
		} catch (SAXException e) {
			log.error(e.getMessage(), e);
		}

		marshaller.setSchema(schema);
	}

	/**
	 * Wrap an OrcidWork inside an empty OrcidMessage
	 */
	private static OrcidMessage wrapWork(OrcidWork work) {
		OrcidWorks works = new OrcidWorks();
		works.getOrcidWork().add(work);
		OrcidActivities activities = new OrcidActivities();
		activities.setOrcidWorks(works);
		OrcidProfile profile = new OrcidProfile();
		profile.setOrcidActivities(activities);
		OrcidMessage message = new OrcidMessage();
		message.setOrcidProfile(profile);
		message.setMessageVersion(MESSAGE_VERSION);
		return message;
	}

	/**
	 * Wrap a list of OrcidWork inside an empty OrcidMessage
	 */
	private static OrcidMessage wrapWorks(OrcidWorks works) {
		OrcidActivities activities = new OrcidActivities();
		activities.setOrcidWorks(works);
		OrcidProfile profile = new OrcidProfile();
		profile.setOrcidActivities(activities);
		OrcidMessage message = new OrcidMessage();
		message.setOrcidProfile(profile);
		message.setMessageVersion(MESSAGE_VERSION);
		return message;
	}

	/**
	 * Wrap a FundingList inside an empty OrcidMessage
	 */
	private static OrcidMessage wrapFundings(FundingList fundings) {
		OrcidActivities activities = new OrcidActivities();
		activities.setFundingList(fundings);
		OrcidProfile profile = new OrcidProfile();
		profile.setOrcidActivities(activities);
		OrcidMessage message = new OrcidMessage();
		message.setOrcidProfile(profile);
		message.setMessageVersion(MESSAGE_VERSION);
		return message;
	}

	/**
	 * Wrap a Funding inside an empty OrcidMessage
	 */
	private static OrcidMessage wrapFunding(Funding funding) {
		FundingList fundings = new FundingList();
		fundings.getFunding().add(funding);
		OrcidActivities activities = new OrcidActivities();
		activities.setFundingList(fundings);
		OrcidProfile profile = new OrcidProfile();
		profile.setOrcidActivities(activities);
		OrcidMessage message = new OrcidMessage();
		message.setOrcidProfile(profile);
		message.setMessageVersion(MESSAGE_VERSION);
		return message;
	}

	/**
	 * Wrap an OrcidProfile inside an empty OrcidMessage
	 */
	private static OrcidMessage wrapProfile(OrcidProfile profile) {
		OrcidMessage message = new OrcidMessage();
		message.setOrcidProfile(profile);
		message.setMessageVersion(MESSAGE_VERSION);
		return message;
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
	public OrcidSearchResults search(String query, int page, int pagesize) throws IOException {
		if (query == null || query.isEmpty()) {
			throw new IllegalArgumentException();
		}

		WebTarget target = restConnector.getClientRest(SEARCH_ENDPOINT);
		target = target.queryParam("q", query);
		if (pagesize >= 0) {
			target = target.queryParam("rows", Integer.toString(pagesize));
		}
		if (page >= 0) {
			target = target.queryParam("start", Integer.toString(page));
		}

		Builder builder = target.request().accept(APPLICATION_ORCID_XML);
		StringReader reader = null;
		try {
			reader = new StringReader(builder.get(String.class));
		} catch (ForbiddenException ex) {
			builder = builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + getMemberSearchToken().getAccess_token());
			reader = new StringReader(builder.get(String.class));
		}

		try {
			Unmarshaller um = orcidMessageContext.createUnmarshaller();
			OrcidMessage message = (OrcidMessage) um.unmarshal(reader);
			if (message.getOrcidSearchResults() == null) {
				// shouldn't happen but there's a bug ORCiD side.
				OrcidSearchResults r = new OrcidSearchResults();
				r.setNumFound(BigInteger.ZERO);
				return r;
			} else {
				return message.getOrcidSearchResults();
			}
		} catch (JAXBException e) {
			log.info("Problem unmarshalling return value " + e);
			throw new IOException(e);
		}
	}

	/**
	 * 
	 * Allows an ORCID client to obtain an OAuth Access Token to make Public API
	 * calls using the Member API (and its service level agreement).
	 * 
	 * @return
	 * @throws IOException
	 */
	public OrcidAccessToken getMemberSearchToken() throws IOException {
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
	public OrcidAccessToken getMemberProfileCreateToken() throws IOException {
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
	public OrcidAccessToken getAuthorizationAccessToken(String code) throws IOException {
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
	private OrcidAccessToken getAccessToken(String code, String codeOrScope, String grantType) throws IOException, JsonProcessingException {
		if(StringUtils.isBlank(clientID) || StringUtils.isBlank(clientSecretKey)) {
		    return null;
		}
	    
	    Client client = ClientBuilder.newClient(restConnector.getClientConfig());	    
		WebTarget target = client.target(tokenURL);
		Form form = new Form();
		form.param("client_id", clientID);
		form.param("client_secret", clientSecretKey);
		form.param("grant_type", grantType);
		form.param(codeOrScope, code);
		Builder builder = target.request().accept(MediaType.APPLICATION_JSON);
		String response = builder.post(Entity.form(form), String.class);
		return new ObjectMapper().reader(OrcidAccessToken.class).readValue(response);
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
	public List<AuthorityValue> queryAuthorities(String field, String text, int start, int max) throws IOException {
		OrcidSearchResults results = search(field + ":" + URLEncoder.encode(text), start, max);

		return getAuthorityValuesFromOrcidResults(results);
	}

	/**
	 * From JAXB OrcidSearchResults to AuthorityValue
	 * 
	 * @param results
	 * @return
	 */
	private List<AuthorityValue> getAuthorityValuesFromOrcidResults(OrcidSearchResults results) {
		List<AuthorityValue> authorities = new ArrayList<AuthorityValue>();
		for (OrcidSearchResult result : results.getOrcidSearchResult()) {
			authorities.add(OrcidAuthorityValue.create(result.getOrcidProfile()));
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
	public List<AuthorityValue> queryAuthorities(String text, int max) throws IOException {
		OrcidSearchResults results = search(URLEncoder.encode(text), 0, max);

		return getAuthorityValuesFromOrcidResults(results);
	}

	//WORKS

	/**
	 * Adds more "works" research activities to the ORCID Record for the scholar
	 * represented by the specified orcid_id.
	 * 
	 * Member API, require '/orcid-works/create' scope.
	 * 
	 * 
	 * @param id
	 * @param token
	 * @param work
	 * @throws IOException
	 * @throws JAXBException
	 */
	public OrcidMessage appendWorks(String id, String token, OrcidWorks works) throws IOException, JAXBException {
		return pushWorks(id, token, works, ORCID_MODE_APPEND);
	}

	/**
	 * Completely replaces all "works" research activities from a given
	 * work-source in the ORCID Record for the scholar represented by the
	 * specified orcid_id. (You can only update works that your client
	 * application has added.)
	 * 
	 * Member API, require '/orcid-works/create' or '/orcid-works/update' scope.
	 * 
	 * 
	 * @param id
	 * @param token
	 * @param work
	 * @throws IOException
	 * @throws JAXBException
	 */
	public OrcidMessage putWorks(String id, String token, OrcidWorks works) throws IOException, JAXBException {
		return pushWorks(id, token, works, ORCID_MODE_UPDATE);
	}
	
	private OrcidMessage pushWorks(String id, String token, OrcidWorks works, String method) throws IOException, JAXBException {
		WebTarget target = restConnector.getClientRest(id + WORK_CREATE_ENDPOINT);

		Builder builder = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer " + token);

		StringWriter sw = new StringWriter();
		Marshaller marshaller = orcidMessageContext.createMarshaller();

		validate(marshaller);

		marshaller.marshal(wrapWorks(works), sw);

		Entity<String> entity = Entity.entity(sw.toString(), APPLICATION_ORCID_XML);

		Response response = null;
		if(testMode) {
			return new OrcidMessage();
		}
		if(method.equals(ORCID_MODE_APPEND)) {
			response = builder.post(entity);
		}else {
			response = builder.put(entity);
		}

		return getOrcidMessage(response);
	}
	
	/**
	 * Adds one "work" research activities to the ORCID Record for the
	 * scholar represented by the specified orcid_id.
	 * 
	 * Member API, require '/orcid-works/create' scope.
	 * 
	 * 
	 * @param idsa
	 * @param token
	 * @param work
	 * @param handle
	 * 
	 * @return putcode
	 * 
	 * @throws IOException
	 * @throws JAXBException
	 */
	public String appendWork(String id, String token, OrcidWork work, String handle) throws IOException, JAXBException {
		WebTarget target = restConnector.getClientRest(id + WORK_CREATE_ENDPOINT);

		Builder builder = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer " + token);

		StringWriter sw = new StringWriter();
		Marshaller marshaller = orcidMessageContext.createMarshaller();

		validate(marshaller);

		marshaller.marshal(wrapWork(work), sw);

		Entity<String> entity = Entity.entity(sw.toString(), APPLICATION_ORCID_XML);
		if(testMode) {
			return "put-code";
		}
		Response response = builder.post(entity);

		OrcidMessage message = getOrcidMessage(response);
		if(message == null) {
           
            if (StringUtils.isNotEmpty(handle))
            {
                // retrieve the orcid works by hand
                OrcidWorks orcidWorks = getWorks(id, token);

                if (orcidWorks != null)
                {
                    for (OrcidWork justWork : orcidWorks.getOrcidWork())
                    {
                        if (StringUtils.equals(
                                justWork.getSource().getSourceName().getContent(),
                                getSourceClientName()))
                        {
                            WorkExternalIdentifiers extIds = justWork
                                    .getWorkExternalIdentifiers();
                            if (extIds != null)
                            {
                                for (WorkExternalIdentifier extId : extIds
                                        .getWorkExternalIdentifier())
                                {
                                    if ("handle".equals(extId
                                            .getWorkExternalIdentifierType()))
                                    {
                                        if (handle.trim()
                                                .equals(extId
                                                        .getWorkExternalIdentifierId()
                                                        .trim()))
                                        {
                                            return justWork.getPutCode()
                                                    .toString();
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                }
            }
            else
            {
                log.warn("No handle found for " + id);
            }
		}
		
        String putCode = null; 
        try {
            putCode = message.getOrcidProfile().getOrcidActivities().getOrcidWorks().getOrcidWork().get(0).getPutCode().toString();
        }
        catch(Exception e) {
            log.warn("No put-code for owner for orcid " + id);
        }
        return putCode;
	}
	
	//Fundings
	/**
	 * Adds more "fundings" research activities to the ORCID Record for the scholar
	 * represented by the specified orcid_id.
	 * 
	 * Member API, require '/orcid-works/create' scope.
	 * 
	 * 
	 * @param id
	 * @param token
	 * @param work
	 * @throws IOException
	 * @throws JAXBException
	 */
	public OrcidMessage appendFundings(String id, String token, FundingList fundings) throws IOException, JAXBException {
		return pushFundings(id, token, fundings, ORCID_MODE_APPEND);
	}

	/**
	 * Completely replaces all "funding" research activities from a given
	 * work-source in the ORCID Record for the scholar represented by the
	 * specified orcid_id. (You can only update works that your client
	 * application has added.)
	 * 
	 * Member API, require '/orcid-works/create' or '/orcid-works/update' scope.
	 * 
	 * 
	 * @param id
	 * @param token
	 * @param work
	 * @throws IOException
	 * @throws JAXBException
	 */
	public OrcidMessage putFundings(String id, String token, FundingList fundings) throws IOException, JAXBException {
		return pushFundings(id, token, fundings, ORCID_MODE_UPDATE);
	}

	private OrcidMessage pushFundings(String id, String token, FundingList fundings, String method)
			throws IOException, JAXBException {
		WebTarget target = restConnector.getClientRest(id + FUNDING_CREATE_ENDPOINT);

		Builder builder = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer " + token);

		StringWriter sw = new StringWriter();
		Marshaller marshaller = orcidMessageContext.createMarshaller();

		validate(marshaller);

		marshaller.marshal(wrapFundings(fundings), sw);

		Entity<String> entity = Entity.entity(sw.toString(), APPLICATION_ORCID_XML);

		Response response = null;
		if(testMode) {
			return new OrcidMessage();
		}
		if (method.equals(ORCID_MODE_APPEND)) {
			response = builder.post(entity);
		} else {
			response = builder.put(entity);
		}

		return getOrcidMessage(response);
	}
	
	/**
	 * Adds one "funding" research activities to the ORCID Record for the
	 * scholar represented by the specified orcid_id.
	 * 
	 * Member API, require '/orcid-works/create' scope.
	 * 
	 * @param id
	 * @param token
	 * @param work
	 * @param uuid
	 * 
	 * @return putcode
	 * 
	 * @throws IOException
	 * @throws JAXBException
	 */
    public String appendFunding(String id, String token, Funding funding,
            String uuid) throws IOException, JAXBException
    {
        WebTarget target = restConnector
                .getClientRest(id + FUNDING_CREATE_ENDPOINT);

        Builder builder = target.request().header(HttpHeaders.AUTHORIZATION,
                "Bearer " + token);

        StringWriter sw = new StringWriter();
        Marshaller marshaller = orcidMessageContext.createMarshaller();

        validate(marshaller);

        marshaller.marshal(wrapFunding(funding), sw);

        Entity<String> entity = Entity.entity(sw.toString(),
                APPLICATION_ORCID_XML);

        if (testMode)
        {
            return "put-code";
        }
        Response response = builder.post(entity);

        OrcidMessage message = getOrcidMessage(response);
        //usually a POST return empty entity
        if (message == null)
        {

            if (StringUtils.isNotEmpty(uuid))
            {
                // retrieve the orcid funding by hand
                FundingList fundings = getFundings(id, token);

                if (fundings != null)
                {

                    for (Funding justWork : fundings.getFunding())
                    {
                        if (StringUtils.equals(justWork.getSource()
                                .getSourceName().getContent(),
                                getSourceClientName()))
                        {
                            FundingExternalIdentifiers extIds = justWork
                                    .getFundingExternalIdentifiers();
                            if (extIds != null)
                            {
                                for (FundingExternalIdentifier extId : extIds
                                        .getFundingExternalIdentifier())
                                {
                                    if ("uuid".equals(extId
                                            .getFundingExternalIdentifierType()))
                                    {
                                        if (uuid.trim()
                                                .equals(extId
                                                        .getFundingExternalIdentifierValue()
                                                        .trim()))
                                        {
                                            return justWork.getPutCode()
                                                    .toString();
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }
            else
            {
                log.warn("No uuid found for " + id);
            }
        }

        //manage single putcode
        String putCode = null;
        try
        {
            putCode = message.getOrcidProfile().getOrcidActivities()
                    .getFundingList().getFunding().get(0).getPutCode()
                    .toString();
        }
        catch (Exception e)
        {
            log.warn("No put-code for owner for orcid " + id);
        }
        return putCode;
    }

	/**
     * Returns the fields set as "Public" in the bio portion of the ORCID Record
     * for the scholar represented by the specified orcid_id. When used with an
     * access token and the Member API, limited-access data is also returned. 
     * 
     * Public API
     * 
     * @param id
     * @return
     */
    public OrcidBio getBio(String id) {

        OrcidAccessToken token = null;
        try {
            token = getMemberSearchToken();
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        String access_token = null;
        if(token != null) {
            access_token = token.getAccess_token();
        }
        return getBio(id, access_token);
    }

    /**
     * Returns the fields set as "Public" in the bio portion of the ORCID Record
     * for the scholar represented by the specified orcid_id. When used with an
     * access token and the Member API, limited-access data is also returned. 
     * 
     * Member API
     * 
     * @param id
     * @param token
     * @return
     */
    public OrcidBio getBio(String id, String token)
    {

        WebTarget target = restConnector
                .getClientRest(id + READ_BIO_ENDPOINT);
        String response;
        if (token != null)
        {
            response = target.request()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_XML).acceptEncoding("UTF-8")
                    .get(String.class);
        }
        else
        {
            response = target.request().accept(MediaType.APPLICATION_XML)
                    .acceptEncoding("UTF-8").get(String.class);
        }
        OrcidMessage message = null;
        try
        {
            StringReader sr = new StringReader(response);
            message = (OrcidMessage) orcidMessageContext.createUnmarshaller()
                    .unmarshal(sr);
        }
        catch (JAXBException e)
        {
            log.error(e);
        }
        return message.getOrcidProfile().getOrcidBio();
    }
	
	/**
	 * Get message from orcid response
	 * 
	 * @param response
	 * @return
	 */
	private OrcidMessage getOrcidMessage(Response response) {
		OrcidMessage message = null;		
		if (response.hasEntity()) {
			try {
				Unmarshaller um = orcidMessageContext.createUnmarshaller();
				message = (OrcidMessage) um.unmarshal((InputStream) response.getEntity());
				if (message.getErrorDesc() != null) {
					log.error(message.getErrorDesc().getContent());
				}
			} catch (JAXBException e) {
				log.info("Problem unmarshalling return value " + e);
			}
		} 
		if (response.getStatus() != HttpStatus.SC_OK && response.getStatus() != HttpStatus.SC_CREATED) {
			if(message!=null && message.getErrorDesc() != null && StringUtils.isNotBlank(message.getErrorDesc().getContent())) {
				throw new RuntimeException(message.getErrorDesc().getContent());	
			}
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}
		return message;
	}
	
	public String getBaseURL() {
		return baseURL;
	}

	public void setBaseURL(String baseURL) {
		this.baseURL = baseURL;
	}
	
	public static void main(String[] args) throws JAXBException, IOException, ParseException
    {
        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption("h", "help", false, "help");
        options.addOption("i", "clientid", true, "Client iD");
        options.addOption("s", "clientsecret", true, "Client Secret");

        CommandLine line = parser.parse(options, args);

        if (line.hasOption('h')) {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("OrcidService \n", options);
            System.out.println(
                    "\n\nUSAGE:\n OrcidService -i <yourclientid> -s <yourclientsecret>\n");
            System.out.println(
                    "\n\nEXAMPLE:\n OrcidService -i XACASCASCSACASAC -s d0adf2-3232-3223-3232\n");
            System.exit(0);
        }

        if (line.hasOption('i') && line.hasOption('s')) {
            String clientid = line.getOptionValue("i");
            String secretid = line.getOptionValue("s");
            System.out.println("Try to validate against ORCID MEMBER API 'http://api.orcid.org/v1.2' and ORCID MEMBER TOKEN URL 'https://api.orcid.org/oauth/token'");
            System.out.println("Your Production Credentials:");
            System.out.println("Client iD:'" + clientid + "'");
            System.out.println("Client Secret:'" + secretid +"'");
            
            OrcidService orcidService = new OrcidService("http://api.orcid.org/v1.2", clientid, secretid, "https://api.orcid.org/oauth/token");
            try {
                orcidService.search("test", 1, 1);
                System.out.println("OK!");
            }
            catch(Exception ex) {                
                System.out.println("ERROR MESSAGE:" + ex.getMessage());                
                System.out.println("FAILED!");
            }
        }
        else {
            System.out.println(
                    "\n\nUSAGE:\n OrcidService -i <yourclientid> -s <yourclientsecret>\n");
            System.out.println("Insert i and s parameters");
            System.out.println("use -h for help");
            System.exit(1);
        }

	    
    }

    public static String getSourceClientName()
    {
        if(sourceClientName==null) {
            sourceClientName = ConfigurationManager.getProperty(
                    "authentication-oauth", "application-client-name");
        }
        return sourceClientName;
    }
}
