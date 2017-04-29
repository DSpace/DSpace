/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package com.jonathanblood.content;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.sql.SQLException;

/**
 * Class representing an rating in DSpace.
 *
 * @author Jonathan Blood
 * @version $Revision$
 */
public class RatingsManager
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(RatingsManager.class);

    /**
     * Update rating SQL
     */
    private String updateRating = "UPDATE ratings SET rating = ? WHERE eperson_id = ? AND dspace_object_id = ?";

    /**
     * Get rating SQL
     */
    private String getRating = "SELECT rating FROM ratings WHERE eperson_id = ? AND dspace_object_id = ?";

    public RatingsManager()
    {

    }

    public void addRating(Context context, int epersonId, int dspaceObjectId, int rating)
    {
        try
        {
            TableRow row = DatabaseManager.create(context, "ratings");
            row.setColumn("eperson_id", epersonId);
            row.setColumn("dspace_object_id", dspaceObjectId);
            row.setColumn("rating", rating);
            DatabaseManager.update(context, row);

        }
        catch (SQLException e)
        {
            log.error("Error adding a rating.", e);
        }
    }

    public boolean hasRating(Context context, int epersonId, int dspaceObjectId)
    {
        String sql = "SELECT * FROM RATINGS WHERE EPERSON_ID = " + epersonId +
                " AND DSPACE_OBJECT_ID = " + dspaceObjectId;
        try
        {
            TableRowIterator iterator = DatabaseManager.query(context, sql);
            if (!iterator.hasNext())
            {
                return false;
            }
        }
        catch (SQLException e)
        {
           log.error("Error checking if object has rating.", e);
        }

        return true;
    }

    public int getRating(Context context, int epersonId, int dspaceObjectId)
    {
        Object[] params = {epersonId,  dspaceObjectId};
        try
        {
            TableRowIterator iterator = DatabaseManager.query(context, getRating, params);
            if(iterator.hasNext())
            {
                TableRow row = iterator.next();
                return row.getIntColumn("rating");
            }
        }
        catch (SQLException e)
        {
            log.error("Error getting rating.", e);
        }

        return -1;
    }

    public void updateRating(Context context, int epersonId, int dspaceObjectId, int rating)
    {

        Object[] params = { rating, epersonId,  dspaceObjectId};
        try
        {
            DatabaseManager.updateQuery(context, updateRating, params);

        } catch (SQLException e)
        {
            log.error("Error updating rating.", e);
        }
    }

    public void deleteObjectRatings(Context context, int dspaceObjectId)
    {
        try
        {
            DatabaseManager.deleteByValue(context, "ratings", "dspace_object_id", dspaceObjectId);
        } catch (SQLException e)
        {
            log.error("Error deleting an item's ratings.", e);
        }
    }


}
