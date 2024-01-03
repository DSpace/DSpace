/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Represents the root of the DSpace Archive.
 * By default, the handle suffix "0" represents the Site, e.g. "1721.1/0"
 */
@Entity
@Table(name = "site")
public class Site extends CacheableDSpaceObject {

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.SiteService#createSite(Context)}
     */
    protected Site() {
    }

    /**
     * Get the type of this object, found in Constants
     *
     * @return type of the object
     */
    @Override
    public int getType() {
        return Constants.SITE;
    }

    /**
     * @deprecated use {@link org.dspace.content.service.SiteService#getName} instead.
     */
    public String getName() {
        ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();
        return configurationService.getProperty("dspace.name");
    }
}
