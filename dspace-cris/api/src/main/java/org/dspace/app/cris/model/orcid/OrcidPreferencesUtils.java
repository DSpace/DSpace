/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.orcid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.configuration.RelationPreferenceConfiguration;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.RelationPreference;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.RelationPreferenceService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

import it.cilea.osd.jdyna.value.BooleanValue;

public class OrcidPreferencesUtils {

	private static final String PREFIX_ORCID_PROFILE_PREF = "orcid-profile-pref-";

	@Transient
	private static Logger log = Logger.getLogger(OrcidPreferencesUtils.class);

	private static final int ORCID_PREFS_DISABLED = 0;
	private static final int ORCID_PREFS_ALL = 1;
	private static final int ORCID_PREFS_SELECTED = 2;
	private static final int ORCID_PREFS_VISIBLE = 3;

	public static final String ORCID_MODE_UPDATE = "PUT";
	public static final String ORCID_MODE_APPEND = "POST";

	public void prepareOrcidQueue(String crisId, DSpaceObject obj) {

		List<OrcidHistory> orcidHistory = getApplicationService().findOrcidHistoryByEntityIdAndTypeId(obj.getID(),
				obj.getType());
		if (orcidHistory != null) {
			// PUT mode
			ResearcherPage rp = getApplicationService().getEntityByCrisId(crisId, ResearcherPage.class);
			RPPropertiesDefinition rpPDef = getApplicationService().findPropertiesDefinitionByShortName(
					RPPropertiesDefinition.class, "orcid-push-" + obj.getTypeText() + "-activate-put");
			if (rpPDef != null) {
				if (rp.getAnagrafica4view().get(rpPDef.getShortName()).isEmpty()) {
					RPProperty rpP = rp.createProprieta(rpPDef);
					BooleanValue value = new BooleanValue();
					value.setOggetto(true);
					rpP.setValue(value);
				}
				getApplicationService().saveOrUpdate(ResearcherPage.class, rp, false);
			}
			else {
				log.warn("Metadata Properties definition not found: orcid-push-" + obj.getTypeText() + "-activate-put");
			}
		} else {
			OrcidQueue orcidQueue = getApplicationService().uniqueOrcidQueueByEntityIdAndTypeIdAndOwnerId(obj.getID(),
					obj.getType(), crisId);
			if (orcidQueue == null) {
				orcidQueue = new OrcidQueue();
				orcidQueue.setEntityId(obj.getID());
				orcidQueue.setOwner(crisId);
				orcidQueue.setTypeId(obj.getType());
				// POST mode
				orcidQueue.setMode(ORCID_MODE_APPEND);
				getApplicationService().saveOrUpdate(OrcidQueue.class, orcidQueue);
			}
		}
	}

	public boolean isProfileSelectedToShare(ResearcherPage researcher) {
		String isEnableBioUpdate = ResearcherPageUtils.getStringValue(researcher,
				"system-orcid-token-orcid-bio-update");
		// this Researcher Profile have enabled the token for update bio on
		// Orcid Registry?
		if (StringUtils.isNotBlank(isEnableBioUpdate)) {
			// check if the Researcher have the preferences
			List<RPPropertiesDefinition> metadataDefinitions = getApplicationService()
					.likePropertiesDefinitionsByShortName(RPPropertiesDefinition.class, PREFIX_ORCID_PROFILE_PREF);
			for (RPPropertiesDefinition rppd : metadataDefinitions) {
				if ((researcher.getProprietaDellaTipologia(rppd) != null
						&& !researcher.getProprietaDellaTipologia(rppd).isEmpty()
						&& (Boolean) researcher.getProprietaDellaTipologia(rppd).get(0).getObject())) {
					return true;
				}
			}
		}
		return false;
	}

