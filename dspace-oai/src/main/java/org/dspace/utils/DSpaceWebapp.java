/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.utils;

import org.dspace.app.util.AbstractDSpaceWebapp;

/**
 * An MBean to identify this web application.
 *
 * @author Christian Scheible (christian.scheible at uni-konstanz dot de)
 */
public class DSpaceWebapp
        extends AbstractDSpaceWebapp
{
    public DSpaceWebapp()
    {
        super("OAI");
    }

    @Override
    public boolean isUI()
    {
        return false;
    }
}
 
