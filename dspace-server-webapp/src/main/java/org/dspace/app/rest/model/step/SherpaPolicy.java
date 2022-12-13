/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import java.util.Date;

import org.dspace.app.sherpa.v2.SHERPAResponse;

/**
 * Java Bean to expose Sherpa policies during in progress submission.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class SherpaPolicy implements SectionData {

    private static final long serialVersionUID = 2440249335255683173L;

    private Date retrievalTime;

    private SHERPAResponse sherpaResponse;

    public Date getRetrievalTime() {
        return retrievalTime;
    }

    public void setRetrievalTime(Date retrievalTime) {
        this.retrievalTime = retrievalTime;
    }

    public SHERPAResponse getSherpaResponse() {
        return sherpaResponse;
    }

    /**
     * Setting a sherpaResponse will automatically set the retrievealTime
     * of the section copying the value from the response if not null
     */
    public void setSherpaResponse(SHERPAResponse sherpaResponse) {
        this.sherpaResponse = sherpaResponse;
        this.retrievalTime = sherpaResponse.getRetrievalTime();
    }

}