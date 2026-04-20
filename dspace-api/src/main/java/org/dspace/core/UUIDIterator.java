/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.AbstractIterator;
import org.dspace.content.DSpaceObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Iterator implementation which allows to iterate over items and commit while
 * iterating. Using an iterator over previous retrieved UUIDs the iterator doesn't
 * get invalidated after a commit that would instead close the database ResultSet
 *
 * @author Andrea Bollini (andrea.bollini at 4science.com)
 * @param  <T> class type
 */
public class UUIDIterator<T extends DSpaceObject> extends AbstractIterator<T> {
    private Class<T> clazz;

    private Iterator<UUID> iterator;

    @Autowired
    private AbstractHibernateDSODAO<T> dao;

    private Context ctx;

    public UUIDIterator(Context ctx, List<UUID> uuids, Class<T> clazz, AbstractHibernateDSODAO<T> dao)
            throws SQLException {
        this.ctx = ctx;
        this.clazz = clazz;
        this.dao = dao;
        this.iterator = uuids.iterator();
    }

    @Override
    protected T computeNext() {
        try {
            if (iterator.hasNext()) {
                T item = dao.findByID(ctx, clazz, iterator.next());
                if (item != null) {
                    return item;
                } else {
                    return computeNext();
                }
            } else {
                return endOfData();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
