/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.data;

import java.util.ArrayList;
import java.util.List;

import org.dspace.core.ConfigurationManager;

import com.lyncode.xoai.dataprovider.data.AbstractAbout;
import com.lyncode.xoai.dataprovider.data.AbstractItem;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public abstract class DSpaceItem extends AbstractItem
{

    @Override
    public List<AbstractAbout> getAbout()
    {
        return new ArrayList<AbstractAbout>();
    }
    
    protected abstract String getHandle ();
    
    private String _prefix = null;

    @Override
    public String getIdentifier()
    {
        if (_prefix == null)
        {
            _prefix = ConfigurationManager.getProperty("oai",
                    "identifier.prefix");
        }
        return "oai:" + _prefix + ":" + this.getHandle();
    }
}
