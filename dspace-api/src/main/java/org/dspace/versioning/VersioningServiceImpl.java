/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.versioning.dao.VersionDAO;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Pascal-Nicolas Becker (dspace at pascal dash becker dot de)
*/
public class VersioningServiceImpl implements VersioningService {

    @Autowired(required = true)
    private VersionHistoryService versionHistoryService;
    @Autowired(required = true)
    protected VersionDAO versionDAO;
    @Autowired(required = true)
    private ItemService itemService;
    @Autowired(required = true)
    private WorkspaceItemService workspaceItemService;
    @Autowired(required = true)
    protected WorkflowItemService workflowItemService;
    
    private DefaultItemVersionProvider provider;

    @Required
    public void setProvider(DefaultItemVersionProvider provider) {
        this.provider = provider;
    }

    protected VersioningServiceImpl()
    {

    }

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
                // first time: create 2 versions: old and new one
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
            // we will first delete the version and then the item
            // after deletion of the version we cannot find the item anymore
            // so we need to get the item now
            Item item = version.getItem();

            VersionHistory history = version.getVersionHistory();
            if (item != null)
            {
                // take care of the item identifiers
                provider.deleteVersionedItem(c, version, history);
            }
            
            // to keep version number stables, we do not delete versions,
            // we set all fields null except versionnumber and versionhistory
            version.setItem(null);
            version.setSummary(null);
            version.setVersionDate(null);
            version.setePerson(null);
            versionDAO.save(c, version);

            // if all versions of a version history were deleted,
            // we delete the version history.
            if (this.getVersionsByHistory(c, history) == null
                    || this.getVersionsByHistory(c, history).isEmpty())
            {
                // hard delete the previously soft deleted versions
                for (Version v : history.getVersions())
                {
                    versionDAO.delete(c, v);
                }
                // delete the version history
                versionHistoryService.delete(c, history);
            }
            
            // Completely delete the item
            if (item != null) {
                // DS-1814 introduce the possibility that submitter can create
                // new versions. To avoid authorithation problems we need to
                // check whether a corresponding workspaceItem exists.
                if (!item.isArchived())
                {
                	WorkspaceItem wsi = workspaceItemService.findByItem(c, item);
        			if(wsi != null) {
                        workspaceItemService.deleteAll(c, wsi);
                	} else {
        				WorkflowItem wfi = workflowItemService.findByItem(c, item);
        				if (wfi != null) {
        					workflowItemService.delete(c, wfi);
        				}
        			}
                }
            	else {
                    itemService.delete(c, item);
                }
            }
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

            version.setVersionNumber(getNextVersionNumer(context, history));
            version.setVersionDate(date);
            version.setePerson(item.getSubmitter());
            version.setItem(item);
            version.setSummary(summary);
            version.setVersionHistory(history);
            versionDAO.save(context, version);
            versionHistoryService.add(context, history, version);
            return version;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    
    @Override
    public List<Version> getVersionsByHistory(Context c, VersionHistory vh) throws SQLException {
        List<Version> versions = versionDAO.findVersionsWithItems(c, vh);
        return versions;
    }


// **** PROTECTED METHODS!!

    protected Version createVersion(Context c, VersionHistory vh, Item item, String summary, Date date) throws SQLException {
        return createNewVersion(c, vh, item, summary, date, getNextVersionNumer(c, vh));
    }

    protected int getNextVersionNumer(Context c, VersionHistory vh) throws SQLException{
        int next = versionDAO.getNextVersionNumber(c, vh);
        
        // check if we have uncommited versions in DSpace's cache
        if (versionHistoryService.getLatestVersion(c, vh) != null 
                && versionHistoryService.getLatestVersion(c, vh).getVersionNumber() >= next)
        {
            next = versionHistoryService.getLatestVersion(c, vh).getVersionNumber() + 1;
        }
        
        return next;
    }
}
