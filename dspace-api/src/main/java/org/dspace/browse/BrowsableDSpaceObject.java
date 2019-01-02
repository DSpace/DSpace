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

/**
 * This is the basic interface that a data model entity need to implement to support browsing/retrieval
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 * @param <PK>
 *            the Class of the primary key
 */
public interface BrowsableDSpaceObject<PK extends Serializable> {
    Map<String, Object> extraInfo = new HashMap<String, Object>();

    /**
     * A map of additional information exposed by the Entity to the browse/retrieve system
     * 
     * @return a map of extra information, by default an empty map is returned
     */
    default public Map<String, Object> getExtraInfo() {
        return extraInfo;
    }

    /**
     * 
     * @return true if the Entity should be considered finalized (archived in terms of DSpace Item)
     */
    default public boolean isArchived() {
        return false;
    }

    /**
     * 
     * @return true if the Entity should be included in the public discovery system (search/browse)
     */
    default public boolean isDiscoverable() {
        return false;
    }

    /**
     * 
     * @return the integer constant representing the Entity Type, @see {@link Constants}
     */
    public int getType();

    /**
     * 
     * @return the primary key of the Entity instance
     */
    public PK getID();

    /**
     * 
     * @return an unique id to index
     */
    default String getUniqueIndexID() {
        return getType() + "-" + getID().toString();
    }

    /**
     * 
     * @return a textual alias of the Entity Type @see {@link #getType()}
     */
    default public String getTypeText() {
        return Constants.typeText[getType()];
    };

    /**
     * 
     * @return the handle, if any of the object
     */
    public String getHandle();
}
