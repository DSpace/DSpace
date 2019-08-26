/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import org.dspace.app.rest.MappedItemRestController;

/**
 * This class acts as an abstract class for the MappedItemResourceWrapperHalLinkFactory to inherit from
 * so it already has the Controller defined
 */
public abstract class MappedItemRestHalLinkFactory<T> extends HalLinkFactory<T, MappedItemRestController> {

    @Override
    protected Class<MappedItemRestController> getControllerClass() {
        return MappedItemRestController.class;
    }
}
