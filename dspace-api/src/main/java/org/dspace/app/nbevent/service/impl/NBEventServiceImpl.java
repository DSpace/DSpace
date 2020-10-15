/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.nbevent.service.impl;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.exception.InvalidEnumeratedDataValueException;
import org.dspace.app.nbevent.dao.impl.NBEventsDaoImpl;
import org.dspace.app.nbevent.service.NBEventService;
import org.dspace.app.nbevent.service.dto.MessageDto;
import org.dspace.app.nbevent.service.dto.NBEventImportDto;
import org.dspace.app.nbevent.service.dto.NBEventQueryDto;
import org.dspace.app.nbevent.service.dto.NBTopic;
import org.dspace.content.Item;
import org.dspace.content.NBEvent;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.SolrImportExportException;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class NBEventServiceImpl implements NBEventService {

    private static final Logger log = Logger.getLogger(NBEventServiceImpl.class);

    @Autowired(required = true)
    protected ConfigurationService configurationService;
    
    @Autowired(required = true)
    protected ItemService itemService;
    
    @Autowired
    private HandleService handleService;
    
    @Autowired
    private NBEventsDaoImpl nbEventsDao;

    private ObjectMapper jsonMapper;
    
    
    public NBEventServiceImpl() {
    	jsonMapper = new JsonMapper();
    	jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    /**
     * Non-Static CommonsHttpSolrServer for processing indexing events.
     */
    protected SolrClient solr = null;


    public static final String ORIGINAL_ID = "original_id";
    public static final String TITLE = "title";
    public static final String TOPIC = "topic";
    public static final String TRUST = "trust";
    public static final String MESSAGE = "message";
    public static final String EVENT_ID = "event_id";
    public static final String RESOURCE_UUID = "resource_uuid";
    public static final String LAST_UPDATE = "last_update";
    
    protected SolrClient getSolr() {
    	if (solr == null) {
            String solrService = DSpaceServicesFactory.getInstance().getConfigurationService()
                    .getProperty("openstar.search.server");
            return new HttpSolrClient.Builder(solrService).build();
        }
        return solr;
    }
    
	@Override
	public int countTopics(Context context) throws SolrServerException, IOException {
    	SolrQuery solrQuery = new SolrQuery();
    	solrQuery.setRows(0);
    	solrQuery.setQuery(TOPIC+":*");
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(0);
        solrQuery.addFacetField(TOPIC);
    	QueryResponse response = getSolr().query(solrQuery);
    	return response.getResults().size();
	}
	

	@Override
	public NBEventImportDto deleteEventByResourceUUID(String id) throws SolrServerException, IOException {
		NBEventImportDto dto = findEventByResourceUUID(id);
		getSolr().deleteById(dto.getHashString());
		getSolr().commit();
		return dto;
	}


    @Override
	public NBTopic findTopicByTopicId(String topicId) throws SolrServerException, IOException, InvalidEnumeratedDataValueException {
    	SolrQuery solrQuery = new SolrQuery();
    	solrQuery.setRows(0);
    	solrQuery.setQuery(TOPIC+":"+OpenstarSupportedTopic.restToSolr(topicId));
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(0);
        solrQuery.addFacetField(TOPIC);
    	QueryResponse response = getSolr().query(solrQuery);
        FacetField facetField = response.getFacetField(TOPIC);
        for(Count c : facetField.getValues()) {
        	if(c.getName().equals(OpenstarSupportedTopic.restToSolr(topicId))) {
               	NBTopic topic = new NBTopic();
            	topic.setId(c.getName());
            	topic.setName(OpenstarSupportedTopic.sorlToRest(c.getName()));
            	topic.setTotalSuggestion(String.valueOf(c.getCount()));
            	topic.setLastEvent("MISS THIS VALUE");
            	return topic;       		
        	}
        }
        return null;
    }

	/**
	 * Method to get all topics and the number of entries for each topic
	 * 
	 * @param context DSpace context
	 * @param offset number of results to skip
	 * @param count number of result to fetch
	 * @return list of topics with number of events
	 * @throws IOException 
	 * @throws SolrServerException 
	 * @throws InvalidEnumeratedDataValueException 
	 * 
	 */
    @Override
	public List<NBTopic> findAllTopics(Context context, long offset, Integer count) throws SolrServerException, IOException, InvalidEnumeratedDataValueException {
    	
    	SolrQuery solrQuery = new SolrQuery();
    	solrQuery.setRows(0);
    	solrQuery.setQuery(TOPIC+":*");
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(0);
        solrQuery.addFacetField(TOPIC);
    	QueryResponse response = getSolr().query(solrQuery);
        FacetField facetField = response.getFacetField(TOPIC);
        List<NBTopic> nbTopics = new ArrayList<>();
        for(Count c : facetField.getValues()) {
        	NBTopic topic = new NBTopic();
        	topic.setId(c.getName());
        	topic.setName(OpenstarSupportedTopic.sorlToRest(c.getName()));
        	topic.setTotalSuggestion(String.valueOf(c.getCount()));
        	topic.setLastEvent("MISS THIS VALUE");
        	nbTopics.add(topic);
        }
        return nbTopics;
    }



	@Override
	public void store(Context context, List<NBEventImportDto> entries) throws SolrImportExportException, SolrServerException, IOException, SQLException {
		UpdateRequest updateRequest = new UpdateRequest();
        for(NBEventImportDto dto : entries) {
        	String topic = null;
        	try {
        		topic = OpenstarSupportedTopic.authorizedTopic(dto.getTopic());
        	} catch(Exception e) {
        		
        	}
        	if(topic != null) {
	    		String checksum = generateChecksumFromToString(dto.getHashString());
	    		try {
					if(!nbEventsDao.isEventStored(context, checksum)) {
						SolrInputDocument doc = new SolrInputDocument();
						doc.addField(EVENT_ID, checksum);
					    doc.addField(ORIGINAL_ID, dto.getOriginalId());
					    doc.addField(TITLE, dto.getTitle());
					    doc.addField(TOPIC, topic);
					    doc.addField(TRUST, dto.getTrust());
					    doc.addField(MESSAGE, jsonMapper.writeValueAsString(dto.getMessage()));
					    doc.addField(LAST_UPDATE, System.currentTimeMillis());
					    doc.addField(RESOURCE_UUID, getResourceUUID(context, dto.getOriginalId()));
						updateRequest.add(doc);
					}
				} catch (SQLException e) {
					log.error("Unable to query RDB, throw Exception and stop import", e);
					throw e;
				}
        	}
        }
        updateRequest.process(getSolr());
        getSolr().commit();
	}
/*	
	@Override
	public void delete(String resourceUUID) throws SolrServerException, IOException {
		UpdateResponse response = getSolr().deleteByQuery(RESOURCE_UUID+":"+resourceUUID);
		getSolr().commit();
	}
	
	@Override
	public void query(String resourceUUID) throws SolrServerException, IOException {
		SolrQuery param = new SolrQuery();
		param.set(RESOURCE_UUID, resourceUUID);
		QueryResponse response = getSolr().query(param);
		if(response == null) {
			return;
		} else {
			NamedList<Object> oItems = response.getResponse();
			for(Object o : oItems) {
				NBEventQueryDto item = (NBEventQueryDto)o;
			}
		}
		return;
	}
*/
	
	@Override
	public NBEventQueryDto findEventByEventId(Context context, String eventId) throws SolrServerException, IOException, InvalidEnumeratedDataValueException {
		SolrQuery param = new SolrQuery();
		param.set(EVENT_ID, OpenstarSupportedTopic.restToSolr(eventId));
		QueryResponse response;
		try {
			response = getSolr().query(param);
			if(response != null) {
		        SolrDocumentList list = response.getResults();
		        if(list != null && list.size() == 1) {
		        	SolrDocument doc = list.get(0);
		        	NBEventQueryDto item = new NBEventQueryDto();
		        	item.setEventId((String)doc.get(EVENT_ID));
		        	item.setLastUpdate((Long)doc.get(LAST_UPDATE));
		        	item.setMessage(jsonMapper.readValue((String)doc.get(MESSAGE), MessageDto.class));
		        	item.setOriginalId((String)doc.get(ORIGINAL_ID));
		        	item.setResourceUUID((String)doc.get(RESOURCE_UUID));
		        	item.setTitle((String)doc.get(TITLE));
		        	item.setTopic((String)doc.get(TOPIC));
		        	item.setTrust((String)doc.get(TRUST));
		        	return item;
		        } 
			}
		} catch (SolrServerException | IOException e) {
			log.error("Exception querying Solr", e);
			throw e;
		}
		return null;
	}




	@Override
	public List<NBEventQueryDto> findEventsByTopicAndPage(Context context, String topic, long offset, int pageSize) throws SolrServerException,
	    IOException, InvalidEnumeratedDataValueException {
    	SolrQuery solrQuery = new SolrQuery();
    	solrQuery.setStart(((Long)offset).intValue());
    	solrQuery.setRows(pageSize);
    	solrQuery.setQuery(TOPIC+":"+OpenstarSupportedTopic.restToSolr(topic));
    	QueryResponse response = getSolr().query(solrQuery);
		if(response != null) {
	        SolrDocumentList list = response.getResults();
	        List<NBEventQueryDto> responseItem = new ArrayList<>();
	        for(SolrDocument doc : list){
	        	NBEventQueryDto item = new NBEventQueryDto();
	        	item.setEventId((String)doc.get(EVENT_ID));
	        	item.setLastUpdate(Long.parseLong((String) doc.get(LAST_UPDATE)));
	        	item.setMessage(jsonMapper.readValue((String)doc.get(MESSAGE), MessageDto.class));
	        	item.setOriginalId((String)doc.get(ORIGINAL_ID));
	        	item.setResourceUUID((String)doc.get(RESOURCE_UUID));
	        	item.setTitle((String)doc.get(TITLE));
	        	item.setTopic((String)doc.get(TOPIC));
	        	item.setTrust((String)doc.get(TRUST));
	        	responseItem.add(item);
	        }
	        return responseItem;
		}
		return null;
	}

	@Override
	public Long countEventsByTopic(Context context, String topic) throws InvalidEnumeratedDataValueException, SolrServerException, IOException {
    	SolrQuery solrQuery = new SolrQuery();
    	solrQuery.setRows(0);
    	solrQuery.setQuery(TOPIC+":"+OpenstarSupportedTopic.restToSolr(topic));
        solrQuery.setFacet(true);
        solrQuery.setFacetMinCount(0);
        solrQuery.addFacetField(TOPIC);
    	QueryResponse response = getSolr().query(solrQuery);
        FacetField facetField = response.getFacetField(TOPIC);
        for(Count c : facetField.getValues()) {
        	if(c.getName().equals(OpenstarSupportedTopic.restToSolr(topic))) {
        		return c.getCount();
        	}
        }
        return 0L;
	}


	private String getResourceUUID(Context context, String originalId) throws SolrImportExportException {
		try {
			String id = getIdFromOriginalId(originalId);
			if(id != null) {
				Item item = (Item) handleService.resolveToObject(context, id);
				if(item != null) {
					return item.getID().toString();
				} else {
					throw new RuntimeException();
				}
			} else {
				throw new RuntimeException();
			}
		} catch (RuntimeException | SQLException e) {
			System.err.println("OriginalID not found");
			return UUID.randomUUID().toString();
//			throw new SolrImportExportException("Original ID is not valid: ");
		}
	}

	//oai:www.openstarts.units.it:10077/21486
	private String getIdFromOriginalId(String originalId) {
		Integer startPosition = originalId.lastIndexOf(':');
		if(startPosition != -1) {
			return originalId.substring(startPosition, originalId.length());
		} else {
			return null;
		}
	}

	private String generateChecksumFromToString(String dataToString) throws SolrImportExportException {
	    MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		    md.update(dataToString.getBytes());
		    byte[] digest = md.digest();
		    return DatatypeConverter.printHexBinary(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new SolrImportExportException("Cannot compute hashed key because \"MD5\" isn't available", e);
		}
	}

	private NBEventImportDto findEventByResourceUUID(String uuid) throws SolrServerException, IOException {
    	SolrQuery solrQuery = new SolrQuery();
    	solrQuery.setStart(0);
    	solrQuery.setRows(1);
    	solrQuery.setQuery(RESOURCE_UUID+":"+uuid);
    	QueryResponse response = getSolr().query(solrQuery);
		if(response != null) {
	        SolrDocumentList list = response.getResults();
	        List<NBEventQueryDto> responseItem = new ArrayList<>();
	        if(list.size() == 1) {
	        	SolrDocument doc = list.get(0);
	        	NBEventQueryDto item = new NBEventQueryDto();
	        	item.setEventId((String)doc.get(EVENT_ID));
	        	item.setLastUpdate(Long.parseLong((String) doc.get(LAST_UPDATE)));
	        	item.setMessage(jsonMapper.readValue((String)doc.get(MESSAGE), MessageDto.class));
	        	item.setOriginalId((String)doc.get(ORIGINAL_ID));
	        	item.setResourceUUID((String)doc.get(RESOURCE_UUID));
	        	item.setTitle((String)doc.get(TITLE));
	        	item.setTopic((String)doc.get(TOPIC));
	        	item.setTrust((String)doc.get(TRUST));
	        	return item;
	        }
		}
		return null;
	}
	
	
	public enum OpenstarSupportedTopic {
		ENRICH_MORE_ABSTRACT("ENRICH/MORE/ABSTRACT","ENRICH!MORE!ABSTRACT"),
		ENRICH_MISSING_ABSTRACT("ENRICH/MISSING/ABSTRACT","ENRICH!MISSING!ABSTRACT"),
		// no entry of this Topic in json
		//ENRICH_MORE_SUBJECT("ENRICH/MORE/SUBJECT","ENRICH!MORE!SUBJECT"),
		ENRICH_MISSING_SUBJECT_ACM("ENRICH/MISSING/SUBJECT/ACM","ENRICH!MISSING!SUBJECT!ACM"),
		ENRICH_MORE_PID("ENRICH/MORE/PID", "ENRICH!MORE!PID"),
		ENRICH_MISSING_PROJECT("ENRICH/MISSING/PROJECT","ENRICH!MISSING!PROJECT"),
		ENRICH_MISSING_PID("ENRICH/MISSING/PID","ENRICH!MISSING!PID");
		
		private String solrTopic;
		
		private String restTopic;
		
		private OpenstarSupportedTopic(String solrTopic, String restTopic) {
			this.solrTopic = solrTopic;
			this.restTopic = restTopic;
		}
		
		public static String authorizedTopic(String topic) throws SolrImportExportException {
			try {
				System.out.println("Topic: "+topic);
				return OpenstarSupportedTopic.solrToEnum(topic).getSolrTopic();
			} catch (InvalidEnumeratedDataValueException e) {
				throw new SolrImportExportException(e.getMessage());
			}
		}

		public String getSolrTopic() {
			return solrTopic;
		}
		
		public String getRestTopic() {
			return restTopic;
		}
		
		public static String restToSolr(String restTopic) throws InvalidEnumeratedDataValueException {
			return OpenstarSupportedTopic.restToEnum(restTopic).getSolrTopic();
		}
		
		public static String sorlToRest(String solrTopic) throws InvalidEnumeratedDataValueException {
			return OpenstarSupportedTopic.solrToEnum(solrTopic).getRestTopic();
		}
		
		public static OpenstarSupportedTopic restToEnum(String restTopic) throws InvalidEnumeratedDataValueException {
			switch(restTopic) {
			case "ENRICH!MORE!ABSTRACT":
				return OpenstarSupportedTopic.ENRICH_MORE_ABSTRACT;
			case "ENRICH!MISSING!ABSTRACT":
				return OpenstarSupportedTopic.ENRICH_MISSING_ABSTRACT;
			case "ENRICH!MISSING!SUBJECT!ACM":
				return OpenstarSupportedTopic.ENRICH_MISSING_SUBJECT_ACM;
			case "ENRICH!MORE!PID":
				return OpenstarSupportedTopic.ENRICH_MORE_PID;
			case "ENRICH!MISSING!PROJECT":
				return OpenstarSupportedTopic.ENRICH_MISSING_PROJECT;
			case "ENRICH!MISSING!PID":
				return OpenstarSupportedTopic.ENRICH_MISSING_PID;
			default:
				throw new InvalidEnumeratedDataValueException("Cannot map the input to any valid restTopic", "restTopic", restTopic);
			}
		}
			
		public static OpenstarSupportedTopic solrToEnum(String solrTopic) throws InvalidEnumeratedDataValueException {
			switch(solrTopic) {
			case "ENRICH/MORE/ABSTRACT":
				return OpenstarSupportedTopic.ENRICH_MORE_ABSTRACT;
			case "ENRICH/MISSING/ABSTRACT":
				return OpenstarSupportedTopic.ENRICH_MISSING_ABSTRACT;
			case "ENRICH/MISSING/SUBJECT!ACM":
				return OpenstarSupportedTopic.ENRICH_MISSING_SUBJECT_ACM;
			case "ENRICH/MORE/PID":
				return OpenstarSupportedTopic.ENRICH_MORE_PID;
    		case "ENRICH/MISSING/PROJECT":
				return OpenstarSupportedTopic.ENRICH_MISSING_PROJECT;
			case "ENRICH/MISSING/PID":
				return OpenstarSupportedTopic.ENRICH_MISSING_PID;
			default:
				throw new InvalidEnumeratedDataValueException("Cannot map the input to any valid solrTopic", "solrTopic", solrTopic);				
			}
		}
	}
}
