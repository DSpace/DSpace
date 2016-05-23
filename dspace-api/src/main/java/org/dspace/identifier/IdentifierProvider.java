/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public abstract class IdentifierProvider {

    protected IdentifierService parentService;

    protected ConfigurationService configurationService;

    @Autowired
    @Required
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void setParentService(IdentifierService parentService) {
        this.parentService = parentService;
    }

    /**
     * Can this provider provide identifiers of a given type?
     * 
     * @param identifier requested type.
     * @return true if the given type is assignable from this provider's type.
     */
    public abstract boolean supports(Class<? extends Identifier> identifier);

    /**
     * Can this provider provide identifiers of a given type?
     * 
     * @param identifier requested type.
     * @return true if this provider can provide the named type of identifier.
     */
    public abstract boolean supports(String identifier);

    /**
     * Create and apply an identifier to a DSpaceObject.
     * If you just mark an identifier for an asynchronous registration, please call
     * {@link org.dspace.content.DSpaceObject#resetIdentifiersCache()} after its registration. If you register
     * identifiers directly in this method the IdentifierService will call this method for you.
     * 
     * @param context
     * @param item object to be named.
     * @return existing identifier of {@code item} if it has one, else a new identifier.
     * @throws IdentifierException if identifier error
     */
    public abstract String register(Context context, DSpaceObject item) throws IdentifierException;

    /**
     * Create an identifier for a DSpaceObject.
     * 
     * @param context
     * @param dso object to be named.
     * @return existing identifier of {@code dso} if it has one, else a new identifier.
     * @throws IdentifierException if identifier error
     */
    public abstract String mint(Context context, DSpaceObject dso) throws IdentifierException;

    /**
     * Find the object named by a given identifier.
     * 
     * @param context
     * @param identifier to be resolved.
     * @param attributes additional information for resolving {@code identifier}.
     * @return the named object.
     * @throws IdentifierNotFoundException if identifier not found
     * @throws IdentifierNotResolvableException if identifier not resolvable 
     */
    public abstract DSpaceObject resolve(Context context, String identifier, String... attributes) throws IdentifierNotFoundException, IdentifierNotResolvableException;;

    /**
     * Return the identifier for a DSpaceObject.
     * 
     * @param context
     * @param object The object to be looked up.
     * @return identifier for {@code object}.
     * @throws IdentifierNotFoundException if identifier not found
     * @throws IdentifierNotResolvableException if identifier not resolvable 
     */
    public abstract String lookup(Context context, DSpaceObject object) throws IdentifierNotFoundException, IdentifierNotResolvableException;;

    /**
     * Unbind this type of identifier(s) from an object.
     * 
     * @param context
     * @param dso object to lose its identity.
     * @throws IdentifierException if identifier error
     */
    public abstract void delete(Context context, DSpaceObject dso) throws IdentifierException;

    /**
     * Unbind the given identifier from an object.
     * 
     * @param context
     * @param dso object to be de-identified.
     * @param identifier to be removed.
     * @throws IdentifierException if identifier error
     */
    public abstract void delete(Context context, DSpaceObject dso, String identifier) throws IdentifierException;

    /**
     * Set an object's identifier.
     * 
     * @param context
     * @param dso object to be identified.
     * @param identifier to be set on the object.
     * @throws IdentifierException if identifier error
     */
    public abstract void reserve(Context context, DSpaceObject dso, String identifier) throws IdentifierException;

    /**
     * Create a specific identifier and apply it to an object.
     * 
     * @param context
     * @param object to be identified.
     * @param identifier to be created.
     * @throws IdentifierException if identifier error
     */
    public abstract void register(Context context, DSpaceObject object, String identifier)
            throws IdentifierException;
}
