/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.swordapp.server.SwordCollection;

/**
 * @author Richard Jones
 *
 * Define an abstract interface for classes wishing to generate ATOM Collections
 * for SWORD service documents
 */
public interface AtomCollectionGenerator
{
    /**
     * Build the ATOM Collection which represents the given DSpace Object.
     *
     * @param dso
     * @throws DSpaceSwordException
     */
    public SwordCollection buildCollection(Context context, DSpaceObject dso,
            SwordConfigurationDSpace config) throws DSpaceSwordException;
}
