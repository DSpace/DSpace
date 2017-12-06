/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Required;

import java.sql.SQLException;
import java.util.Date;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class VersioningServiceImpl implements VersioningService{

    private VersionHistoryDAO versionHistoryDAO;
    private VersionDAO versionDAO;
    private DefaultItemVersionProvider provider;


    /** Service Methods */
    public Version createNewVersion(Context c, int itemId){
        return createNewVersion(c, itemId, null);
    }

    public Version createNewVersion(Context c, int itemId, String summary) {
        try{
            Item item = Item.find(c, itemId);
            VersionHistory vh = versionHistoryDAO.find(c, item.getID(), versionDAO);
            if(vh==null)
            {
                // first time: create 2 versions, .1(old version) and .2(new version)
                vh=versionHistoryDAO.create(c);

                // get dc:date.accessioned to be set as first version date...
                Metadatum[] values = item.getMetadata("dc", "date", "accessioned", Item.ANY);
                Date versionDate = new Date();
                if(values!=null && values.length > 0){
                    String date = values[0].value;
                    versionDate = new DCDate(date).toDate();
                }
                createVersion(c, vh, item, "", versionDate);
            }
            // Create new Item
            Item itemNew = provider.createNewItemAndAddItInWorkspace(c, item);

            // create new version
            Version version = createVersion(c, vh, itemNew, summary, new Date());

            // Complete any update of the Item and new Identifier generation that needs to happen
            provider.updateItemState(c, itemNew, item);

            return version;
        }catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void removeVersion(Context c, int versionID) {
        Version version = versionDAO.find(c, versionID);
        if(version!=null){
            removeVersion(c, version);
        }
    }

    public void removeVersion(Context c, Item item) {
        Version version = versionDAO.findByItem(c, item);
        if(version!=null){
            removeVersion(c, version);
        }
    }

    protected void removeVersion(Context c, Version version) {
        try{
            VersionHistory history = versionHistoryDAO.findById(c, version.getVersionHistoryID(), versionDAO);
            provider.deleteVersionedItem(c, version, history);
            versionDAO.delete(c, version.getVersionId());

            history.remove(version);

            if(history.isEmpty()){
                versionHistoryDAO.delete(c, version.getVersionHistoryID(), versionDAO);
            }
            //Delete the item linked to the version
            Item item = version.getItem();
            Collection[] collections = item.getCollections();

            // Remove item from all the collections it's in (so our item is also deleted)
            for (Collection collection : collections)
            {
                collection.removeItem(item);
            }
        }catch (Exception e) {
            c.abort();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Version getVersion(Context c, int versionID) {
        return versionDAO.find(c, versionID);
    }


    public Version restoreVersion(Context c, int versionID){
        return restoreVersion(c, versionID, null);
    }

    public Version restoreVersion(Context c, int versionID, String summary)  {
        return null;
    }

    public VersionHistory findVersionHistory(Context c, int itemId){
        return versionHistoryDAO.find(c, itemId, versionDAO);
    }

    public Version updateVersion(Context c, int itemId, String summary) {
        VersionImpl version = versionDAO.findByItemId(c, itemId);
        version.setSummary(summary);
        versionDAO.update(version);
        return version;
    }

    public Version getVersion(Context c, Item item){
        return versionDAO.findByItemId(c, item.getID());    
    }

// **** PROTECTED METHODS!!

    protected VersionImpl createVersion(Context c, VersionHistory vh, Item item, String summary, Date date) {
        try {
            VersionImpl version = versionDAO.create(c);

            version.setVersionNumber(getNextVersionNumer(vh.getLatestVersion()));
            version.setVersionDate(date);
            version.setEperson(item.getSubmitter());
            version.setItemID(item.getID());
            version.setSummary(summary);
            version.setVersionHistory(vh.getVersionHistoryId());
            versionDAO.update(version);
            vh.add(version);
            return version;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected int getNextVersionNumer(Version latest){
        if(latest==null) return 1;

        return latest.getVersionNumber()+1;
    }



    public VersionHistoryDAO getVersionHistoryDAO() {
        return versionHistoryDAO;
    }

    public void setVersionHistoryDAO(VersionHistoryDAO versionHistoryDAO) {
        this.versionHistoryDAO = versionHistoryDAO;
    }

    public VersionDAO getVersionDAO() {
        return versionDAO;
    }

    public void setVersionDAO(VersionDAO versionDAO) {
        this.versionDAO = versionDAO;
    }

    @Required
    public void setProvider(DefaultItemVersionProvider provider) {
        this.provider = provider;
    }
}
