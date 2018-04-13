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

import org.orcid.jaxb.model.common_v2.OrcidIdentifier;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author Angel Montenegro
 * 
 */
@XmlRootElement(name = "application-group-orcid")
public class GroupOrcid extends OrcidIdentifier {
    private static final long serialVersionUID = -7831298842584309866L;

}
