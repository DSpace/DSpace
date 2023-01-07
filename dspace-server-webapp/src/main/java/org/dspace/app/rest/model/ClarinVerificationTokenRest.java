/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * The ClarinVerificationToken REST Resource
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinVerificationTokenRest extends BaseObjectRest<Integer> {
    public static final String NAME = "clarinverificationtoken";
    public static final String CATEGORY = RestAddressableModel.CORE;

    private String ePersonNetID;
    private String email;
    private String shibHeaders;
    private String token;

    public ClarinVerificationTokenRest() {
    }

    public String getePersonNetID() {
        return ePersonNetID;
    }

    public void setePersonNetID(String ePersonNetID) {
        this.ePersonNetID = ePersonNetID;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getShibHeaders() {
        return shibHeaders;
    }

    public void setShibHeaders(String shibHeaders) {
        this.shibHeaders = shibHeaders;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }
}
