/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import java.util.Map;
import org.dspace.identifier.IdentifierException;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 *
 * @author Pascal-Nicolas Becker (p dot becker at tu hyphen berlin dot de)
 */
public abstract class RegistrationAgency {
    protected ConfigurationService configurationService;
    
    @Autowired
    @Required
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }
    
    /**
     * RegistrationAgencies needs to support default constructor.
     */
    public RegistrationAgency() {}

    public abstract boolean create(String identifier, Map<String,String> metadata) throws IdentifierException;
    public abstract boolean reserve(String identifier, Map<String,String> metadata) throws IdentifierException;
    public abstract String mint(Map<String,String> metadata) throws IdentifierException;
    public abstract boolean delete(String id) throws IdentifierException;
    
}
