/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.versioning.dao.VersionHistoryDAO;
import org.dspace.versioning.service.VersionHistoryService;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class VersionHistoryServiceImpl implements VersionHistoryService
{
    @Autowired(required = true)
    protected VersionHistoryDAO versionHistoryDAO;

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
        versionHistoryDAO.save(context, versionHistory);
    }

    @Override
    public void delete(Context context, VersionHistory versionHistory) throws SQLException, AuthorizeException {
        versionHistoryDAO.delete(context, new VersionHistory());
    }

    // LIST order: descending
    @Override
    public Version getPrevious(VersionHistory versionHistory, Version version) {
        List<Version> versions = versionHistory.getVersions();
        int index = versions.indexOf(version);

        if( (index+1)==versions.size()) return null;

        return versions.get(index + 1);
    }

    // LIST order: descending
    @Override
    public Version getNext(VersionHistory versionHistory, Version version)
    {
        List<Version> versions = versionHistory.getVersions();
        int index = versionHistory.getVersions().indexOf(version);

        if(index==0)
        {
            return null;
        }

        return versions.get(index-1);
    }

    @Override
    public Version getVersion(VersionHistory versionHistory, Item item) {
       List<Version> versions = versionHistory.getVersions();
       for(Version v : versions)
       {
           if(v.getItem().getID()==item.getID())
           {
               return v;
           }
       }
       return null;
    }

    @Override
    public boolean hasNext(VersionHistory versionHistory, Item item)
    {
        Version version = getVersion(versionHistory, item);
        return hasNext(versionHistory, version);
    }

    @Override
    public boolean hasNext(VersionHistory versionHistory, Version version)
    {
        return getNext(versionHistory, version)!=null;
    }

    @Override
    public void add(VersionHistory versionHistory, Version version)
    {
        List<Version> versions = versionHistory.getVersions();
        if(versions==null) versions=new ArrayList<Version>();
        versions.add(0, version);
    }

    @Override
    public Version getLatestVersion(VersionHistory versionHistory)
    {
        List<Version> versions = versionHistory.getVersions();
        if(versions==null || versions.size()==0)
        {
            return null;
        }

        return versions.get(0);
    }

    @Override
    public Version getFirstVersion(VersionHistory versionHistory)
    {
        List<Version> versions = versionHistory.getVersions();
        if(versions==null || versions.size()==0)
        {
            return null;
        }

        return versions.get(versions.size()-1);
    }


    @Override
    public boolean isFirstVersion(VersionHistory versionHistory, Version version)
    {
        List<Version> versions = versionHistory.getVersions();
        Version first = versions.get(versions.size()-1);
        return first.equals(version);
    }

    @Override
    public boolean isLastVersion(VersionHistory versionHistory, Version version)
    {
        List<Version> versions = versionHistory.getVersions();
        Version last = versions.get(0);
        return last.equals(version);
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
