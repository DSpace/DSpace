/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import java.util.Date;

import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderResponse;

/**
 * Java Bean to expose Open Policy Finder policies during in progress submission.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class OpenPolicyFinderPolicy implements SectionData {

    private static final long serialVersionUID = 2440249335255683173L;

    private Date retrievalTime;

    private OpenPolicyFinderResponse opfResponse;

    public Date getRetrievalTime() {
        return retrievalTime;
    }

    public void setRetrievalTime(Date retrievalTime) {
        this.retrievalTime = retrievalTime;
    }

    public OpenPolicyFinderResponse getOpfResponse() {
        return opfResponse;
    }

    /**
     * Setting a opfResponse will automatically set the retrievealTime
     * of the section copying the value from the response if not null
     */
    public void setOpfResponse(OpenPolicyFinderResponse opfResponse) {
        this.opfResponse = opfResponse;
        this.retrievalTime = opfResponse.getRetrievalTime();
    }

}