/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.packager.DSpaceMETSIngester;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.packager.PackageUtils;
import org.dspace.content.packager.PackageValidationException;
import org.dspace.core.Context;

import java.io.IOException;
import java.sql.SQLException;

public class SwordMETSPackageIngester extends DSpaceMETSIngester
{
    /**
     * Policy:  For DSpace deposit license, take deposit license
     * supplied by explicit argument first, else use collection's
     * default deposit license.
     * For Creative Commons, look for a rightsMd containing a CC license.
     *
     * This override basically fixes a bug in the DSpaceMETSIngester which
     * allows (in fact enforces) a null licence during replace, but which
     * then requires it to be not-null here.
     *
     */
    @Override
    public void addLicense(Context context, Item item, String license,
            Collection collection, PackageParameters params)
            throws PackageValidationException,
            AuthorizeException, SQLException, IOException
    {
        if (PackageUtils.findDepositLicense(context, item) == null &&
                license != null)
        {
            PackageUtils.addDepositLicense(context, license, item, collection);
        }
    }
}
