/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.xmlworkflow.state.actions.ActionAdvancedInfo;

/**
 * Abstract class for {@link ActionAdvancedInfo}
 *
 * @author Marie Verdonck (Atmire) on 03/02/23
 */
public abstract class AdvancedInfoRest {

    String id;
    String type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
