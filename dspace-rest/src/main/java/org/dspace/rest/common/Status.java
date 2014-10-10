package org.dspace.rest.common;

import org.codehaus.jackson.annotate.JsonProperty;
import org.dspace.core.Context;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Used to handle/determine status of REST API.
 * Mainly to know your authentication status
 *
 */
@XmlRootElement(name = "status")
public class Status
{
    private boolean okay;
    private boolean authenticated;
    private String email;
    private String fullname;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    private String token;

    public Status() {
        setOkay(true);
        setAuthenticated(false);
    }

    public Status(String email, String fullname, String token) {
        setOkay(true);
        setAuthenticated(true);
        setEmail(email);
        setFullname(fullname);
        setToken(token);
    }

    public Status(Context context, String token) {
        setOkay(true);
        if(context.getCurrentUser() != null) {
            setAuthenticated(true);
            setEmail(context.getCurrentUser().getEmail());
            setFullname(context.getCurrentUser().getFullName());
            setToken(token);
        } else {
            setAuthenticated(false);
        }
    }

    @JsonProperty("okay")
    public boolean isOkay()
    {
        return this.okay;
    }

    @JsonProperty("okay")
    public void setOkay(boolean okay)
    {
        this.okay = okay;
    }

    @JsonProperty("authenticated")
    public boolean isAuthenticated() {
        return authenticated;
    }

    @JsonProperty("authenticated")
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    @JsonProperty("email")
    public void setEmail(String email) {
        this.email = email;
    }

    @JsonProperty("fullname")
    public String getFullname() {
        return fullname;
    }

    @JsonProperty("fullname")
    public void setFullname(String fullname) {
        this.fullname = fullname;
    }
}
