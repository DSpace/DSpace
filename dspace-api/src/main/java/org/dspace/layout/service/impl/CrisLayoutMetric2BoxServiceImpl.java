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
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutMetric2Box;
import org.dspace.layout.dao.CrisLayoutMetric2BoxDAO;
import org.dspace.layout.service.CrisLayoutMetric2BoxService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of service to manage Metric component of layout
 * 
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public class CrisLayoutMetric2BoxServiceImpl implements CrisLayoutMetric2BoxService {

    @Autowired
    private CrisLayoutMetric2BoxDAO dao;

    @Override
    public CrisLayoutMetric2Box create(Context context) throws SQLException, AuthorizeException {
        return dao.create(context, new CrisLayoutMetric2Box());
    }

    @Override
    public CrisLayoutMetric2Box find(Context context, int id) throws SQLException {
        return dao.findByID(context, CrisLayoutMetric2Box.class, id);
    }

    @Override
    public void update(Context context, CrisLayoutMetric2Box metric) throws SQLException, AuthorizeException {
        dao.save(context, metric);
    }

    @Override
    public void update(Context context, List<CrisLayoutMetric2Box> metricList) throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(metricList)) {
            for (CrisLayoutMetric2Box field: metricList) {
                update(context, field);
            }
        }
    }

    @Override
    public void delete(Context context, CrisLayoutMetric2Box metric) throws SQLException, AuthorizeException {
        dao.delete(context, metric );
    }

    @Override
    public CrisLayoutMetric2Box create(Context context, CrisLayoutMetric2Box metric) throws SQLException {
        return dao.create(context, metric);
    }

    @Override
    public CrisLayoutBox addMetrics(Context context, CrisLayoutBox box, List<String> metrics) throws SQLException {
        box.getMetric2box().clear();
        this.createMetrics(context, box, metrics, 0);
        return box;
    }

    @Override
    public CrisLayoutBox appendMetrics(Context context, CrisLayoutBox box, List<String> metrics) throws SQLException {
        this.createMetrics(context, box, metrics, box.getMetric2box().size());
        return box;
    }

    private void createMetrics(Context context, CrisLayoutBox box,
            List<String> metrics, int initialPosition) throws SQLException {
        for (String metric : metrics) {
            CrisLayoutMetric2Box m2b = new CrisLayoutMetric2Box(box, metric, initialPosition++);
            this.create(context, m2b);
        }
    }

}
