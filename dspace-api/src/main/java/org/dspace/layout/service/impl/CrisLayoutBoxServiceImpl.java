/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.EntityType;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.dao.CrisLayoutBoxDAO;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.springframework.beans.factory.annotation.Autowired;

public class CrisLayoutBoxServiceImpl implements CrisLayoutBoxService {

    @Autowired
    private CrisLayoutBoxDAO dao;

    @Override
    public CrisLayoutBox create(Context context) throws SQLException, AuthorizeException {
        return dao.create(context, new CrisLayoutBox());
    }

    @Override
    public CrisLayoutBox find(Context context, int id) throws SQLException {
        return dao.findByID(context, CrisLayoutBox.class, id);
    }

    @Override
    public void update(Context context, CrisLayoutBox boxList) throws SQLException, AuthorizeException {
        update(context,Collections.singletonList(boxList));
    }

    @Override
    public void update(Context context, List<CrisLayoutBox> boxList) throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(boxList)) {
            for (CrisLayoutBox box: boxList) {
                dao.save(context, box);
            }
        }
    }

    @Override
    public void delete(Context context, CrisLayoutBox box) throws SQLException, AuthorizeException {
        dao.delete(context, box);
    }

    @Override
    public CrisLayoutBox create(Context context, CrisLayoutBox box) throws SQLException {
        return dao.create(context, box);
    }

    @Override
    public CrisLayoutBox create(Context context, EntityType eType, boolean collapsed, int priority, boolean minor)
            throws SQLException {
        CrisLayoutBox box = new CrisLayoutBox();
        box.setEntitytype(eType);
        box.setCollapsed(collapsed);
        box.setPriority(priority);
        box.setMinor(minor);
        return dao.create(context, box);
    }

}
