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
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.dao.CrisLayoutTabDAO;
import org.dspace.layout.service.CrisLayoutTabService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CrisLayoutTabServiceImpl implements CrisLayoutTabService {

    @Autowired(required = true)
    private CrisLayoutTabDAO dao;

    @Override
    public CrisLayoutTab create(Context c, CrisLayoutTab tab) throws SQLException {
        return dao.create(c, tab);
    }

    @Override
    public CrisLayoutTab create(Context context) throws SQLException, AuthorizeException {
        return dao.create(context, new CrisLayoutTab());
    }

    @Override
    public CrisLayoutTab find(Context context, int id) throws SQLException {
        return dao.findByID(context, CrisLayoutTab.class, id);
    }

    @Override
    public void update(Context context, CrisLayoutTab tabList) throws SQLException, AuthorizeException {
        update(context,Collections.singletonList(tabList));
    }

    @Override
    public void update(Context context, List<CrisLayoutTab> tabList) throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(tabList)) {
            for (CrisLayoutTab tab: tabList) {
                dao.save(context, tab);
            }
        }
    }

    @Override
    public void delete(Context context, CrisLayoutTab tab) throws SQLException, AuthorizeException {
        dao.delete(context, tab);
    }

    @Override
    public CrisLayoutTab create(Context context, EntityType eType, Integer priority) throws SQLException {
        CrisLayoutTab tab = new CrisLayoutTab();
        tab.setEntity(eType);
        tab.setPriority(priority);
        return dao.create(context, tab);
    }

}
