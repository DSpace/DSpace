package org.dspace.identifier;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 9/1/11
 * Time: 11:21 AM
 * To change this template use File | Settings | File Templates.
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

    public abstract boolean supports(String identifier);

    public abstract String register(Context context, DSpaceObject item) throws IdentifierException;

    public abstract String mint(Context context, DSpaceObject dso) throws IdentifierException;

    public abstract DSpaceObject resolve(Context context, String identifier, String... attributes) throws IdentifierNotFoundException, IdentifierNotResolvableException;;

    public abstract void delete(Context context, DSpaceObject dso) throws IdentifierException;
}
