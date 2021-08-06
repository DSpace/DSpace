/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

import static org.dspace.app.rest.utils.Utils.PROJECTION_PARAM_NAME;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletRequest;

import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class for projections which add properties to the JSON response.
 * This is used to help define the projection parameters to add to the HAL links
 * It will only add the projection parameters to HAL links which are impacted by the given projection
 * It will also only add the projection parameters if the projection was actually requested.
 *
 * @author Maria Verdonck (Atmire) on 09/07/2021
 */
public abstract class PropertyProjection extends AbstractProjection {

    @Autowired
    RequestService requestService;

    /**
     * Getter for the name of the query parameter linked to this {@link PropertyProjection}
     *
     * @return The name of the query parameter linked to this {@link PropertyProjection}
     */
    public abstract String getParamName();

    /**
     * Whether or not this {@link PropertyProjection} supports the given {@link Class<RestAddressableModel>}
     *
     * @param restAddressableModelClass The class of the object of the link we're optionally adding projection query
     *                                  parameters to
     * @return True if this {@link PropertyProjection} supports the given {@link Class<RestAddressableModel>},
     * otherwise false
     */
    public abstract boolean supportsRestAddressableModelClasses(Class<RestAddressableModel> restAddressableModelClass);

    @Override
    public Map<String, List<String>> getProjectionParametersForHalLink(
        Class<RestAddressableModel> restAddressableModelClass) {

        if (this.supportsRestAddressableModelClasses(restAddressableModelClass)) {
            Map<String, List<String>> mapProjectionParams = new HashMap<>();
            // Projection section of links ex:
            // projection=CheckRelatedItem&checkRelatedItem=isAuthorOfPublication=b1b2c768-bda1-448a-a073-fc541e8b24d9
            mapProjectionParams.put(PROJECTION_PARAM_NAME, Arrays.asList(this.getName()));
            ServletRequest servletRequest = requestService.getCurrentRequest().getServletRequest();
            mapProjectionParams
                .put(this.getParamName(), Arrays.asList(servletRequest.getParameterValues(this.getParamName())));
            return mapProjectionParams;
        }
        return super.getProjectionParametersForHalLink(restAddressableModelClass);
    }

}
