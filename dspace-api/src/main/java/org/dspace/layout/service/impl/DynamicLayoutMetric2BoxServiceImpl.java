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

import org.apache.commons.collections.CollectionUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.DynamicLayoutMetric2Box;
import org.dspace.layout.dao.DynamicLayoutMetric2BoxDAO;
import org.dspace.layout.service.DynamicLayoutMetric2BoxService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of service to manage Metric component of layout
 * 
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public class DynamicLayoutMetric2BoxServiceImpl implements DynamicLayoutMetric2BoxService {

    @Autowired
    private DynamicLayoutMetric2BoxDAO dao;

    @Override
    public DynamicLayoutMetric2Box create(Context context) throws SQLException, AuthorizeException {
        return dao.create(context, new DynamicLayoutMetric2Box());
    }

    @Override
    public DynamicLayoutMetric2Box find(Context context, int id) throws SQLException {
        return dao.findByID(context, DynamicLayoutMetric2Box.class, id);
    }

    @Override
    public void update(Context context, DynamicLayoutMetric2Box metric) throws SQLException, AuthorizeException {
        dao.save(context, metric);
    }

    @Override
    public void update(Context context, List<DynamicLayoutMetric2Box> metricList)
        throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(metricList)) {
            for (DynamicLayoutMetric2Box field: metricList) {
                update(context, field);
            }
        }
    }

    @Override
    public void delete(Context context, DynamicLayoutMetric2Box metric) throws SQLException, AuthorizeException {
        dao.delete(context, metric );
    }

    @Override
    public DynamicLayoutMetric2Box create(Context context, DynamicLayoutMetric2Box metric) {
        try {
            return dao.create(context, metric);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    @Override
    public DynamicLayoutBox addMetrics(Context context, DynamicLayoutBox box, List<String> metrics) {
        box.getMetric2box().clear();
        return appendMetrics(context, box, metrics);
    }

    @Override
    public DynamicLayoutBox appendMetrics(Context context, DynamicLayoutBox box, List<String> metrics) {
        int initialPosition = box.getMetric2box().size();
        for (String metric : metrics) {
            DynamicLayoutMetric2Box m2b = new DynamicLayoutMetric2Box();
            m2b.setPosition(initialPosition++);
            m2b.setType(metric);
            box.addMetric2box(m2b);
        }
        return box;
    }

}
