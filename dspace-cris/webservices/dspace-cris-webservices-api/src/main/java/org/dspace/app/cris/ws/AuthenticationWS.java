/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.ws;


import java.net.UnknownHostException;

import org.dspace.app.cris.model.ws.User;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.AddressUtils;

public class AuthenticationWS
{
    private ApplicationService applicationService;

    public User authenticateToken(String ipAddress, String token)
            throws RuntimeException
    {
        User userWS = applicationService.getUserWSByToken(token);
        try
        {
            if (!AddressUtils.checkIsInPattern(userWS.getFromIP(), ipAddress) ||
                    (userWS.getToIP() != null && !AddressUtils.checkIsInRange(userWS.getFromIP(),
                    userWS.getToIP(), ipAddress)))
            {
                throw new RuntimeException(
                        "Invalid token/IP");
            }
        }
        catch (UnknownHostException e)
        {
            throw new RuntimeException(e.getMessage());
        }

        return userWS;

    }

    public User authenticateNormal(String username, String password)
    {
        User userWS = applicationService.getUserWSByUsernameAndPassword(
                username, password);
        return userWS;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }
    
    public ApplicationService getApplicationService()
    {
        return applicationService;
    }
}
