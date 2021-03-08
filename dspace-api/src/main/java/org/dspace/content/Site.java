/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.SiteService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Represents the root of the DSpace Archive.
 * By default, the handle suffix "0" represents the Site, e.g. "1721.1/0"
 */
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Table(name = "site")
public class Site extends DSpaceObject {

    @Transient
    private transient SiteService siteService;

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

    @Override
    public String getName() {
        return getSiteService().getName(this);
    }

    public String getURL() {
        ConfigurationService configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        return configurationService.getProperty("dspace.ui.url");
    }

    private SiteService getSiteService() {
        if (siteService == null) {
            siteService = ContentServiceFactory.getInstance().getSiteService();
        }
        return siteService;
    }

}
