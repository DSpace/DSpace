/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit;

import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract processing class for DSpace Submission Steps. This retrieve several
 * collaborators that are usually needed by all the processing class.
 */
public abstract class AbstractProcessingStep implements DataProcessingStep {
    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance()
            .getBitstreamFormatService();
    protected BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
}
