/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

@LinksRest(links = {
    @LinkRest(name = ChecksumHistoryRest.BITSTREAM, method = "getBitstream")
})
public class ChecksumHistoryRest extends BaseObjectRest<Integer> {

    public static final String NAME = "checksumhistory";
    public static final String PLURAL_NAME = "checksumhistories";
    public static final String CATEGORY = RestAddressableModel.CORE;
    public static final String BITSTREAM = "bitstream";

    private Date processStartDate;
    private Date processEndDate;
    private String checksumExpected;
    private String checksumCalculated;
    // simplified ChecksumResult - just enum value
    private String resultCodeValue;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class<RestResourceController> getController() {
        return RestResourceController.class;
    }

    public void setProcessStartDate(Date processStartDate) {
        this.processStartDate = processStartDate;
    }

    public void setProcessEndDate(Date processEndDate) {
        this.processEndDate = processEndDate;
    }

    public void setChecksumExpected(String checksumExpected) {
        this.checksumExpected = checksumExpected;
    }

    public void setChecksumCalculated(String checksumCalculated) {
        this.checksumCalculated = checksumCalculated;
    }

    public void setResultCodeValue(String resultCodeValue) {
        this.resultCodeValue = resultCodeValue;
    }

    public Date getProcessStartDate() {
        return processStartDate;
    }

    public Date getProcessEndDate() {
        return processEndDate;
    }

    public String getChecksumExpected() {
        return checksumExpected;
    }

    public String getChecksumCalculated() {
        return checksumCalculated;
    }

    public String getResultCodeValue() {
        return resultCodeValue;
    }
}
