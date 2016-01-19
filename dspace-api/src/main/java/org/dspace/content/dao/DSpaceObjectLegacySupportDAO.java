/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Database Access Object interface interface class that adds support to retrieve DSpaceObject by the old integer based identifier which was used
 * to identify DSpaceObjects prior to DSpace 6.0
 *
 * @author kevinvandevelde at atmire.com
 * @param <T>
 */
public interface DSpaceObjectLegacySupportDAO<T extends DSpaceObject> extends DSpaceObjectDAO<T> {

    public T findByLegacyId(Context context, int legacyId, Class<T> clazz) throws SQLException;
}
