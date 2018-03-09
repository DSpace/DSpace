/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.dao;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.harvest.HarvestedItem;

import java.sql.SQLException;

/**
 * Database Access Object interface class for the HarvestedItem object.
 * The implementation of this class is responsible for all database calls for the HarvestedItem object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface HarvestedItemDAO extends GenericDAO<HarvestedItem> {

    public HarvestedItem findByItem(Context context, Item item) throws SQLException;

    public HarvestedItem findByOAIId(Context context, String itemOaiID, Collection collection) throws SQLException;
}
