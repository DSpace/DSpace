/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.xoai;

import com.lyncode.xoai.dataprovider.core.ListSetsResult;
import com.lyncode.xoai.dataprovider.core.Set;
import com.lyncode.xoai.dataprovider.services.api.SetRepository;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.xoai.data.DSpaceSet;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DSpaceSetRepository implements SetRepository
{
    private static Logger log = LogManager.getLogger(DSpaceSetRepository.class);

    private Context _context;

    public DSpaceSetRepository(Context context)
    {
        _context = context;
    }

    private int getCommunityCount()
    {
        String query = "SELECT COUNT(*) as count FROM community";
        try
        {
            TableRowIterator iterator = DatabaseManager.query(_context, query);
            if (iterator.hasNext())
                return (int) iterator.next().getLongColumn("count");
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return 0;
    }

    private int getCollectionCount()
    {
        String query = "SELECT COUNT(*) as count FROM collection";
        try
        {
            TableRowIterator iterator = DatabaseManager.query(_context, query);
            if (iterator.hasNext())
                return (int) iterator.next().getLongColumn("count");
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return 0;
    }

    private List<Set> community(int offset, int length)
    {
        List<Set> array = new ArrayList<Set>();
        StringBuffer query = new StringBuffer("SELECT community_id, name, handle FROM community c, handle h WHERE h.resource_id=community_id AND h.resource_type_id=? ORDER BY community_id");
        List<Serializable> params = new ArrayList<Serializable>();
        params.add(Constants.COMMUNITY);

        DatabaseManager.applyOffsetAndLimit(query,params,offset,length);

        try
        {
            TableRowIterator iterator = DatabaseManager.query(_context, query.toString(),
                    params.toArray());
            int i = 0;
            while (iterator.hasNext() && i < length)
            {
                TableRow row = iterator.next();
                array.add(DSpaceSet.newDSpaceCommunitySet(
                        row.getStringColumn("handle"),
                        row.getStringColumn("name")));
                i++;
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return array;
    }

    private List<Set> collection(int offset, int length)
    {
        List<Set> array = new ArrayList<Set>();
        StringBuffer query = new StringBuffer("SELECT collection_id, name, handle FROM collection c, handle h WHERE h.resource_id=collection_id AND h.resource_type_id=? ORDER BY collection_id");
        List<Serializable> params = new ArrayList<Serializable>();
        params.add(Constants.COLLECTION);

        DatabaseManager.applyOffsetAndLimit(query,params,offset,length);

        try
        {
            TableRowIterator iterator = DatabaseManager.query(_context, query.toString(),
                    params.toArray());
            int i = 0;
            while (iterator.hasNext() && i < length)
            {
                TableRow row = iterator.next();
                array.add(DSpaceSet.newDSpaceCollectionSet(
                        row.getStringColumn("handle"),
                        row.getStringColumn("name")));
                i++;
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return array;
    }

    @Override
    public ListSetsResult retrieveSets(int offset, int length)
    {
        // Only database sets (virtual sets are added by lyncode common library)
        log.debug("Quering sets. Offset: " + offset + " - Length: " + length);
        List<Set> array = new ArrayList<Set>();
        int communityCount = this.getCommunityCount();
        log.debug("Communities: " + communityCount);
        int collectionCount = this.getCollectionCount();
        log.debug("Collections: " + collectionCount);

        if (offset < communityCount)
        {
            if (offset + length > communityCount)
            {
                // Add some collections
                List<Set> tmp = community(offset, length);
                array.addAll(tmp);
                array.addAll(collection(0, length - tmp.size()));
            }
            else
                array.addAll(community(offset, length));
        }
        else if (offset < communityCount + collectionCount)
        {
            array.addAll(collection(offset - communityCount, length));
        }
        log.debug("Has More Results: "
                + ((offset + length < communityCount + collectionCount) ? "Yes"
                        : "No"));
        return new ListSetsResult(offset + length < communityCount
                + collectionCount, array, communityCount + collectionCount);
    }

    @Override
    public boolean supportSets()
    {
        return true;
    }

    @Override
    public boolean exists(String setSpec)
    {
        if (setSpec.startsWith("col_"))
        {
            try
            {
                DSpaceObject dso = HandleManager.resolveToObject(_context,
                        setSpec.replace("col_", "").replace("_", "/"));
                if (dso == null || !(dso instanceof Collection))
                    return false;
                return true;
            }
            catch (Exception ex)
            {
                log.error(ex.getMessage(), ex);
            }
        }
        else if (setSpec.startsWith("com_"))
        {
            try
            {
                DSpaceObject dso = HandleManager.resolveToObject(_context,
                        setSpec.replace("com_", "").replace("_", "/"));
                if (dso == null || !(dso instanceof Community))
                    return false;
                return true;
            }
            catch (Exception ex)
            {
                log.error(ex.getMessage(), ex);
            }
        }
        return false;
    }

}
