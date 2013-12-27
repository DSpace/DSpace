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
 * Please pay attention to the method {@link #purgeCachedInformation()}!
 *
 * @author Pascal-Nicolas Becker
 */
public interface DOIConnector {
    public boolean isDOIReserved(Context context, String doi)
            throws DOIIdentifierException;
    
    public boolean isDOIReserved(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException;
    
    public boolean isDOIRegistered(Context context, String doi)
            throws DOIIdentifierException;
    
    public boolean isDOIRegistered(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException;

    /**
     * Sends the DELETE-Request to the DOI registry.
     * 
     * <p>This method sends a request to "delete" a DOI. As DOIs are persistent
     * identifiers they should never be deleted. For example, if you send a HTTP
     * DELETE request to the DataCite Metadata API directly, it will set the DOI
     * to inactive.</p>
     *
     * <p>A DOIConnector does not have to check whether the DOI is reserved,
     * registered or not. It will only send the request and return the answer in
     * form of a boolean weather the deletion was successful or not. It may even
     * throw an DOIIdentifierException in case you are not allowed to delete a
     * DOI, the DOI does not exist, ... So please be sure that the deletion of a
     * DOI is conform with the rules of the registry and that the DOI is in the
     * appropriate state (f.e. reserved but not registered).</p>
     * 
     * @param context
     * @param doi
     * @return
     * @throws DOIIdentifierException 
     */
    public void deleteDOI(Context context, String doi)
            throws DOIIdentifierException;
    
    /**
     * Sends a request to the DOI registry to reserve a DOI.
     *
     * Please check on your own if the DOI is already reserved or even
     * registered before you try to reserve it. You can use
     * {@link isDOIRegistered} and {@link isDOIReserved} for it. The
     * DOIConnector won't do any tests and throws an DOIIdentifierException in
     * case of any problems with the DOI you want to reserve.
     *
     * @param context
     * @param dso
     * @param doi
     * @return
     * @throws DOIIdentifierException 
     */
    public void reserveDOI(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException;
    /**
     * Sends a request to the DOI registry to register a DOI.
     * 
     * Please check on your own if the DOI is already reserved or even
     * registered before you try to register it. You can use the methods
     * {@code DOIConnector.isDOIRegistered(...)} and
     * {@code DOIConnector.isDOIReserved(...)} for it. The DOIConnector won't
     * do any tests and throws an DOIIdentifierException in case of any problems
     * with the DOI you want to register.
     * 
     * @param context
     * @param dso
     * @param doi
     * @return
     * @throws DOIIdentifierException 
     */
    public void registerDOI(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException;
    
    /**
     * Sends a request to the DOI registry to update Metadata for a DOI.
     * The DOIConnector won't do any tests and throws an IdentifierException 
     * in case of any problems with the DOI you want to update the metadata.
     * 
     * @param context
     * @param dso
     * @param doi
     * @return
     * @throws IdentifierException 
     */
    public void updateMetadata(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException;
}
