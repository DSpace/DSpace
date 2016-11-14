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
import org.dspace.core.Context;
import org.dspace.xoai.data.DSpaceSet;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

/**
 *
 * @author Lyncode Development Team (dspace at lyncode dot com)
 */
public class DSpaceSetRepository implements SetRepository
{
    private static final Logger log = LogManager.getLogger(DSpaceSetRepository.class);

    private final Context _context;

    private final HandleService handleService;
    private final CommunityService communityService;
    private final CollectionService collectionService;

    public DSpaceSetRepository(Context context)
    {
        _context = context;
        handleService = HandleServiceFactory.getInstance().getHandleService();
        communityService = ContentServiceFactory.getInstance().getCommunityService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
    }

    private int getCommunityCount()
    {
        try
        {
            List<Community> communityList = communityService.findAll(_context);

            return communityList.size();
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return 0;
    }

    private int getCollectionCount()
    {
        try
        {
            List<Collection> collectionList = collectionService.findAll(_context);

            return collectionList.size();
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Produce a list of DSpaceCommunitySet.  The list is a segment of the full
     * list of Community ordered alphabetically by name.
     *
     * @param offset start this far down the list of Community.
     * @param length return up to this many Sets.
     * @return some Sets representing the Community list segment.
     */
    private List<Set> community(int offset, int length)
    {
        List<Set> array = new ArrayList<Set>();

        try
        {
            List<Community> communityList = communityService.findAll(_context, length, offset);

            for(Community community : communityList)
            {
                array.add(DSpaceSet.newDSpaceCommunitySet(
                        community.getHandle(),
                        community.getName()));
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return array;
    }

    /**
     * Produce a list of DSpaceCollectionSet.  The list is a segment of the full
     * list of Collection ordered alphabetically by name.
     *
     * @param offset start this far down the list of Collection.
     * @param length return up to this many Sets.
     * @return some Sets representing the Collection list segment.
     */
    private List<Set> collection(int offset, int length)
    {
        List<Set> array = new ArrayList<Set>();

        try
        {
            List<Collection> collectionList = collectionService.findAll(_context, length, offset);

            for(Collection collection : collectionList)
            {
                array.add(DSpaceSet.newDSpaceCollectionSet(
                        collection.getHandle(),
                        collection.getName()));
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
        log.debug("Querying sets. Offset: " + offset + " - Length: " + length);
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

                DSpaceObject dso = handleService.resolveToObject(_context,
                        setSpec.replace("col_", "").replace("_", "/"));
                if (dso == null || !(dso instanceof Collection))
                    return false;
                return true;
            }
            catch (SQLException ex)
            {
                log.error(ex.getMessage(), ex);
            }
        }
        else if (setSpec.startsWith("com_"))
        {
            try
            {
                DSpaceObject dso = handleService.resolveToObject(_context,
                        setSpec.replace("com_", "").replace("_", "/"));
                if (dso == null || !(dso instanceof Community))
                    return false;
                return true;
            }
            catch (SQLException ex)
            {
                log.error(ex.getMessage(), ex);
            }
        }
        return false;
    }

}

