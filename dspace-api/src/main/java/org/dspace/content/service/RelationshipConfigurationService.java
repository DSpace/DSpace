/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.RelationshipTypeConfiguration;
import org.dspace.core.Context;

/**
 * Resolves stable relationship semantics from Spring definitions or existing CRIS authority configuration.
 *
 * @author Ben Bosman (ben . bosman at atmire.com)
 */
public interface RelationshipConfigurationService {
    RelationshipTypeConfiguration findForMetadataValue(MetadataValue value);

    RelationshipTypeConfiguration getByKey(String key);

    void validate(Context context, RelationshipTypeConfiguration configuration, Item left, Item right)
        throws SQLException;
}
