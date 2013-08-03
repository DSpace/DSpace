/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.util.Comparator;

import org.dspace.core.ConfigurationManager;

public class StringConfigurationComparator implements Comparator<String>
{
    private String prefix;
    
    public StringConfigurationComparator(String prefix)
    {
        this.prefix = prefix;
    }
    
    public StringConfigurationComparator()
    {
        prefix = "";
    }
    
    
    public int compare(String o1, String o2)
    {
        int i1 = ConfigurationManager.getIntProperty(prefix+o1, Integer.MAX_VALUE);
        int i2 = ConfigurationManager.getIntProperty(prefix+o2, Integer.MAX_VALUE);
        
        if (i1 == i2)
        {
            if (o1 != null)
            {
                return o1.compareTo(o2);
            }
            else if (o2 != null)
            {
                return o2.compareTo(o1);
            }
            else return 0;
        }
        else return i1 - i2;
    }
}
