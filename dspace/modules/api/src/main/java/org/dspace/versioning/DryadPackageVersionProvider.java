package org.dspace.versioning;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierService;
import org.dspace.utils.DSpace;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Mar 31, 2011
 * Time: 1:46:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class DryadPackageVersionProvider extends AbstractVersionProvider implements ItemVersionProvider {
    private VersionHistoryDAO versionHistoryDAO;
    private VersionDAO versionDAO;


    public Item createNewItemAndAddItInWorkspace(Context c, Item previousItem) {
        try{
            WorkspaceItem workspaceItem = WorkspaceItem.create(c, previousItem.getOwningCollection(), false);
            Item itemNew = workspaceItem.getItem();
            itemNew.update();

            return itemNew;

        }catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (AuthorizeException e) {
           throw new RuntimeException(e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    public Item updateItemState(Context c, Item itemNew, Item previousItem) {
        try{

            copyMetadata(itemNew, previousItem);
            createBundlesAndAddBitstreams(itemNew, previousItem);
            createIdentifier(c, itemNew);
            itemNew.addMetadata("internal", "workflow", "submitted", null, Boolean.TRUE.toString());
            createDataFiles(c, previousItem, itemNew);
            itemNew.update();

            return itemNew;

        }catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (AuthorizeException e) {
           throw new RuntimeException(e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (IdentifierException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    private void createDataFiles(Context c, Item previousItem, Item itemNew) throws SQLException, AuthorizeException, IOException, IdentifierException {

        Item[] items = org.dspace.workflow.DryadWorkflowUtils.getDataFiles(c, previousItem);
        Version version = getVersion(c, previousItem.getID());

        for(Item dataFileToCopy : items){

            VersionHistory vh = versionHistoryDAO.find(c, dataFileToCopy.getID(), versionDAO);

            if(vh!=null){
                Version latest = vh.getLatestVersion();
                updateVersion(c, latest.getItem().getID(), version.getSummary());
            }
            else{
                vh=versionHistoryDAO.create(c);
                createVersion(c, vh, dataFileToCopy, version.getSummary());
            }


            WorkspaceItem workspaceItemDataFile = WorkspaceItem.create(c, dataFileToCopy.getOwningCollection(), true);
            workspaceItemDataFile.getItem().setSubmitter(c.getCurrentUser());
            Item dataFileNew = workspaceItemDataFile.getItem();

            createVersion(c, vh, dataFileNew, "current version");

            // Wire the dataFile to the dataPackage before asking for an identifier, otherwise the DOI service fails!
            DryadPublicationDataUtil.wireDF2DP(itemNew, dataFileNew);
            dataFileNew.update();

            copyMetadata(dataFileNew, dataFileToCopy);
            createBundlesAndAddBitstreams(dataFileNew, dataFileToCopy);
            createIdentifier(c, dataFileNew);
            // Wire dataPackage to DataFile
            DryadPublicationDataUtil.wireDP2DF(itemNew, dataFileNew);

            dataFileNew.addMetadata("internal", "workflow", "submitted", null, Boolean.TRUE.toString());            
            dataFileNew.update();            
        }

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

    public boolean isResponsible() {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }


    private String createIdentifier(Context context, Item item) throws AuthorizeException, SQLException, IdentifierException {
        IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
        identifierService.reserve(context, item);
        DCValue[] identifiers = item.getMetadata(MetadataSchema.DC_SCHEMA, "identifier", null, Item.ANY);
        return identifiers[0].value;
    }

    public Version updateVersion(Context c, int itemId, String summary) {
        VersionImpl version = versionDAO.findByItemId(c, itemId);
        version.setSummary(summary);
        versionDAO.update(version);
        return version;
    }

    private VersionImpl createVersion(Context c, VersionHistory vh, Item item, String summary) {
        try {
            VersionImpl version = versionDAO.create(c);

            int nextNumber=1;
            if(vh.getLatestVersion()!=null)
                nextNumber = vh.getLatestVersion().getVersionNumber()+1;


            version.setVersionNumber(nextNumber);
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


     public Version getVersion(Context c, int itemId) {
        VersionImpl version = versionDAO.findByItemId(c, itemId);        
        return version;
    }


    public void deleteVersionedItem(Context c, Version versionToDelete, VersionHistory history) {
        try {
            // if versionToDelete is the current version we have to reinstate the previous version
            // and reset canonical
            if(history.isLastVersion(versionToDelete) && history.size() > 1){
                // reset the previous version to archived
                Item item = history.getPrevious(versionToDelete).getItem();
                item.setArchived(true);
                item.update();
            }

            // assign tombstone to the DOI and reset canonical to the previous version only if there is a previous version
            IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
            Item itemToDelete=versionToDelete.getItem();
            identifierService.delete(c, itemToDelete);
            versionDAO.delete(c, versionToDelete.getVersionId());

            history.remove(versionToDelete);
            if(history.isEmpty()){
               versionHistoryDAO.delete(c, versionToDelete.getVersionHistoryID(), versionDAO);
            }

//            if (history.size() == 1) {
//                // in case remains only one version we should remove also that one...
//                versionDAO.delete(c, history.getLatestVersion().getVersionId());
//                versionHistoryDAO.delete(c, versionToDelete.getVersionHistoryID(), versionDAO);
//            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (IdentifierException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
