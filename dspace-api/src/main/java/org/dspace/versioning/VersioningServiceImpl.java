/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.content.DCDate;
import org.dspace.content.MetadataValue;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.versioning.dao.VersionDAO;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class VersioningServiceImpl implements VersioningService {

    @Autowired(required = true)
    private VersionHistoryService versionHistoryService;
    @Autowired(required = true)
    protected VersionDAO versionDAO;
    @Autowired(required = true)
    private ItemService itemService;
    private DefaultItemVersionProvider provider;


    /** Service Methods */
    @Override
    public Version createNewVersion(Context c, Item item){
        return createNewVersion(c, item, null);
    }

    @Override
    public Version createNewVersion(Context c, Item item, String summary) {
        try{
            VersionHistory vh = versionHistoryService.findByItem(c, item);
            if(vh==null)
            {
                // first time: create 2 versions, .1(old version) and .2(new version)
                vh= versionHistoryService.create(c);

                // get dc:date.accessioned to be set as first version date...
                List<MetadataValue> values = itemService.getMetadata(item, "dc", "date", "accessioned", Item.ANY);
                Date versionDate = new Date();
                if(values!=null && values.size() > 0){
                    String date = values.get(0).getValue();
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

    @Override
    public void removeVersion(Context c, Version version) throws SQLException {
        try{
            VersionHistory history = version.getVersionHistory();
            provider.deleteVersionedItem(c, version, history);
            versionDAO.delete(c, version);

            history.removeVersion(version);

            if(CollectionUtils.isEmpty(history.getVersions())){
                versionHistoryService.delete(c, history);
            }
            //Delete the item linked to the version
            Item item = version.getItem();
            // Completely delete the item
            itemService.delete(c, item);
        }catch (Exception e) {
            c.abort();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void removeVersion(Context c, Item item) throws SQLException {
        Version version = versionDAO.findByItem(c, item);
        if(version!=null){
            removeVersion(c, version);
        }
    }

    @Override
    public Version getVersion(Context c, int versionID) throws SQLException {
        return versionDAO.findByID(c, Version.class, versionID);
    }


    @Override
    public Version restoreVersion(Context c, Version version){
        return restoreVersion(c, version, null);
    }

    @Override
    public Version restoreVersion(Context c, Version version, String summary)  {
        return null;
    }

    @Override
    public VersionHistory findVersionHistory(Context c, Item item) throws SQLException {
        return versionHistoryService.findByItem(c, item);
    }

    @Override
    public Version updateVersion(Context c, Item item, String summary) throws SQLException {
        Version version = versionDAO.findByItem(c, item);
        version.setSummary(summary);
        versionDAO.save(c, version);
        return version;
    }

    @Override
    public Version getVersion(Context c, Item item) throws SQLException {
        return versionDAO.findByItem(c, item);
    }

    @Override
    public Version createNewVersion(Context context, VersionHistory history, Item item, String summary, Date date, int versionNumber) {
        try {
            Version version = versionDAO.create(context, new Version());

            version.setVersionNumber(getNextVersionNumer(versionHistoryService.getLatestVersion(history)));
            version.setVersionDate(date);
            version.setePerson(item.getSubmitter());
            version.setItem(item);
            version.setSummary(summary);
            version.setVersionHistory(history);
            versionDAO.save(context, version);
            versionHistoryService.add(history, version);
            return version;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

// **** PROTECTED METHODS!!

    protected Version createVersion(Context c, VersionHistory vh, Item item, String summary, Date date) {
        return createNewVersion(c, vh, item, summary, date, getNextVersionNumer(versionHistoryService.getLatestVersion(vh)));
    }

    protected int getNextVersionNumer(Version latest){
        if(latest==null) return 1;

        return latest.getVersionNumber()+1;
    }

    @Required
    public void setProvider(DefaultItemVersionProvider provider) {
        this.provider = provider;
    }
}
