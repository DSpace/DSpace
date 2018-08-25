/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.dspace.core.Constants;

public interface BrowsableDSpaceObject<PK extends Serializable> {
    Map<String, Object> extraInfo = new HashMap<String, Object>();

    default public Map<String, Object> getExtraInfo() {
        return extraInfo;
    }

    default public boolean isArchived() {
        return false;
    }

    default public boolean isDiscoverable() {
        return false;
    }

    public int getType();

    public PK getID();

    default String getUniqueIndexID() {
        return getType() + "-" + getID().toString();
    }

    default public String getTypeText() {
        return Constants.typeText[getType()];
    };

    public String getHandle();
}
