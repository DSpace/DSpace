/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.openstar.service.impl;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.deduplication.service.dto.OpenstarDto;
import org.dspace.app.exception.InvalidEnumeratedDataValueException;
import org.dspace.app.openstar.service.OpenstarService;
import org.dspace.content.service.ItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.SolrImportExportException;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;

public class OpenstarServiceImpl implements OpenstarService {

    private static final Logger log = Logger.getLogger(OpenstarServiceImpl.class);

    @Autowired(required = true)
    protected ConfigurationService configurationService;
    
    @Autowired(required = true)
    protected ItemService itemService;
    
    /**
     * Non-Static CommonsHttpSolrServer for processing indexing events.
     */
    protected SolrClient solr = null;

    private DSpace dspace = new DSpace();


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
	public void store(List<OpenstarDto> entries) throws SolrImportExportException, SolrServerException, IOException {
		UpdateRequest updateRequest = new UpdateRequest();
        for(OpenstarDto dto : entries) {
			SolrInputDocument doc = new SolrInputDocument();
			doc.addField(EVENT_ID, generateChecksumFromToString(dto.getHashString()));
	        doc.addField(ORIGINAL_ID, dto.getOriginalId());
	        doc.addField(TITLE, dto.getTitle());
	        doc.addField(TOPIC, OpenstarSupportedTopic.authorizedTopic(dto.getTopic()));
	        doc.addField(TRUST, dto.getTrust());
	        doc.addField(MESSAGE, dto.getMessage());
	        doc.addField(LAST_UPDATE, System.currentTimeMillis());
			updateRequest.add(doc);
		}
        updateRequest.process(getSolr()); 
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

	@Override
	public void query() {
		// TODO Auto-generated method stub
		
	}

	public void deleteById() {

	}
	
	public void deleteByOriginalId() {
		
	}

	@Override
	public void commit() {
		// TODO Auto-generated method stub
		
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
		
		public static Object authorizedTopic(String topic) throws SolrImportExportException {
			try {
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



	@Override
	public void delete() {
		// TODO Auto-generated method stub
		
	}
}
