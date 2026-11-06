/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.dspace.app.rest.RestResourceController;

/**
 * Model a Workflow statistics for a single step.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@SuppressWarnings({ "serial", "rawtypes" })
public class WorkflowStepStatisticsRest extends BaseObjectRest<String> {

    public static final String NAME = "workflowStep";
    public static final String NAME_PLURAL = "wfSteps";
    public static final String CATEGORY = RestModel.STATISTICS;

    private String name;

    private long count;

    @JsonInclude(value = Include.NON_EMPTY)
    private Map<String, Long> actionCounts;

    /** @return the name of the workflow step */
    public String getName() {
        return name;
    }

    /** @param name the workflow step name to set */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the total number of workflow actions performed in this step */
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

    /** @return the number of workflow actions performed in this step, keyed by action name */
    public Map<String, Long> getActionCounts() {
        return actionCounts;
    }

    /** @param actionCounts the per-action counts to set, keyed by action name */
    public void setActionCounts(Map<String, Long> actionCounts) {
        this.actionCounts = actionCounts;
    }

}
