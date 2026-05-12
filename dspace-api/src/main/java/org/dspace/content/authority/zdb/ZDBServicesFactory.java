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

    public abstract ZDBService getZDBService();

    public abstract List<ZDBExtraMetadataGenerator> getMetadataGenerators();

    public static ZDBServicesFactory getInstance() {
        return new ZDBServicesFactoryImpl();
    }
}
