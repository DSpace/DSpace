/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;

/**
 * The WorkflowItem REST Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class WorkflowItemRest extends AInprogressSubmissionRest {
    public static final String NAME = "workflowitem";
    public static final String CATEGORY = RestAddressableModel.WORKFLOW;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }
}
