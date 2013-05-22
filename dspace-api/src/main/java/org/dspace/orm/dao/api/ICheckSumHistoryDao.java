/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.dao.api;

import java.util.List;

import org.dspace.orm.entity.CheckSumHistory;

/**
 * @author Miguel Pinto <mpinto@lyncode.com>
 * @version $Revision$
 */
public interface ICheckSumHistoryDao {
	Integer save(CheckSumHistory c);

	CheckSumHistory selectByKey(String key);

    boolean delete(CheckSumHistory c);

    // Listing
    List<CheckSumHistory> selectAll();
}
