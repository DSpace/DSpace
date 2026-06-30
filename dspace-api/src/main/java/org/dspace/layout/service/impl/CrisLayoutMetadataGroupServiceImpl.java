/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisMetadataGroup;
import org.dspace.layout.dao.CrisLayoutMetadataGroupDAO;
import org.dspace.layout.service.CrisLayoutMetadataGroupService;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * Implementation of service to manage Fields component of layout group
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.com)
 *
 */
public class CrisLayoutMetadataGroupServiceImpl implements CrisLayoutMetadataGroupService {

    @Autowired
    private CrisLayoutMetadataGroupDAO dao;
    @Override
    public CrisMetadataGroup create(Context context) throws SQLException, AuthorizeException {
        return dao.create(context, new CrisMetadataGroup());
    }
    @Override
    public CrisMetadataGroup find(Context context, int id) throws SQLException {
        return dao.findByID(context, CrisMetadataGroup.class, id);
    }
    @Override
    public void update(Context context, CrisMetadataGroup nestedField) throws SQLException, AuthorizeException {
        dao.save(context, nestedField);
    }
    @Override
    public void update(Context context, List<CrisMetadataGroup> nestedFieldList)
        throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(nestedFieldList)) {
            for (CrisMetadataGroup field: nestedFieldList) {
                update(context, field);
            }
        }
    }
    @Override
    public void delete(Context context, CrisMetadataGroup nestedField) throws SQLException, AuthorizeException {
        dao.delete(context, nestedField);
    }
    @Override
    public CrisMetadataGroup create(Context context, CrisMetadataGroup nestedField) throws SQLException {
        return dao.create(context, nestedField);
    }
    @Override
    public Long countNestedFieldInCrisField(Context context, Integer field_id) throws SQLException {
        return dao.countByFieldId(context, field_id);
    }
    @Override
    public List<CrisMetadataGroup> findNestedFieldByFieldId(Context context,
                                                            Integer field_id,
                                                            Integer limit,
                                                            Integer offset)
            throws SQLException {
        return dao.findByFieldId(context, field_id, limit, offset);
    }

    @Override
    public List<CrisMetadataGroup> findNestedFieldByFieldId(Context context, Integer field_id)
            throws SQLException {
        return dao.findByFieldId(context, field_id, null, null);
    }
    @Override
    public CrisMetadataGroup create(Context context, MetadataField mf, CrisLayoutField cf, Integer priority)
            throws SQLException {
        CrisMetadataGroup nestedField = new CrisMetadataGroup();
        nestedField.setPriority(priority);
        nestedField.setMetadataField(mf);
        nestedField.setCrisLayoutField(cf);
        return dao.create(context, nestedField);
    }
}