/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.ldn.model.NotifyRequestStatus;
import org.dspace.app.rest.model.NotifyRequestStatusRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the NotifyRequestStatus in the DSpace API data model and
 * the REST data model
 *
 * @author Francesco Bacchelli (francesco.bacchelli at 4science.it)
 */
@Component
public class NotifyRequestStatusConverter implements DSpaceConverter<NotifyRequestStatus, NotifyRequestStatusRest> {

    @Override
    public NotifyRequestStatusRest convert(NotifyRequestStatus modelObject, Projection projection) {
        NotifyRequestStatusRest result = new NotifyRequestStatusRest();
        result.setItemuuid(modelObject.getItemUuid());
        result.setNotifyStatus(modelObject.getNotifyStatus());
        return result;
    }

    @Override
    public Class<NotifyRequestStatus> getModelClass() {
        return NotifyRequestStatus.class;
    }

}
