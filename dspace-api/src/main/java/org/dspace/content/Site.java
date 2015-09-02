/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.SiteService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Represents the root of the DSpace Archive.
 * By default, the handle suffix "0" represents the Site, e.g. "1721.1/0"
 */
@Entity
@Table(name = "site")
public class Site extends DSpaceObject
{

    @Transient
    private SiteService siteService;

    /**
     * Get the type of this object, found in Constants
     *
     * @return type of the object
     */
    @Override
    public int getType()
    {
        return Constants.SITE;
    }

    @Override
    public String getName()
    {
        return getSiteService().getName(this);
    }

    public String getURL()
    {
        return ConfigurationManager.getProperty("dspace.url");
    }

    private SiteService getSiteService() {
        if(siteService == null)
        {
            siteService = ContentServiceFactory.getInstance().getSiteService();
        }
        return siteService;
    }

}
