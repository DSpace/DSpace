/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.batch.factory;

import org.dspace.batch.service.ImpBitstreamMetadatavalueService;
import org.dspace.batch.service.ImpBitstreamService;
import org.dspace.batch.service.ImpMetadatavalueService;
import org.dspace.batch.service.ImpRecordService;
import org.dspace.batch.service.ImpServiceFactory;
import org.dspace.batch.service.ImpWorkflowNStateService;
import org.springframework.beans.factory.annotation.Autowired;

public class ImpServiceFactoryImpl extends ImpServiceFactory {

    @Autowired(required = true)
    private ImpBitstreamService impBitstreamService;

    @Autowired(required = true)
    private ImpBitstreamMetadatavalueService impBitstreamMetadatavalueService;

    @Autowired(required = true)
    private ImpMetadatavalueService impMetadatavalueService;

    @Autowired(required = true)
    private ImpRecordService impRecordService;

    @Autowired(required = true)
    private ImpWorkflowNStateService impWorkflowNStateService;

    @Override
    public ImpBitstreamService getImpBitstreamService() {
        return impBitstreamService;
    }

    @Override
    public ImpBitstreamMetadatavalueService getImpBitstreamMetadatavalueService() {
        return impBitstreamMetadatavalueService;
    }

    @Override
    public ImpMetadatavalueService getImpMetadatavalueService() {
        return impMetadatavalueService;
    }

    @Override
    public ImpRecordService getImpRecordService() {
        return impRecordService;
    }

    @Override
    public ImpWorkflowNStateService getImpWorkflowNStateService() {
        return impWorkflowNStateService;
    }

}
