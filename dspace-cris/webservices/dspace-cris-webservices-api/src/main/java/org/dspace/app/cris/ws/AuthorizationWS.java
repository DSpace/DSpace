/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.ws;

import org.dspace.app.cris.model.ws.Criteria;
import org.dspace.app.cris.model.ws.User;

public class AuthorizationWS
{
    public static boolean authorize(User userWS, String type)
    {
        for (Criteria criteria : userWS.getCriteria())
        {
            if (type.equals(criteria.getCriteria()))
            {
                return criteria.isEnabled();
            }
        }
        return false;
    }
}
