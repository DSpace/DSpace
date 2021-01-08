/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutMetric2Box;
import org.dspace.layout.service.CrisLayoutMetric2BoxService;

/**
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public class CrisLayoutMetric2BoxBuilder extends AbstractBuilder<CrisLayoutMetric2Box, CrisLayoutMetric2BoxService> {

    private static final Logger log = Logger.getLogger(CrisLayoutMetric2BoxBuilder.class);

    private CrisLayoutMetric2Box metric;

    public CrisLayoutMetric2BoxBuilder(Context context) {
        super(context);
    }
    /* (non-Javadoc)
     * @see org.dspace.app.rest.builder.AbstractBuilder#cleanup()
     */
    @Override
    public void cleanup() throws Exception {
        delete(metric);
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.builder.AbstractBuilder#build()
     */
    @Override
    public CrisLayoutMetric2Box build() throws SQLException, AuthorizeException {
        try {
            getService().update(context, metric);
            context.dispatchEvents();

            indexingService.commit();
        } catch (Exception e) {
            log.error("Error in CrisLayoutMetric2BoxBuilder.build(), error: ", e);
        }
        return metric;
    }

    public static CrisLayoutMetric2BoxBuilder create(Context ctx, CrisLayoutBox box, String metricType, int position) {
        CrisLayoutMetric2BoxBuilder builder = new CrisLayoutMetric2BoxBuilder(ctx);
        CrisLayoutMetric2Box metric = new CrisLayoutMetric2Box(box, metricType, position);
        return builder.create(ctx, metric);
    }

    private CrisLayoutMetric2BoxBuilder create(Context context, CrisLayoutMetric2Box metric) {
        try {
            this.context = context;
            this.metric = getService().create(context, metric);
        } catch (Exception e) {
            log.error("Error in CrisLayoutMetric2BoxBuilder.create(..), error: ", e);
        }
        return this;
    }

    @Override
    public void delete(Context c, CrisLayoutMetric2Box dso) throws Exception {
        if (dso != null) {
            getService().delete(c, dso);
        }
    }

    public void delete(CrisLayoutMetric2Box dso) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            getService().delete(c, metric);
            c.complete();
        }

        indexingService.commit();
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.builder.AbstractBuilder#getService()
     */
    @Override
    protected CrisLayoutMetric2BoxService getService() {
        return crisLayoutMetric2BoxService;
    }

    public CrisLayoutMetric2BoxBuilder withBox(CrisLayoutBox box) {
        this.metric.setBox(box);
        return this;
    }

    public CrisLayoutMetric2BoxBuilder withMetricType(String metricType) {
        this.metric.setType(metricType);
        return this;
    }

}
