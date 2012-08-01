/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content.service;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import java.sql.SQLException;

/**
 * User: Robin Taylor
 * Date: 31/05/12
 * Time: 15:55
 */
public interface CollectionService {

    public Collection find(Context context, int id) throws SQLException;
}
