package edu.umd.lib.dspace.content.factory;

import org.springframework.beans.factory.annotation.Autowired;

import edu.umd.lib.dspace.content.service.EmbargoDTOService;

/**
 * Factory implementation to get drum specific services, use
 * DrumServiceFactory.getInstance() to retrieve an implementation
 */
public class DrumServiceFactoryImpl extends DrumServiceFactory {

    @Autowired(required = true)
    private EmbargoDTOService embargoDTOService;

    @Override
    public EmbargoDTOService getEmbargoDTOService() {
        return embargoDTOService;
    }
}
