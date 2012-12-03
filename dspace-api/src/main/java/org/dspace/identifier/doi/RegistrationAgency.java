/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.identifier.doi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import org.dspace.identifier.IdentifierException;

/**
 *
 * @author Pascal-Nicolas Becker (p dot becker at tu hyphen berlin dot de)
 */
public abstract class RegistrationAgency {
    public abstract boolean create(String identifier, Map<String,String> metadata) throws IdentifierException;
    public abstract boolean reserve(String identifier, Map<String,String> metadata) throws IdentifierException;
    public abstract String mint(Map<String,String> metadata) throws IdentifierException;
    public abstract boolean delete(String id) throws IdentifierException;
    
}
