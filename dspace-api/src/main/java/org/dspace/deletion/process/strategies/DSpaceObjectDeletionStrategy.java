/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.deletion.process.strategies;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.authorize.AuthorizeException;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Strategy interface for deleting DSpace objects.
 * Implementations should provide the logic for deleting a specific type of DSpaceObject.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public interface DSpaceObjectDeletionStrategy {

    /**
     * Returns true if this strategy supports the given DSpaceObject.
     */
    boolean supports(DSpaceObject dso);

    /**
     * Deletes the given DSpaceObject from the system.
     * @param context the DSpace context
     * @param dso the object to delete
     */
    void delete(Context context, DSpaceObject dso) throws SQLException, AuthorizeException, IOException;

} 