/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

/**
 * The CorrectionTypeQAEventMessageRest class implements the QAEventMessageRest
 * interface and represents a message structure for Quality Assurance (QA)
 * events related to correction types.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class CorrectionTypeQAEventMessageRest implements QAEventMessageRest {

    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

}
