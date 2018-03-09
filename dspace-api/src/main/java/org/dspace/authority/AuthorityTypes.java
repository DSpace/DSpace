/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class contains a list of active authority types.
 * It can be used to created a new instance of a specific type.
 * However if you need to make a new instance to store it in solr, you need to use AuthorityValueGenerator.
 * To create an instance from a solr record, use AuthorityValue#fromSolr(SolrDocument).
 *
 * This class is instantiated in spring and accessed by a static method in AuthorityValue.
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
    private static Logger log = Logger.getLogger(AuthorityTypes.class);

    protected List<AuthorityValue> types = new ArrayList<AuthorityValue>();

    protected Map<String, AuthorityValue> fieldDefaults = new HashMap<String, AuthorityValue>();



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
                    result = authorityValue.getClass().newInstance();
                } catch (InstantiationException e) {
                    log.error("Error", e);
                } catch (IllegalAccessException e) {
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
