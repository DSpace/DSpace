/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.core.factory.CoreServiceFactory;
import org.swordapp.server.SwordError;

public class WorkflowManagerFactory
{
    public static WorkflowManager getInstance()
            throws DSpaceSwordException, SwordError
    {
        WorkflowManager manager = (WorkflowManager) CoreServiceFactory.getInstance().getPluginService()
                .getSinglePlugin(WorkflowManager.class);
        if (manager == null)
        {
            throw new SwordError(DSpaceUriRegistry.REPOSITORY_ERROR,
                    "No workflow manager configured");
        }
        return manager;
    }
}
