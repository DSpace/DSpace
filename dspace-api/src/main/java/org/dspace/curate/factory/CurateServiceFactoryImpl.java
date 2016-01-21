/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate.factory;

import org.dspace.curate.service.WorkflowCuratorService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the curate package, use CurateServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class CurateServiceFactoryImpl extends CurateServiceFactory {

    @Autowired(required = true)
    private WorkflowCuratorService workflowCurator;

    @Override
    public WorkflowCuratorService getWorkflowCuratorService() {
        return workflowCurator;
    }
}
