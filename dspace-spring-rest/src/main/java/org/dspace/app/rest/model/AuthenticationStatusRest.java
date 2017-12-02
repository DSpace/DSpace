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

/**
 * Find out your authentication status.
 *
 */
public class AuthenticationStatusRest extends BaseObjectRest<Integer>
{
    private boolean okay;
    private boolean authenticated;

    public static final String NAME = "status";
    public static final String CATEGORY = "authn";

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    @JsonIgnore
    public String getTypePlural() {
        return getType();
    }

    public Class getController() {
        return RestResourceController.class;
    }


    private EPersonRest ePersonRest;

    public AuthenticationStatusRest() {
        setOkay(true);
        setAuthenticated(false);
    }

    public AuthenticationStatusRest(EPersonRest eperson) {
        setOkay(true);
        if(eperson != null) {
            setAuthenticated(true);
            this.ePersonRest = eperson;
        }
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