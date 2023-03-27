/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.RestResourceController;
import org.dspace.xmlworkflow.state.actions.ActionAdvancedInfo;

/**
 * The rest resource used for workflow actions
 *
 * @author Maria Verdonck (Atmire) on 06/01/2020
 */
public class WorkflowActionRest extends BaseObjectRest<String> {

    public static final String CATEGORY = "config";
    public static final String NAME = "workflowaction";
    public static final String NAME_PLURAL = "workflowactions";

    private List<String> options;
    private List<String> advancedOptions;
    private List<ActionAdvancedInfo> advancedInfo;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getType() {
        return NAME;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<String> getAdvancedOptions() {
        return advancedOptions;
    }

    public void setAdvancedOptions(List<String> advancedOptions) {
        this.advancedOptions = advancedOptions;
    }

    public boolean getAdvanced() {
        return CollectionUtils.isNotEmpty(getAdvancedOptions());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<ActionAdvancedInfo> getAdvancedInfo() {
        return advancedInfo;
    }

    public void setAdvancedInfo(List<ActionAdvancedInfo> advancedInfo) {
        this.advancedInfo = advancedInfo;
    }
}
