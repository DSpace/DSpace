/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.io.Serializable;
import java.sql.SQLException;

import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.core.Context;

/**
 * Service interface class for any BrowsableDSpaceObject.
 * All BrowsableObject service classes should implement this class since it offers some basic methods which all
 * BrowsableObjects are required to have.
 *
 * @param <T> class type
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface BrowsableObjectService<T extends BrowsableDSpaceObject<PK>, PK extends Serializable> {


    /**
     * Generic find for when the precise type of a BDSO is not known, just the
     * a pair of type number and database ID.
     *
     * @param context - the context
     * @param id      - id within table of type'd objects
     * @return the object found, or null if it does not exist.
     * @throws SQLException only upon failure accessing the database.
     */
    public T find(Context context, PK id) throws SQLException;

    /**
     * Returns the Constants which this service supports
     *
     * @return a org.dspace.core.Constants that represents a BrowsableDSpaceObject type
     */
    public int getSupportsTypeConstant();
}
