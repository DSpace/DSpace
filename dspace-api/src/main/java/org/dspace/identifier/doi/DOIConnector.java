/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier.doi;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.identifier.IdentifierException;

/**
 *
 * @author Pascal-Nicolas Becker
 */
public interface DOIConnector {
    public boolean isDOIReserved(Context context, String doi) throws IdentifierException;
    
    public boolean isDOIReserved(Context context, DSpaceObject dso, String doi) throws IdentifierException;
    
    public boolean isDOIRegistered(Context context, String doi) throws IdentifierException;
    
    public boolean isDOIRegistered(Context context, DSpaceObject dso, String doi) throws IdentifierException;

    public boolean deleteDOI(Context context, String doi) throws IdentifierException;
    
    public boolean reserveDOI(Context context, DSpaceObject dso, String doi) throws IdentifierException;
    
    public boolean registerDOI(Context context, DSpaceObject dso, String doi) throws IdentifierException;
    
    public void relax();
}
