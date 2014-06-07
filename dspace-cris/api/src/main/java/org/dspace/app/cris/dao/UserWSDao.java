/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.dao;

import it.cilea.osd.common.dao.PaginableObjectDao;

import org.dspace.app.cris.model.ws.User;

/**
 * This interface define the methods available to retrieve User for web services
 * 
 * @author cilea
 * 
 */
public interface UserWSDao extends
        PaginableObjectDao<User, Integer>
{

    User uniqueByUsernameAndPassword(String username, String password);

    User uniqueByToken(String token);
   
}
