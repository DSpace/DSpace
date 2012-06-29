 /**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content.dao;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import java.sql.SQLException;

/**
 * User: Robin Taylor
 * Date: 16/03/12
 * Time: 13:46
 */
public interface CollectionDAO {

    public Collection find(Context context, int id) throws SQLException;


}
