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
