/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.utils.Utils;

/**
 * A base class for DSpace Rest HAL Resource. The HAL Resource wraps the REST
 * Resource adding support for the links and embedded resources. Each property
 * of the wrapped REST resource is automatically translated in a link and the
 * available information included as embedded resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class DSpaceResource<T extends RestAddressableModel> extends HALResource<T> {

    public DSpaceResource(T data, Utils utils) {
        super(data);
        utils.embedMethodLevelRels(this);
    }

    //Trick to make Java understand that our content extends RestAddressableModel
    @JsonUnwrapped
    @Override
    public T getContent() {
        return super.getContent();
    }
}
