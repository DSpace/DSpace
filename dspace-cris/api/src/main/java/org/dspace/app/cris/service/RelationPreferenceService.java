/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.app.cris.configuration.RelationPreferenceConfiguration;
import org.dspace.app.cris.configuration.RelationPreferenceServiceConfiguration;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.RelationPreference;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

public class RelationPreferenceService
{
    private ApplicationService applicationService;

    private CrisSearchService crisSearchService;

    private RelationPreferenceServiceConfiguration _configurationService;

    public RelationPreferenceServiceConfiguration getConfigurationService()
    {
        if (_configurationService == null)
        {
            _configurationService = new DSpace()
                    .getServiceManager()
                    .getServiceByName(
                            RelationPreferenceServiceConfiguration.class
                                    .getName(),
                            RelationPreferenceServiceConfiguration.class);
        }
        return _configurationService;

    }

    public boolean unlink(Context context, ACrisObject cris,
            String relationType, List<String> toUnLink)
    {
        String action = RelationPreference.UNLINKED;
        return executeAction(context, cris, relationType, toUnLink, action);
    }

    public boolean active(Context context, ACrisObject cris,
            String relationType, List<String> toActivate)
    {
        String action = null;
        return executeAction(context, cris, relationType, toActivate, action);
    }

    public boolean hide(Context context, ACrisObject cris, String relationType,
            List<String> toHide)
    {
        String action = RelationPreference.HIDED;
        return executeAction(context, cris, relationType, toHide, action);
    }

    public boolean select(Context context, ACrisObject cris,
            String relationType, List<String> newSelectedItems)
    {
        String confName = getConfigurationName(cris, relationType);
        RelationPreferenceConfiguration conf = getConfigurationService()
                .getRelationPreferenceConfiguration(confName);
        String uuid = cris.getUuid();
        authorizedAction(context, confName, conf, RelationPreference.SELECTED);
        List<String> currSelected = getSelectedUUIDs(cris, relationType);

        boolean isItemID = conf.getRelationConfiguration().getRelationClass().isAssignableFrom(
                Item.class);

        boolean doneChange = false;
        for (int idx = 0; idx < newSelectedItems.size(); idx++)
        {
            // the item is a new selection or has changed position
            if (idx >= currSelected.size()
                    || !currSelected.get(idx).equals(newSelectedItems.get(idx)))
            {
                doneChange = true;
                createOrUpdateRelationPreference(context, conf, confName,
                        cris, isItemID, newSelectedItems.get(idx),
                        RelationPreference.SELECTED, idx);
                if (idx < currSelected.size()
                        && !newSelectedItems.contains(currSelected.get(idx)))
                {
                    createOrUpdateRelationPreference(context, conf,
                            relationType, cris, isItemID,
                            newSelectedItems.get(idx), null);
                }
            }
        }
        return doneChange;
    }

    public String getConfigurationName(ACrisObject cris, String relationType)
    {
        String confName = "cris" + cris.getPublicPath() + "." + relationType;
        return confName;
    }

    private List<String> getSelectedUUIDs(ACrisObject cris, String relationType)
    {
        List<RelationPreference> relations = applicationService
                .findRelationsPreferencesOfUUID(cris.getUuid(), relationType);
        List<String> result = new ArrayList<String>();
        for (RelationPreference rp : relations)
        {
            result.add(rp.getTargetUUID() != null ? rp.getTargetUUID() : rp
                    .getItemID().toString());
        }
        return result;
    }

    private boolean executeAction(Context context, ACrisObject cris,
            String relationType, List<String> toUnLink, String action)
    {
        String confName = getConfigurationName(cris, relationType);
        RelationPreferenceConfiguration conf = getConfigurationService()
                .getRelationPreferenceConfiguration(confName);
        String uuid = cris.getUuid();
        if (toUnLink == null || toUnLink.size() == 0)
        {
            return false;
        }
        boolean isItemID = conf.getRelationConfiguration().getRelationClass().isAssignableFrom(
                Item.class);

        authorizedAction(context, confName, conf, action);
        for (String o : toUnLink)
        {
            createOrUpdateRelationPreference(context, conf, confName, cris,
                    isItemID, o, action);
        }
        return true;
    }

    private void authorizedAction(Context context, String confName,
            RelationPreferenceConfiguration conf, String action)
    {
        try
        {
            if (!conf
                    .isActionEnabled(action, AuthorizeManager.isAdmin(context)))
            {
                throw new IllegalStateException(action
                        + " action is disabled for the relation " + confName);
            }
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Error checking authorization for "
                    + action + " action on relation " + confName + ": "
                    + e.getMessage(), e);
        }
    }

    private void createOrUpdateRelationPreference(Context context,
            RelationPreferenceConfiguration conf, String configurationName,
            ACrisObject cris, boolean isItemID, String o, String action)
    {
        createOrUpdateRelationPreference(context, conf, configurationName, cris,
                isItemID, o, action, 0);
    }

    private void createOrUpdateRelationPreference(Context context,
            RelationPreferenceConfiguration conf, String configurationName,
            ACrisObject cris, boolean isItemID, String o, String action,
            int priority)
    {
        String uuid = cris.getUuid();
        RelationPreference relPref = null;
        if (isItemID)
        {
            relPref = applicationService.getRelationPreferenceForUUIDItemID(
                    uuid, Integer.parseInt(o), configurationName);
        }
        else
        {
            relPref = applicationService.getRelationPreferenceForUUIDs(uuid, o,
                    configurationName);
        }
        String previousState = null;
        int previousPriority = 0;
        DSpaceObject dso = null;
        if (isItemID)
        {
            try
            {
                dso = Item.find(context, Integer.parseInt(o));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        else
        {
            dso = applicationService.getEntityByUUID(o);
        }
        if (relPref == null)
        {
            relPref = new RelationPreference();
            relPref.setRelationType(configurationName);
            relPref.setSourceUUID(uuid);
            relPref.setStatus(action);
            relPref.setPriority(priority);
            if (isItemID)
            {
                relPref.setItemID(Integer.parseInt(o));
            }
            else
            {
                relPref.setTargetUUID(o);
            }
        }
        else
        {
            previousState = relPref.getStatus();
            previousPriority = relPref.getPriority();
            relPref.setStatus(action);
            relPref.setPriority(priority);
        }
		if (action != null) {
			applicationService.saveOrUpdate(RelationPreference.class, relPref);
		} else if (relPref.getId() != null) {
			applicationService
					.delete(RelationPreference.class, relPref.getId());
		}
        if (!conf.executeExtraAction(context, cris, dso.getID(), previousState,
                previousPriority, action, priority))
        {
            try
            {
                crisSearchService.indexContent(context, dso, true);
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

	public ApplicationService getApplicationService() {
		return applicationService;
	}
	
    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public void setCrisSearchService(CrisSearchService crisSearchService)
    {
        this.crisSearchService = crisSearchService;
    }

    public List<RelationPreference> findRelationsPreferencesByUUIDByRelTypeAndStatus(
            String uuid, String relationType, String status)
    {
        return applicationService.findRelationsPreferencesByUUIDByRelTypeAndStatus(uuid, relationType, status);
    }

}
