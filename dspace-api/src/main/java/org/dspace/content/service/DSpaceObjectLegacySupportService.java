/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Service interface class that adds support to retrieve DSpaceObject by the old integer based identifier which was used
 * to identify DSpaceObjects prior to DSpace 6.0
 *
 * @author kevinvandevelde at atmire.com
 * @param <T> class type
 */
public interface DSpaceObjectLegacySupportService<T extends DSpaceObject> {

    public T findByIdOrLegacyId(Context context, String id) throws SQLException;


    /**
     * Generic find for when the precise type of a DSO is not known, just the
     * a pair of type number and database ID.
     *
     * @param context - the context
     * @param id - the legacy id within table of type'd objects
     * @return the object found, or null if it does not exist.
     * @throws java.sql.SQLException only upon failure accessing the database.
     */
    public T findByLegacyId(Context context, int id) throws SQLException;

    /**
     * Returns the Constants which this service supports
     *
     * @return a org.dspace.core.Constants that represents a DSpaceObjct type
     */
    public int getSupportsTypeConstant();
}
