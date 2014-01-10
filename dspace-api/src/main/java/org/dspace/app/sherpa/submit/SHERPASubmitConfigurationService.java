/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.submit;

import java.util.List;

public class SHERPASubmitConfigurationService
{
    private List<ISSNItemExtractor> issnItemExtractors;

    public void setIssnItemExtractors(List<ISSNItemExtractor> issnItemExtractors)
    {
        this.issnItemExtractors = issnItemExtractors;
    }
    
    public List<ISSNItemExtractor> getIssnItemExtractors()
    {
        return issnItemExtractors;
    }
}