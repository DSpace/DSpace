/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.dspace.app.rest.model.AccessConditionDTO;

/**
 * Java Bean to expose the access conditions during in progress submission.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class DataAccessCondition implements SectionData {

    private static final long serialVersionUID = -502652293641527738L;

    /**
     * If discoverable = 'false' indicates whether the current item hidden
     * from all search/browse/OAI results, and is therefore only
     * accessible via direct link (or bookmark).
     */
    private Boolean discoverable;

    /**
     * An array of all the policies that has been applied by the user to the item.
     */
    private List<AccessConditionDTO> accessConditions;

    public List<AccessConditionDTO> getAccessConditions() {
        if (Objects.isNull(accessConditions)) {
            accessConditions = new ArrayList<>();
        }
        return accessConditions;
    }

    public void setAccessConditions(List<AccessConditionDTO> accessConditions) {
        this.accessConditions = accessConditions;
    }

    public Boolean getDiscoverable() {
        return discoverable;
    }

    public void setDiscoverable(Boolean discoverable) {
        this.discoverable = discoverable;
    }

}