/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.io.Serializable;

import org.dspace.core.Constants;

/**
 * This is the basic interface that a data model entity need to implement to support browsing/retrieval
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 * @param <PK>
 *            the Class of the primary key
 */
public interface BrowsableObject<PK extends Serializable> {

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

}
