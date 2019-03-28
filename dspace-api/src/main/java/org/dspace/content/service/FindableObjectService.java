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

import org.dspace.core.Context;

/**
 * Base Service interface class for any Persistent Entity findable by a primary key.
 *
 * @param <T> class type of the persistent entity
 * @param <PK> class type of the primary key
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface FindableObjectService<T, PK extends Serializable> {


    /**
     * Generic find for when the precise type of an Entity is not known
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
