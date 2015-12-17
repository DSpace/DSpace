/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;

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

    public Status(EPerson eperson, String token) {
        setOkay(true);
        if(eperson != null) {
            setAuthenticated(true);
            setEmail(eperson.getEmail());
            setFullname(eperson.getFullName());
            setToken(token);
        } else {
            setAuthenticated(false);
        }
    }

    @XmlElement(name="okay")
    public boolean isOkay()
    {
        return this.okay;
    }

    public void setOkay(boolean okay)
    {
        this.okay = okay;
    }

    @XmlElement(name="authenticated")
    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    @XmlElement(name="email")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @XmlElement(name="fullname")
    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }
}
