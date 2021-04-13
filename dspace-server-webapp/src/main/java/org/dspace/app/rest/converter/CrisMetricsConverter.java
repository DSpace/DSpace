/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.rest.model.CrisMetricsRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.metrics.CrisItemMetricsService;
import org.dspace.metrics.embeddable.model.EmbeddableCrisMetrics;
import org.springframework.stereotype.Component;

/**
 * This is the converter from Entity CrisMetrics to the REST data model
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
public class CrisMetricsConverter implements DSpaceConverter<CrisMetrics, CrisMetricsRest> {

    /* (non-Javadoc)
     * @see org.dspace.app.rest.converter.DSpaceConverter#convert
     * (java.lang.Object, org.dspace.app.rest.projection.Projection)
     */
    @Override
    public CrisMetricsRest convert(CrisMetrics model, Projection projection) {
        CrisMetricsRest rest = new CrisMetricsRest();
        rest.setId(convertId(model));
        rest.setMetricType(model.getMetricType());
        rest.setMetricCount(model.getMetricCount());
        rest.setAcquisitionDate(model.getAcquisitionDate());
        rest.setStartDate(model.getStartDate());
        rest.setEndDate(model.getEndDate());
        rest.setRemark(model.getRemark());
        rest.setLast(model.getLast());
        rest.setDeltaPeriod1(model.getDeltaPeriod1());
        rest.setDeltaPeriod2(model.getDeltaPeriod2());
        rest.setRank(model.getRank());
        return rest;
    }

    protected String convertId(CrisMetrics model) {
        if (model instanceof EmbeddableCrisMetrics) {
            return ((EmbeddableCrisMetrics)model).getEmbeddableId();
        }
        return CrisItemMetricsService.STORED_METRIC_ID_PREFIX + model.getId();
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.converter.DSpaceConverter#getModelClass()
     */
    @Override
    public Class<CrisMetrics> getModelClass() {
        return CrisMetrics.class;
    }

}
