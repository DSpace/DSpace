/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.batch.service;

import org.dspace.services.factory.DSpaceServicesFactory;

/***
 * Abstract factory to get services for the DMS Import Framework package, use
 * ImpServiceFactory.getInstance() to retrieve an implementation
 * 
 * @author fcadili (francesco.cadili at 4science.it)
 */
public abstract class ImpServiceFactory {

    public abstract ImpBitstreamService getImpBitstreamService();

    public abstract ImpBitstreamMetadatavalueService getImpBitstreamMetadatavalueService();

    public abstract ImpMetadatavalueService getImpMetadatavalueService();

    public abstract ImpRecordService getImpRecordService();

    public abstract ImpWorkflowNStateService getImpWorkflowNStateService();

    public static ImpServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("impServiceFactory",
                ImpServiceFactory.class);
    }
}
