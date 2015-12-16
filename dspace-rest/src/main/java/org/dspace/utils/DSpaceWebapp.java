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
 * @author Bram Luyten <bram@atmire.com>
 */
public class DSpaceWebapp
        extends AbstractDSpaceWebapp
{
    public DSpaceWebapp()
    {
        super("REST");
    }

    @Override
    public boolean isUI()
    {
        return false;
    }
}
 