	public void prepareUpdateProfile(Map<String, Map<String, List<String>>> mapResearcherMetadataToSend,
			ResearcherPage researcher, boolean force) {
		List<RPPropertiesDefinition> metadataDefinitions = getApplicationService()
				.likePropertiesDefinitionsByShortName(RPPropertiesDefinition.class, PREFIX_ORCID_PROFILE_PREF);
		for (RPPropertiesDefinition rppd : metadataDefinitions) {
			String metadataShortnameINTERNAL = rppd.getShortName().replaceFirst(PREFIX_ORCID_PROFILE_PREF, "");
			String metadataShortnameORCID = rppd.getLabel();

			List<RPProperty> metadatas = researcher.getAnagrafica4view().get(metadataShortnameINTERNAL);

			Map<String, List<String>> mapMetadata = new HashMap<String, List<String>>();
			List<String> listMetadata = new ArrayList<String>();

			for (RPProperty metadata : metadatas) {
				if (force || (researcher.getProprietaDellaTipologia(rppd) != null
						&& !researcher.getProprietaDellaTipologia(rppd).isEmpty()
						&& (Boolean) researcher.getProprietaDellaTipologia(rppd).get(0).getObject())) {
					listMetadata.add(metadata.toString());
				}
			}

			if (!listMetadata.isEmpty()) {
				mapMetadata.put(metadataShortnameORCID, listMetadata);
			}

			mapResearcherMetadataToSend.put(researcher.getCrisID(), mapMetadata);

		}
	}

	public List<Integer> getPreferiteWorksToSendToOrcid(String crisID) {
		ResearcherPage researcher = getApplicationService().getEntityByCrisId(crisID, ResearcherPage.class);
		List<Integer> itemIDsToSend = new ArrayList<Integer>();
		if (researcher != null) {
			String publicationsPrefs = ResearcherPageUtils.getStringValue(researcher, "orcid-publications-prefs");

			if (publicationsPrefs != null) {
				if (Integer.parseInt(publicationsPrefs) != ORCID_PREFS_DISABLED) {
					if (Integer.parseInt(publicationsPrefs) == ORCID_PREFS_ALL) {
						log.debug("...it will work on all researcher...");
						SolrQuery query = new SolrQuery("*:*");
						query.addFilterQuery("{!field f=search.resourcetype}" + Constants.ITEM);
						query.addFilterQuery("author_authority:" + crisID);
						query.setFields("search.resourceid", "search.resourcetype");
						query.setRows(Integer.MAX_VALUE);
						try {
							QueryResponse response = getSearchService().search(query);
							SolrDocumentList docList = response.getResults();
							Iterator<SolrDocument> solrDoc = docList.iterator();
							while (solrDoc.hasNext()) {
								SolrDocument doc = solrDoc.next();
								Integer rpId = (Integer) doc.getFirstValue("search.resourceid");
								itemIDsToSend.add(rpId);
							}
						} catch (SearchServiceException e) {
							log.error("Error retrieving documents", e);
						}
					} else {
						if (Integer.parseInt(publicationsPrefs) == ORCID_PREFS_SELECTED) {
							List<RelationPreference> selected = new ArrayList<RelationPreference>();
							for (RelationPreferenceConfiguration configuration : getRelationPreferenceService()
									.getConfigurationService().getList()) {
								if (configuration.getRelationConfiguration().getRelationClass().equals(Item.class)) {
									selected = getRelationPreferenceService()
											.findRelationsPreferencesByUUIDByRelTypeAndStatus(researcher.getUuid(),
													configuration.getRelationConfiguration().getRelationName(),
													RelationPreference.SELECTED);
								}
								for (RelationPreference sel : selected) {
									itemIDsToSend.add(sel.getItemID());
								}
							}

						} else {
							if (Integer.parseInt(publicationsPrefs) == ORCID_PREFS_VISIBLE) {
								List<Integer> itemIDsToSendTmp = new ArrayList<Integer>();
								log.debug("...it will work on all researcher...");
								SolrQuery query = new SolrQuery("*:*");
								query.addFilterQuery("{!field f=search.resourcetype}" + Constants.ITEM);
								query.addFilterQuery("author_authority:" + crisID);
								query.setFields("search.resourceid", "search.resourcetype");
								query.setRows(Integer.MAX_VALUE);
								try {
									QueryResponse response = getSearchService().search(query);
									SolrDocumentList docList = response.getResults();
									Iterator<SolrDocument> solrDoc = docList.iterator();
									while (solrDoc.hasNext()) {
										SolrDocument doc = solrDoc.next();
										Integer rpId = (Integer) doc.getFirstValue("search.resourceid");
										itemIDsToSendTmp.add(rpId);
									}
								} catch (SearchServiceException e) {
									log.error("Error retrieving documents", e);
								}
								List<RelationPreference> hided = new ArrayList<RelationPreference>();
								for (RelationPreferenceConfiguration configuration : getRelationPreferenceService()
										.getConfigurationService().getList()) {
									if (configuration.getRelationConfiguration().getRelationClass()
											.equals(Item.class)) {
										hided = getRelationPreferenceService()
												.findRelationsPreferencesByUUIDByRelTypeAndStatus(researcher.getUuid(),
														configuration.getRelationConfiguration().getRelationName(),
														RelationPreference.HIDED);
									}
									for (Integer itemId : itemIDsToSendTmp) {
										boolean founded = false;
										internal: for (RelationPreference hid : hided) {
											if (hid.getItemID().equals(itemId)) {
												founded = true;
											}
											if (founded) {
												break internal;
											}
										}
										if (!founded) {
											itemIDsToSend.add(itemId);
										}
									}
								}
							} else {
								log.warn(crisID + " - publications preferences NOT recognized");
							}
						}
					}
				} else {
					log.debug(crisID + " - DISABLED publications preferences");
				}
			}
		}
		return itemIDsToSend;
	}

