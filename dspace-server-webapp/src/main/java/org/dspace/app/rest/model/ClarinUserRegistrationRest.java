/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * The Clarin User Registration REST Resource
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@LinksRest(links = {
        @LinkRest(
                name = ClarinUserRegistrationRest.CLARIN_LICENSES,
                method = "getClarinLicenses"
        ),
        @LinkRest(
                name = ClarinUserRegistrationRest.USER_METADATA,
                method = "getUserMetadata"
        )
})
public class ClarinUserRegistrationRest extends BaseObjectRest<Integer> {
    public static final String NAME = "clarinuserregistration";
    public static final String CATEGORY = RestAddressableModel.CORE;

    public static final String CLARIN_LICENSES = "clarinLicenses";
    public static final String USER_METADATA = "userMetadata";

    public UUID ePersonID;
    public String email;
    public String organization;
    public boolean confirmation;

    public ClarinUserRegistrationRest() {
    }

    public UUID getePersonID() {
        return ePersonID;
    }

    public void setePersonID(UUID ePersonID) {
        this.ePersonID = ePersonID;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public boolean isConfirmation() {
        return confirmation;
    }

    public void setConfirmation(boolean confirmation) {
        this.confirmation = confirmation;
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
