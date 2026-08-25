/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.Date;

import org.dspace.app.rest.model.ChecksumHistoryRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.checker.ChecksumHistory;
import org.springframework.stereotype.Component;

@Component
public class ChecksumHistoryConverter implements DSpaceConverter<ChecksumHistory, ChecksumHistoryRest> {

    @Override
    public ChecksumHistoryRest convert(ChecksumHistory modelObject, Projection projection) {
        ChecksumHistoryRest rest = new ChecksumHistoryRest();
        rest.setProjection(projection);

        rest.setId(Math.toIntExact(modelObject.getID()));
        rest.setChecksumExpected(modelObject.getChecksumExpected());
        rest.setChecksumCalculated(modelObject.getChecksumCalculated());

        if (modelObject.getProcessStartDate() != null) {
            rest.setProcessStartDate(Date.from(modelObject.getProcessStartDate()));
        }
        if (modelObject.getProcessEndDate() != null) {
            rest.setProcessEndDate(Date.from(modelObject.getProcessEndDate()));
        }

        if (modelObject.getResult() != null && modelObject.getResult().getResultCode() != null) {
            rest.setResultCodeValue(modelObject.getResult().getResultCode().name());
        }

        return rest;
    }

    @Override
    public Class<ChecksumHistory> getModelClass() {
        return ChecksumHistory.class;
    }
}
