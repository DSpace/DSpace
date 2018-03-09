/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.SiteDAO;
import org.dspace.content.service.SiteService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Service implementation for the Site object.
 * This class is responsible for all business logic calls for the Site object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class SiteServiceImpl extends DSpaceObjectServiceImpl<Site> implements SiteService {

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Autowired(required = true)
    protected SiteDAO siteDAO;

    protected SiteServiceImpl()
    {
        super();
    }

    @Override
    public Site createSite(Context context) throws SQLException {
        Site site = findSite(context);
        if(site == null)
        {
            //Only one site can be created at any point in time
            site = siteDAO.create(context, new Site());
            handleService.createHandle(context, site, configurationService.getProperty("handle.prefix") + "/0");
        }
        return site;
    }

    @Override
    public Site findSite(Context context) throws SQLException {
        return siteDAO.findSite(context);
    }

    @Override
    public Site find(Context context, UUID id) throws SQLException {
        return siteDAO.findByID(context, Site.class, id);
    }

    @Override
    public void updateLastModified(Context context, Site dso) throws SQLException, AuthorizeException {
        //Not used at the moment
    }

    @Override
    public void update(Context context, Site site) throws SQLException, AuthorizeException {
        if(!authorizeService.isAdmin(context)){
            throw new AuthorizeException();
        }

        super.update(context, site);

        if(site.isMetadataModified())
        {
            context.addEvent(new Event(Event.MODIFY_METADATA, site.getType(), site.getID(), site.getDetails(), getIdentifiers(context, site)));
        }
        if(site.isModified()) {
            context.addEvent(new Event(Event.MODIFY, site.getType(), site.getID(), site.getDetails(), getIdentifiers(context, site)));
        }
        site.clearModified();
        site.clearDetails();

        siteDAO.save(context, site);
    }

    @Override
    public String getName(Site dso)
    {
        return ConfigurationManager.getProperty("dspace.name");
    }

    @Override
    public void delete(Context context, Site dso) throws SQLException, AuthorizeException, IOException {
        throw new AuthorizeException("Site object cannot be deleted");
    }

    @Override
    public int getSupportsTypeConstant() {
        return Constants.SITE;
    }
}
