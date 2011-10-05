package org.dspace.versioning;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Mar 31, 2011
 * Time: 1:45:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class PluggableVersioningService implements VersioningService{

    private VersionHistoryDAO versionHistoryDAO;
    private VersionDAO versionDAO;
    private List<ItemVersionProvider> providers;


    /** Service Methods */
    public Version createNewVersion(Context c, int itemId){
        return createNewVersion(c, itemId, null);
    }

    public Version createNewVersion(Context c, int itemId, String summary) {
        try{
            Item item = Item.find(c, itemId);
            Version version = null;
            VersionHistory vh = versionHistoryDAO.find(c, item.getID(), versionDAO);;
            if(vh!=null){
                version = updateVersion(c,item.getID(), summary);
            }
            else{
                vh=versionHistoryDAO.create(c);
                version = createVersion(c, vh, item, summary);
            }

            ItemVersionProvider provider = getProvider();

            // Create new Item
            Item itemNew = provider.createNewItemAndAddItInWorkspace(c, item);


            // create current version
            createVersion(c, vh, itemNew, "current version");



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

    private void removeVersion(Context c, Version version) {
        try{
            VersionHistory history = versionHistoryDAO.findById(c, version.getVersionHistoryID(), versionDAO);
            Item itemToDelete = version.getItem();
            ItemVersionProvider provider = getProvider();
            provider.deleteVersionedItem(c, version, history);
            versionDAO.delete(c, version.getVersionId());
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
        Version version = versionDAO.find(c, versionID);
        Item ItemToRestore = version.getItem();

        VersionHistory history = versionHistoryDAO.findById(c, version.getVersionHistoryID(), versionDAO);
        Version latest = history.getLatestVersion();
        Item itemToWire = latest.getItem();
        updateVersion(c, itemToWire.getID(), summary);

        ItemVersionProvider provider = getProvider();

        if(provider==null) throw new RuntimeException("Provider not defined!");

        Item itemNew = provider.createNewItemAndAddItInWorkspace(c, ItemToRestore);

        // create current version
        VersionImpl versionNew = createVersion(c, history, itemNew, "current version");

        return versionNew;        
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

// **** PRIVATE METHODS!!

    private VersionImpl createVersion(Context c, VersionHistory vh, Item item, String summary) {
        try {
            VersionImpl version = versionDAO.create(c);

            version.setVersionNumber(getNextVersionNumer(vh.getLatestVersion()));
            version.setVersionDate(new Date());
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

    private ItemVersionProvider getProvider(){
        //TODO versionig: re-enable these lines when spring configuration will work
        for(ItemVersionProvider provider : providers){
            if(provider.isResponsible()){
                return provider;
            }
        }
        //return new DryadPackageVersionProvider();
        return null;
    }


    private int getNextVersionNumer(Version latest){
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


    public List<ItemVersionProvider> getProviders() {
        return providers;
    }

    public void setProviders(List<ItemVersionProvider> providers) {
        this.providers = providers;
    }




}
