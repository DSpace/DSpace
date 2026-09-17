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
import org.dspace.layout.DynamicLayoutField;
import org.dspace.layout.DynamicMetadataGroup;
import org.dspace.layout.dao.DynamicLayoutMetadataGroupDAO;
import org.dspace.layout.service.DynamicLayoutMetadataGroupService;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * Implementation of service to manage Fields component of layout group
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.com)
 *
 */
public class DynamicLayoutMetadataGroupServiceImpl implements DynamicLayoutMetadataGroupService {

    @Autowired
    private DynamicLayoutMetadataGroupDAO dao;
    @Override
    public DynamicMetadataGroup create(Context context) throws SQLException, AuthorizeException {
        return dao.create(context, new DynamicMetadataGroup());
    }
    @Override
    public DynamicMetadataGroup find(Context context, int id) throws SQLException {
        return dao.findByID(context, DynamicMetadataGroup.class, id);
    }
    @Override
    public void update(Context context, DynamicMetadataGroup nestedField) throws SQLException, AuthorizeException {
        dao.save(context, nestedField);
    }
    @Override
    public void update(Context context, List<DynamicMetadataGroup> nestedFieldList)
        throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(nestedFieldList)) {
            for (DynamicMetadataGroup field: nestedFieldList) {
                update(context, field);
            }
        }
    }
    @Override
    public void delete(Context context, DynamicMetadataGroup nestedField) throws SQLException, AuthorizeException {
        dao.delete(context, nestedField);
    }
    @Override
    public DynamicMetadataGroup create(Context context, DynamicMetadataGroup nestedField) throws SQLException {
        return dao.create(context, nestedField);
    }
    @Override
    public Long countNestedFieldInCrisField(Context context, Integer field_id) throws SQLException {
        return dao.countByFieldId(context, field_id);
    }
    @Override
    public List<DynamicMetadataGroup> findNestedFieldByFieldId(Context context,
                                                            Integer field_id,
                                                            Integer limit,
                                                            Integer offset)
            throws SQLException {
        return dao.findByFieldId(context, field_id, limit, offset);
    }

    @Override
    public List<DynamicMetadataGroup> findNestedFieldByFieldId(Context context, Integer field_id)
            throws SQLException {
        return dao.findByFieldId(context, field_id, null, null);
    }
    @Override
    public DynamicMetadataGroup create(Context context, MetadataField mf, DynamicLayoutField cf, Integer priority)
            throws SQLException {
        DynamicMetadataGroup nestedField = new DynamicMetadataGroup();
        nestedField.setPriority(priority);
        nestedField.setMetadataField(mf);
        nestedField.setDynamicLayoutField(cf);
        return dao.create(context, nestedField);
    }
}