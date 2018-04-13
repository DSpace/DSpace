/**
 * =============================================================================
 *
 * ORCID (R) Open Source
 * http://orcid.org
 *
 * Copyright (c) 2012-2014 ORCID, Inc.
 * Licensed under an MIT-Style License (MIT)
 * http://orcid.org/open-source-license
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 *
 * =============================================================================
 */
package org.orcid.jaxb.model.record_v2;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * 
 * @author Angel Montenegro
 * 
 */
@XmlRootElement(name = "credit-name", namespace = "http://www.orcid.org/ns/personal-details")
public class CreditName extends org.orcid.jaxb.model.common_v2.CreditName implements Serializable {
    private static final long serialVersionUID = -4407704518314072926L;

}
