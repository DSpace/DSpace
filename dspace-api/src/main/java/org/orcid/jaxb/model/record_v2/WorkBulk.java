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

import org.orcid.jaxb.model.error_v2.OrcidError;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "bulk" })
@XmlRootElement(name = "bulk", namespace=  "http://www.orcid.org/ns/bulk")
public class WorkBulk implements Serializable {    
    private static final long serialVersionUID = 1338769097760031210L;
    
    @XmlElements({
        @XmlElement(namespace = "http://www.orcid.org/ns/work", name = "work", type = Work.class),
        @XmlElement(namespace = "http://www.orcid.org/ns/error", name = "error", type = OrcidError.class)
    })
    private List<BulkElement> bulk;

    public List<BulkElement> getBulk() {
        if(bulk == null) {
            bulk = new ArrayList<BulkElement>();
        }
        return bulk;
    }

    public void setBulk(List<BulkElement> bulk) {
        this.bulk = bulk;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bulk == null) ? 0 : bulk.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WorkBulk other = (WorkBulk) obj;
        if (bulk == null) {
            if (other.bulk != null)
                return false;
        } else if (!bulk.equals(other.bulk))
            return false;
        return true;
    }        
}
