/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.orcid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Transient;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
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
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.model.jdyna.RPPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.service.RelationPreferenceService;
import org.dspace.app.cris.util.Researcher;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.authority.orcid.jaxb.address.AddressCtype;
import org.dspace.authority.orcid.jaxb.address.Addresses;
import org.dspace.authority.orcid.jaxb.common.ExternalId;
import org.dspace.authority.orcid.jaxb.common.Visibility;
import org.dspace.authority.orcid.jaxb.email.EmailCtype;
import org.dspace.authority.orcid.jaxb.email.Emails;
import org.dspace.authority.orcid.jaxb.keyword.KeywordCtype;
import org.dspace.authority.orcid.jaxb.keyword.Keywords;
import org.dspace.authority.orcid.jaxb.othername.OtherNameCtype;
import org.dspace.authority.orcid.jaxb.othername.OtherNames;
import org.dspace.authority.orcid.jaxb.person.Person;
import org.dspace.authority.orcid.jaxb.person.externalidentifier.ExternalIdentifiers;
import org.dspace.authority.orcid.jaxb.personaldetails.BiographyCtype;
import org.dspace.authority.orcid.jaxb.personaldetails.NameCtype.CreditName;
import org.dspace.authority.orcid.jaxb.personaldetails.NameCtype.FamilyName;
import org.dspace.authority.orcid.jaxb.personaldetails.NameCtype.GivenNames;
import org.dspace.authority.orcid.jaxb.researcherurl.ResearcherUrlCtype;
import org.dspace.authority.orcid.jaxb.researcherurl.ResearcherUrls;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

import it.cilea.osd.jdyna.value.BooleanValue;

public class OrcidPreferencesUtils
{
	private static ThreadLocal<Set<String>> importedORCIDs = new ThreadLocal<Set<String>>() {
		@Override protected Set<String> initialValue() {
            return new HashSet<String>();
		}
	};
	
	public static final String RPPDEF_ORCID_WEBHOOK = "orcid-webhook";
	
    public static final String[] ORCID_RESEARCHER_ATTRIBUTES = new String[] {
            "primary-email", "name", "other-names", "other-emails",
            "iso-3166-country", "keywords", "biography", "credit-name" };

    public static final String ORCID_PUBLICATIONS_PREFS = "orcid-publications-prefs";

    public static final String ORCID_PROJECTS_PREFS = "orcid-projects-prefs";

    public static final String ORCID_PUSH_ITEM_ACTIVATE_PUT = "orcid-push-item-activate-put";

    public static final String ORCID_PUSH_CRISPJ_ACTIVATE_PUT = "orcid-push-crispj-activate-put";

    public static final String ORCID_PUSH_CRISRP_ACTIVATE_PUT = "orcid-push-crisrp-activate-put";

    /**
     * This configuration prefix match editable via DSpace-CRIS configuration
     * model
     */
    public static final String PREFIX_ORCID_PROFILE_PREF = "orcid-profile-pref-";

    /**
     * This configuration prefix match the non-editable fields introduced in API
     * 2.0
     */
    public static final String PREFIX_ORCID_SYSTEMPROFILE_PREF = "system-orcid-profile-pref-";

    @Transient
    private static Logger log = Logger.getLogger(OrcidPreferencesUtils.class);

    private static final int ORCID_PREFS_DISABLED = 0;

    private static final int ORCID_PREFS_ALL = 1;

    private static final int ORCID_PREFS_SELECTED = 2;

    private static final int ORCID_PREFS_VISIBLE = 3;

    public void prepareOrcidQueue(String crisId, DSpaceObject obj)
    {
		if (obj instanceof ResearcherPage && hasBeenImportedInThisThread((ResearcherPage) obj)) {
			return;
		}
        OrcidQueue orcidQueue = getApplicationService()
                .uniqueOrcidQueueByEntityIdAndTypeIdAndOwnerId(obj.getID(),
                        obj.getType(), crisId);
        if (orcidQueue == null)
        {
            orcidQueue = new OrcidQueue();
            orcidQueue.setEntityId(obj.getID());
            orcidQueue.setOwner(crisId);
            orcidQueue.setTypeId(obj.getType());
            orcidQueue.setFastlookupObjectName(obj.getName());
            orcidQueue.setFastlookupUuid(obj.getHandle());
            getApplicationService().saveOrUpdate(OrcidQueue.class, orcidQueue);
        }
    }

