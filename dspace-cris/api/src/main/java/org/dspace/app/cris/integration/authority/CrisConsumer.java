/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.authority;

import it.cilea.osd.jdyna.utils.HashUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.integration.CRISAuthority;
import org.dspace.app.cris.integration.RPAuthority;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.AuthorityValueGenerator;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.utils.DSpace;

/**
 */
public class CrisConsumer implements Consumer {

	private static final String SOURCE_INTERNAL = "INTERNAL-SUBMISSION";

	private static final Logger log = Logger.getLogger(CrisConsumer.class);

	private SearchService searcher = new DSpace().getServiceManager().getServiceByName(SearchService.class.getName(),
			SearchService.class);

	private ApplicationService applicationService = new DSpace().getServiceManager().getServiceByName(
			"applicationService", ApplicationService.class);

	private boolean activateNewObject = false;
	public void initialize() throws Exception {
		activateNewObject = ConfigurationManager.getBooleanProperty("cris.activate.import.in.submission", false);
	}

	public void consume(Context ctx, Event event) throws Exception {
		DSpaceObject dso = event.getSubject(ctx);
		if (dso instanceof Item) {
			Item item = (Item) dso;

			Set<String> listAuthoritiesManager = ChoiceAuthorityManager.getManager().getAuthorities();

			Map<String, List<DCValue>> toBuild = new HashMap<String, List<DCValue>>();
			Map<String, String> toBuildType = new HashMap<String, String>();
			Map<String, CRISAuthority> toBuildChoice = new HashMap<String, CRISAuthority>();
			
			for (String crisAuthority : listAuthoritiesManager) {
				List<String> listMetadata = ChoiceAuthorityManager.getManager().getAuthorityMetadataForAuthority(
						crisAuthority);

				for (String metadata : listMetadata) {
					DCValue[] dcvalues = item.getMetadata(metadata);
					ChoiceAuthority choiceAuthority = ChoiceAuthorityManager.getManager().getChoiceAuthority(metadata);
					if (CRISAuthority.class.isAssignableFrom(choiceAuthority.getClass())) {

						for (DCValue dcval : dcvalues) {
							String authority = dcval.authority;
							if (StringUtils.isNotBlank(authority)) {
								if (authority.startsWith(AuthorityValueGenerator.GENERATE)) {

									String[] split = StringUtils.split(authority, AuthorityValueGenerator.SPLIT);
									String type = null, info = null;
									if (split.length > 0) {
										type = split[1];
										if (split.length > 1) {
											info = split[2];
										}
									}

									toBuildType.put(info, type);

									List<DCValue> list = new ArrayList<DCValue>();
									if (toBuild.containsKey(info)) {
										list = toBuild.get(info);
										list.add(dcval);
									} else {
										list.add(dcval);
									}
									toBuild.put(info, list);							
									toBuildChoice.put(info, (CRISAuthority)choiceAuthority);
								}								
							}
							else {
								String valueHashed = HashUtil.hashMD5(dcval.value);
								List<DCValue> list = new ArrayList<DCValue>();
								if (toBuild.containsKey(valueHashed)) {
									list = toBuild.get(valueHashed);
									list.add(dcval);
								} else {
									list.add(dcval);
								}
								toBuild.put(valueHashed, list);
								toBuildType.put(valueHashed, SOURCE_INTERNAL);
								toBuildChoice.put(valueHashed, (CRISAuthority)choiceAuthority);
							}
							
						}
					}
				}
			}
			
			Map<String, String> toBuildAuthority = new HashMap<String, String>();

			for (String authorityKey : toBuild.keySet()) {

				String rpKey = null;

				CRISAuthority choiceAuthorityObject = toBuildChoice.get(authorityKey);
				String typeAuthority = toBuildType.get(authorityKey);
				
				Class<ACrisObject> crisTargetClass = choiceAuthorityObject.getCRISTargetClass();				
				ACrisObject rp = applicationService.getEntityBySourceId(typeAuthority.toUpperCase(), authorityKey,
						crisTargetClass);

				if (rp != null) {
					rpKey = rp.getCrisID();
				} else {
					// build a simple RP
					rp = choiceAuthorityObject.getNewCrisObject();
					
					SolrQuery query = new SolrQuery();
					
					if(choiceAuthorityObject.getCRISTargetTypeID()==-1) {
						query.setQuery("search.resourcetype:[1001 TO 9999]");
					}
					else {
						query.setQuery("search.resourcetype:"+ choiceAuthorityObject.getCRISTargetTypeID());
					}
					
					if (StringUtils.isNotBlank(authorityKey)) {						
						if ((typeAuthority.equalsIgnoreCase(SOURCE_INTERNAL))) {							
							query.addFilterQuery("cris-sourceref:"+ typeAuthority.toLowerCase());
							query.addFilterQuery("cris-sourceid:"+ authorityKey);
						}
						else {
							String filterQuery = "cris" + rp.getPublicPath() + "."
									+ typeAuthority.toLowerCase() + ":\"" + authorityKey + "\"";
							query.addFilterQuery(filterQuery);
						}
					}
					
					QueryResponse qResp = searcher.search(query);
					SolrDocumentList docList = qResp.getResults();
					if (docList.size() > 1) {
						SolrDocument doc = docList.get(0);
						rpKey = (String) doc.getFirstValue("cris"+rp.getPublicPath()+".this_authority");
					}

					if (rpKey == null) {

						rp.setSourceID(authorityKey);
						rp.setSourceRef(typeAuthority.toUpperCase());
						
						List<DCValue> dcvalueAuthority = toBuild.get(authorityKey);
						
						ResearcherPageUtils.buildTextValue(rp, dcvalueAuthority.get(0).value, rp.getMetadataFieldTitle());
						// ResearcherPageUtils.buildTextValue(rp, email,
						// "email");
						if(!(typeAuthority.equalsIgnoreCase(SOURCE_INTERNAL))) {
							ResearcherPageUtils.buildTextValue(rp, authorityKey, typeAuthority.toLowerCase());	
						}						

						if(activateNewObject) {
							rp.setStatus(true);
						}
						applicationService.saveOrUpdate(crisTargetClass, rp);
						rpKey = rp.getCrisID();
					}

				}
				
				if (StringUtils.isNotBlank(rpKey)) {
					toBuildAuthority.put(authorityKey, rpKey);
				}
			}		
			
			for (String orcid : toBuildAuthority.keySet()) {
				for (DCValue dcvalue : toBuild.get(orcid)) {
					DCValue newValue = dcvalue.copy();
					newValue.authority = toBuildAuthority.get(orcid);
					newValue.confidence = Choices.CF_ACCEPTED;
					item.replaceMetadataValue(dcvalue, newValue);
					item.update();
				}
			}
		}
	}

	public void end(Context ctx) throws Exception {
		// nothing to do
	}

	public void finish(Context ctx) throws Exception {
		// nothing to do
	}
}