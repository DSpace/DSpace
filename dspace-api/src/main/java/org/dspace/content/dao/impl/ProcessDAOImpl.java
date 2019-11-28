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

import org.dspace.content.dao.ProcessDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.scripts.Process;
import org.dspace.scripts.Process_;

/**
 * 
 * Implementation class for {@link ProcessDAO}
 */
public class ProcessDAOImpl extends AbstractHibernateDAO<Process> implements ProcessDAO {

    @Override
    public List<Process> findAllSortByScript(Context context) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Process.class);
        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(processRoot);
        criteriaQuery.orderBy(criteriaBuilder.asc(processRoot.get(Process_.name)));

        return list(context, criteriaQuery, false, Process.class, -1, -1);

    }

    @Override
    public List<Process> findAllSortByStartTime(Context context) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Process.class);
        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(processRoot);
        criteriaQuery.orderBy(criteriaBuilder.desc(processRoot.get(Process_.startTime)),
                              criteriaBuilder.desc(processRoot.get(Process_.processId)));

        return list(context, criteriaQuery, false, Process.class, -1, -1);
    }

    @Override
    public List<Process> findAll(Context context, int limit, int offset) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Process.class);
        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(processRoot);

        return list(context, criteriaQuery, false, Process.class, limit, offset);
    }

    @Override
    public int countRows(Context context) throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Process.class);
        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(processRoot);

        return count(context, criteriaQuery, criteriaBuilder, processRoot);

    }
}


