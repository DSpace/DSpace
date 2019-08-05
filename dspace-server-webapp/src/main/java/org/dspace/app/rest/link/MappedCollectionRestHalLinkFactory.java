/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import org.dspace.app.rest.MappedCollectionRestController;

/**
 * This class acts as an abstract class for the MappedCollectionResourceWrapperHalLinkFactory to inherit from
 * so it already has the Controller defined
 */
public abstract class MappedCollectionRestHalLinkFactory<T>
    extends HalLinkFactory<T, MappedCollectionRestController> {

    @Override
    protected Class<MappedCollectionRestController> getControllerClass() {
        return MappedCollectionRestController.class;
    }
}
