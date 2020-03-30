/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.batch;

import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.dspace.batch.dao.ImpRecordToItemDAO;
import org.dspace.batch.service.ImpRecordToItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/***
 * Service implementation used to access ImpRecordToItem entities.
 * 
 * @See {@link org.dspace.batch.ImpRecordToItem}
 * 
 * @author fcadili (francesco.cadili at 4science.it)
 *
 */
public class ImpRecordToItemServiceImpl implements ImpRecordToItemService {
    /**
     * log4j category
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(ImpRecordToItemService.class);

    @Autowired(required = true)
    private ImpRecordToItemDAO impRecordToItemDAO;

    @Override
    public ImpRecordToItem create(Context context, ImpRecordToItem impRecordToItem) throws SQLException {
        impRecordToItem = impRecordToItemDAO.create(context, impRecordToItem);
        return impRecordToItem;
    }

    @Override
    public ImpRecordToItem findByPK(Context context, String impRecordId) throws SQLException {
        return impRecordToItemDAO.findByPK(context, impRecordId);
    }

    @Override
    public void update(Context context, ImpRecordToItem impRecordToItem) throws SQLException {
        impRecordToItemDAO.save(context, impRecordToItem);
    }

    @Override
    public void delete(Context context, ImpRecordToItem impRecordToItem) throws SQLException {
        impRecordToItemDAO.delete(context, impRecordToItem);
    }

}