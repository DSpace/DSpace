/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.TableRow;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class VersionHistoryImpl implements VersionHistory
{

    private int versionHistoryId;
    private List<Version>versions;

    private Context myContext;
    private TableRow myRow;


    protected VersionHistoryImpl(VersionHistoryDAO vhDAO){

    }


    protected VersionHistoryImpl(Context c, TableRow row)
    {
        myContext = c;
        myRow = row;

        c.cache(this, row.getIntColumn(VersionHistoryDAO.VERSION_HISTORY_ID));
    }

    public int getVersionHistoryId() {
        return myRow.getIntColumn(VersionHistoryDAO.VERSION_HISTORY_ID);
    }

    // LIST order: descending
    public Version getPrevious(Version version) {
        int index = versions.indexOf(version);

        if( (index+1)==versions.size()) return null;

        return versions.get(index+1);
    }

    // LIST order: descending
    public Version getNext(Version version)
    {

        int index = versions.indexOf(version);

        if(index==0)
        {
            return null;
        }

        return versions.get(index-1);
    }

    public Version getVersion(Item item) {
       for(Version v : versions)
       {
           if(v.getItem().getID()==item.getID())
           {
               return v;
           }
       }
       return null;
    }

    public boolean hasNext(Item item)
    {
        Version version = getVersion(item);
        return hasNext(version);
    }

    public boolean hasNext(Version version)
    {
        return getNext(version)!=null;
    }

    public List<Version> getVersions()
    {
        return versions;
    }

    public void setVersions(List<Version> versions)
    {
        this.versions = versions;
    }

    public void add(Version version)
    {
        if(versions==null) versions=new ArrayList<Version>();
        versions.add(0, version);
    }

    public Version getLatestVersion()
    {
        if(versions==null || versions.size()==0)
        {
            return null;
        }

        return versions.get(0);
    }

    public Version getFirstVersion()
    {
        if(versions==null || versions.size()==0)
        {
            return null;
        }

        return versions.get(versions.size()-1);
    }


    public boolean isFirstVersion(Version version)
    {
        Version first = versions.get(versions.size()-1);
        return first.equals(version);
    }

    public boolean isLastVersion(Version version)
    {
        Version last = versions.get(0);
        return last.equals(version);
    }

    public void remove(Version version)
    {
        versions.remove(version);
    }

    public boolean isEmpty()
    {
        return versions.size()==0;
    }

    public int size()
    {
        return versions.size();
    }

    protected TableRow getMyRow()
    {
        return myRow;
    }

    

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        VersionHistoryImpl that = (VersionHistoryImpl) o;
        return versionHistoryId == that.versionHistoryId;

    }

    @Override
    public int hashCode()
    {
        int hash=7;
        hash=79*hash+ (this.getVersionHistoryId() ^ (this.getVersionHistoryId() >>> 32));
        return hash;
    }
}
