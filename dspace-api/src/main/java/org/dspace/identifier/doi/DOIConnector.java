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

/**
 * A DOIConnector handles all calls to the API of your DOI registry.
 * 
 * A DOIConnector should care about rules of the registration agency. For
 * example, if the registration agency wants us to reserve a DOI before we can
 * register it, the DOIConnector should check if a DOI is reserved. Use a
 * {@link org.dspace.identifier.doi.DOIIdentifierException#DOIIdentifierException DOIIdentifierException}.
 * and set its error code in case of any errors.
 * For the given example you should use
 * {@code DOIIdentifierException.RESERVE_FIRST} as error code.
 *
 * @author Pascal-Nicolas Becker
 */
public interface DOIConnector {
    public boolean isDOIReserved(Context context, String doi)
            throws DOIIdentifierException;
    
    public boolean isDOIRegistered(Context context, String doi)
            throws DOIIdentifierException;
    
    /**
     * Sends the DELETE-Request to the DOI registry.
     * 
     * <p>This method sends a request to "delete" a DOI. As DOIs are persistent
     * identifiers they should never be deleted. For example, if you send a HTTP
     * DELETE request to the DataCite Metadata API directly, it will set the DOI
     * to inactive.</p>
     * 
     * @param context
     * @param doi
     * @throws DOIIdentifierException if DOI error
     */
    public void deleteDOI(Context context, String doi)
            throws DOIIdentifierException;
    
    /**
     * Sends a request to the DOI registry to reserve a DOI.
     * 
     * The DOIConnector should check weather this DOI is reserved for another
     * object already. In this case it should throw an 
     * {@link org.dspace.identifier.doi.DOIIdentifierException#DOIIdentifierException DOIIdentifierException}.
     * DOIIdentifierException} and set the error code to {@code 
     * DOIIdentifierException.DOI_ALREADY_EXISTS}.
     *
     * @param context
     * @param dso
     * @param doi
     * @throws DOIIdentifierException if DOI error
     */
    public void reserveDOI(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException;
    /**
     * Sends a request to the DOI registry to register a DOI.
     * 
     * The DOIConnector ensures compliance with the workflow of the registration
     * agency. For example, if a DOI has to be reserved before it can be
     * registered the DOIConnector has to check if it is reserved. In this case
     * you can throw an 
     * {@link org.dspace.identifier.doi.DOIIdentifierException#DOIIdentifierException DOIIdentifierException}.
     * and set the error code to 
     * {@code DOIIdentifierException.RESERVE_FIRST}.
     * 
     * @param context
     * @param dso
     * @param doi
     * @throws DOIIdentifierException if DOI error
     */
    public void registerDOI(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException;
    
    /**
     * Sends a request to the DOI registry to update metadata for a DOI.
     * 
     * The DOIConnector should check weather the DOI is reserved or registered 
     * for the specified DSpace Object before it sends the metadata update.
     * 
     * @param context
     * @param dso
     * @param doi
     * @throws DOIIdentifierException if DOI error
     */
    public void updateMetadata(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException;
}
