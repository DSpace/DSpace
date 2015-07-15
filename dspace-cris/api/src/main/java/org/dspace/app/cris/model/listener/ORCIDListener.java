/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.listener;

import java.util.List;

import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.RelationPreference;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.ProjectProperty;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.model.jdyna.value.RPPointer;
import org.dspace.app.cris.model.orcid.OrcidPreferencesUtils;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;

import it.cilea.osd.common.listener.NativePostUpdateEventListener;
import it.cilea.osd.common.model.Identifiable;
import it.cilea.osd.jdyna.value.BooleanValue;

public class ORCIDListener implements NativePostUpdateEventListener, PostLoadEventListener {

	private static final String RELATION_CRISPJ_PROJECTS = "crispj.projects";

	private static final String RELATION_CRISRP_PUBLICATIONS = "crisrp.publications";

	private static final String ORCID_PUBLICATIONS_PREFS = "orcid-publications-prefs";

	private static final String ORCID_PROJECTS_PREFS = "orcid-projects-prefs";

	@Transient
	private static Logger log = Logger.getLogger(ORCIDListener.class);

	private OrcidPreferencesUtils orcidPreferencesUtils;

	@Override
	public <T extends Identifiable> void onPostUpdate(T entity) {
		Object object = entity;
		if (!(object instanceof ACrisObject) && !(object instanceof RelationPreference)) {
			// nothing to do
			return;
		}
		try {
			if (object instanceof ACrisObject) {
				ACrisObject crisObj = (ACrisObject) object;
				if (crisObj.getType() == 9 || crisObj.getType() == 10) {
					String crisID = crisObj.getCrisID();
					if (StringUtils.isNotBlank(crisID)) {
						try {

							if (crisObj.getType() == 9) {

								ResearcherPage rp = (ResearcherPage) crisObj;
								// check if profile will be send to Orcid
								// Registry
								boolean share = orcidPreferencesUtils.isProfileSelectedToShare(rp);
								if (share) {
									orcidPreferencesUtils.prepareOrcidQueue(crisID, crisObj);
								}

								// check if we have a change of preference
								String oldPrefProject = rp.getOldOrcidProjectsPreference();
								List<RPProperty> rpPropsPJ = rp.getAnagrafica4view().get(ORCID_PROJECTS_PREFS);
								for (RPProperty rpProp : rpPropsPJ) {
									if (StringUtils.isNotBlank(oldPrefProject)) {
										if (!(oldPrefProject.equals(rpProp.getValue().toString()))) {
											// project preference change
											List<Integer> projectsIDs = orcidPreferencesUtils
													.getPreferiteFundingToSendToOrcid(crisID);
											for (Integer pjId : projectsIDs) {
												Project project = orcidPreferencesUtils.getApplicationService()
														.get(Project.class, pjId);
												orcidPreferencesUtils.prepareOrcidQueue(crisID, project);
											}
										}
									}
									break;
								}

								String oldPrefPublications = rp.getOldOrcidPublicationsPreference();
								List<RPProperty> rpPropsItem = rp.getAnagrafica4view().get(ORCID_PUBLICATIONS_PREFS);
								for (RPProperty rpProp : rpPropsItem) {
									if (StringUtils.isNotBlank(oldPrefPublications)) {
										if (!(oldPrefPublications.equals(rpProp.getValue().toString()))) {
											// publications preference change
											List<Integer> itemIDs = orcidPreferencesUtils
													.getPreferiteWorksToSendToOrcid(crisID);
											Context context = null;
											try {
												context = new Context();
												for (Integer itemID : itemIDs) {
													Item item = Item.find(context, itemID);
													orcidPreferencesUtils.prepareOrcidQueue(crisID, item);
												}
											} catch (Exception ex) {
												log.error(ex.getMessage(), ex);
											} finally {
												if (context != null && context.isValid()) {
													context.abort();
												}
											}

										}
									}
									break;
								}

							} else {
								Project project = (Project) crisObj;
								List<ProjectProperty> pjProps = project.getAnagrafica4view()
										.get("principalinvestigator");
								for (ProjectProperty pjProp : pjProps) {
									RPPointer rpPointer = (RPPointer) pjProp.getValue();
									// check if the project is into set of
									// preferite
									// projects will be send to Orcid Registry
									boolean isAPreferiteProject = orcidPreferencesUtils.isAPreferiteToSendToOrcid(
											rpPointer.getObject().getCrisID(), project, ORCID_PROJECTS_PREFS);
									if (isAPreferiteProject) {
										orcidPreferencesUtils.prepareOrcidQueue(rpPointer.getObject().getCrisID(),
												crisObj);
									}
								}
							}
						} catch (Exception e) {
							log.error(
									"Failed to build ORCID queue " + crisObj.getTypeText() + "/" + crisObj.getCrisID(),
									e);
						}
					}
				}
			} else {
				RelationPreference relPref = (RelationPreference) object;
				String uuid = relPref.getSourceUUID();
				String authority = orcidPreferencesUtils.getApplicationService().getEntityByUUID(uuid).getCrisID();
				if (relPref.getRelationType().equals(RELATION_CRISRP_PUBLICATIONS)) {
					// check if the publications is in the set of preferences to
					// send
					Context context = null;
					try {
						context = new Context();

						Item item = Item.find(context, relPref.getItemID());
						boolean isAPreferiteWork = orcidPreferencesUtils.isAPreferiteToSendToOrcid(authority, item,
								ORCID_PUBLICATIONS_PREFS);
						// 4)if the publications match the preference add
						// publication to
						// queue
						if (isAPreferiteWork) {
							orcidPreferencesUtils.prepareOrcidQueue(authority, item);
						}
					} catch (Exception ex) {
						log.error(ex.getMessage(), ex);
					} finally {
						if (context != null && context.isValid()) {
							context.abort();
						}
					}

				} else {
					if (relPref.getRelationType().equals(RELATION_CRISPJ_PROJECTS)) {
						// check if the project is in the set of preferences to
						// send
						Integer projectId = relPref.getItemID();
						Project project = orcidPreferencesUtils.getApplicationService().get(Project.class, projectId);

						boolean isAPreferiteProject = orcidPreferencesUtils.isAPreferiteToSendToOrcid(authority,
								project, ORCID_PROJECTS_PREFS);
						if (isAPreferiteProject) {
							orcidPreferencesUtils.prepareOrcidQueue(authority, project);
						}
					}
				}
			}
		} catch (Exception ex) {
			log.error("Generic Fails when try to build ORCID queue", ex);
		}

	}

