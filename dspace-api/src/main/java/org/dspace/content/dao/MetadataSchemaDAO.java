/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

import java.sql.SQLException;

/**
 * Database Access Object interface class for the MetadataSchema object.
 * The implementation of this class is responsible for all database calls for the MetadataSchema object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface MetadataSchemaDAO extends GenericDAO<MetadataSchema> {

    public MetadataSchema findByNamespace(Context context, String namespace) throws SQLException;

    public boolean uniqueNamespace(Context context, int metadataSchemaId, String namespace) throws SQLException;

    public boolean uniqueShortName(Context context, int metadataSchemaId, String name) throws SQLException;

    public MetadataSchema find(Context context, String shortName) throws SQLException;
}
