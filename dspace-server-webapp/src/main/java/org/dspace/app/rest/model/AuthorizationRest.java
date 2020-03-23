/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import org.dspace.app.rest.RestResourceController;

/**
 * The Authorization REST Resource. An authorization is the representation of some rights that are available to a
 * specific user (eperson) on a defined object, eventually the whole repository (site object).
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@LinksRest(links = {
        @LinkRest(method = "getEperson", name = AuthorizationRest.EPERSON),
        @LinkRest(method = "getFeature", name = AuthorizationRest.FEATURE),
        @LinkRest(method = "getObject", name = AuthorizationRest.OBJECT)
})
public class AuthorizationRest extends BaseObjectRest<String> {
    public static final String NAME = "authorization";
    public static final String CATEGORY = RestAddressableModel.AUTHORIZATION;

    public static final String EPERSON = "eperson";
    public static final String FEATURE = "feature";
    public static final String OBJECT = "object";

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

}
