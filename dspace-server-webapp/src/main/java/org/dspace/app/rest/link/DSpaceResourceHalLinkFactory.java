/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.LinkedList;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This class' purpose is to provide a means to add links to the HalResources
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
@Component
public class DSpaceResourceHalLinkFactory extends HalLinkFactory<DSpaceResource, RestResourceController> {

    @Autowired
    private Utils utils;

    protected void addLinks(DSpaceResource halResource, Pageable page, LinkedList<Link> list) throws Exception {
        RestAddressableModel data = halResource.getContent();

        try {
            for (PropertyDescriptor pd : Introspector.getBeanInfo(data.getClass()).getPropertyDescriptors()) {
                Method readMethod = pd.getReadMethod();
                String name = pd.getName();
                if (readMethod != null && !"class".equals(name)) {
                    LinkRest linkRest = utils.findLinkAnnotation(readMethod);

                    if (linkRest != null) {
                        if (StringUtils.isNotBlank(linkRest.name())) {
                            name = linkRest.name();
                        }

                        Link linkToSubResource = utils.linkToSubResource(data, name);
                        // no method is specified to retrieve the linked object(s) so check if it is already here
                        if (StringUtils.isBlank(linkRest.method())) {
                            Object linkedObject = readMethod.invoke(data);

                            if (linkedObject instanceof RestAddressableModel) {

                                linkToSubResource = utils
                                    .linkToSingleResource((RestAddressableModel) linkedObject, name);
                            }

                            if (!halResource.getContent().getProjection().allowLinking(halResource, linkRest)) {
                                continue; // projection disallows this optional method-level link
                            }

                            halResource.add(linkToSubResource);
                        }

                    } else if (RestModel.class.isAssignableFrom(readMethod.getReturnType())) {
                        Link linkToSubResource = utils.linkToSubResource(data, name);
                        halResource.add(linkToSubResource);
                    }
                }
            }
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }

        halResource.add(utils.linkToSingleResource(data, Link.REL_SELF));
    }

    protected Class<RestResourceController> getControllerClass() {
        return RestResourceController.class;
    }

    protected Class<DSpaceResource> getResourceClass() {
        return DSpaceResource.class;
    }

}
