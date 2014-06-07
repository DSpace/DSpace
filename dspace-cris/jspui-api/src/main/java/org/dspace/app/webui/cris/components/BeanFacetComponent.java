/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components;

import java.util.ArrayList;
import java.util.List;



public class BeanFacetComponent extends BeanComponent
{    
    @Override
    public List<String> getFilters()
    {     
        List<String> result = new ArrayList<String>();
        result.add(getFacetQuery());
        return result;
    }
 
}
