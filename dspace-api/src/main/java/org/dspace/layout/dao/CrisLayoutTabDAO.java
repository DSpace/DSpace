/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.layout.CrisLayoutTab;

public interface CrisLayoutTabDAO extends GenericDAO<CrisLayoutTab> {

    public Long countTotal(Context context) throws SQLException;

    public Long countByEntityType(Context context, String entityType) throws SQLException;

    public List<CrisLayoutTab> findByEntityType(Context context, String entityType) throws SQLException;

    public List<CrisLayoutTab> findByEntityType(
            Context context, String entityType, Integer limit, Integer offset) throws SQLException;

    public Long totalMetadatafield(Context context, Integer tabId) throws SQLException;

    public List<MetadataField> getMetadataField(Context context, Integer tabId, Integer limit, Integer offset)
            throws SQLException;
}
