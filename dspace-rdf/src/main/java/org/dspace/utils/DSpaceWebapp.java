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
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class DSpaceWebapp
extends AbstractDSpaceWebapp
{
    public DSpaceWebapp()
    {
        super("RDF");
    }

    @Override
    public boolean isUI()
    {
        return false;
    }
}
