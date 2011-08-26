/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.swordapp.server;

public class AuthCredentials
{
    private String username;
    private String password;
    private String onBehalfOf;

    public AuthCredentials(String username, String password, String onBehalfOf)
    {
        this.username = username;
        this.password = password;
        this.onBehalfOf = onBehalfOf;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public String getOnBehalfOf()
    {
        return onBehalfOf;
    }
}
