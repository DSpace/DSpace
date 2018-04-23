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
import java.util.List;

/**
 * 
 * @author Angel Montenegro
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType( propOrder = { "scopePath" })
@XmlRootElement(name = "scope-paths", namespace = "http://www.orcid.org/ns/person")
public class ScopePaths implements Serializable {    
    private static final long serialVersionUID = -4080254383102192041L;
    @XmlElement(name = "scope-path", namespace = "http://www.orcid.org/ns/person")
    protected List<ScopePath> scopePath;

    public List<ScopePath> getScopePath() {
        return scopePath;
    }

    public void setScopePath(List<ScopePath> scopePath) {
        this.scopePath = scopePath;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((scopePath == null) ? 0 : scopePath.hashCode());
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
        ScopePaths other = (ScopePaths) obj;
        if (scopePath == null) {
            if (other.scopePath != null)
                return false;
        } else if (!scopePath.equals(other.scopePath))
            return false;
        return true;
    }        
}
