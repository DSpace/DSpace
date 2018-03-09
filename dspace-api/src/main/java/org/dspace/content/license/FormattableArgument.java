/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.license;

import java.util.Formattable;
import java.util.Formatter;

import org.dspace.core.factory.CoreServiceFactory;

/**
 * Wrapper class to make formattable any argument used in the license template.
 * The formatter behavior is delegated to a specific class on "type" basis
 * using the PluginService
 * 
 * @see Formattable
 * @see LicenseArgumentFormatter
 * @author bollini
 * 
 */
public class FormattableArgument implements Formattable
{
    private String type;

    private Object object;

    public FormattableArgument(String type, Object object)
    {
        this.type = type;
        this.object = object;
    }

    @Override
    public void formatTo(Formatter formatter, int flags, int width,
            int precision)
    {
        LicenseArgumentFormatter laf = (LicenseArgumentFormatter) CoreServiceFactory.getInstance().getPluginService()
                .getNamedPlugin(LicenseArgumentFormatter.class, type);
        if (laf != null)
        {
            laf.formatTo(formatter, flags, width, object, type);
        }
        else
        {
            formatter.format(object.toString());
        }
    }
}
