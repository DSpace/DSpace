/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.license;

import java.util.Formatter;

public interface LicenseArgumentFormatter
{

    /**
     * Format the object following the <code>java.util.Formatter</code> rules.
     * The object type is expected to be know to the implementer can is free to
     * assume safe to cast as appropriate. If a <code>null</code> object is
     * supplied is expected that the implementer will work as if a "sample data"
     * was requested.
     * 
     * @see Formatter
     * @param formatter
     *            the current formatter that need to process the object
     * @param flags
     *            the flags option for the formatter
     * @param width
     *            the width option for the formatter
     * @param object
     *            the object to be formatted
     * @param type
     *            the type of the object (this is an alias not the class name! -
     *            i.e. item, collection, eperson, etc.)
     */
    void formatTo(Formatter formatter, int flags, int width, Object object,
            String type);
}
