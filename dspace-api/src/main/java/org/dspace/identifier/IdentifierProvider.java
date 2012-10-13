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

    public abstract boolean supports(Class<? extends Identifier> identifier);

    public abstract boolean supports(String identifier);

    public abstract String register(Context context, DSpaceObject item) throws IdentifierException;

    public abstract String mint(Context context, DSpaceObject dso) throws IdentifierException;

    public abstract DSpaceObject resolve(Context context, String identifier, String... attributes) throws IdentifierNotFoundException, IdentifierNotResolvableException;;

    public abstract String lookup(Context context, DSpaceObject object) throws IdentifierNotFoundException, IdentifierNotResolvableException;;

    public abstract void delete(Context context, DSpaceObject dso) throws IdentifierException;

    public abstract void delete(Context context, DSpaceObject dso, String identifier) throws IdentifierException;

    public abstract void reserve(Context context, DSpaceObject dso, String identifier) throws IdentifierException;

    public abstract void register(Context context, DSpaceObject object, String identifier);
}
