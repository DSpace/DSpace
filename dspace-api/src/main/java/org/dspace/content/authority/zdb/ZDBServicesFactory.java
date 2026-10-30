/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.zdb;

import java.util.List;

import org.dspace.content.authority.ZDBExtraMetadataGenerator;

/**
 * Factory for ZDB services.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public abstract class ZDBServicesFactory {

    /**
     * Return the {@link ZDBService} instance for querying the ZDB API.
     *
     * @return the ZDB service
     */
    public abstract ZDBService getZDBService();

    /**
     * Return the list of configured {@link ZDBExtraMetadataGenerator}
     * implementations used to enrich ZDB authority choices with extra metadata.
     *
     * @return list of metadata generators, may be empty
     */
    public abstract List<ZDBExtraMetadataGenerator> getMetadataGenerators();

    /**
     * Return the singleton instance of this factory.
     *
     * @return the {@link ZDBServicesFactory} instance
     */
    public static ZDBServicesFactory getInstance() {
        return new ZDBServicesFactoryImpl();
    }
}
