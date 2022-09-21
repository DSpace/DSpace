/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.dao.impl;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.Handle;
import org.dspace.handle.Handle_;
import org.dspace.handle.dao.HandleClarinDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the Handle object.
 * This class is responsible for specific database calls for the Handle object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class HandleClarinDAOImpl extends AbstractHibernateDAO<Handle> implements HandleClarinDAO {

    /**
     * The constant for the sorting option `url:external`.
     */
    private static final String EXTERNAL = "external";

    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(HandleClarinDAOImpl.class);

    @Override
    public List<Handle> findAll(Context context, String sortingColumnDef, int maxResult, int offset)
            throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Handle.class);
        Root<Handle> handleRoot = criteriaQuery.from(Handle.class);
        criteriaQuery.select(handleRoot);

        // If the sortingColumnDef is null return all Handles
        if (Objects.isNull(sortingColumnDef)) {
            return executeCriteriaQuery(context, criteriaQuery, false, maxResult, offset);
        }

        // load sortingColumn
        // the sortingColumnDefAsList should have 2 elements
        int sortingColumnIndex = 0;
        int sortingValueIndex = 1;
        String[] sortingColumnDefAsList = sortingColumnDef.split(":");
        if (ArrayUtils.isEmpty(sortingColumnDefAsList) || sortingColumnDefAsList.length < 2) {
            return executeCriteriaQuery(context, criteriaQuery, false, maxResult, offset);
        }

        String sortingValue = sortingColumnDefAsList[sortingValueIndex];
        String sortingColumnName = sortingColumnDefAsList[sortingColumnIndex];
        // set up the `where` clause to the criteria query
        switch (sortingColumnName) {
            case Handle_.RESOURCE_TYPE_ID:
                // set the Item resource type as default
                Integer sortingValueInt = Constants.ITEM;
                try {
                    sortingValueInt = Integer.parseInt(sortingValue);
                } catch (Exception e) {
                    log.error("Cannot search Handles with sorting option: resourceTypeId because the sorting " +
                            "definition is wrong. Cannot parse String to Integer because: " + e.getMessage());
                }
                criteriaQuery.where(criteriaBuilder.equal(handleRoot.get(Handle_.resourceTypeId), sortingValueInt));
                break;
            case Handle_.URL:
                if (StringUtils.equals(sortingValue, EXTERNAL)) {
                    criteriaQuery.where(criteriaBuilder.isNotNull(handleRoot.get(Handle_.url)));
                } else {
                    criteriaQuery.where(criteriaBuilder.isNull(handleRoot.get(Handle_.url)));
                }
                break;
            default:
                criteriaQuery.where(criteriaBuilder.like(handleRoot.get(Handle_.handle), sortingValue + "%"));
                break;
        }

        // orderBy
        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.desc(handleRoot.get(Handle_.handle)));

        return list(context, criteriaQuery, false, Handle.class, maxResult, offset);
    }
}
