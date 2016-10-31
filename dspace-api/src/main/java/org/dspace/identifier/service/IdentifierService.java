/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.identifier.Identifier;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;

import java.sql.SQLException;
import java.util.List;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public interface IdentifierService {

    /**
     * Get an identifier for a given object which is assignment-compatible
     * with a given Identifier type.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param dso the object to be identified.
     * @param identifier instance of an Identifier of the required type.
     * @return the matching identifier, or the site identifier if the object
     *  is a Site, or null if no matching identifier is found.
     */
    String lookup(Context context, DSpaceObject dso, Class<? extends Identifier> identifier);
    
    /**
     * Gets the identifiers all registered IdentifierProvider returns if asked 
     * to lookup the provided DSpaceObject.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param dso the object to be identified.
     * @return the matching identifiers, or the site identifier if the object
     *  is a Site, or an empty array if no matching identifier is found.
     */
    List<String> lookup(Context context, DSpaceObject dso);

    /**
     *
     * This will resolve a DSpaceObject based on a provided Identifier.
     * The Service will interrogate the providers in no particular order
     * and return the first successful result discovered.  If no resolution
     * is successful, the method will return null if no object is found.
     *
     * TODO: Verify null is returned.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param identifier
     *     instance of an Identifier of the required type.
     * @return the DSpace object associated with the identifier
     * @throws IdentifierNotFoundException if identifier not found
     * @throws IdentifierNotResolvableException if identifier not resolvable
     */
    DSpaceObject resolve(Context context, String identifier) throws IdentifierNotFoundException, IdentifierNotResolvableException;

    /**
     *
     * Reserves any identifiers necessary based on the capabilities of all providers in the service.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param dso
     *     DSpace object to be reserved
     * @throws AuthorizeException if authorization error
     * @throws SQLException if database error
     * @throws IdentifierException if identifier error
     */
    void reserve(Context context, DSpaceObject dso) throws AuthorizeException, SQLException, IdentifierException;

    /**
     *
     * Used to Reserve a Specific Identifier (for example a Handle,  hdl:1234.5/6) The provider is responsible for
     * Detecting and Processing the appropriate identifier, all Providers are interrogated, multiple providers
     * can process the same identifier.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param dso
     *     DSpace object to be reserved
     * @param identifier
     *     instance of an Identifier of the required type.
     * @throws AuthorizeException if authorization error
     * @throws SQLException if database error
     * @throws IdentifierException if identifier error
     */
    void reserve(Context context, DSpaceObject dso, String identifier) throws AuthorizeException, SQLException, IdentifierException;

    /**
     *
     * @param context
     *     The relevant DSpace Context.
     * @param dso
     *     DSpace object to be registered
     * @throws AuthorizeException if authorization error
     * @throws SQLException if database error
     * @throws IdentifierException if identifier error
     */
    void register(Context context, DSpaceObject dso) throws AuthorizeException, SQLException, IdentifierException;

    /**
     *
     * Used to Register a specific Identifier (for example a Handle, hdl:1234.5/6)
     * The provider is responsible for detecting and processing the appropriate
     * identifier.  All Providers are interrogated.  Multiple providers
     * can process the same identifier.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param dso
     *     DSpace object to be registered
     * @param identifier
     *     instance of an Identifier of the required type.
     * @throws AuthorizeException if authorization error
     * @throws SQLException if database error
     * @throws IdentifierException if identifier error
     */
    void register(Context context, DSpaceObject dso, String identifier) throws AuthorizeException, SQLException, IdentifierException;

    /**
     * Delete (Unbind) all identifiers registered for a specific DSpace item. Identifiers are "unbound" across
     * all providers in no particular order.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param dso
     *     DSpace object to be deleted
     * @throws AuthorizeException if authorization error
     * @throws SQLException if database error
     * @throws IdentifierException if identifier error
     */
    void delete(Context context, DSpaceObject dso) throws AuthorizeException, SQLException, IdentifierException;

    /**
     * Used to Delete a Specific Identifier (for example a Handle,  hdl:1234.5/6) The provider is responsible for
     * Detecting and Processing the appropriate identifier, all Providers are interrogated, multiple providers
     * can process the same identifier.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param dso
     *     DSpace object to be deleted
     * @param identifier
     *     instance of an Identifier of the required type.
     * @throws AuthorizeException if authorization error
     * @throws SQLException if database error
     * @throws IdentifierException if identifier error
     */
    void delete(Context context, DSpaceObject dso, String identifier) throws AuthorizeException, SQLException, IdentifierException;

}