	public OrcidPreferencesUtils getOrcidPreferencesUtils() {
		return orcidPreferencesUtils;
	}

	public void setOrcidPreferencesUtils(OrcidPreferencesUtils orcidPreferencesUtils) {
		this.orcidPreferencesUtils = orcidPreferencesUtils;
	}

	@Override
	public void onPostLoad(PostLoadEvent event) {
		// TODO Auto-generated method stub
		Object object = event.getEntity();
		if (object instanceof ResearcherPage) {
			ResearcherPage rp = (ResearcherPage) object;

			List<RPProperty> propsPublications = rp.getAnagrafica4view().get(ORCID_PUBLICATIONS_PREFS);
			List<RPProperty> propsProjects = rp.getAnagrafica4view().get(ORCID_PROJECTS_PREFS);

			for (RPProperty prop : propsPublications) {
				rp.setOldOrcidPublicationsPreference(prop.toString());
			}
			for (RPProperty prop : propsProjects) {
				rp.setOldOrcidProjectsPreference(prop.toString());
			}

			List<RPPropertiesDefinition> metadataDefinitions = orcidPreferencesUtils.getApplicationService()
					.likePropertiesDefinitionsByShortName(RPPropertiesDefinition.class, "orcid-profile-pref");
			for (RPPropertiesDefinition rppd : metadataDefinitions) {
				String metadataShortnameINTERNAL = rppd.getShortName().replaceFirst("orcid-profile-pref-", "");
				List<RPProperty> propsRps = rp.getAnagrafica4view().get(rppd.getShortName());
				for (RPProperty prop : propsRps) {
					BooleanValue booleanValue = (BooleanValue) (prop.getValue());
					if (booleanValue.getObject()) {
						rp.getOldOrcidProfilePreference().add(metadataShortnameINTERNAL);
					}
				}
			}

		}
	}

}
