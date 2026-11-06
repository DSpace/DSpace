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

    /** @return the email of the owner */
    public String getEmail() {
        return email;
    }

    /** @param email the owner email to set */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return the name of the owner (user or group) */
    public String getName() {
        return name;
    }

    /** @param name the owner name to set */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the total number of workflow actions performed by the owner */
    public long getCount() {
        return count;
    }

    /** @param count the total number of workflow actions to set */
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

    /** @return the number of workflow actions performed by the owner, keyed by action name */
    public Map<String, Long> getActionCounts() {
        return actionCounts;
    }

    /** @param actionCounts the per-action counts to set, keyed by action name */
    public void setActionCounts(Map<String, Long> actionCounts) {
        this.actionCounts = actionCounts;
    }

}
