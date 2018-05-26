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
import org.dspace.content.RootObject;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RootEntityService;
import org.dspace.content.service.SiteService;
import org.dspace.content.service.SupervisedItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the content package, use ContentServiceFactory.getInstance() to
 * retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class ContentServiceFactoryImpl extends ContentServiceFactory {


    @Autowired(required = true)
    private List<RootEntityService<? extends RootObject>> rootObjectServices;

    @Autowired(required = true)
    private List<DSpaceObjectService<? extends DSpaceObject>> dSpaceObjectServices;
    @Autowired(required = true)
    private List<DSpaceObjectLegacySupportService<? extends DSpaceObject>> dSpaceObjectLegacySupportServices;

    @Autowired(required = true)
    private BitstreamFormatService bitstreamFormatService;
    @Autowired(required = true)
    private BitstreamService bitstreamService;
    @Autowired(required = true)
    private BundleService bundleService;
    @Autowired(required = true)
    private ItemService itemService;
    @Autowired(required = true)
    private CollectionService collectionService;
    @Autowired(required = true)
    private CommunityService communityService;
    @Autowired(required = true)
    private MetadataSchemaService metadataSchemaService;
    @Autowired(required = true)
    private MetadataFieldService metadataFieldService;
    @Autowired(required = true)
    private MetadataValueService metadataValueService;
    @Autowired(required = true)
    private WorkspaceItemService workspaceItemService;
    @Autowired(required = true)
    private InstallItemService installItemService;
    @Autowired(required = true)
    private SupervisedItemService supervisedItemService;
    @Autowired(required = true)
    private SiteService siteService;


    @Override
    public List<DSpaceObjectService<? extends DSpaceObject>> getDSpaceObjectServices() {
        return dSpaceObjectServices;
    }

    @Override
    public List<DSpaceObjectLegacySupportService<? extends DSpaceObject>> getDSpaceObjectLegacySupportServices() {
        return dSpaceObjectLegacySupportServices;
    }

    @Override
    public BitstreamFormatService getBitstreamFormatService() {
        return bitstreamFormatService;
    }

    @Override
    public BitstreamService getBitstreamService() {
        return bitstreamService;
    }

    @Override
    public BundleService getBundleService() {
        return bundleService;
    }

    @Override
    public CollectionService getCollectionService() {
        return collectionService;
    }

    @Override
    public CommunityService getCommunityService() {
        return communityService;
    }

    @Override
    public ItemService getItemService() {
        return itemService;
    }

    @Override
    public MetadataSchemaService getMetadataSchemaService() {
        return metadataSchemaService;
    }

    @Override
    public MetadataFieldService getMetadataFieldService() {
        return metadataFieldService;
    }

    @Override
    public MetadataValueService getMetadataValueService() {
        return metadataValueService;
    }

    @Override
    public WorkspaceItemService getWorkspaceItemService() {
        return workspaceItemService;
    }

    @Override
    public InstallItemService getInstallItemService() {
        return installItemService;
    }

    @Override
    public SupervisedItemService getSupervisedItemService() {
        return supervisedItemService;
    }

    @Override
    public SiteService getSiteService() {
        return siteService;
    }

    @Override
    public List<RootEntityService<? extends RootObject>> getRootObjectServices() {
        return rootObjectServices;
    }
}
