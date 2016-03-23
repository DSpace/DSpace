/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.eperson.EPerson;
import org.dspace.app.util.Util;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Determine status of REST API - is it running, accessible and without errors?.
 * Find out API version (DSpace major version) and DSpace source version.
 * Find out your authentication status.
 *
 */
@XmlRootElement(name = "status")
public class Status
{
    private boolean okay;
    private boolean authenticated;
    private String email;
    private String fullname;
    private String sourceVersion;
    private String apiVersion;

    public Status() {
        setOkay(true);

        setSourceVersion(Util.getSourceVersion());
        String[] version = Util.getSourceVersion().split("\\.");
        setApiVersion(version[0]); // major version

        setAuthenticated(false);
    }

    public Status(String email, String fullname) {
        setOkay(true);
        setAuthenticated(true);
        setEmail(email);
        setFullname(fullname);
    }

    public Status(EPerson eperson) {
        setOkay(true);
        if(eperson != null) {
            setAuthenticated(true);
            setEmail(eperson.getEmail());
            setFullname(eperson.getFullName());
        } else {
            setAuthenticated(false);
        }
    }

    @JsonProperty("okay")
    public boolean isOkay()
    {
        return this.okay;
    }

    public void setOkay(boolean okay)
    {
        this.okay = okay;
    }

    @JsonProperty("authenticated")
    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JsonProperty("fullname")
    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    @JsonProperty("sourceVersion")
    public String getSourceVersion() {
        return this.sourceVersion;
    }

    public void setSourceVersion(String sourceVersion) {
        this.sourceVersion = sourceVersion;
    }

    @JsonProperty("apiVersion")
    public String getApiVersion() {
        return this.apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }
}
