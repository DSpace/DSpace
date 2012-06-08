/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content.dao;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Robin Taylor
 * Date: 16/03/12
 * Time: 13:46
 */
public class OracleCollectionDAO implements CollectionDAO {

    private static Logger log = Logger.getLogger(OracleCollectionDAO.class);

    public String getDBName() {
        return "oracle";
    }

    /**
     * Get a collection from the database. Loads in the metadata
     *
     * @param context DSpace context object
     * @param id      ID of the collection
     * @return the collection, or null if the ID is invalid.
     * @throws SQLException
     */
    public Collection find(Context context, int id) throws SQLException
    {
        TableRow row = DatabaseManager.find(context, "collection", id);

        if (row == null) {
            return null;
        }

        // not null, return Collection
        return new Collection(context, row);
    }


}
