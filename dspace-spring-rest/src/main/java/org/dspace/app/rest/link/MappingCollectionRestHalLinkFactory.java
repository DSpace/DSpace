/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import org.dspace.app.rest.MappingCollectionRestController;

/**
 * This class acts as an abstract class for the MappingCollectionResourceWrapperHalLinkFactory to inherit from
 * so it already has the Controller defined
 */
public abstract class MappingCollectionRestHalLinkFactory<T>
    extends HalLinkFactory<T, MappingCollectionRestController> {

    @Override
    protected Class<MappingCollectionRestController> getControllerClass() {
        return MappingCollectionRestController.class;
    }
}
