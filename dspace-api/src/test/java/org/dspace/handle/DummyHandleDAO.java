/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.dao.HandleDAO;

/**
 * Test fixture for Handles.
 *
 * @author mwood
 */
class DummyHandleDAO implements HandleDAO {
    final Item item;

    DummyHandleDAO(Item item) {
        this.item = item;
    }

    @Override
    public Handle findByHandle(Context context, String handle) throws SQLException {
        Handle theHandle = new Handle();
        theHandle.setHandle(handle);
        theHandle.setResourceTypeId(Constants.ITEM);
        theHandle.setDSpaceObject(item);
        return theHandle;
    }

    @Override
    public List<Handle> findByPrefix(Context context, String prefix) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long countHandlesByPrefix(Context context, String prefix) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int countRows(Context context) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Handle create(Context context, Handle t) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void save(Context context, Handle t) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delete(Context context, Handle t) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Handle> findAll(Context context, Class<Handle> clazz) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Handle> findAll(Context context, Class<Handle> clazz, Integer limit, Integer offset)
            throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Handle findUnique(Context context, String query) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Handle findByID(Context context, Class clazz, int id) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Handle findByID(Context context, Class clazz, UUID id) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Handle findByID(Context context, Class clazz, String id) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Handle> findMany(Context context, String query) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Handle> getHandlesByDSpaceObject(Context context, DSpaceObject dso) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Long getNextHandleSuffix(Context context) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int updateHandlesWithNewPrefix(Context context, String newPrefix, String oldPrefix) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
