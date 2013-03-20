/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.identifier.doi;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.identifier.IdentifierException;

/**
 *
 * @author Pascal-Nicolas Becker (p dot becker at tu hyphen berlin dot de)
 */
public class EZIDConnector implements DOIConnector {

    @Override
    public boolean isDOIReserved(Context context, String doi) throws IdentifierException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isDOIReserved(Context context, DSpaceObject dso, String doi) throws IdentifierException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isDOIRegistered(Context context, String doi) throws IdentifierException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isDOIRegistered(Context context, DSpaceObject dso, String doi) throws IdentifierException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean deleteDOI(Context context, String doi) throws IdentifierException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean reserveDOI(Context context, DSpaceObject dso, String doi) throws IdentifierException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean registerDOI(Context context, DSpaceObject dso, String doi) throws IdentifierException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void relax() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
