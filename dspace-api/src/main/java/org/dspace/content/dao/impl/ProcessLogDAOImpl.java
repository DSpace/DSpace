/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import java.sql.SQLException;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.dspace.content.dao.ProcessLogDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.scripts.Process;
import org.dspace.scripts.ProcessLog;
import org.dspace.scripts.ProcessLog_;

/**
 * This is the implementing class for the {@link ProcessLogDAO}
 */
public class ProcessLogDAOImpl extends AbstractHibernateDAO<ProcessLog> implements ProcessLogDAO {

    @Override
    public List<ProcessLog> findByProcess(Context context, Process process) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ProcessLog.class);
        Root<ProcessLog> processLogRoot = criteriaQuery.from(ProcessLog.class);
        criteriaQuery.select(processLogRoot);
        criteriaQuery.where(criteriaBuilder.equal(processLogRoot.get(ProcessLog_.process), process));

        return list(context, criteriaQuery, false, ProcessLog.class, -1, -1);
    }
}
