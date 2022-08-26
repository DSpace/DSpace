/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class contains a list of active authority types.
 * It can be used to created a new instance of a specific type.
 * However if you need to make a new instance to store it in Solr, you need to use {@link AuthorityValueGenerator}.
 * To create an instance from a Solr record, use {@link AuthorityValue#fromSolr(SolrDocument)}.
 *
 * This class is instantiated in Spring and accessed by a static method in AuthorityValue.
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthorityTypes {

    /**
     * log4j logger
     */
    private static final Logger log = LogManager.getLogger(AuthorityTypes.class);

    protected List<AuthorityValue> types = new ArrayList<>();

    protected Map<String, AuthorityValue> fieldDefaults = new HashMap<>();


    public List<AuthorityValue> getTypes() {
        return types;
    }

    public void setTypes(List<AuthorityValue> types) {
        this.types = types;
    }

    public Map<String, AuthorityValue> getFieldDefaults() {
        return fieldDefaults;
    }

    public void setFieldDefaults(Map<String, AuthorityValue> fieldDefaults) {
        this.fieldDefaults = fieldDefaults;
    }

    public AuthorityValue getEmptyAuthorityValue(String type) {
        AuthorityValue result = null;
        for (AuthorityValue authorityValue : types) {
            if (authorityValue.getAuthorityType().equals(type)) {
                try {
                    result = authorityValue.getClass().getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException
                        | NoSuchMethodException | SecurityException
                        | IllegalArgumentException | InvocationTargetException e) {
                    log.error("Error", e);
                }
            }
        }
        if (result == null) {
            result = new AuthorityValue();
        }
        return result;
    }

}
