/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.versioning.dao.VersionHistoryDAO;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Pascal-Nicolas Becker (dspace at pascal dash becker dot de)
 */
public class VersionHistoryServiceImpl implements VersionHistoryService
{
    @Autowired(required = true)
    protected VersionHistoryDAO versionHistoryDAO;
    
    @Autowired(required = true)
    private VersioningService versioningService;

    protected VersionHistoryServiceImpl()
    {

    }

    @Override
    public VersionHistory create(Context context) throws SQLException {
        return versionHistoryDAO.create(context, new VersionHistory());
    }

    @Override
    public VersionHistory find(Context context, int id) throws SQLException {
        return versionHistoryDAO.findByID(context, VersionHistory.class, id);
    }

    @Override
    public void update(Context context, VersionHistory versionHistory) throws SQLException, AuthorizeException {
        update(context, Collections.singletonList(versionHistory));
    }

    @Override
    public void update(Context context, List<VersionHistory> versionHistories) throws SQLException, AuthorizeException {
        if(CollectionUtils.isNotEmpty(versionHistories)) {
            for (VersionHistory versionHistory : versionHistories) {
                versionHistoryDAO.save(context, versionHistory);
            }
        }
    }

    @Override
    public void delete(Context context, VersionHistory versionHistory) throws SQLException, AuthorizeException {
        versionHistoryDAO.delete(context, new VersionHistory());
    }

    // LIST order: descending
    @Override
    public Version getPrevious(Context context, VersionHistory versionHistory, Version version)
    throws SQLException
    {
        List<Version> versions = versioningService.getVersionsByHistory(context, versionHistory);
        int index = versions.indexOf(version);
        if (index + 1 < versions.size())
        {
            return versions.get(index+1);
        }
        
        return null;
    }

    // LIST order: descending
    @Override
    public Version getNext(Context c, VersionHistory versionHistory, Version version)
        throws SQLException
    {
        List<Version> versions = versioningService.getVersionsByHistory(c, versionHistory);
        int index = versions.indexOf(version);

        if (index -1 >= 0)
        {
            return versions.get(index -1);
        }
        
        return null;
    }

    @Override
    public Version getVersion(Context context, VersionHistory versionHistory, Item item)
            throws SQLException
    {
        Version v = versioningService.getVersion(context, item);
        if (v != null);
        {
            if (versionHistory.equals(v.getVersionHistory()))
            {
                return v;
            }
        }
        return null;
    }
    
    @Override
    public boolean hasNext(Context context, VersionHistory versionHistory, Item item)
        throws SQLException
    {
        Version version = getVersion(context, versionHistory, item);
        if (version == null)
        {
            return false;
        }
        return hasNext(context, versionHistory, version);
    }

    @Override
    public boolean hasNext(Context context, VersionHistory versionHistory, Version version)
            throws SQLException
    {
        return getNext(context, versionHistory, version)!=null;
    }

    @Override
    public boolean hasVersionHistory(Context context, Item item)
            throws SQLException
    {
        return findByItem(context, item) != null;
    }
    
    @Override
    public void add(Context context, VersionHistory versionHistory, Version version)
            throws SQLException
    {
        List<Version> versions = versionHistory.getVersions();
        if(versions==null) versions=new ArrayList<Version>();
        versions.add(0, version);
    }

    @Override
    public Version getLatestVersion(Context context, VersionHistory versionHistory)
            throws SQLException
    {
        List<Version> versions = versioningService.getVersionsByHistory(context, versionHistory);
        if(versions != null && !versions.isEmpty())
        {
            return versions.get(0);
        }
        return null;
    }

    @Override
    public Version getFirstVersion(Context context, VersionHistory versionHistory)
            throws SQLException
    {
        List<Version> versions = versioningService.getVersionsByHistory(context, versionHistory);

        if (versions == null)
        {
            return null;
        }
        
        if (versions.size()-1 >= 0)
        {
            return versions.get(versions.size()-1);
        }
        
        return null;
    }

    @Override
    public boolean isFirstVersion(Context context, Item item) throws SQLException
    {
        VersionHistory vh = findByItem(context, item);
        if (vh == null)
        {
            return true;
        }
        Version version = versioningService.getVersion(context, item);
        return isFirstVersion(context, vh, version);
    }
    
    @Override
    public boolean isFirstVersion(Context context, VersionHistory versionHistory, Version version)
            throws SQLException
    {
        return getFirstVersion(context, versionHistory).equals(version);
    }
    
    @Override
    public boolean isLastVersion(Context context, Item item) throws SQLException
    {
        VersionHistory vh = findByItem(context, item);
        if (vh == null)
        {
            return true;
        }
        Version version = versioningService.getVersion(context, item);
        return isLastVersion(context, vh, version);
    }

    @Override
    public boolean isLastVersion(Context context, VersionHistory versionHistory, Version version)
            throws SQLException
    {
        return getLatestVersion(context, versionHistory).equals(version);
    }

    @Override
    public void remove(VersionHistory versionHistory, Version version)
    {
        List<Version> versions = versionHistory.getVersions();
        versions.remove(version);
    }

    @Override
    public VersionHistory findByItem(Context context, Item item) throws SQLException {
        return versionHistoryDAO.findByItem(context, item);
    }

}
