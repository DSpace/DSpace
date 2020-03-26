/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.batch;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.batch.dao.ImpBitstreamDAO;
import org.dspace.batch.service.ImpBitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/***
 * Service implementation used to access ImpBitstream entities.
 * 
 * @See {@link org.dspace.batch.ImpBitstream}
 * 
 * @author fcadili (francesco.cadili at 4science.it)
 *
 */
public class ImpBitstreamServiceImpl implements ImpBitstreamService {
    /**
     * log4j category
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(ImpBitstreamServiceImpl.class);

    @Autowired(required = true)
    private ImpBitstreamDAO impBitstreamDAO;

    @Override
    public ImpBitstream create(Context context, ImpBitstream impBitstream) throws SQLException {
        impBitstream = impBitstreamDAO.create(context, impBitstream);
        return impBitstream;
    }

    @Override
    public List<ImpBitstream> searchByImpRecord(Context context, ImpRecord impRecord) throws SQLException {
        return impBitstreamDAO.searchByImpRecord(context, impRecord);
    }

    @Override
    public ImpBitstream findByID(Context context, int id) throws SQLException {
        return impBitstreamDAO.findByID(context, ImpBitstream.class, id);
    }

    @Override
    public void update(Context context, ImpBitstream impBitstream) throws SQLException {
        impBitstreamDAO.save(context, impBitstream);
    }

    @Override
    public void delete(Context context, ImpBitstream impBitstream) throws SQLException {
        impBitstreamDAO.delete(context, impBitstream);
    }
}
