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
