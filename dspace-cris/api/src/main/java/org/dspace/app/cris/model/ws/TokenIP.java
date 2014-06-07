/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.ws;

import javax.persistence.Embeddable;

@Embeddable
public class TokenIP
{
    private String token;
    
    private String fromIP;

    private String toIP;
    
    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public String getFromIP()
    {
        return fromIP;
    }

    public void setFromIP(String fromIP)
    {
        this.fromIP = fromIP;
    }

    public String getToIP()
    {
        return toIP;
    }

    public void setToIP(String toIP)
    {
        this.toIP = toIP;
    }

}
