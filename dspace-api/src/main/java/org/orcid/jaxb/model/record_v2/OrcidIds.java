/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * 
 * http://www.dspace.org/license/
 */
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

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "orcidIds" })
@XmlRootElement(name = "orcid-ids", namespace = "http://www.orcid.org/ns/orcid-id")
public class OrcidIds implements Serializable {
    private static final long serialVersionUID = 921607209700657276L;
    @XmlElement(name = "orcid-id", namespace = "http://www.orcid.org/ns/orcid-id")
    List<OrcidId> orcidIds;

    public List<OrcidId> getOrcidIds() {
        if (orcidIds == null) {
            orcidIds = new ArrayList<>();
        }
        return orcidIds;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((orcidIds == null) ? 0 : orcidIds.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        OrcidIds other = (OrcidIds) obj;
        if (orcidIds == null && other.orcidIds != null) {
            return false;
        }

        if (orcidIds.size() != other.orcidIds.size()) {
            return false;
        }

        for (int i = 0; i < orcidIds.size(); i++) {
            if (!orcidIds.get(i).equals(other.orcidIds.get(i))) {
                return false;
            }
        }
        return true;
    }

}
