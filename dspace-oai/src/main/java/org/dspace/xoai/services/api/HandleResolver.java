/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.api;

import org.dspace.content.DSpaceObject;

public interface HandleResolver {
    DSpaceObject resolve (String handle) throws HandleResolverException;
    String getHandle (DSpaceObject object) throws HandleResolverException;
}
