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
import org.dspace.app.cris.integration.PushToORCID;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.RelationPreference;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.VisibilityConstants;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.RelationPreferenceService;
import org.dspace.app.cris.util.Researcher;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.authority.orcid.jaxb.Address;
import org.dspace.authority.orcid.jaxb.Biography;
import org.dspace.authority.orcid.jaxb.ContactDetails;
import org.dspace.authority.orcid.jaxb.Country;
import org.dspace.authority.orcid.jaxb.CreditName;
import org.dspace.authority.orcid.jaxb.Email;
import org.dspace.authority.orcid.jaxb.ExternalIdentifier;
import org.dspace.authority.orcid.jaxb.ExternalIdentifiers;
import org.dspace.authority.orcid.jaxb.Keyword;
import org.dspace.authority.orcid.jaxb.Keywords;
import org.dspace.authority.orcid.jaxb.OrcidBio;
import org.dspace.authority.orcid.jaxb.OrcidProfile;
import org.dspace.authority.orcid.jaxb.OtherNames;
import org.dspace.authority.orcid.jaxb.PersonalDetails;
import org.dspace.authority.orcid.jaxb.ResearcherUrl;
import org.dspace.authority.orcid.jaxb.ResearcherUrls;
import org.dspace.authority.orcid.jaxb.Visibility;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

import it.cilea.osd.jdyna.value.BooleanValue;

public class OrcidPreferencesUtils {
	public static final String[] ORCID_RESEARCHER_ATTRIBUTES = new String[] { "primary-email", "name", "other-names", "other-emails",
			"iso-3166-country", "keywords", "biography", "credit-name" };
	
	public static final String ORCID_PUBLICATIONS_PREFS = "orcid-publications-prefs";
	public static final String ORCID_PROJECTS_PREFS = "orcid-projects-prefs";

	public static final String ORCID_PUSH_ITEM_ACTIVATE_PUT = "orcid-push-item-activate-put";

	public static final String ORCID_PUSH_CRISPJ_ACTIVATE_PUT = "orcid-push-crispj-activate-put";

	public static final String ORCID_PUSH_CRISRP_ACTIVATE_PUT = "orcid-push-crisrp-activate-put";

	public static final String PREFIX_ORCID_PROFILE_PREF = "orcid-profile-pref-";

	@Transient
	private static Logger log = Logger.getLogger(OrcidPreferencesUtils.class);

	private static final int ORCID_PREFS_DISABLED = 0;
	private static final int ORCID_PREFS_ALL = 1;
	private static final int ORCID_PREFS_SELECTED = 2;
	private static final int ORCID_PREFS_VISIBLE = 3;

	public void prepareOrcidQueue(String crisId, DSpaceObject obj) {

		OrcidHistory orcidHistory = getApplicationService()
				.uniqueOrcidHistoryInSuccessByOwnerAndEntityIdAndTypeId(crisId, obj.getID(), obj.getType());
		if (orcidHistory != null && obj.getType() != 9) {
			// PUT mode (we have object already send but now is not selected)
			ResearcherPage rp = getApplicationService().getEntityByCrisId(crisId, ResearcherPage.class);
			notifyPut(rp, "orcid-push-" + obj.getTypeText() + "-activate-put");
		} else {
			OrcidQueue orcidQueue = getApplicationService().uniqueOrcidQueueByEntityIdAndTypeIdAndOwnerId(obj.getID(),
					obj.getType(), crisId);
			if (orcidQueue == null) {
				orcidQueue = new OrcidQueue();
				orcidQueue.setEntityId(obj.getID());
				orcidQueue.setOwner(crisId);
				orcidQueue.setTypeId(obj.getType());
				orcidQueue.setFastlookupObjectName(obj.getName());
				orcidQueue.setFastlookupUuid(obj.getHandle());
				// POST mode
				orcidQueue.setMode(OrcidService.ORCID_MODE_APPEND);
				getApplicationService().saveOrUpdate(OrcidQueue.class, orcidQueue);
			}
		}
	}

