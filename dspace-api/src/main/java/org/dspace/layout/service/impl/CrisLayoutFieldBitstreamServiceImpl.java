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
import org.dspace.layout.CrisLayoutFieldBitstream;
import org.dspace.layout.dao.CrisLayoutFieldBitstreamDAO;
import org.dspace.layout.service.CrisLayoutFieldBitstreamService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of service to manage Field Bitstream component of layout
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class CrisLayoutFieldBitstreamServiceImpl implements CrisLayoutFieldBitstreamService {

    @Autowired
    private CrisLayoutFieldBitstreamDAO dao;

    @Override
    public CrisLayoutFieldBitstream create(Context context) throws SQLException, AuthorizeException {
        return dao.create(context, new CrisLayoutFieldBitstream());
    }

    @Override
    public CrisLayoutFieldBitstream find(Context context, int id) throws SQLException {
        return dao.findByID(context, CrisLayoutFieldBitstream.class, id);
    }

    @Override
    public void update(Context context, CrisLayoutFieldBitstream stream) throws SQLException, AuthorizeException {
        dao.save(context, stream);
    }

    @Override
    public void update(Context context, List<CrisLayoutFieldBitstream> streamList)
            throws SQLException, AuthorizeException {
        if (CollectionUtils.isNullOrEmpty(streamList)) {
            for (CrisLayoutFieldBitstream stream: streamList) {
                update(context, stream);
            }
        }
    }

    @Override
    public void delete(Context context, CrisLayoutFieldBitstream t) throws SQLException, AuthorizeException {
        dao.delete(context, t);
    }

    @Override
    public CrisLayoutFieldBitstream create(Context ctx, CrisLayoutFieldBitstream stream) throws SQLException {
        return dao.create(ctx, stream);
    }

}
