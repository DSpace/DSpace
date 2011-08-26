package org.dspace.sword2;

import org.dspace.core.PluginManager;
import org.swordapp.server.SwordError;

public class WorkflowManagerFactory
{
	public static WorkflowManager getInstance()
            throws DSpaceSwordException, SwordError
    {
        WorkflowManager manager = (WorkflowManager) PluginManager.getSinglePlugin(WorkflowManager.class);
        if (manager == null)
        {
            throw new SwordError(DSpaceUriRegistry.REPOSITORY_ERROR, "No workflow manager configured");
        }
        return manager;
	}
}
