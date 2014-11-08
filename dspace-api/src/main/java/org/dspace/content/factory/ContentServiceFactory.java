/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.factory;

import org.dspace.content.DSpaceObject;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.*;
import org.dspace.core.Constants;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.utils.DSpace;
import org.dspace.workflow.factory.WorkflowServiceFactory;

/**
 * Abstract factory to get services for the content package, use ContentServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class ContentServiceFactory {

    public abstract BitstreamFormatService getBitstreamFormatService();

    public abstract BitstreamService getBitstreamService();

    public abstract BundleService getBundleService();

    public abstract CollectionService getCollectionService();

    public abstract CommunityService getCommunityService();

    public abstract ItemService getItemService();

    public abstract MetadataFieldService getMetadataFieldService();

    public abstract MetadataSchemaService getMetadataSchemaService();

    public abstract MetadataValueService getMetadataValueService();

    public abstract WorkspaceItemService getWorkspaceItemService();

    public abstract InstallItemService getInstallItemService();

    public abstract SupervisedItemService getSupervisedItemService();

    public abstract SiteService getSiteService();

    public InProgressSubmissionService getInProgressSubmissionService(InProgressSubmission inProgressSubmission)
    {
        if(inProgressSubmission instanceof WorkspaceItem)
        {
            return getWorkspaceItemService();
        }
        else
        {
            return WorkflowServiceFactory.getInstance().getWorkflowItemService();
        }
    }
    public<T extends DSpaceObject> DSpaceObjectService<T> getDSpaceObjectService(T dso)
    {
        // No need to worry when supressing, as long as our "getDSpaceObjectManager" method is properly implemented
        // no casting issues should occur
        @SuppressWarnings("unchecked")
        DSpaceObjectService<T> manager = getDSpaceObjectService(dso.getType());
        return manager;
    }

    public DSpaceObjectService getDSpaceObjectService(int type)
    {
        switch (type)
        {
            case Constants.BITSTREAM:
                return getBitstreamService();
            case Constants.BUNDLE:
                return getBundleService();
            case Constants.ITEM:
                return getItemService();
            case Constants.COLLECTION:
                return getCollectionService();
            case Constants.COMMUNITY:
                return getCommunityService();
            case Constants.GROUP:
                return EPersonServiceFactory.getInstance().getGroupService();
            case Constants.EPERSON:
                return EPersonServiceFactory.getInstance().getEPersonService();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public DSpaceObjectLegacySupportService<? extends DSpaceObject> getDSpaceLegacyObjectService(int type)
    {
        switch (type)
        {
            case Constants.BITSTREAM:
                return getBitstreamService();
            case Constants.BUNDLE:
                return getBundleService();
            case Constants.ITEM:
                return getItemService();
            case Constants.COLLECTION:
                return getCollectionService();
            case Constants.COMMUNITY:
                return getCommunityService();
            case Constants.GROUP:
                return EPersonServiceFactory.getInstance().getGroupService();
            case Constants.EPERSON:
                return EPersonServiceFactory.getInstance().getEPersonService();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public static ContentServiceFactory getInstance(){
        return new DSpace().getServiceManager().getServiceByName("contentServiceFactory", ContentServiceFactory.class);
    }

}
