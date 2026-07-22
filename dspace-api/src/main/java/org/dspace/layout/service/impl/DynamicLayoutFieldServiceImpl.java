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
import org.dspace.layout.dao.DynamicLayoutFieldDAO;
import org.dspace.layout.service.DynamicLayoutFieldService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of service to manage Fields component of layout
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class DynamicLayoutFieldServiceImpl implements DynamicLayoutFieldService {

    @Autowired
    private DynamicLayoutFieldDAO dao;

    @Override
    public DynamicLayoutField create(Context context) throws SQLException, AuthorizeException {
        return dao.create(context, new DynamicLayoutField());
    }

    @Override
    public DynamicLayoutField find(Context context, int id) throws SQLException {
        return dao.findByID(context, DynamicLayoutField.class, id);
    }

    @Override
    public void update(Context context, DynamicLayoutField field) throws SQLException, AuthorizeException {
        dao.save(context, field);
    }

    @Override
    public void update(Context context, List<DynamicLayoutField> fieldList) throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(fieldList)) {
            for (DynamicLayoutField field: fieldList) {
                update(context, field);
            }
        }
    }

    @Override
    public void delete(Context context, DynamicLayoutField field) throws SQLException, AuthorizeException {
        dao.delete(context, field);
    }

    @Override
    public DynamicLayoutField create(Context context, DynamicLayoutField field) throws SQLException {
        return dao.create(context, field);
    }

    @Override
    public Long countFieldInBox(Context context, Integer boxId) throws SQLException {
        return dao.countByBoxId(context, boxId);
    }

    @Override
    public List<DynamicLayoutField> findFieldByBoxId(Context context, Integer boxId, Integer limit, Integer offset)
            throws SQLException {
        return dao.findByBoxId(context, boxId, limit, offset);
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.service.DynamicLayoutFieldService#
     * create(org.dspace.core.Context, java.lang.Integer, java.lang.Integer)
     */
    @Override
    public DynamicLayoutField create(Context context, MetadataField mf, Integer row, Integer priority)
            throws SQLException {
        DynamicLayoutField field = new DynamicLayoutField();
        field.setRow(row);
        field.setPriority(priority);
        field.setMetadataField(mf);
        return dao.create(context, field);
    }

    /* (non-Javadoc)
     * @see org.dspace.layout.service.DynamicLayoutFieldService#
     * findFieldByBoxId(org.dspace.core.Context, java.lang.Integer, java.lang.Integer)
     */
    @Override
    public List<DynamicLayoutField> findFieldByBoxId(Context context, Integer boxId, Integer row) throws SQLException {
        return dao.findByBoxId(context, boxId, row);
    }

}
