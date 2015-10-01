/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.license;

import java.util.Formatter;

import org.dspace.content.DSpaceObject;

/**
 * This is a simple implementation of the LicenseArgumentFormatter for a
 * DSpaceObject. The formatter options width/precision are not take in care.
 * 
 * @author bollini
 * 
 */
public class SimpleDSpaceObjectLicenseFormatter implements
        LicenseArgumentFormatter
{
    @Override
    public void formatTo(Formatter formatter, int flags, int width,
            Object object, String type)
    {
        if (object == null)
        {
            formatter.format("sample "+type);
        }
        else
        {
            DSpaceObject dso = (DSpaceObject) object;
            String name = dso.getName();
            if (name != null)
            {
                formatter.format(name);
            }
            else
            {
                formatter.format("");
            }
        }
    }
}
