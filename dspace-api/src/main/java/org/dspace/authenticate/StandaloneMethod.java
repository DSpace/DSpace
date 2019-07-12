/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

/**
 *
 * 
 */
public interface StandaloneMethod {

    public int connect(Context context, String username, String password, String realm, HttpServletRequest request) throws SQLException, SearchServiceException;
    
}