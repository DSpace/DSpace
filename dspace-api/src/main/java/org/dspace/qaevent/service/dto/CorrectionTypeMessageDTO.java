/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.service.dto;

import java.io.Serializable;

/**
 * The CorrectionTypeMessageDTO class implements the QAMessageDTO interface
 * and represents a Data Transfer Object (DTO) for holding information
 * related to a correction type message in the context of Quality Assurance (QA).
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CorrectionTypeMessageDTO implements QAMessageDTO, Serializable {

    private static final long serialVersionUID = 2718151302291303796L;

    private String reason;

    public CorrectionTypeMessageDTO() {}

    public CorrectionTypeMessageDTO(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

}
