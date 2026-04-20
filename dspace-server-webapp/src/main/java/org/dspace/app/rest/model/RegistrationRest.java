/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;


/**
 * This class acts as the REST representation of the RegistrationData model class.
 * This class acts as a data holder for the RegistrationResource
 * Refer to {@link org.dspace.eperson.RegistrationData} for explanation about the properties
 */
public class RegistrationRest extends RestAddressableModel {

    public static final String NAME = "registration";
    public static final String PLURAL_NAME = "registrations";
    public static final String CATEGORY = EPERSON;

    private Integer id;
    private String email;
    private UUID user;
    private String registrationType;
    private String netId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private MetadataRest<RegistrationMetadataRest> registrationMetadata;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Generic getter for the email
     *
     * @return the email value of this RegisterRest
     */
    public String getEmail() {
        return email;
    }

    /**
     * Generic setter for the email
     *
     * @param email The email to be set on this RegisterRest
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Generic getter for the user
     *
     * @return the user value of this RegisterRest
     */
    public UUID getUser() {
        return user;
    }

    /**
     * Generic setter for the user
     *
     * @param user The user to be set on this RegisterRest
     */
    public void setUser(UUID user) {
        this.user = user;
    }

    public String getRegistrationType() {
        return registrationType;
    }

    public void setRegistrationType(String registrationType) {
        this.registrationType = registrationType;
    }

    public String getNetId() {
        return netId;
    }

    public void setNetId(String netId) {
        this.netId = netId;
    }

    public MetadataRest<RegistrationMetadataRest> getRegistrationMetadata() {
        return registrationMetadata;
    }

    public void setRegistrationMetadata(
        MetadataRest<RegistrationMetadataRest> registrationMetadata) {
        this.registrationMetadata = registrationMetadata;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }
}