	public boolean isProfileSelectedToShare(ResearcherPage researcher) {
		if (isTokenReleasedForSync(researcher, "system-orcid-token-orcid-bio-update")) {

			Map<String, List<String>> oldMapOrcidProfilePreference = researcher.getOldMapOrcidProfilePreference();

			// if metadata set to go on Orcid Registry have modifications return
			// true
			List<RPPropertiesDefinition> metadataDefinitions = getApplicationService()
					.likePropertiesDefinitionsByShortName(RPPropertiesDefinition.class, PREFIX_ORCID_PROFILE_PREF);
			for (RPPropertiesDefinition rppd : metadataDefinitions) {
				String metadataShortnameINTERNAL = rppd.getShortName().replaceFirst(PREFIX_ORCID_PROFILE_PREF, "");

				List<RPProperty> propsRps = researcher.getAnagrafica4view().get(rppd.getShortName());

				for (RPProperty prop : propsRps) {
					BooleanValue booleanValue = (BooleanValue) (prop.getValue());
					if (booleanValue.getObject()) {
						if (!researcher.getOldOrcidProfilePreference().contains(metadataShortnameINTERNAL)) {
							return true;
						}
					}
				}

				List<String> rpPropValues = new ArrayList<String>();
				if (oldMapOrcidProfilePreference.containsKey(metadataShortnameINTERNAL)) {
					rpPropValues = oldMapOrcidProfilePreference.get(metadataShortnameINTERNAL);
				}

				List<RPProperty> propsFoundedRps = researcher.getAnagrafica4view().get(metadataShortnameINTERNAL);
				boolean founded = false;
				for (String rpPropValue : rpPropValues) {
					for (RPProperty propFoundedRp : propsFoundedRps) {
						if (rpPropValue.equals(propFoundedRp.toString())) {
							founded = true;
						}
					}
				}
				if (!founded && propsFoundedRps != null && !propsFoundedRps.isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isTokenReleasedForSync(ResearcherPage researcher, String tokenName) {
		String isEnableBioUpdate = ResearcherPageUtils.getStringValue(researcher, tokenName);
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
			String publicationsPrefs = ResearcherPageUtils.getStringValue(researcher, ORCID_PUBLICATIONS_PREFS);

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
							if (dso.getType() == Constants.ITEM && configuration.getRelationConfiguration().getRelationClass().equals(Item.class)) {
								selected = getRelationPreferenceService()
										.findRelationsPreferencesByUUIDByRelTypeAndStatus(researcher.getUuid(),
												configuration.getRelationConfiguration().getRelationName(),
												RelationPreference.SELECTED);
							}
							else {
								if (dso.getType() == CrisConstants.PROJECT_TYPE_ID && configuration.getRelationConfiguration().getRelationClass().equals(Project.class)) {
									selected = getRelationPreferenceService()
											.findRelationsPreferencesByUUIDByRelTypeAndStatus(researcher.getUuid(),
													configuration.getRelationConfiguration().getRelationName(),
													RelationPreference.SELECTED);
								}	
							}
							for (RelationPreference sel : selected) {
								if (dso.getType() == Constants.ITEM) {
									if (sel.getItemID() == dso.getID()) {
										return true;
									}
								} else {
									if (sel.getTargetUUID() == dso.getHandle()) {
										return true;
									}
								}
							}
						}

					} else {
						if (Integer.parseInt(publicationsPrefs) == ORCID_PREFS_VISIBLE) {

							List<RelationPreference> hided = new ArrayList<RelationPreference>();
							for (RelationPreferenceConfiguration configuration : getRelationPreferenceService()
									.getConfigurationService().getList()) {
								if (dso.getType() == Constants.ITEM && configuration.getRelationConfiguration().getRelationClass().equals(Item.class)) {
									hided = getRelationPreferenceService()
											.findRelationsPreferencesByUUIDByRelTypeAndStatus(researcher.getUuid(),
													configuration.getRelationConfiguration().getRelationName(),
													RelationPreference.HIDED);
								}
								else {
									if (dso.getType() == CrisConstants.PROJECT_TYPE_ID && configuration.getRelationConfiguration().getRelationClass().equals(Project.class)) {
										hided = getRelationPreferenceService()
												.findRelationsPreferencesByUUIDByRelTypeAndStatus(researcher.getUuid(),
														configuration.getRelationConfiguration().getRelationName(),
														RelationPreference.HIDED);
									}	
								}
								for (RelationPreference hid : hided) {
									if (dso.getType() == Constants.ITEM) {
										if (hid.getItemID() == dso.getID()) {
											return false;
										}

									} else {
										if (hid.getTargetUUID() == dso.getHandle()) {
											return false;
										}
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

	public List<String> getPreferiteFundingToSendToOrcid(String crisID) {
		ResearcherPage researcher = getApplicationService().getEntityByCrisId(crisID, ResearcherPage.class);
		List<String> projectsIDsToSend = new ArrayList<String>();
		if (researcher != null) {
			String projectsPrefs = ResearcherPageUtils.getStringValue(researcher, ORCID_PROJECTS_PREFS);
			if (StringUtils.isNotBlank(projectsPrefs)) {
				if (Integer.parseInt(projectsPrefs) != ORCID_PREFS_DISABLED) {
					if (Integer.parseInt(projectsPrefs) == ORCID_PREFS_ALL) {
						log.info("...it will work on all researcher...");
						SolrQuery query = new SolrQuery("*:*");
						query.addFilterQuery("{!field f=search.resourcetype}" + CrisConstants.PROJECT_TYPE_ID);
						query.addFilterQuery("projectinvestigators_authority:" + crisID);
						query.setFields("search.resourceid", "search.resourcetype", "cris-uuid");
						query.setRows(Integer.MAX_VALUE);
						try {
							QueryResponse response = getSearchService().search(query);
							SolrDocumentList docList = response.getResults();
							Iterator<SolrDocument> solrDoc = docList.iterator();
							while (solrDoc.hasNext()) {
								SolrDocument doc = solrDoc.next();
								String rpId = (String) doc.getFirstValue("cris-uuid");
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
									projectsIDsToSend.add(sel.getTargetUUID());
								}
							}

						} else {
							if (Integer.parseInt(projectsPrefs) == ORCID_PREFS_VISIBLE) {
								List<String> projectsIDsToSendTmp = new ArrayList<String>();
								log.info("...it will work on all researcher...");
								SolrQuery query = new SolrQuery("*:*");
								query.addFilterQuery("{!field f=search.resourcetype}" + CrisConstants.PROJECT_TYPE_ID);
								query.addFilterQuery("projectinvestigators_authority:" + crisID);
								query.setFields("search.resourceid", "search.resourcetype", "cris-uuid");
								query.setRows(Integer.MAX_VALUE);
								try {
									QueryResponse response = getSearchService().search(query);
									SolrDocumentList docList = response.getResults();
									Iterator<SolrDocument> solrDoc = docList.iterator();
									while (solrDoc.hasNext()) {
										SolrDocument doc = solrDoc.next();
										String rpId = (String) doc.getFirstValue("cris-uuid");
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
									for (String itemId : projectsIDsToSendTmp) {
										boolean founded = false;
										internal: for (RelationPreference hid : hided) {
											if (hid.getTargetUUID().equals(itemId)) {
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

	public CrisSearchService getSearchService() {
		return new DSpace().getServiceManager().getServiceByName("org.dspace.discovery.SearchService",
				CrisSearchService.class);
	}

	public RelationPreferenceService getRelationPreferenceService() {
		return new DSpace().getServiceManager().getServiceByName(
				"org.dspace.app.cris.service.RelationPreferenceService", RelationPreferenceService.class);
	}

	public ApplicationService getApplicationService() {
		return new DSpace().getServiceManager().getServiceByName("applicationService", ApplicationService.class);
	}

	public void deleteOrcidQueueByOwnerAndType(String crisID, int typeId) {
		getApplicationService().deleteOrcidQueueByOwnerAndTypeId(crisID, typeId);
	}

	public boolean postOrcidProfile(String owner, String uuId) {
		return PushToORCID.sendOrcidProfile(getApplicationService(), owner, uuId);
	}

	public boolean postOrcidFunding(String owner, String uuId) {
		return PushToORCID.sendOrcidFunding(getApplicationService(), owner, uuId);
	}

	public boolean postOrcidWork(String owner, String uuId) {
		return PushToORCID.sendOrcidWork(getApplicationService(), owner, uuId);
	}

	public boolean putOrcidProfiles(String owner) {
		ResearcherPage rp = getApplicationService().getEntityByCrisId(owner, ResearcherPage.class);
		cleanPut(rp, ORCID_PUSH_CRISRP_ACTIVATE_PUT);
		return PushToORCID.putOrcidProfile(getApplicationService(), owner);
	}

	public boolean putOrcidFundings(String owner) {
		ResearcherPage rp = getApplicationService().getEntityByCrisId(owner, ResearcherPage.class);
		cleanPut(rp, ORCID_PUSH_CRISPJ_ACTIVATE_PUT);
		// retrieve the preferite
		List<String> pjIDs = getPreferiteFundingToSendToOrcid(owner);
		List<OrcidHistory> orcidHistories = getOrcidHistoryInSuccessByOwnerAndTypeId(owner,
				CrisConstants.PROJECT_TYPE_ID);
		Map<Integer, String> putProjectIDs = new HashMap<Integer, String>();
		for (OrcidHistory history : orcidHistories) {
			if (pjIDs.contains(history.getEntityUuid())) {
				// PUT
				pjIDs.remove(history.getEntityId());
				putProjectIDs.put(history.getEntityId(), history.getPutCode());
			} else {
				// REMOVE
				getApplicationService().delete(OrcidHistory.class, history.getId());
			}
		}
		Context context = null;
		try {
			context = new Context();
			for (String itemID : pjIDs) {
				Project project = (Project)getApplicationService().getEntityByUUID(itemID);
				prepareOrcidQueue(owner, project);
			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		} finally {
			if (context != null && context.isValid()) {
				context.abort();
			}
		}
		return PushToORCID.putOrcidFundings(getApplicationService(), owner, putProjectIDs);
	}

	public boolean putOrcidWorks(String owner) {
		ResearcherPage rp = getApplicationService().getEntityByCrisId(owner, ResearcherPage.class);
		cleanPut(rp, ORCID_PUSH_ITEM_ACTIVATE_PUT);
		// retrieve the preferite
		List<Integer> itemIDs = getPreferiteWorksToSendToOrcid(owner);
		List<OrcidHistory> orcidHistories = getOrcidHistoryInSuccessByOwnerAndTypeId(owner, Constants.ITEM);
		Map<Integer, String> putItemIDs = new HashMap<Integer, String>();
		for (OrcidHistory history : orcidHistories) {
			if (itemIDs.contains(history.getEntityId())) {
				// PUT
				itemIDs.remove(history.getEntityId());
				putItemIDs.put(history.getEntityId(), history.getPutCode());
			} else {
				// REMOVE
				getApplicationService().delete(OrcidHistory.class, history.getId());
			}
		}
		Context context = null;
		try {
			context = new Context();
			for (Integer itemID : itemIDs) {
				Item item = Item.find(context, itemID);
				prepareOrcidQueue(owner, item);
			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		} finally {
			if (context != null && context.isValid()) {
				context.abort();
			}
		}
		return PushToORCID.putOrcidWorks(getApplicationService(), owner, putItemIDs);
	}

	public void cleanPut(ResearcherPage rp, String metadata) {
		RPPropertiesDefinition rpPDef = getApplicationService()
				.findPropertiesDefinitionByShortName(RPPropertiesDefinition.class, metadata);
		if (rpPDef != null) {
			List<RPProperty> rpprops = rp.getProprietaDellaTipologia(rpPDef);
			int propDaEliminare = rpprops.size();

			for (int i = 0; i < propDaEliminare; i++) {
				// devo eliminare sempre l'ultima proprieta' perche' la lista e'
				// una referenza
				// alla lista mantenuta dalla cache a4v che viene alterata dal
				// removeProprieta
				rp.removeProprieta(rpprops.get(rpprops.size() - 1));
			}
			getApplicationService().saveOrUpdate(ResearcherPage.class, rp, false);
		} else {
			log.warn("Metadata Properties definition not found:" + metadata);
		}
	}

	public void notifyPut(ResearcherPage rp, String metadata) {
		RPPropertiesDefinition rpPDef = getApplicationService()
				.findPropertiesDefinitionByShortName(RPPropertiesDefinition.class, metadata);
		if (rpPDef != null) {

			List<RPProperty> rpprops = rp.getProprietaDellaTipologia(rpPDef);
			if (rpprops != null && !rpprops.isEmpty()) {
				for (RPProperty rpprop : rpprops) {
					BooleanValue bval = (BooleanValue) (rpprop.getValue());
					bval.setOggetto(true);
				}
			} else {
				RPProperty rpP = rp.createProprieta(rpPDef);
				BooleanValue value = new BooleanValue();
				value.setOggetto(true);
				rpP.setValue(value);
				rpP.setVisibility(0);

			}

		} else {
			log.warn("Metadata Properties definition not found:" + metadata);
		}
	}

	public List<OrcidHistory> getOrcidHistoryInSuccessByOwner(String crisID) {
		return getApplicationService().findOrcidHistoryByOwnerAndSuccess(crisID);
	}

	public List<OrcidHistory> getOrcidHistoryInSuccessByOwnerAndTypeId(String crisID, int type) {
		return getApplicationService().findOrcidHistoryInSuccessByOwnerAndType(crisID, type);
	}

	public static boolean populateRP(ResearcherPage crisObject, String ORCID) {
		return populateRP(crisObject, ORCID, null);
	}
	public static boolean populateRP(ResearcherPage crisObject, String ORCID, String token) {
		List<RPPropertiesDefinition> metadataDefinitions = new Researcher().getApplicationService()
				.likePropertiesDefinitionsByShortName(RPPropertiesDefinition.class, OrcidPreferencesUtils.PREFIX_ORCID_PROFILE_PREF);
		Map<String, String> mapMetadata = new HashMap<String, String>();
		for (RPPropertiesDefinition rppd : metadataDefinitions) {
			String metadataShortnameINTERNAL = rppd.getShortName().replaceFirst(OrcidPreferencesUtils.PREFIX_ORCID_PROFILE_PREF, "");
			String metadataShortnameORCID = rppd.getLabel();
			mapMetadata.put(metadataShortnameORCID, metadataShortnameINTERNAL);
		}
		
		OrcidService orcidService = OrcidService.getOrcid();
		OrcidProfile orcidProfile = orcidService.getProfile(ORCID, token);
		if (orcidProfile != null) {
			ResearcherPageUtils.buildTextValue(crisObject, ORCID, "orcid");
			
			OrcidBio orcidBio = orcidProfile.getOrcidBio();
			
			if (orcidBio != null) {
				if (mapMetadata.containsKey("biography")) {
					Biography biography = orcidBio.getBiography();
					if (biography != null) {
						ResearcherPageUtils.buildTextValue(crisObject, biography.getValue(), mapMetadata.get("biography"), 
								biography.getVisibility() == Visibility.PUBLIC?VisibilityConstants.PUBLIC:VisibilityConstants.HIDE);
					}
				}
				
				ContactDetails contactDetails = orcidBio.getContactDetails();
				if (contactDetails != null) {
					if (mapMetadata.containsKey("other-emails") 
							|| mapMetadata.containsKey("primary-email")) {
						List<Email> emails = contactDetails.getEmail();
						if (emails != null) {
							for (Email email : emails) {
								if (email.isPrimary()) {
									ResearcherPageUtils.buildTextValue(crisObject, email.getValue(), mapMetadata.get("primary-email"), 
											email.getVisibility() == Visibility.PUBLIC?VisibilityConstants.PUBLIC:VisibilityConstants.HIDE);
								}
								else {
									ResearcherPageUtils.buildTextValue(crisObject, email.getValue(), mapMetadata.get("other-emails"), 
											email.getVisibility() == Visibility.PUBLIC?VisibilityConstants.PUBLIC:VisibilityConstants.HIDE);
								}
							}
						}
					}
					
					if (mapMetadata.containsKey("iso-3166-country")) {
						Address address = contactDetails.getAddress();
						if (address != null) {
							Country country = address.getCountry();
							if (country != null) {
								ResearcherPageUtils.buildTextValue(crisObject,
										country.getValue(),
										mapMetadata.get("iso-3166-country"),
										country
												.getVisibility() == Visibility.PUBLIC ? VisibilityConstants.PUBLIC
														: VisibilityConstants.HIDE);
							}
						}
					}
				}
				
				PersonalDetails personalDetails = orcidBio.getPersonalDetails();
				if (personalDetails != null) {
					if (mapMetadata.containsKey("credit-name")) {
						CreditName creditName = personalDetails.getCreditName();
						if (creditName != null) {
							ResearcherPageUtils.buildTextValue(crisObject, creditName.getValue(), mapMetadata.get("credit-name"), 
									creditName.getVisibility() == Visibility.PUBLIC?VisibilityConstants.PUBLIC:VisibilityConstants.HIDE);
						}
					}
					
					if (mapMetadata.containsKey("other-names")) {
						OtherNames otherNames = personalDetails.getOtherNames();
						if (otherNames != null) {
							for (String name : otherNames.getOtherName()) {
								ResearcherPageUtils.buildTextValue(crisObject, name, mapMetadata.get("other-names"), 
										otherNames.getVisibility() == Visibility.PUBLIC?VisibilityConstants.PUBLIC:VisibilityConstants.HIDE);
							}
						}
					}
				}
				
				if (mapMetadata.containsKey("keywords")) {
					Keywords keywords = orcidBio.getKeywords();
					if (keywords != null) {
						for (Keyword key : keywords.getKeyword()) {
							ResearcherPageUtils.buildTextValue(crisObject, key.getContent(), mapMetadata.get("keywords"), 
									keywords.getVisibility() == Visibility.PUBLIC?VisibilityConstants.PUBLIC:VisibilityConstants.HIDE);
						}
					}
				}
				
				if (mapMetadata.containsKey("researcher-urls")) {
					ResearcherUrls urls = orcidBio.getResearcherUrls();
					if (urls != null) {
						for (ResearcherUrl url : urls.getResearcherUrl()) {
							ResearcherPageUtils.buildLinkValue(crisObject, url.getUrlName(), url.getUrl().getValue(),
									mapMetadata.get("researcher-urls"), urls.getVisibility() == Visibility.PUBLIC
											? VisibilityConstants.PUBLIC : VisibilityConstants.HIDE);
						}
					}
				}
				
				ExternalIdentifiers eids = orcidBio.getExternalIdentifiers();
				if (eids != null) {
					for (ExternalIdentifier eid : eids.getExternalIdentifier()) {
						if (mapMetadata.containsKey("external-identifier-" + eid.getExternalIdCommonName().getContent())) {
							ResearcherPageUtils.buildTextValue(crisObject, eid.getExternalIdReference().getContent(),
									mapMetadata.get("external-identifier-" + eid.getExternalIdCommonName().getContent()),
									eids.getVisibility() == Visibility.PUBLIC ? VisibilityConstants.PUBLIC
											: VisibilityConstants.HIDE);
						}
					}
				}
			}
			return true;
		}
		return false;
	}

	public static void printXML(Object jaxbSerializableObject) {
		try {
			if (log.isDebugEnabled()) {
	            javax.xml.bind.JAXBContext jaxbCtx = javax.xml.bind.JAXBContext.newInstance(jaxbSerializableObject.getClass().getPackage().getName());
	            javax.xml.bind.Marshaller marshaller = jaxbCtx.createMarshaller();
	            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8"); //NOI18N
	            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	            marshaller.marshal(jaxbSerializableObject, System.out);
			}
        } catch (javax.xml.bind.JAXBException ex) {
            java.util.logging.Logger.getLogger("global").log(java.util.logging.Level.SEVERE, null, ex); //NOI18N
        }
	}

}
