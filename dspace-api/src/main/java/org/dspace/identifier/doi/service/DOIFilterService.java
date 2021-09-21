package org.dspace.identifier.doi.service;

import org.dspace.content.DSpaceObject;

public interface DOIFilterService {
    
    /**
     * Determine if DSO is eligible for register at DOI connector registration agency. It is determined 
     * based on the check of different filters conditions.
     * @param dso
     * @return false if the item does not apply any filters conditions. Else, return true.
     */
    public boolean isEligibleDSO(DSpaceObject dso);
    
    /**
     * Determine if DSO has external DOI among it metadata. External DOIs are those that not match the
     * DOI prefix of the repository (set at "identifier.doi.prefix").
     * @param dso
     * @return false if the item does not have external DOI. Else, return true.
     */
    public boolean hasExternalDOI(DSpaceObject dso);

}
