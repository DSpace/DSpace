/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;


/**
 * The SubmissionVisibility REST Resource. It is not addressable directly, only
 * used as inline object in the SubmissionPanel resource and SubmissionForm's fields
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class SubmissionVisibilityRest {

    /**
     * Attribute indicating the associated visibility of the specified scopes. The
     * absence of a scope in this map indicates that the submission section/field is
     * visible for that scope.
     */
    private Map<ScopeEnum, VisibilityEnum> visibilities;

    public void addVisibility(ScopeEnum scope, VisibilityEnum visibility) {
        if (visibilities == null) {
            visibilities = new HashMap<>();
        }
        visibilities.put(scope, visibility);
    }

    @JsonAnyGetter
    public Map<ScopeEnum, VisibilityEnum> getVisibilities() {
        return visibilities;
    }

    @JsonAnySetter
    public void setVisibilities(Map<ScopeEnum, VisibilityEnum> visibilities) {
        this.visibilities = visibilities;
    }

}
