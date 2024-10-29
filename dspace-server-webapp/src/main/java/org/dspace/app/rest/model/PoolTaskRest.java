/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.RestResourceController;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;

/**
 * The PoolTask REST Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@LinksRest(links = {
    @LinkRest(name = PoolTaskRest.STEP, method = "getStep")
})
public class PoolTaskRest extends BaseObjectRest<Integer> {
    public static final String NAME = "pooltask";
    public static final String PLURAL_NAME = "pooltasks";
    public static final String CATEGORY = RestAddressableModel.WORKFLOW;

    public static final String STEP = "step";

    private String action;

    @JsonIgnore
    private EPersonRest eperson;

    @JsonIgnore
    private GroupRest group;

    @JsonIgnore
    private WorkflowItemRest workflowitem;

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

    /**
     * @see PoolTask#getActionID()
     * @return
     */
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    /**
     * @see PoolTask#getEperson()
     * @return
     */
    public EPersonRest getEperson() {
        return eperson;
    }

    public void setEperson(EPersonRest eperson) {
        this.eperson = eperson;
    }

    /**
     * @see PoolTask#getGroup()
     * @return
     */
    public GroupRest getGroup() {
        return group;
    }

    public void setGroup(GroupRest group) {
        this.group = group;
    }

    /**
     * 
     * @return the WorkflowItemRest that belong to this pool task
     */
    public WorkflowItemRest getWorkflowitem() {
        return workflowitem;
    }

    public void setWorkflowitem(WorkflowItemRest workflowitem) {
        this.workflowitem = workflowitem;
    }
}
