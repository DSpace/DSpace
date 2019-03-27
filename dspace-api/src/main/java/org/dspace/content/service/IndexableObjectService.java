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

import org.dspace.browse.IndexableObject;
import org.dspace.core.Context;

/**
 * Service interface class for any IndexableObject.
 * All IndexableObject service classes should implement this class since it offers some basic methods which all
 * IndexableObjects are required to have.
 *
 * @param <T> class type
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface IndexableObjectService<T extends IndexableObject<PK>, PK extends Serializable> {


    /**
     * Generic find for when the precise type of an IndexableObject is not known
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
     * @return a org.dspace.core.Constants that represents a IndexableObject type
     */
    public int getSupportsTypeConstant();
}
