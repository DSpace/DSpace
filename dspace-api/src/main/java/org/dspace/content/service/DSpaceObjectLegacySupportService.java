/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.sql.SQLException;

import org.dspace.content.DSpaceObject;
import org.hibernate.Session;

/**
 * Support to retrieve a DSpaceObject by the old integer based identifier which
 * was used to identify DSpaceObjects prior to DSpace 6.0.
 *
 * @param <T> class type
 * @author kevinvandevelde at atmire.com
 */
public interface DSpaceObjectLegacySupportService<T extends DSpaceObject> {

    public T findByIdOrLegacyId(Session session, String id) throws SQLException;

    /**
     * Generic find for when the precise type of a DSO is not known, just the
     * a pair of type number and database ID.
     *
     * @param session - current request's database context.
     * @param id      - the legacy id within table of type'd objects
     * @return the object found, or null if it does not exist.
     * @throws java.sql.SQLException only upon failure accessing the database.
     */
    public T findByLegacyId(Session session, int id) throws SQLException;

    /**
     * Returns the Constants which this service supports
     *
     * @return a org.dspace.core.Constants that represents a DSpaceObjct type
     */
    public int getSupportsTypeConstant();
}