	public boolean isAPreferiteToSendToOrcid(String crisID, DSpaceObject dso, String preferenceMetadataDefinition) {
		ResearcherPage researcher = getApplicationService().getEntityByCrisId(crisID, ResearcherPage.class);
		if (researcher != null) {
			String publicationsPrefs = ResearcherPageUtils.getStringValue(researcher, preferenceMetadataDefinition);

			if (publicationsPrefs != null) {
				if (Integer.parseInt(publicationsPrefs) == ORCID_PREFS_DISABLED) {
					return false;
				}

				if (Integer.parseInt(publicationsPrefs) == ORCID_PREFS_ALL) {
					return true;
				} else {
					if (Integer.parseInt(publicationsPrefs) == ORCID_PREFS_SELECTED) {
						List<RelationPreference> selected = new ArrayList<RelationPreference>();
						for (RelationPreferenceConfiguration configuration : getRelationPreferenceService()
								.getConfigurationService().getList()) {
							if (configuration.getRelationConfiguration().getRelationClass().equals(Item.class)) {
								selected = getRelationPreferenceService().findRelationsPreferencesByUUIDByRelTypeAndStatus(
										researcher.getUuid(),
										configuration.getRelationConfiguration().getRelationName(),
										RelationPreference.SELECTED);
							}
							for (RelationPreference sel : selected) {
								if (sel.getItemID() == dso.getID()) {
									return true;
								}
							}
						}

					} else {
						if (Integer.parseInt(publicationsPrefs) == ORCID_PREFS_VISIBLE) {

							List<RelationPreference> hided = new ArrayList<RelationPreference>();
							for (RelationPreferenceConfiguration configuration : getRelationPreferenceService()
									.getConfigurationService().getList()) {
								if (configuration.getRelationConfiguration().getRelationClass().equals(Item.class)) {
									hided = getRelationPreferenceService().findRelationsPreferencesByUUIDByRelTypeAndStatus(
											researcher.getUuid(),
											configuration.getRelationConfiguration().getRelationName(),
											RelationPreference.HIDED);
								}

								for (RelationPreference hid : hided) {
									if (hid.getItemID() == dso.getID()) {
										return false;
									}
								}
								return true;
							}

						} else {
							log.warn(crisID + " - " + preferenceMetadataDefinition + " - preferences NOT recognized");
						}
					}
				}
			} else {
				log.debug(crisID + " - " + preferenceMetadataDefinition + " -  DISABLED entity preferences");
			}

		}
		return false;

	}

