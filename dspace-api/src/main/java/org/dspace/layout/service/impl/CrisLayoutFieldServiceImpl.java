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
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.dao.CrisLayoutFieldDAO;
import org.dspace.layout.service.CrisLayoutFieldService;
import org.springframework.beans.factory.annotation.Autowired;

public class CrisLayoutFieldServiceImpl implements CrisLayoutFieldService {

    @Autowired
    private CrisLayoutFieldDAO dao;

    @Override
    public CrisLayoutField create(Context context) throws SQLException, AuthorizeException {
        return dao.create(context, new CrisLayoutField());
    }

    @Override
    public CrisLayoutField find(Context context, int id) throws SQLException {
        return dao.findByID(context, CrisLayoutField.class, id);
    }

    @Override
    public void update(Context context, CrisLayoutField field) throws SQLException, AuthorizeException {
        dao.save(context, field);
    }

    @Override
    public void update(Context context, List<CrisLayoutField> fieldList) throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(fieldList)) {
            for (CrisLayoutField field: fieldList) {
                update(context, field);
            }
        }
    }

    @Override
    public void delete(Context context, CrisLayoutField field) throws SQLException, AuthorizeException {
        dao.delete(context, field);
    }

    @Override
    public CrisLayoutField create(Context context, CrisLayoutField field) throws SQLException {
        return dao.create(context, field);
    }

}