    public boolean isProfileSelectedToShare(ResearcherPage researcher)
    {
        //if the profile have already acquired authorization scope
        if (isTokenReleasedForSync(researcher,
                OrcidService.SYSTEM_ORCID_TOKEN_PROFILE_CREATE_SCOPE))
        {

            //prepare the old value
            Map<String, List<String>> oldMapOrcidProfilePreference = researcher
                    .getOldMapOrcidProfilePreference();

            //retrieve all metadata that match Orcid Registry metadata 
            List<RPPropertiesDefinition> metadataDefinitions = getApplicationService()
                    .likePropertiesDefinitionsByShortName(
                            RPPropertiesDefinition.class,
                            PREFIX_ORCID_PROFILE_PREF);
            
            // if metadata is enable to go on Orcid Registry 
            for (RPPropertiesDefinition rppd : metadataDefinitions)
            {
                String metadataShortnameINTERNAL = rppd.getShortName()
                        .replaceFirst(PREFIX_ORCID_PROFILE_PREF, "");

                List<RPProperty> propsRps = researcher.getAnagrafica4view()
                        .get(rppd.getShortName());

                boolean preference = false;
                for (RPProperty prop : propsRps)
                {
                    BooleanValue booleanValue = (BooleanValue) (prop
                            .getValue());
                    if (booleanValue.getObject())
                    {
                        // if there is a new metadata enabled on the user preferences
                        if (!researcher.getOldOrcidProfilePreference()
                                .contains(metadataShortnameINTERNAL))
                        {
                            return true;
                        }
                        preference = true;
                    }
                }

                // get old values
                List<String> rpPropValues = new ArrayList<String>();
                if (oldMapOrcidProfilePreference
                        .containsKey(metadataShortnameINTERNAL))
                {
                    rpPropValues = oldMapOrcidProfilePreference
                            .get(metadataShortnameINTERNAL);
                }

                // get current values
                List<RPProperty> propsFoundedRps = researcher
                        .getAnagrafica4view().get(metadataShortnameINTERNAL);
                List<String> listProps = new ArrayList<String>();
                for (RPProperty props : propsFoundedRps)
                {
                    // manage only first value
                    listProps.add(props.toString());
                }
                
                // diff current vs old
                boolean founded = false;
                
                Collections.sort(listProps);
                Collections.sort(rpPropValues);
                founded = CollectionUtils.isEqualCollection(listProps, rpPropValues);
               
                //if there are changes return true
                if (preference && !founded && (propsFoundedRps != null
                        && !propsFoundedRps.isEmpty()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTokenReleasedForSync(ResearcherPage researcher,
            String tokenName)
    {
        String isEnableBioUpdate = ResearcherPageUtils
                .getStringValue(researcher, tokenName);
        // this Researcher Profile have enabled the token for update bio on
        // Orcid Registry?
        if (StringUtils.isNotBlank(isEnableBioUpdate))
        {
            // check if the Researcher have the preferences
            List<RPPropertiesDefinition> metadataDefinitions = getApplicationService()
                    .likePropertiesDefinitionsByShortName(
                            RPPropertiesDefinition.class,
                            PREFIX_ORCID_PROFILE_PREF);
            for (RPPropertiesDefinition rppd : metadataDefinitions)
            {
                if ((researcher.getProprietaDellaTipologia(rppd) != null
                        && !researcher.getProprietaDellaTipologia(rppd)
                                .isEmpty()
                        && (Boolean) researcher.getProprietaDellaTipologia(rppd)
                                .get(0).getObject()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Integer> getPreferiteWorksToSendToOrcid(String crisID)
    {
        ResearcherPage researcher = getApplicationService()
                .getEntityByCrisId(crisID, ResearcherPage.class);
        List<Integer> itemIDsToSend = new ArrayList<Integer>();
        if (researcher != null)
        {
            String publicationsPrefs = ResearcherPageUtils
                    .getStringValue(researcher, ORCID_PUBLICATIONS_PREFS);

            if (publicationsPrefs != null)
            {
                if (Integer.parseInt(publicationsPrefs) != ORCID_PREFS_DISABLED)
                {
                    if (Integer.parseInt(publicationsPrefs) == ORCID_PREFS_ALL)
                    {
                        log.debug("...it will work on all researcher...");
                        SolrQuery query = new SolrQuery("*:*");
                        query.addFilterQuery("{!field f=search.resourcetype}"
                                + Constants.ITEM);
                        query.addFilterQuery("author_authority:" + crisID);
                        query.setFields("search.resourceid",
                                "search.resourcetype");
                        query.setRows(Integer.MAX_VALUE);
                        try
                        {
                            QueryResponse response = getSearchService()
                                    .search(query);
                            SolrDocumentList docList = response.getResults();
                            Iterator<SolrDocument> solrDoc = docList.iterator();
                            while (solrDoc.hasNext())
                            {
                                SolrDocument doc = solrDoc.next();
                                Integer rpId = (Integer) doc
                                        .getFirstValue("search.resourceid");
                                itemIDsToSend.add(rpId);
                            }
                        }
                        catch (SearchServiceException e)
                        {
                            log.error("Error retrieving documents", e);
                        }
                    }
                    else
                    {
                        if (Integer.parseInt(
                                publicationsPrefs) == ORCID_PREFS_SELECTED)
                        {
                            List<RelationPreference> selected = new ArrayList<RelationPreference>();
                            for (RelationPreferenceConfiguration configuration : getRelationPreferenceService()
                                    .getConfigurationService().getList())
                            {
                                if (configuration.getRelationConfiguration()
                                        .getRelationClass().equals(Item.class))
                                {
                                    selected = getRelationPreferenceService()
                                            .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                                    researcher.getUuid(),
                                                    configuration
                                                            .getRelationConfiguration()
                                                            .getRelationName(),
                                                    RelationPreference.SELECTED);
                                }
                                for (RelationPreference sel : selected)
                                {
                                    itemIDsToSend.add(sel.getItemID());
                                }
                            }

                        }
                        else
                        {
                            if (Integer.parseInt(
                                    publicationsPrefs) == ORCID_PREFS_VISIBLE)
                            {
                                List<Integer> itemIDsToSendTmp = new ArrayList<Integer>();
                                log.debug(
                                        "...it will work on all researcher...");
                                SolrQuery query = new SolrQuery("*:*");
                                query.addFilterQuery(
                                        "{!field f=search.resourcetype}"
                                                + Constants.ITEM);
                                query.addFilterQuery(
                                        "author_authority:" + crisID);
                                query.setFields("search.resourceid",
                                        "search.resourcetype");
                                query.setRows(Integer.MAX_VALUE);
                                try
                                {
                                    QueryResponse response = getSearchService()
                                            .search(query);
                                    SolrDocumentList docList = response
                                            .getResults();
                                    Iterator<SolrDocument> solrDoc = docList
                                            .iterator();
                                    while (solrDoc.hasNext())
                                    {
                                        SolrDocument doc = solrDoc.next();
                                        Integer rpId = (Integer) doc
                                                .getFirstValue(
                                                        "search.resourceid");
                                        itemIDsToSendTmp.add(rpId);
                                    }
                                }
                                catch (SearchServiceException e)
                                {
                                    log.error("Error retrieving documents", e);
                                }
                                List<RelationPreference> hided = new ArrayList<RelationPreference>();
                                for (RelationPreferenceConfiguration configuration : getRelationPreferenceService()
                                        .getConfigurationService().getList())
                                {
                                    if (configuration.getRelationConfiguration()
                                            .getRelationClass()
                                            .equals(Item.class))
                                    {
                                        hided = getRelationPreferenceService()
                                                .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                                        researcher.getUuid(),
                                                        configuration
                                                                .getRelationConfiguration()
                                                                .getRelationName(),
                                                        RelationPreference.HIDED);
                                    }
                                    for (Integer itemId : itemIDsToSendTmp)
                                    {
                                        boolean founded = false;
                                        internal: for (RelationPreference hid : hided)
                                        {
                                            if (hid.getItemID().equals(itemId))
                                            {
                                                founded = true;
                                            }
                                            if (founded)
                                            {
                                                break internal;
                                            }
                                        }
                                        if (!founded)
                                        {
                                            itemIDsToSend.add(itemId);
                                        }
                                    }
                                }
                            }
                            else
                            {
                                log.warn(crisID
                                        + " - publications preferences NOT recognized");
                            }
                        }
                    }
                }
                else
                {
                    log.debug(crisID + " - DISABLED publications preferences");
                }
            }
        }
        return itemIDsToSend;
    }

    public boolean isAPreferiteToSendToOrcid(String crisID, DSpaceObject dso,
            String preferenceMetadataDefinition)
    {
        ResearcherPage researcher = getApplicationService()
                .getEntityByCrisId(crisID, ResearcherPage.class);
        if (researcher != null)
        {
            String publicationsPrefs = ResearcherPageUtils
                    .getStringValue(researcher, preferenceMetadataDefinition);

            if (publicationsPrefs != null)
            {
                if (Integer.parseInt(publicationsPrefs) == ORCID_PREFS_DISABLED)
                {
                    return false;
                }

                if (Integer.parseInt(publicationsPrefs) == ORCID_PREFS_ALL)
                {
                    return true;
                }
                else
                {
                    if (Integer.parseInt(
                            publicationsPrefs) == ORCID_PREFS_SELECTED)
                    {
                        List<RelationPreference> selected = new ArrayList<RelationPreference>();
                        for (RelationPreferenceConfiguration configuration : getRelationPreferenceService()
                                .getConfigurationService().getList())
                        {
                            if (dso.getType() == Constants.ITEM && configuration
                                    .getRelationConfiguration()
                                    .getRelationClass().equals(Item.class))
                            {
                                selected = getRelationPreferenceService()
                                        .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                                researcher.getUuid(),
                                                configuration
                                                        .getRelationConfiguration()
                                                        .getRelationName(),
                                                RelationPreference.SELECTED);
                            }
                            else
                            {
                                if (dso.getType() == CrisConstants.PROJECT_TYPE_ID
                                        && configuration
                                                .getRelationConfiguration()
                                                .getRelationClass()
                                                .equals(Project.class))
                                {
                                    selected = getRelationPreferenceService()
                                            .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                                    researcher.getUuid(),
                                                    configuration
                                                            .getRelationConfiguration()
                                                            .getRelationName(),
                                                    RelationPreference.SELECTED);
                                }
                            }
                            for (RelationPreference sel : selected)
                            {
                                if (dso.getType() == Constants.ITEM)
                                {
                                    if (sel.getItemID() == dso.getID())
                                    {
                                        return true;
                                    }
                                }
                                else
                                {
                                    if (sel.getTargetUUID() == dso.getHandle())
                                    {
                                        return true;
                                    }
                                }
                            }
                        }

                    }
                    else
                    {
                        if (Integer.parseInt(
                                publicationsPrefs) == ORCID_PREFS_VISIBLE)
                        {

                            List<RelationPreference> hided = new ArrayList<RelationPreference>();
                            for (RelationPreferenceConfiguration configuration : getRelationPreferenceService()
                                    .getConfigurationService().getList())
                            {
                                if (dso.getType() == Constants.ITEM
                                        && configuration
                                                .getRelationConfiguration()
                                                .getRelationClass()
                                                .equals(Item.class))
                                {
                                    hided = getRelationPreferenceService()
                                            .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                                    researcher.getUuid(),
                                                    configuration
                                                            .getRelationConfiguration()
                                                            .getRelationName(),
                                                    RelationPreference.HIDED);
                                }
                                else
                                {
                                    if (dso.getType() == CrisConstants.PROJECT_TYPE_ID
                                            && configuration
                                                    .getRelationConfiguration()
                                                    .getRelationClass()
                                                    .equals(Project.class))
                                    {
                                        hided = getRelationPreferenceService()
                                                .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                                        researcher.getUuid(),
                                                        configuration
                                                                .getRelationConfiguration()
                                                                .getRelationName(),
                                                        RelationPreference.HIDED);
                                    }
                                }
                                for (RelationPreference hid : hided)
                                {
                                    if (dso.getType() == Constants.ITEM)
                                    {
                                        if (hid.getItemID() == dso.getID())
                                        {
                                            return false;
                                        }

                                    }
                                    else
                                    {
                                        if (hid.getTargetUUID() == dso
                                                .getHandle())
                                        {
                                            return false;
                                        }
                                    }

                                }
                                return true;
                            }

                        }
                        else
                        {
                            log.warn(crisID + " - "
                                    + preferenceMetadataDefinition
                                    + " - preferences NOT recognized");
                        }
                    }
                }
            }
            else
            {
                log.debug(crisID + " - " + preferenceMetadataDefinition
                        + " -  DISABLED entity preferences");
            }

        }
        return false;

    }

    public List<String> getPreferiteFundingToSendToOrcid(String crisID)
    {
        ResearcherPage researcher = getApplicationService()
                .getEntityByCrisId(crisID, ResearcherPage.class);
        List<String> projectsIDsToSend = new ArrayList<String>();
        if (researcher != null)
        {
            String projectsPrefs = ResearcherPageUtils
                    .getStringValue(researcher, ORCID_PROJECTS_PREFS);
            if (StringUtils.isNotBlank(projectsPrefs))
            {
                if (Integer.parseInt(projectsPrefs) != ORCID_PREFS_DISABLED)
                {
                    if (Integer.parseInt(projectsPrefs) == ORCID_PREFS_ALL)
                    {
                        log.info("...it will work on all researcher...");
                        SolrQuery query = new SolrQuery("*:*");
                        query.addFilterQuery("{!field f=search.resourcetype}"
                                + CrisConstants.PROJECT_TYPE_ID);
                        query.addFilterQuery(
                                "projectinvestigators_authority:" + crisID);
                        query.setFields("search.resourceid",
                                "search.resourcetype", "cris-uuid");
                        query.setRows(Integer.MAX_VALUE);
                        try
                        {
                            QueryResponse response = getSearchService()
                                    .search(query);
                            SolrDocumentList docList = response.getResults();
                            Iterator<SolrDocument> solrDoc = docList.iterator();
                            while (solrDoc.hasNext())
                            {
                                SolrDocument doc = solrDoc.next();
                                String rpId = (String) doc
                                        .getFirstValue("cris-uuid");
                                projectsIDsToSend.add(rpId);
                            }
                        }
                        catch (SearchServiceException e)
                        {
                            log.error("Error retrieving documents", e);
                        }
                    }
                    else
                    {
                        if (Integer.parseInt(
                                projectsPrefs) == ORCID_PREFS_SELECTED)
                        {
                            List<RelationPreference> selected = new ArrayList<RelationPreference>();
                            for (RelationPreferenceConfiguration configuration : getRelationPreferenceService()
                                    .getConfigurationService().getList())
                            {
                                if (configuration.getRelationConfiguration()
                                        .getRelationClass()
                                        .equals(Project.class))
                                {
                                    selected = getRelationPreferenceService()
                                            .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                                    researcher.getUuid(),
                                                    configuration
                                                            .getRelationConfiguration()
                                                            .getRelationName(),
                                                    RelationPreference.SELECTED);
                                }
                                for (RelationPreference sel : selected)
                                {
                                    projectsIDsToSend.add(sel.getTargetUUID());
                                }
                            }

                        }
                        else
                        {
                            if (Integer.parseInt(
                                    projectsPrefs) == ORCID_PREFS_VISIBLE)
                            {
                                List<String> projectsIDsToSendTmp = new ArrayList<String>();
                                log.info(
                                        "...it will work on all researcher...");
                                SolrQuery query = new SolrQuery("*:*");
                                query.addFilterQuery(
                                        "{!field f=search.resourcetype}"
                                                + CrisConstants.PROJECT_TYPE_ID);
                                query.addFilterQuery(
                                        "projectinvestigators_authority:"
                                                + crisID);
                                query.setFields("search.resourceid",
                                        "search.resourcetype", "cris-uuid");
                                query.setRows(Integer.MAX_VALUE);
                                try
                                {
                                    QueryResponse response = getSearchService()
                                            .search(query);
                                    SolrDocumentList docList = response
                                            .getResults();
                                    Iterator<SolrDocument> solrDoc = docList
                                            .iterator();
                                    while (solrDoc.hasNext())
                                    {
                                        SolrDocument doc = solrDoc.next();
                                        String rpId = (String) doc
                                                .getFirstValue("cris-uuid");
                                        projectsIDsToSendTmp.add(rpId);
                                    }
                                }
                                catch (SearchServiceException e)
                                {
                                    log.error("Error retrieving documents", e);
                                }
                                List<RelationPreference> hided = new ArrayList<RelationPreference>();
                                for (RelationPreferenceConfiguration configuration : getRelationPreferenceService()
                                        .getConfigurationService().getList())
                                {
                                    if (configuration.getRelationConfiguration()
                                            .getRelationClass()
                                            .equals(Project.class))
                                    {
                                        hided = getRelationPreferenceService()
                                                .findRelationsPreferencesByUUIDByRelTypeAndStatus(
                                                        researcher.getUuid(),
                                                        configuration
                                                                .getRelationConfiguration()
                                                                .getRelationName(),
                                                        RelationPreference.HIDED);
                                    }
                                    for (String itemId : projectsIDsToSendTmp)
                                    {
                                        boolean founded = false;
                                        internal: for (RelationPreference hid : hided)
                                        {
                                            if (hid.getTargetUUID()
                                                    .equals(itemId))
                                            {
                                                founded = true;
                                            }
                                            if (founded)
                                            {
                                                break internal;
                                            }
                                        }
                                        if (!founded)
                                        {
                                            projectsIDsToSend.add(itemId);
                                        }
                                    }
                                }
                            }
                            else
                            {
                                log.warn(crisID
                                        + " - projects preferences NOT recognized");
                            }
                        }
                    }
                }
                else
                {
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

    public RelationPreferenceService getRelationPreferenceService()
    {
        return new DSpace().getServiceManager().getServiceByName(
                "org.dspace.app.cris.service.RelationPreferenceService",
                RelationPreferenceService.class);
    }

    public ApplicationService getApplicationService()
    {
        return new DSpace().getServiceManager().getServiceByName(
                "applicationService", ApplicationService.class);
    }

    public void deleteOrcidQueueByOwnerAndType(String crisID, int typeId)
    {
        getApplicationService().deleteOrcidQueueByOwnerAndTypeId(crisID,
                typeId);
    }

    public boolean sendOrcidProfile(String owner, String uuId)
    {
        return PushToORCID.sendOrcidProfile(getApplicationService(), owner);
    }

    public boolean sendOrcidFunding(String owner, String uuId)
    {
        return PushToORCID.sendOrcidFunding(getApplicationService(), owner,
                uuId);
    }

    public boolean sendOrcidWork(String owner, String uuId)
    {
        return PushToORCID.sendOrcidWork(getApplicationService(), owner, uuId);
    }

    public List<OrcidHistory> getOrcidHistoryInSuccessByOwner(String crisID)
    {
        return getApplicationService()
                .findOrcidHistoryByOwnerAndSuccess(crisID);
    }

    public List<OrcidHistory> getOrcidHistoryInSuccessByOwnerAndTypeId(
            String crisID, int type)
    {
        return getApplicationService()
                .findOrcidHistoryInSuccessByOwnerAndType(crisID, type);
    }

    public static boolean populateRP(ResearcherPage crisObject, String ORCID)
    {
        return populateRP(crisObject, ORCID, null);
    }

    public static boolean populateRP(ResearcherPage crisObject, String ORCID,
            String token) {
    	return populateRP(crisObject, ORCID, token, null, null);
    }

	/**
	 * By default only properties not yet filled in the RP will be populated from
	 * ORCID. The propsToSkip list can be used to skip the import of some specific
	 * information, the propsToReplace will force the system to replace the current
	 * values with the ones received from ORCID (including empty values). Both lists
	 * must contains the shortName of the properties on the DSpace-CRIS side (i.e.
	 * fullName not name)
	 * 
	 * @param crisObject
	 * @param ORCID
	 * @param token
	 * @param propToSkip
	 * @param propToAppend
	 * @param propsToReplace
	 * @return
	 */
    public static boolean populateRP(ResearcherPage crisObject, String ORCID,
            String token, List<String> propsToSkip, List<String> propsToReplace)
    {
        if (OrcidService.isValid(ORCID))
        {
        	importedORCIDs.get().add(crisObject.getCrisID());
            ApplicationService applicationService = new Researcher()
                    .getApplicationService();
            List<RPPropertiesDefinition> metadataDefinitions = applicationService
                    .likePropertiesDefinitionsByShortName(
                            RPPropertiesDefinition.class,
                            OrcidPreferencesUtils.PREFIX_ORCID_PROFILE_PREF);
            metadataDefinitions.addAll(
                    applicationService.likePropertiesDefinitionsByShortName(
                            RPPropertiesDefinition.class,
                            OrcidPreferencesUtils.PREFIX_ORCID_SYSTEMPROFILE_PREF));
            Map<String, String> mapMetadata = new HashMap<String, String>();
            // special case for the Family and GiveName stored separately in ORCID
            mapMetadata.put("$fullname", "fullName");
            for (RPPropertiesDefinition rppd : metadataDefinitions)
            {
            	String metadataShortnameORCID = rppd.getLabel();
            	String metadataShortnameINTERNAL;
                if (rppd.getShortName().startsWith(
                        OrcidPreferencesUtils.PREFIX_ORCID_PROFILE_PREF))
                {
                    metadataShortnameINTERNAL = rppd.getShortName()
                            .replaceFirst(
                                    OrcidPreferencesUtils.PREFIX_ORCID_PROFILE_PREF,
                                    "");
                }
                else
                {
                    metadataShortnameINTERNAL = rppd.getShortName()
                            .replaceFirst(
                                    OrcidPreferencesUtils.PREFIX_ORCID_SYSTEMPROFILE_PREF,
                                    "");
                }
                
                if (propsToSkip == null || !propsToSkip.contains(metadataShortnameINTERNAL)) {
	                mapMetadata.put(metadataShortnameORCID,
	                        metadataShortnameINTERNAL);
                }
            }

            OrcidService orcidService = OrcidService.getOrcid();
            Person orcidProfile = null;
            try
            {
                orcidProfile = orcidService.getPerson(ORCID, token);
            }
            catch (Exception ex)
            {
                log.warn("Failed populate ResearcherPage by Orcid", ex);
                return false;
            }
            if (orcidProfile != null)
            {
            	if (StringUtils.isBlank(ResearcherPageUtils.getStringValue(crisObject, "orcid"))) {
            		ResearcherPageUtils.buildTextValue(crisObject, ORCID, "orcid");
            	}

                if (mapMetadata.containsKey("biography"))
                {
                    BiographyCtype biography = orcidProfile.getBiography();
                    if (biography != null && checkSyncAllowed(crisObject, mapMetadata.get("biography"), propsToReplace))
                    {
                        ResearcherPageUtils.buildTextValue(crisObject,
                                biography.getContent(),
                                mapMetadata.get("biography"),
                                biography.getVisibility() == Visibility.PUBLIC
                                        ? VisibilityConstants.PUBLIC
                                        : VisibilityConstants.HIDE);
                    }
                }
                if (mapMetadata.containsKey("other-emails")
                        || mapMetadata.containsKey("primary-email"))
                {
                    Emails emails = orcidProfile.getEmails();
                    if (emails != null)
                    {
                        for (EmailCtype email : emails.getEmail())
                        {
                            if (email.isVerified())
                            {
                                if (email.isPrimary() && checkSyncAllowed(crisObject, mapMetadata.get("primary-email"), propsToReplace))
                                {
                                    ResearcherPageUtils.buildTextValue(
                                            crisObject, email.getEmail(),
                                            mapMetadata.get("primary-email"),
                                            email.getVisibility() == Visibility.PUBLIC
                                                    ? VisibilityConstants.PUBLIC
                                                    : VisibilityConstants.HIDE);
                                }
                                else
                                {
                                	if (checkSyncAllowed(crisObject, mapMetadata.get("other-emails"), propsToReplace)) {
	                                    ResearcherPageUtils.buildTextValue(
	                                            crisObject, email.getEmail(),
	                                            mapMetadata.get("other-emails"),
	                                            email.getVisibility() == Visibility.PUBLIC
	                                                    ? VisibilityConstants.PUBLIC
	                                                    : VisibilityConstants.HIDE);
                                	}
                                }
                            }
                        }
                    }
                }
                
                // special case for the ORCID principal name stored as family + given
                if (mapMetadata.containsKey("$fullname")) {
                	FamilyName surname = orcidProfile.getName().getFamilyName();
                	GivenNames firstname = orcidProfile.getName().getGivenNames();
                	if (surname != null) {
                		String fullname = surname.getValue();
	                	if (firstname != null) {
	                		fullname += ", " + firstname.getValue();
	                	}
	                	if (checkSyncAllowed(crisObject, mapMetadata.get("$fullname"), propsToReplace)) {
	                		ResearcherPageUtils.buildTextValue(crisObject,
	                                fullname,
	                                mapMetadata.get("$fullname"),
	                                orcidProfile.getName()
	                                        .getVisibility() == Visibility.PUBLIC
	                                                ? VisibilityConstants.PUBLIC
	                                                : VisibilityConstants.HIDE);
	                	}
                	}
                }

                if (mapMetadata.containsKey("credit-name"))
                {
                    CreditName creditName = orcidProfile.getName()
                            .getCreditName();
                    if (creditName != null && checkSyncAllowed(crisObject, mapMetadata.get("credit-name"), propsToReplace))
                    {
                        ResearcherPageUtils.buildTextValue(crisObject,
                                creditName.getValue(),
                                mapMetadata.get("credit-name"),
                                orcidProfile.getName()
                                        .getVisibility() == Visibility.PUBLIC
                                                ? VisibilityConstants.PUBLIC
                                                : VisibilityConstants.HIDE);
                    }
                }
                
                if (mapMetadata.containsKey("other-names"))
                {
                    OtherNames otherNames = orcidProfile.getOtherNames();
                    if (otherNames != null)
                    {
                    	boolean found = false;
                        for (OtherNameCtype otherName : otherNames.getOtherName())
                        {
                            String value = otherName.getContent();
                            if (StringUtils.isNotBlank(value))
                            {
                            	found = true;
                            	break;
                            }
                        }
                        
                        if (found && checkSyncAllowed(crisObject, mapMetadata.get("other-names"), propsToReplace)) {
                        	for (OtherNameCtype otherName : otherNames.getOtherName())
                            {
                                String value = otherName.getContent();
                                if (StringUtils.isNotBlank(value))
                                {
                                    ResearcherPageUtils.buildTextValue(crisObject,
                                            value,
                                            mapMetadata.get("other-names"),
                                            otherName.getVisibility() == Visibility.PUBLIC
                                                    ? VisibilityConstants.PUBLIC
                                                    : VisibilityConstants.HIDE);
                                }
                            }	
                        }
                    }
                }
                
                if (mapMetadata.containsKey("iso-3166-country"))
                {
                    Addresses addresses = orcidProfile.getAddresses();
                    if (addresses != null)
                    {
                    	boolean found = false;
                        for (AddressCtype address : addresses.getAddress())
                        {
                            String country = address.getCountry();
                            if (StringUtils.isNotBlank(country))
                            {
                            	found = true;
                            	break;
                            }
                        }
                        
                        if (found && checkSyncAllowed(crisObject, mapMetadata.get("iso-3166-country"), propsToReplace)) {
                        	for (AddressCtype address : addresses.getAddress())
                            {
                                String country = address.getCountry();
                                if (StringUtils.isNotBlank(country))
                                {
                                	found = true;
                                	ResearcherPageUtils.buildTextValue(crisObject,
                                            country,
                                            mapMetadata.get("iso-3166-country"),
                                            address.getVisibility() == Visibility.PUBLIC
                                                    ? VisibilityConstants.PUBLIC
                                                    : VisibilityConstants.HIDE);
                                }
                            }
                        }
                    }
                }

                if (mapMetadata.containsKey("keywords"))
                {
                    Keywords keywords = orcidProfile.getKeywords();
                    if (keywords != null && keywords.getKeyword() != null && keywords.getKeyword().size() > 0 && checkSyncAllowed(crisObject, mapMetadata.get("keywords"), propsToReplace))
                    {
                        for (KeywordCtype key : keywords.getKeyword())
                        {
                            ResearcherPageUtils.buildTextValue(crisObject,
                                    key.getContent(),
                                    mapMetadata.get("keywords"),
                                    key.getVisibility() == Visibility.PUBLIC
                                            ? VisibilityConstants.PUBLIC
                                            : VisibilityConstants.HIDE);
                        }
                    }
                }

                if (mapMetadata.containsKey("researcher-urls"))
                {
                    ResearcherUrls urls = orcidProfile.getResearcherUrls();
                    if (urls != null && urls.getResearcherUrl() != null && urls.getResearcherUrl().size() > 0 && checkSyncAllowed(crisObject, mapMetadata.get("researcher-urls"), propsToReplace))
                    {
                        for (ResearcherUrlCtype url : urls.getResearcherUrl())
                        {
                            ResearcherPageUtils.buildLinkValue(crisObject,
                                    url.getUrlName(), url.getUrl().getValue(),
                                    mapMetadata.get("researcher-urls"),
                                    url.getVisibility() == Visibility.PUBLIC
                                            ? VisibilityConstants.PUBLIC
                                            : VisibilityConstants.HIDE);
                        }
                    }
                }

                ExternalIdentifiers eids = orcidProfile
                        .getExternalIdentifiers();
                if (eids != null)
                {
                    for (ExternalId eid : eids.getExternalIdentifier())
                    {
                        if (mapMetadata.containsKey("external-identifier-"
                                + eid.getExternalIdValue()) && checkSyncAllowed(crisObject, mapMetadata.get("external-identifier-"
                                        + eid.getExternalIdValue()), propsToReplace))
                        {
                            ResearcherPageUtils.buildTextValue(crisObject,
                                    eid.getExternalIdType(),
                                    mapMetadata.get("external-identifier-"
                                            + eid.getExternalIdValue()),
                                    eid.getVisibility() == Visibility.PUBLIC
                                            ? VisibilityConstants.PUBLIC
                                            : VisibilityConstants.HIDE);
                        }
                    }
                }

        		// should we register a webhook?
				if ("all".equalsIgnoreCase(
						ConfigurationManager.getProperty("authentication-oauth", "orcid-webhook"))) {
        			registerOrcidWebHook(crisObject);
        		}
                return true;
            } // end orcidProfile != null
        }
        return false;
    }
    
    private static boolean hasBeenImportedInThisThread(ResearcherPage crisObject) {
    	return importedORCIDs.get().contains(crisObject.getCrisID());
    }
    
    private static boolean checkSyncAllowed(ResearcherPage crisObject, String pdefKey, List<String> propsToReplace) {
		if (propsToReplace != null && (propsToReplace.contains(pdefKey) || propsToReplace.contains("*"))) {
    		ResearcherPageUtils.cleanPropertyByPropertyDefinition(crisObject, pdefKey);
    		return true;
    	}
    	List<RPProperty> listMetadata = crisObject.getAnagrafica4view().get(pdefKey);
		if (listMetadata == null || listMetadata.size() == 0) {
			return true;
		}
    	return false;
    }

    public static void printXML(Object jaxbSerializableObject)
    {
        try
        {
            if (log.isDebugEnabled())
            {
                javax.xml.bind.JAXBContext jaxbCtx = javax.xml.bind.JAXBContext
                        .newInstance(jaxbSerializableObject.getClass()
                                .getPackage().getName());
                javax.xml.bind.Marshaller marshaller = jaxbCtx
                        .createMarshaller();
                marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING,
                        "UTF-8"); // NOI18N
                marshaller.setProperty(
                        javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT,
                        Boolean.TRUE);
                marshaller.marshal(jaxbSerializableObject, System.out);
            }
        }
        catch (javax.xml.bind.JAXBException ex)
        {
            java.util.logging.Logger.getLogger("global")
                    .log(java.util.logging.Level.SEVERE, null, ex); // NOI18N
        }
    }

    public boolean prepareOrcidQueueByNested(ACrisNestedObject crisNestedObject)
    {
        if (crisNestedObject.getParent() instanceof ResearcherPage)
        {
            String shortname = crisNestedObject.getTypo().getShortName();
            ResearcherPage rp = (ResearcherPage) crisNestedObject.getParent();
            if (hasBeenImportedInThisThread(rp)) {
            	return false;
            }
            List<RPPropertiesDefinition> metadataDefinitions = getApplicationService()
                    .likePropertiesDefinitionsByShortName(
                            RPPropertiesDefinition.class,
                            PREFIX_ORCID_PROFILE_PREF);
            for (RPPropertiesDefinition rppd : metadataDefinitions)
            {
                String metadataShortnameINTERNAL = rppd.getShortName()
                        .replaceFirst(PREFIX_ORCID_PROFILE_PREF, "");
                List<RPProperty> propsRps = rp.getAnagrafica4view()
                        .get(rppd.getShortName());
                for (RPProperty prop : propsRps)
                {
                    BooleanValue booleanValue = (BooleanValue) (prop
                            .getValue());
                    if (booleanValue.getObject())
                    {
                        if (metadataShortnameINTERNAL.endsWith(shortname))
                        {
                            prepareOrcidQueue(rp.getCrisID(), rp);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

	public static boolean registerOrcidWebHook(ResearcherPage rp) {
		String orcid = ResearcherPageUtils.getStringValue(rp, "orcid");
		if (orcid != null && OrcidService.getOrcid().registerWebHook(orcid)) {
			// clear previous flags
			List<RPProperty> rppp = rp.getAnagrafica4view().get(RPPDEF_ORCID_WEBHOOK);
			if (rppp != null && !rppp.isEmpty()) {
				for (RPProperty rpppp : rppp) {
					rp.removeProprieta(rpppp);
				}
			}
			ResearcherPageUtils.buildGenericValue(rp, Boolean.TRUE, RPPDEF_ORCID_WEBHOOK, VisibilityConstants.PUBLIC);
			return true;
		}
		return false;
	}

	public static boolean unregisterOrcidWebHook(ResearcherPage rp) {
		String orcid = ResearcherPageUtils.getStringValue(rp, "orcid");
		if (orcid != null && OrcidService.getOrcid().unregisterWebHook(orcid)) {
			// clear previous flags
			List<RPProperty> rppp = rp.getAnagrafica4view().get(RPPDEF_ORCID_WEBHOOK);
			if (rppp != null && !rppp.isEmpty()) {
				for (RPProperty rpppp : rppp) {
					rp.removeProprieta(rpppp);
				}
			}
			ResearcherPageUtils.buildGenericValue(rp, Boolean.FALSE, RPPDEF_ORCID_WEBHOOK, VisibilityConstants.PUBLIC);
			return true;
		}
		return false;
	}

	public static void setTokens(ResearcherPage rp, String token) {
		String scopeMetadata = ConfigurationManager.getProperty("authentication-oauth",
				"application-client-scope");
		if (StringUtils.isNotBlank(scopeMetadata)) {
			for (String scopeConfigurated : OAuthUtils.decodeScopes(scopeMetadata)) {
				// clear all token
				List<RPProperty> rppp = rp.getAnagrafica4view()
						.get("system-orcid-token" + scopeConfigurated.replace("/", "-"));
				if (rppp != null && !rppp.isEmpty()) {
					for (RPProperty rpppp : rppp) {
						rp.removeProprieta(rpppp);
					}
				}
			}
		}
		// rebuild token
		if (StringUtils.isNotBlank(scopeMetadata) && StringUtils.isNotBlank(token)) {
			for (String scopeConfigurated : OAuthUtils.decodeScopes(scopeMetadata)) {
				ResearcherPageUtils.buildTextValue(rp, token,
						"system-orcid-token" + scopeConfigurated.replace("/", "-"));
			}
		}
	}
	
	//UTILITY METHODS
	public static String getTokenReleasedForSync(ResearcherPage researcher,
	        String tokenName)
	{
	    return ResearcherPageUtils.getStringValue(researcher, tokenName);
	}
}
