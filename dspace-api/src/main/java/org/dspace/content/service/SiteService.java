/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import org.dspace.content.Site;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Service interface class for the Site object.
 * The implementation of this class is responsible for all business logic calls for the Site object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface SiteService extends DSpaceObjectService<Site>
{

    public Site createSite(Context context) throws SQLException;

    public Site findSite(Context context) throws SQLException;
}
