/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.QASourceRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.qaevent.QASource;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link DSpaceConverter} that converts {@link QASource} to
 * {@link QASourceRest}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component
public class QASourceConverter implements DSpaceConverter<QASource, QASourceRest> {

    @Override
    public Class<QASource> getModelClass() {
        return QASource.class;
    }

    @Override
    public QASourceRest convert(QASource modelObject, Projection projection) {
        QASourceRest rest = new QASourceRest();
        rest.setProjection(projection);
        rest.setId(modelObject.getName()
                + (modelObject.getFocus() != null ? ":" + modelObject.getFocus().toString() : ""));
        rest.setLastEvent(modelObject.getLastEvent());
        rest.setTotalEvents(modelObject.getTotalEvents());
        return rest;
    }

}
