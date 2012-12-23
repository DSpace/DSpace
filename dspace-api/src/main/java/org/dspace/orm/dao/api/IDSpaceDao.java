package org.dspace.orm.dao.api;

import java.util.List;

/**
 * @author Miguel Pinto <mpinto@lyncode.com>
 * @version $Revision$
 */

public interface IDSpaceDao<T> {
	Integer save(T c);

    T selectById(int id);

    boolean delete(T c);

    // Listing
    List<T> selectAll();
}
