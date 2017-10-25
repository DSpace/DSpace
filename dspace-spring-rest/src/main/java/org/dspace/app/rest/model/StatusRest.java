/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.util.Util;
import org.dspace.eperson.EPerson;

/**
 * Determine status of REST API - is it running, accessible and without errors?.
 * Find out API version (DSpace major version) and DSpace source version.
 * Find out your authentication status.
 *
 */
public class StatusRest
{
    private boolean okay;
    private boolean authenticated;
    private String email;
    private String fullname;
    private String sourceVersion;
    private String apiVersion;

    public StatusRest() {
        setOkay(true);

        setSourceVersion(Util.getSourceVersion());
        String[] version = Util.getSourceVersion().split("\\.");
        setApiVersion(version[0]); // major version

        setAuthenticated(false);
    }

    public StatusRest(String email, String fullname) {
        setOkay(true);
        setAuthenticated(true);
        setEmail(email);
        setFullname(fullname);
        setSourceVersion(Util.getSourceVersion());
        String[] version = Util.getSourceVersion().split("\\.");
        setApiVersion(version[0]); // major version
    }

    public StatusRest(EPerson eperson) {
        setOkay(true);
        setSourceVersion(Util.getSourceVersion());
        String[] version = Util.getSourceVersion().split("\\.");
        setApiVersion(version[0]); // major version
        if(eperson != null) {
            setAuthenticated(true);
            setEmail(eperson.getEmail());
            setFullname(eperson.getFullName());
        } else {
            setAuthenticated(false);
        }
    }


    public boolean isOkay()
    {
        return this.okay;
    }

    public void setOkay(boolean okay)
    {
        this.okay = okay;
    }

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


    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getSourceVersion() {
        return this.sourceVersion;
    }

    public void setSourceVersion(String sourceVersion) {
        this.sourceVersion = sourceVersion;
    }


    public String getApiVersion() {
        return this.apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }
}