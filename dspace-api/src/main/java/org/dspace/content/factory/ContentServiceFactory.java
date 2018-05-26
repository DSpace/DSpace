/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.factory;

import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.RootObject;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.InProgressSubmissionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RootEntityService;
import org.dspace.content.service.SiteService;
import org.dspace.content.service.SupervisedItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.factory.WorkflowServiceFactory;

/**
 * Abstract factory to get services for the content package, use ContentServiceFactory.getInstance() to retrieve an
 * implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class ContentServiceFactory {

    public abstract List<DSpaceObjectService<? extends DSpaceObject>> getDSpaceObjectServices();

    public abstract List<RootEntityService<? extends RootObject>> getRootObjectServices();

    public abstract List<DSpaceObjectLegacySupportService<? extends DSpaceObject>>
        getDSpaceObjectLegacySupportServices();

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

    public InProgressSubmissionService getInProgressSubmissionService(InProgressSubmission inProgressSubmission) {
        if (inProgressSubmission instanceof WorkspaceItem) {
            return getWorkspaceItemService();
        } else {
            return WorkflowServiceFactory.getInstance().getWorkflowItemService();
        }
    }

    public <T extends RootObject> RootEntityService<T> getRootObjectService(T dso) {
        // No need to worry when supressing, as long as our "getDSpaceObjectManager" method is properly implemented
        // no casting issues should occur
        @SuppressWarnings("unchecked")
        RootEntityService<T> manager = getRootObjectService(dso.getType());
        return manager;
    }

    public RootEntityService getRootObjectService(int type) {
        for (int i = 0; i < getRootObjectServices().size(); i++) {
            RootEntityService objectService = getRootObjectServices().get(i);
            if (objectService.isSupportsTypeConstant(type)) {
                return objectService;
            }
        }
        throw new UnsupportedOperationException("Unknown DSpace type: " + type);
    }

    public <T extends DSpaceObject> DSpaceObjectService<T> getDSpaceObjectService(T dso) {
        // No need to worry when supressing, as long as our "getDSpaceObjectManager" method is properly implemented
        // no casting issues should occur
        @SuppressWarnings("unchecked")
        DSpaceObjectService<T> manager = getDSpaceObjectService(dso.getType());
        return manager;
    }

    public DSpaceObjectService getDSpaceObjectService(int type) {
        for (int i = 0; i < getDSpaceObjectServices().size(); i++) {
            DSpaceObjectService objectService = getDSpaceObjectServices().get(i);
            if (objectService.getSupportsTypeConstant() == type) {
                return objectService;
            }
        }
        throw new UnsupportedOperationException("Unknown DSpace type: " + type);
    }

    public DSpaceObjectLegacySupportService<? extends DSpaceObject> getDSpaceLegacyObjectService(int type) {
        for (int i = 0; i < getDSpaceObjectLegacySupportServices().size(); i++) {
            DSpaceObjectLegacySupportService<? extends DSpaceObject> objectLegacySupportService =
                getDSpaceObjectLegacySupportServices()
                    .get(i);
            if (objectLegacySupportService.getSupportsTypeConstant() == type) {
                return objectLegacySupportService;
            }

        }
        throw new UnsupportedOperationException("Unknown DSpace type: " + type);
    }

    public static ContentServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("contentServiceFactory", ContentServiceFactory.class);
    }

}
