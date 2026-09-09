/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.DynamicLayoutMetric2Box;
import org.dspace.layout.service.DynamicLayoutMetric2BoxService;

/**
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public class DynamicLayoutMetric2BoxBuilder
    extends AbstractBuilder<DynamicLayoutMetric2Box, DynamicLayoutMetric2BoxService> {

    private static Logger log = LogManager.getLogger(DynamicLayoutMetric2BoxBuilder.class);

    private DynamicLayoutMetric2Box metric;

    public DynamicLayoutMetric2BoxBuilder(Context context) {
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
    public DynamicLayoutMetric2Box build() throws SQLException, AuthorizeException {
        try {
            getService().update(context, metric);
            context.dispatchEvents();

            indexingService.commit();
        } catch (Exception e) {
            log.error("Error in DynamicLayoutMetric2BoxBuilder.build(), error: ", e);
        }
        return metric;
    }

    public static DynamicLayoutMetric2BoxBuilder create(Context ctx,
                                                        DynamicLayoutBox box,
                                                        String metricType,
                                                        int position) {
        DynamicLayoutMetric2BoxBuilder builder = new DynamicLayoutMetric2BoxBuilder(ctx);
        DynamicLayoutMetric2Box metric = new DynamicLayoutMetric2Box(box, metricType, position);
        return builder.create(ctx, metric);
    }

    private DynamicLayoutMetric2BoxBuilder create(Context context, DynamicLayoutMetric2Box metric) {
        try {
            this.context = context;
            this.metric = getService().create(context, metric);
        } catch (Exception e) {
            log.error("Error in DynamicLayoutMetric2BoxBuilder.create(..), error: ", e);
        }
        return this;
    }

    @Override
    public void delete(Context c, DynamicLayoutMetric2Box dso) throws Exception {
        if (dso != null) {
            getService().delete(c, dso);
        }
    }

    public void delete(DynamicLayoutMetric2Box dso) throws Exception {
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
    protected DynamicLayoutMetric2BoxService getService() {
        return dynamicLayoutMetric2BoxService;
    }

    public DynamicLayoutMetric2BoxBuilder withBox(DynamicLayoutBox box) {
        this.metric.setBox(box);
        return this;
    }

    public DynamicLayoutMetric2BoxBuilder withMetricType(String metricType) {
        this.metric.setType(metricType);
        return this;
    }

}
