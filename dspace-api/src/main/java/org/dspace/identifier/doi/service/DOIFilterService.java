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

}
