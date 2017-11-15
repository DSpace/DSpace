/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.RestResourceController;
import org.dspace.app.util.Util;

/**
 * Determine status of REST API - is it running, accessible and without errors?.
 * Find out API version (DSpace major version) and DSpace source version.
 * Find out your authentication status.
 *
 */
public class StatusRest extends BaseObjectRest<Integer>
{

    private String sourceVersion;
    private String apiVersion;

    private boolean okay;
    private boolean authenticated;

    public static final String NAME = "status";
    public static final String CATEGORY = "";

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getType() {
        return NAME;
    }

    public Class getController() {
        return RestResourceController.class;
    }


    private EPersonRest ePersonRest;

    public StatusRest() {
        setSourceVersion(Util.getSourceVersion());
        String[] version = Util.getSourceVersion().split("\\.");
        setApiVersion(version[0]); // major version
        setOkay(true);
        setAuthenticated(false);
    }

//    public StatusRest(String email, String fullname) {
//        setSourceVersion(Util.getSourceVersion());
//        String[] version = Util.getSourceVersion().split("\\.");
//        setApiVersion(version[0]); // major version
//    }

    public StatusRest(EPersonRest eperson) {
        setSourceVersion(Util.getSourceVersion());
        String[] version = Util.getSourceVersion().split("\\.");
        setApiVersion(version[0]); // major version
        setOkay(true);
        if(eperson != null) {
            setAuthenticated(true);
            this.ePersonRest = eperson;
        }
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

    @LinkRest(linkClass = EPersonRest.class, name = "eperson", optional = true)
    @JsonIgnore
    public EPersonRest getEPersonRest() {
        return ePersonRest;
    }

    public void setEPersonRest(EPersonRest ePersonRest) {
        this.ePersonRest = ePersonRest;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public boolean isOkay() {
        return okay;
    }

    public void setOkay(boolean okay) {
        this.okay = okay;
    }
}