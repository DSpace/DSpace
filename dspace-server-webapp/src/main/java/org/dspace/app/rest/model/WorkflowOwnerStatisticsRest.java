/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Map;

import org.dspace.app.rest.RestResourceController;

/**
 * Model a Workflow statistics for a single owner (user or group).
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@SuppressWarnings({ "serial", "rawtypes" })
public class WorkflowOwnerStatisticsRest extends BaseObjectRest<String> {

    public static final String NAME = "workflowOwner";
    public static final String CATEGORY = RestModel.STATISTICS;
    public static final String NAME_PLURAL = "workflowOwners";

    private String name;

    private String email;

    private long count;

    private Map<String, Long> actionCounts;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return NAME_PLURAL;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    public Map<String, Long> getActionCounts() {
        return actionCounts;
    }

    public void setActionCounts(Map<String, Long> actionCounts) {
        this.actionCounts = actionCounts;
    }

}
