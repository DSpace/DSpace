/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.configuration;

import java.util.List;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.RelationPreference;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Required;

public class RelationPreferenceConfiguration
{
    public static final int DISABLED = 0;

    public static final int ENABLED = 1;

    public static final int ONLY_SYSTEM_ADMIN = 2;

    private int selectActionAuthLevel, unlinkActionAuthLevel,
            hideActionAuthLevel;

    private RelationPreferenceExtraAction extraAction;

    private List<ColumnVisualizationConfiguration> columnsVisualizationConfiguration;

    private RelationConfiguration relationConfiguration;

    public boolean isActionEnabled(String action, boolean sysAdmin)
    {
        if (action == null)
        {
            return true;
        }
        else if (action == RelationPreference.HIDED)
        {
            return hideActionAuthLevel == ENABLED
                    || (sysAdmin && hideActionAuthLevel == ONLY_SYSTEM_ADMIN);
        }
        else if (action == RelationPreference.SELECTED)
        {
            return selectActionAuthLevel == ENABLED
                    || (sysAdmin && selectActionAuthLevel == ONLY_SYSTEM_ADMIN);
        }
        else if (action == RelationPreference.UNLINKED)
        {
            return unlinkActionAuthLevel == ENABLED
                    || (sysAdmin && unlinkActionAuthLevel == ONLY_SYSTEM_ADMIN);
        }
        return false;
    }

    public boolean executeExtraAction(Context context, ACrisObject cris, int o,
            String previousAction, int previousPriority, String action,
            int priority)
    {
        if (previousAction == action && previousPriority == priority)
        {
            return false;
        }
        if (extraAction == null)
        {
            return true;
        }
        else
        {
            return extraAction.executeExtraAction(context, cris, o,
                    previousAction, previousPriority, action, priority);
        }

    }

    public void setSelectActionAuthLevel(int selectAction)
    {
        this.selectActionAuthLevel = selectAction;
    }

    public void setUnlinkActionAuthLevel(int unlinkAction)
    {
        this.unlinkActionAuthLevel = unlinkAction;
    }

    public void setHideActionAuthLevel(int hideAction)
    {
        this.hideActionAuthLevel = hideAction;
    }

    public void setExtraAction(RelationPreferenceExtraAction extraAction)
    {
        this.extraAction = extraAction;
        if (getRelationConfiguration() != null)
        {
            String relationName = relationConfiguration.getRelationName();
            if (relationName != null)
            {
                if (extraAction.getRelationName() != null)
                {
                    throw new IllegalStateException(
                            "The extra action specified for the configuration "
                                    + relationName
                                    + " has been already bind to the "
                                    + extraAction.getRelationName()
                                    + " configuration");
                }
                extraAction.setRelationName(relationName);
            }
        }
    }

    @Required
    public void setColumnsVisualizationConfiguration(
            List<ColumnVisualizationConfiguration> columnsVisualizationConfiguration)
    {
        this.columnsVisualizationConfiguration = columnsVisualizationConfiguration;
    }

    public List<ColumnVisualizationConfiguration> getColumnsVisualizationConfiguration()
    {
        return columnsVisualizationConfiguration;
    }

    public RelationConfiguration getRelationConfiguration()
    {
        return relationConfiguration;
    }

    public void setRelationConfiguration(
            RelationConfiguration relationConfiguration)
    {
        this.relationConfiguration = relationConfiguration;
    }

}
