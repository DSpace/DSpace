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
package org.orcid.jaxb.model.message;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement(name = "source-orcid")
public class SourceOrcid extends OrcidIdBase implements Serializable {

    private static final long serialVersionUID = 1L;

    public SourceOrcid() {
        super();
    }

    public SourceOrcid(String path) {
        super(path);
    }

    public SourceOrcid(OrcidIdBase other) {
        super(other);
    }

}
