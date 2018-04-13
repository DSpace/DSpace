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

import org.orcid.jaxb.model.common_v2.*;

public interface Affiliation {
    String getDepartmentName();

    void setDepartmentName(String value);

    String getRoleTitle();

    void setRoleTitle(String value);

    FuzzyDate getStartDate();

    void setStartDate(FuzzyDate value);

    FuzzyDate getEndDate();

    void setEndDate(FuzzyDate value);

    Organization getOrganization();

    void setOrganization(Organization value);

    Source getSource();

    void setSource(Source value);

    Visibility getVisibility();

    void setVisibility(Visibility value);

    Long getPutCode();

    void setPutCode(Long value);

    CreatedDate getCreatedDate();

    void setCreatedDate(CreatedDate value);

    LastModifiedDate getLastModifiedDate();

    void setLastModifiedDate(LastModifiedDate value);
}