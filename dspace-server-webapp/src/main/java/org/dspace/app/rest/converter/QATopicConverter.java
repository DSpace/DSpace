/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.QATopicRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.qaevent.QATopic;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link DSpaceConverter} that converts {@link QATopic} to
 * {@link QATopicRest}.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component
public class QATopicConverter implements DSpaceConverter<QATopic, QATopicRest> {

    @Override
    public Class<QATopic> getModelClass() {
        return QATopic.class;
    }

    @Override
    public QATopicRest convert(QATopic modelObject, Projection projection) {
        QATopicRest rest = new QATopicRest();
        rest.setProjection(projection);
        rest.setId(modelObject.getSource() + ":" +
                modelObject.getKey().replace("/", "!") +
                (modelObject.getFocus() != null ? ":" + modelObject.getFocus().toString() : ""));
        rest.setName(modelObject.getKey());
        rest.setLastEvent(modelObject.getLastEvent());
        rest.setTotalEvents(modelObject.getTotalEvents());
        return rest;
    }

}
