/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import java.text.SimpleDateFormat;
import java.util.Date;

public class VerboseDescription
{
    private StringBuilder sb;

    public VerboseDescription()
    {
        this.sb = new StringBuilder();
    }

    public VerboseDescription append(String s)
    {
        this.sb.append(this.getDatePrefix() + s + "\n");
        return this;
    }

    public String toString()
    {
        return this.sb.toString();
    }

    private String getDatePrefix()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return "[" + sdf.format(new Date()) + "] ";
    }
}
