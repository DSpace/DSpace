/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service;

import java.sql.SQLException;
import java.util.Set;

import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.layout.LayoutSecurity;

/**
 * Service that checks access rights related to  {@link LayoutSecurity} policy values.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public interface LayoutSecurityService {

    /**
     * Checks if given a {@link LayoutSecurity} value, a set of {@link MetadataField} metadata security fields,
     * access to required {@link Item} to {@link EPerson} user has to be granted
     *
     * @param layoutSecurity         security policy to be checked
     * @param context                current context
     * @param user                   user whom access right has to be checked
     * @param metadataSecurityFields set of {@link MetadataField} driving security policy
     * @param groupSecurityFields    set of {@link Group} driving security policy
     * @param item                   Item to check whether or not access has to be granted
     * @return {@code true} if access has to be granted, {@code false} otherwise.
     */
    boolean hasAccess(LayoutSecurity layoutSecurity, Context context, EPerson user,
                      Set<MetadataField> metadataSecurityFields, Set<Group> groupSecurityFields,
                      Item item) throws SQLException;
}
