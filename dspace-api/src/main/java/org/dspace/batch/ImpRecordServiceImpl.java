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

import org.dspace.batch.dao.ImpBitstreamDAO;
import org.dspace.batch.dao.ImpBitstreamMetadatavalueDAO;
import org.dspace.batch.dao.ImpMetadatavalueDAO;
import org.dspace.batch.dao.ImpRecordDAO;
import org.dspace.batch.dao.ImpWorkflowNStateDAO;
import org.dspace.batch.service.ImpRecordService;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;

/***
 * Service implementation used to access ImpRecord entities.
 * 
 * @See {@link org.dspace.batch.ImpRecord}
 * 
 * @author fcadili (francesco.cadili at 4science.it)
 *
 */
public class ImpRecordServiceImpl implements ImpRecordService {

    @Autowired(required = true)
    private ImpRecordDAO impRecordDAO;

    @Autowired(required = true)
    private ImpBitstreamDAO impBitstreamDAO;

    @Autowired(required = true)
    private ImpBitstreamMetadatavalueDAO impBitstreamMetadatavalueDAO;

    @Autowired(required = true)
    private ImpMetadatavalueDAO impMetadatavalueDAO;

    @Autowired(required = true)
    private ImpWorkflowNStateDAO impWorkflowNStateDAO;

    @Override
    public ImpRecord create(Context context, ImpRecord impRecord) throws SQLException {
        impRecord = impRecordDAO.create(context, impRecord);
        return impRecord;
    }

    @Override
    public void setImpCollection(ImpRecord impRecord, Collection collection) {
        impRecord.setImpCollectionUuid(collection.getID());
    }

    @Override
    public void setImpEperson(ImpRecord impRecord, EPerson ePerson) {
        impRecord.setImpEpersonUuid(ePerson.getID());
    }

    @Override
    public void setStatus(ImpRecord impRecord, Character status) {
        if (status != 'p' && status != 'w' && status != 'z' && status != 'g') {
            throw new IllegalArgumentException("The status (" + status + ") is not valid. Use p, w, z, g");
        } else {
            impRecord.setStatus(status.toString());
        }
    }

    @Override
    public void setOperation(ImpRecord impRecord, String operation) {
        if (!"update".equalsIgnoreCase(operation) && !"delete".equalsIgnoreCase(operation)) {
            throw new IllegalArgumentException("The operation (" + operation + ") is not valid. Use update or delete");
        } else {
            impRecord.setOperation(operation);
        }
    }

    public List<ImpRecord> searchNewRecords(Context context) throws SQLException {
        return impRecordDAO.searchNewRecords(context);
    }

    @Override
    public ImpRecord findByID(Context context, int id) throws SQLException {
        return impRecordDAO.findByID(context, ImpRecord.class, id);
    }

    @Override
    public int countNewImpRecords(Context context, ImpRecord impRecord) throws SQLException {
        return impRecordDAO.countNewImpRecords(context, impRecord);
    }

    @Override
    public void update(Context context, ImpRecord impRecord) throws SQLException {
        impRecordDAO.save(context, impRecord);
    }

    @Override
    public void delete(Context context, ImpRecord impRecord) throws SQLException {
        impRecordDAO.delete(context, impRecord);
    }

    @Override
    public void cleanupTables(Context context) throws SQLException {
        // removes the data from the reverse order of creating the tables
        impBitstreamMetadatavalueDAO.deleteAll(context);
        impBitstreamDAO.deleteAll(context);
        impMetadatavalueDAO.deleteAll(context);
        impWorkflowNStateDAO.deleteAll(context);
        impRecordDAO.deleteAll(context);
    }
}