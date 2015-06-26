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
import java.sql.SQLException;
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

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.orcid.jaxb.Biography;
import org.dspace.authority.orcid.jaxb.ContactDetails;
import org.dspace.authority.orcid.jaxb.CreditName;
import org.dspace.authority.orcid.jaxb.Email;
import org.dspace.authority.orcid.jaxb.Funding;
import org.dspace.authority.orcid.jaxb.FundingList;
import org.dspace.authority.orcid.jaxb.OrcidActivities;
import org.dspace.authority.orcid.jaxb.OrcidBio;
import org.dspace.authority.orcid.jaxb.OrcidMessage;
import org.dspace.authority.orcid.jaxb.OrcidPreferences;
import org.dspace.authority.orcid.jaxb.OrcidProfile;
import org.dspace.authority.orcid.jaxb.OrcidSearchResult;
import org.dspace.authority.orcid.jaxb.OrcidSearchResults;
import org.dspace.authority.orcid.jaxb.OrcidWork;
import org.dspace.authority.orcid.jaxb.OrcidWorks;
import org.dspace.authority.orcid.jaxb.PersonalDetails;
import org.dspace.authority.orcid.jaxb.Scope;
import org.dspace.authority.orcid.jaxb.Visibility;
import org.dspace.authority.rest.RestSource;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCPersonName;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Context;
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

	/**
	 * log4j logger
	 */
	private static Logger log = Logger.getLogger(OrcidService.class);

	private static final String WORK_CREATE_ENDPOINT = "/orcid-works";
	private static final String PROFILE_CREATE_ENDPOINT = "/orcid-profile";
	private static final String READ_PROFILE_ENDPOINT = "/orcid-profile";
	private static final String READ_WORKS_ENDPOINT = "/orcid-works";
	private static final String BIO_UPDATE_ENDPOINT = "/orcid-bio";	
	private static final String SEARCH_ENDPOINT = "search/orcid-bio/";
	private static final String FUNDING_CREATE_ENDPOINT = "/funding";
	
	private static final String READ_PUBLIC_SCOPE = "/read-public";
	private static final String PROFILE_CREATE_SCOPE = "/orcid-profile/create";
	
	public static final String MESSAGE_VERSION = "1.2";

	private static OrcidService orcid;

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
	 * orcid_id. When used with an access token and the Member API,
	 * limited-access data is also returned.
	 * 
	 * Public API
	 * 
	 * @param id
	 * @param token
	 * @return
	 */
	public OrcidProfile getProfile(String id) {

		WebTarget target = restConnector.getClientRest(id + READ_PROFILE_ENDPOINT);
		String response = target.request().accept(MediaType.APPLICATION_XML).acceptEncoding("UTF-8").get(String.class);

		OrcidMessage message = null;
		try {
			StringReader sr = new StringReader(response);
			message = (OrcidMessage) orcidMessageContext.createUnmarshaller().unmarshal(sr);
		} catch (JAXBException e) {
			log.error(e);
		}
		return message.getOrcidProfile();
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
	public OrcidProfile getProfile(String id, String token) {

		WebTarget target = restConnector.getClientRest(id + READ_PROFILE_ENDPOINT);
		String response = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.accept(MediaType.APPLICATION_XML).acceptEncoding("UTF-8").get(String.class);
		OrcidMessage message = null;
		try {
			StringReader sr = new StringReader(response);
			message = (OrcidMessage) orcidMessageContext.createUnmarshaller().unmarshal(sr);
		} catch (JAXBException e) {
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
			query += "family-name:" + tmpPersonName.getLastName().trim()
					+ (StringUtils.isNotBlank(tmpPersonName.getFirstNames()) ? "" : "*");
		}

		if (StringUtils.isNotBlank(tmpPersonName.getFirstNames())) {
			query += (query.length() > 0 ? " AND given-names:" : "given-names:") + tmpPersonName.getFirstNames().trim()
					+ "*";
		}

		OrcidSearchResults results = search(query, start, max);

		return getAuthorityValuesFromOrcidResults(results);
	}

	/**
	 * Adds one or more "works" research activities to the ORCID Record for the
	 * scholar represented by the specified orcid_id.
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
	public void appendWork(String id, String token, OrcidWork work) throws IOException, JAXBException {
		WebTarget target = restConnector.getClientRest(id + WORK_CREATE_ENDPOINT);

		Builder builder = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer " + token);

		StringWriter sw = new StringWriter();
		Marshaller marshaller = orcidMessageContext.createMarshaller();

		validate(marshaller);

		marshaller.marshal(wrapWork(work), sw);

		Entity<String> entity = Entity.entity(sw.toString(), APPLICATION_ORCID_XML);

		Response response = builder.post(entity);

		if (response.getStatus() != HttpStatus.SC_OK && response.getStatus() != HttpStatus.SC_CREATED) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}
	}

	/**
	 * Adds more "works" research activities to the ORCID Record for the
	 * scholar represented by the specified orcid_id.
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
	public void appendWorks(String id, String token, OrcidWorks works) throws IOException, JAXBException {
		WebTarget target = restConnector.getClientRest(id + WORK_CREATE_ENDPOINT);

		Builder builder = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer " + token);

		StringWriter sw = new StringWriter();
		Marshaller marshaller = orcidMessageContext.createMarshaller();

		validate(marshaller);

		marshaller.marshal(wrapWorks(works), sw);

		Entity<String> entity = Entity.entity(sw.toString(), APPLICATION_ORCID_XML);

		Response response = builder.post(entity);

		if (response.getStatus() != HttpStatus.SC_OK && response.getStatus() != HttpStatus.SC_CREATED) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}
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

		validate(marshaller);

		marshaller.marshal(wrapProfile(profile), sw);

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

		Response response = builder.put(entity);
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
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}
		return message;
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
		return getAccessToken(code);
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
		return getAccessToken(code);
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
	private OrcidAccessToken getAccessToken(String scope) throws IOException, JsonProcessingException {
		Client client = ClientBuilder.newClient(restConnector.getClientConfig());
		WebTarget target = client.target(tokenURL);
		Form form = new Form();
		form.param("client_id", clientID);
		form.param("client_secret", clientSecretKey);
		form.param("grant_type", "client_credentials");
		form.param("scope", scope);
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

	
	public void appendFundings(String id, String token, FundingList fundings) throws IOException, JAXBException {
		WebTarget target = restConnector.getClientRest(id + FUNDING_CREATE_ENDPOINT);

		Builder builder = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer " + token);

		StringWriter sw = new StringWriter();
		Marshaller marshaller = orcidMessageContext.createMarshaller();

		validate(marshaller);

		marshaller.marshal(wrapFundings(fundings), sw);

		Entity<String> entity = Entity.entity(sw.toString(), APPLICATION_ORCID_XML);

		Response response = builder.post(entity);

		if (response.getStatus() != HttpStatus.SC_OK && response.getStatus() != HttpStatus.SC_CREATED) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}
	}

	
	public void appendFunding(String id, String token, Funding funding) throws IOException, JAXBException {
		WebTarget target = restConnector.getClientRest(id + FUNDING_CREATE_ENDPOINT);

		Builder builder = target.request().header(HttpHeaders.AUTHORIZATION, "Bearer " + token);

		StringWriter sw = new StringWriter();
		Marshaller marshaller = orcidMessageContext.createMarshaller();

		validate(marshaller);

		marshaller.marshal(wrapFunding(funding), sw);

		Entity<String> entity = Entity.entity(sw.toString(), APPLICATION_ORCID_XML);

		Response response = builder.post(entity);

		if (response.getStatus() != HttpStatus.SC_OK && response.getStatus() != HttpStatus.SC_CREATED) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}
	}
	
	public static void main(String[] args)
			throws CrosswalkException, IOException, SQLException, AuthorizeException, JAXBException {

		Context context = new Context();

		OrcidService orcid = OrcidService.getOrcid();
		// OrcidWork work = new OrcidWork();
		//
		// Item item = Item.find(context, 211);
		//
		// final StreamDisseminationCrosswalk streamCrosswalkDefault =
		// (StreamDisseminationCrosswalk) PluginManager
		// .getNamedPlugin(StreamDisseminationCrosswalk.class, "bibtex");
		//
		// OutputStream outputStream = new ByteArrayOutputStream();
		// streamCrosswalkDefault.disseminate(context, item, outputStream);
		// String citationFromBTE = outputStream.toString();
		//
		// Citation citation = new Citation();
		// citation.setWorkCitationType(CitationType.BIBTEX);
		// citation.setCitation(citationFromBTE);
		//
		// WorkTitle title = new WorkTitle();
		// title.setTitle(item.getName());
		// work.setWorkTitle(title);
		// work.setWorkCitation(citation);
		// work.setWorkType("book");
		//
		// WorkExternalIdentifiers externalIdentifiers = new
		// WorkExternalIdentifiers();
		// // WorkExternalIdentifier externalIdentifier = new
		// // WorkExternalIdentifier();
		// //
		// externalIdentifier.setWorkExternalIdentifierId(StringEscapeUtils.escapeXml("<![CDATA["+item.getHandle()+"]]>"));
		// // externalIdentifier.setWorkExternalIdentifierType("﻿handle");
		// //
		// externalIdentifiers.getWorkExternalIdentifier().add(externalIdentifier);
		//
		// WorkExternalIdentifier externalIdentifier1 = new
		// WorkExternalIdentifier();
		// externalIdentifier1.setWorkExternalIdentifierId("" + item.getID());
		// externalIdentifier1.setWorkExternalIdentifierType("source-work-id");
		// externalIdentifiers.getWorkExternalIdentifier().add(externalIdentifier1);
		//
		// work.setWorkExternalIdentifiers(externalIdentifiers);
		//
		// try {
		// orcid.appendWork("0000-0001-9753-8285",
		// "d0c29317-cb20-4a39-a475-2bcbf5cf650b", work);
		// } catch (Exception ex) {
		// log.error(ex.getMessage(), ex);
		// }
		//
		// try {
		// orcid.getProfile("0000-0001-9753-8285",
		// "d0c29317-cb20-4a39-a475-2bcbf5cf650b");
		// } catch (Exception ex) {
		// log.error(ex.getMessage(), ex);
		// }
		//
		// try {
		// orcid.search("Pascarelli Luigi Andrea", 0, 10);
		// } catch (Exception ex) {
		// log.error(ex.getMessage(), ex);
		// }
		//
		// try {
		// OrcidProfile profile = new OrcidProfile();
		//
		// OrcidActivities activities = new OrcidActivities();
		// profile.setOrcidActivities(activities);
		//
		// OrcidPreferences prefs = new OrcidPreferences();
		// prefs.setLocale("en");
		//
		// profile.setOrcidPreferences(prefs);
		// OrcidBio bio = new OrcidBio();
		// Scope scope = Scope.UPDATE;
		// bio.setScope(scope);
		// PersonalDetails details = new PersonalDetails();
		// details.setGivenNames("Luigi Andrea");
		// details.setFamilyName("Pascarelli");
		// CreditName creditName = new CreditName();
		// creditName.setValue("Pascarelli Luigi Andrea");
		// creditName.setVisibility(Visibility.PUBLIC);
		// details.setCreditName(creditName);
		// bio.setPersonalDetails(details);
		//
		// ContactDetails contact = new ContactDetails();
		//
		// Email email = new Email();
		// email.setPrimary(true);
		// email.setVisibility(Visibility.PUBLIC);
		// email.setValue("oooooooo@mailinator.org");
		// contact.getEmail().add(email);
		// bio.setContactDetails(contact);
		//
		// Biography biography = new Biography();
		// biography.setValue("Test test test");
		// bio.setBiography(biography);
		//
		// profile.setOrcidBio(bio);
		// orcid.buildProfile(profile);
		// } catch (Exception ex) {
		// log.error(ex.getMessage(), ex);
		// }
		// }
	}

	public String getBaseURL() {
		return baseURL;
	}

	public void setBaseURL(String baseURL) {
		this.baseURL = baseURL;
	}
}
