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
import org.dspace.discovery.IndexableObject;

/**
 * Base Service interface class for any IndexableObject. The name of the methods contains IndexableObject to avoid
 * ambiguity reference as some implementation supports both this interface than the DSpaceObectService interface
 *
 * @param <T>
 *            class type of the indexable object
 * @param <PK>
 *            class type of the primary key
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface IndexableObjectService<T extends IndexableObject<PK>, PK extends Serializable> {

    /**
     * Generic find for when the precise type of an IndexableObject is not known
     *
     * @param context - the context
     * @param id      - id within table of type'd indexable objects
     * @return the indexable object found, or null if it does not exist.
     * @throws SQLException only upon failure accessing the database.
     */
    public T findIndexableObject(Context context, PK id) throws SQLException;

    /**
     * Returns the Constants which this service supports
     *
     * @return a org.dspace.core.Constants that represents a IndexableObject type
     */
    public int getSupportsIndexableObjectTypeConstant();
}
