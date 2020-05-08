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

import com.amazonaws.util.CollectionUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBitstream;
import org.dspace.layout.dao.CrisLayoutBitstreamDAO;
import org.dspace.layout.service.CrisLayoutBitstreamService;
import org.springframework.beans.factory.annotation.Autowired;

public class CrisLayoutBitstreamServiceImpl implements CrisLayoutBitstreamService {

    @Autowired
    private CrisLayoutBitstreamDAO dao;

    @Override
    public CrisLayoutBitstream create(Context context) throws SQLException, AuthorizeException {
        return dao.create(context, new CrisLayoutBitstream());
    }

    @Override
    public CrisLayoutBitstream find(Context context, int id) throws SQLException {
        return dao.findByID(context, CrisLayoutBitstream.class, id);
    }

    @Override
    public void update(Context context, CrisLayoutBitstream stream) throws SQLException, AuthorizeException {
        dao.save(context, stream);
    }

    @Override
    public void update(Context context, List<CrisLayoutBitstream> streamList) throws SQLException, AuthorizeException {
        if (CollectionUtils.isNullOrEmpty(streamList)) {
            for (CrisLayoutBitstream stream: streamList) {
                update(context, stream);
            }
        }
    }

    @Override
    public void delete(Context context, CrisLayoutBitstream t) throws SQLException, AuthorizeException {
        dao.delete(context, t);
    }

    @Override
    public CrisLayoutBitstream create(Context ctx, CrisLayoutBitstream stream) throws SQLException {
        return dao.create(ctx, stream);
    }

}
