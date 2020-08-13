/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.ProcessStatus;
import org.dspace.content.dao.ProcessDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.scripts.Process;
import org.dspace.scripts.ProcessQueryParameterContainer;
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

    @Override
    public List<Process> search(Context context, ProcessQueryParameterContainer processQueryParameterContainer,
                                int limit, int offset) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Process.class);
        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(processRoot);

        addProcessQueryParameters(processQueryParameterContainer, criteriaBuilder, criteriaQuery, processRoot);
        return list(context, criteriaQuery, false, Process.class, limit, offset);

    }

    /**
     * This method will apply the variables in the {@link ProcessQueryParameterContainer} as criteria for the
     * {@link Process} objects to the given CriteriaQuery.
     * They'll need to adhere to these variables in order to be eligible for return
     * @param processQueryParameterContainer    The object containing the variables for the {@link Process}
     *                                          to adhere to
     * @param criteriaBuilder                   The current CriteriaBuilder
     * @param criteriaQuery                     The current CriteriaQuery
     * @param processRoot                       The processRoot
     */
    private void addProcessQueryParameters(ProcessQueryParameterContainer processQueryParameterContainer,
                                           CriteriaBuilder criteriaBuilder, CriteriaQuery criteriaQuery,
                                           Root<Process> processRoot) {
        List<Predicate> andPredicates = new LinkedList<>();
        String scriptName = processQueryParameterContainer.getScriptName();
        EPerson ePerson = processQueryParameterContainer.getEPerson();
        ProcessStatus processStatus = processQueryParameterContainer.getProcessStatus();
        if (StringUtils.isNotBlank(scriptName)) {
            andPredicates.add(criteriaBuilder.equal(processRoot.get(Process_.name), scriptName));
        }
        if (ePerson != null) {
            andPredicates.add(criteriaBuilder.equal(processRoot.get(Process_.E_PERSON), ePerson));
        }
        if (processStatus != null) {
            andPredicates.add(criteriaBuilder.equal(processRoot.get(Process_.PROCESS_STATUS), processStatus));
        }
        criteriaQuery.where(criteriaBuilder.and(andPredicates.toArray(new Predicate[]{})));
    }

    @Override
    public int countTotalWithParameters(Context context, ProcessQueryParameterContainer processQueryParameterContainer)
        throws SQLException {

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Process.class);
        Root<Process> processRoot = criteriaQuery.from(Process.class);
        criteriaQuery.select(processRoot);

        addProcessQueryParameters(processQueryParameterContainer, criteriaBuilder, criteriaQuery, processRoot);
        return count(context, criteriaQuery, criteriaBuilder, processRoot);
    }


}


