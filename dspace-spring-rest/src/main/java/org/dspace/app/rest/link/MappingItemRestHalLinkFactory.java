/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import org.dspace.app.rest.MappingItemRestController;

public abstract class MappingItemRestHalLinkFactory<T> extends HalLinkFactory<T, MappingItemRestController> {

    @Override
    protected Class<MappingItemRestController> getControllerClass() {
        return MappingItemRestController.class;
    }
}
