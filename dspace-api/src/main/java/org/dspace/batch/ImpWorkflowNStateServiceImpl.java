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
import org.dspace.batch.dao.ImpWorkflowNStateDAO;
import org.dspace.batch.service.ImpWorkflowNStateService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/***
 * Service implementation used to access ImpWorkflowNState entities.
 * 
 * @See {@link org.dspace.batch.ImpWorkflowNState}
 * 
 * @author fcadili (francesco.cadili at 4science.it)
 *
 */
public class ImpWorkflowNStateServiceImpl implements ImpWorkflowNStateService {
    /**
     * log4j category
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(ImpWorkflowNStateServiceImpl.class);

    @Autowired(required = true)
    private ImpWorkflowNStateDAO impWorkflowNState;

    @Override
    public ImpWorkflowNState create(Context context, ImpWorkflowNState impRecord) throws SQLException {
        return impWorkflowNState.create(context, impRecord);
    }

    @Override
    public List<ImpWorkflowNState> searchWorkflowOps(Context context, ImpRecord impRecord) throws SQLException {
        return impWorkflowNState.searchWorkflowOps(context, impRecord);
    }

    @Override
    public void update(Context context, ImpWorkflowNState impRecord) throws SQLException {
        impWorkflowNState.save(context, impRecord);
    }

    @Override
    public void delete(Context context, ImpWorkflowNState impRecord) throws SQLException {
        impWorkflowNState.delete(context, impRecord);
    }

}
