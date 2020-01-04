/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import org.dspace.app.rest.RestResourceController;

/**
 * The Authorization REST Resource. An authorization is the representation of some rights that are available to a
 * specific user (eperson) on a defined object, eventually the whole repository (site object).
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class AuthorizationRest extends DSpaceObjectRest {
    public static final String NAME = "authorization";
    public static final String CATEGORY = RestAddressableModel.AUTHORIZATION;

    private String id;

    private EPersonRest eperson;

    private AuthorizationFeatureRest feature;

    private RestAddressableModel object;

    @Override
    @JsonProperty(access = Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @LinkRest(linkClass = EPersonRest.class)
    @JsonIgnore
    public EPersonRest getEperson() {
        return eperson;
    }

    public void setEperson(EPersonRest eperson) {
        this.eperson = eperson;
    }

    @LinkRest(linkClass = AuthorizationFeatureRest.class)
    @JsonIgnore
    public AuthorizationFeatureRest getFeature() {
        return feature;
    }

    public void setFeature(AuthorizationFeatureRest feature) {
        this.feature = feature;
    }

    @LinkRest(linkClass = RestAddressableModel.class)
    @JsonIgnore
    public RestAddressableModel getObject() {
        return object;
    }

    public void setObject(RestAddressableModel object) {
        this.object = object;
    }

}
