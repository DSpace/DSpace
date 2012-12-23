package org.dspace.orm.dao.api;

import java.util.List;

import org.dspace.orm.entity.CheckSumResults;

/**
 * @author Miguel Pinto <mpinto@lyncode.com>
 * @version $Revision$
 */
public interface ICheckSumResultsDao {
	Integer save(CheckSumResults c);

	CheckSumResults selectByKey(String key);

    boolean delete(CheckSumResults c);

    // Listing
    List<CheckSumResults> selectAll();
}