	public List<Integer> getPreferiteFundingToSendToOrcid(String crisID) {
		ResearcherPage researcher = getApplicationService().getEntityByCrisId(crisID, ResearcherPage.class);
		List<Integer> projectsIDsToSend = new ArrayList<Integer>();
		if (researcher != null) {
			String projectsPrefs = ResearcherPageUtils.getStringValue(researcher, "orcid-projects-prefs");
			if (StringUtils.isNotBlank(projectsPrefs)) {
				if (Integer.parseInt(projectsPrefs) != ORCID_PREFS_DISABLED) {
					if (Integer.parseInt(projectsPrefs) == ORCID_PREFS_ALL) {
						log.info("...it will work on all researcher...");
						SolrQuery query = new SolrQuery("*:*");
						query.addFilterQuery("{!field f=search.resourcetype}" + CrisConstants.PROJECT_TYPE_ID);
						query.addFilterQuery("projectinvestigators_authority:" + crisID);
						query.setFields("search.resourceid", "search.resourcetype");
						query.setRows(Integer.MAX_VALUE);
						try {
							QueryResponse response = getSearchService().search(query);
							SolrDocumentList docList = response.getResults();
							Iterator<SolrDocument> solrDoc = docList.iterator();
							while (solrDoc.hasNext()) {
								SolrDocument doc = solrDoc.next();
								Integer rpId = (Integer) doc.getFirstValue("search.resourceid");
								projectsIDsToSend.add(rpId);
							}
						} catch (SearchServiceException e) {
							log.error("Error retrieving documents", e);
						}
					} else {
						if (Integer.parseInt(projectsPrefs) == ORCID_PREFS_SELECTED) {
							List<RelationPreference> selected = new ArrayList<RelationPreference>();
							for (RelationPreferenceConfiguration configuration : getRelationPreferenceService()
									.getConfigurationService().getList()) {
								if (configuration.getRelationConfiguration().getRelationClass().equals(Project.class)) {
									selected = getRelationPreferenceService()
											.findRelationsPreferencesByUUIDByRelTypeAndStatus(researcher.getUuid(),
													configuration.getRelationConfiguration().getRelationName(),
													RelationPreference.SELECTED);
								}
								for (RelationPreference sel : selected) {
									projectsIDsToSend.add(sel.getItemID());
								}
							}

						} else {
							if (Integer.parseInt(projectsPrefs) == ORCID_PREFS_VISIBLE) {
								List<Integer> projectsIDsToSendTmp = new ArrayList<Integer>();
								log.info("...it will work on all researcher...");
								SolrQuery query = new SolrQuery("*:*");
								query.addFilterQuery("{!field f=search.resourcetype}" + CrisConstants.PROJECT_TYPE_ID);
								query.addFilterQuery("projectinvestigators_authority:" + crisID);
								query.setFields("search.resourceid", "search.resourcetype");
								query.setRows(Integer.MAX_VALUE);
								try {
									QueryResponse response = getSearchService().search(query);
									SolrDocumentList docList = response.getResults();
									Iterator<SolrDocument> solrDoc = docList.iterator();
									while (solrDoc.hasNext()) {
										SolrDocument doc = solrDoc.next();
										Integer rpId = (Integer) doc.getFirstValue("search.resourceid");
										projectsIDsToSendTmp.add(rpId);
									}
								} catch (SearchServiceException e) {
									log.error("Error retrieving documents", e);
								}
								List<RelationPreference> hided = new ArrayList<RelationPreference>();
								for (RelationPreferenceConfiguration configuration : getRelationPreferenceService()
										.getConfigurationService().getList()) {
									if (configuration.getRelationConfiguration().getRelationClass()
											.equals(Project.class)) {
										hided = getRelationPreferenceService()
												.findRelationsPreferencesByUUIDByRelTypeAndStatus(researcher.getUuid(),
														configuration.getRelationConfiguration().getRelationName(),
														RelationPreference.HIDED);
									}
									for (Integer itemId : projectsIDsToSendTmp) {
										boolean founded = false;
										internal: for (RelationPreference hid : hided) {
											if (hid.getItemID().equals(itemId)) {
												founded = true;
											}
											if (founded) {
												break internal;
											}
										}
										if (!founded) {
											projectsIDsToSend.add(itemId);
										}
									}
								}
							} else {
								log.warn(crisID + " - projects preferences NOT recognized");
							}
						}
					}
				} else {
					log.info(crisID + " - DISABLED projects preferences");
				}
			}
		}
		return projectsIDsToSend;
	}

	
	public CrisSearchService getSearchService()
    {
        return new DSpace().getServiceManager().getServiceByName(
                "org.dspace.discovery.SearchService", CrisSearchService.class);
    }
	public RelationPreferenceService getRelationPreferenceService() {
        return new DSpace().getServiceManager().getServiceByName(
        		"org.dspace.app.cris.service.RelationPreferenceService",
                RelationPreferenceService.class);
	}
	
	public ApplicationService getApplicationService()
    {
        return new DSpace().getServiceManager().getServiceByName(
                "applicationService", ApplicationService.class);
    }

}
