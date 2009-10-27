/*
 * PackageDisseminator.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.content.packager;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Context;

/**
 * Plugin Interface to produce Dissemination Information Package (DIP)
 * of a DSpace object.
 * <p>
 * An implementation translates DSpace objects to some external
 * "package" format.  A package is a single data stream (or file)
 * containing enough information to reconstruct the object.  It can be
 * anything from an archive like a Zip file with a manifest and metadata,
 * to a simple manifest containing external references to the content,
 * to a self-contained file such as a PDF.
 * <p>
 * A DIP implementation has two methods: <code>disseminate</code>
 * to produce the package itself, and <code>getMIMEType</code> to
 * identify its Internet format type (needed when transmitting the package
 * over HTTP).
 * <p>
 * Both of these methods are given an attribute-values
 * list of "parameters", which may modify their actions.  Since the
 * format output by <code>disseminate</code> may be affected by
 * parameters, it is given to the <code>getMIMEType</code> method as well.
 * The parameters list is a generalized mechanism to pass parameters
 * from the package requestor to the packager, since different packagers will
 * understand different sets of parameters.
 *
 * @author Larry Stone
 * @version $Revision$
 * @see PackageParameters
 */
public interface PackageDisseminator
{
    /**
     * Export the object (Item, Collection, or Community) as a
     * "package" on the indicated OutputStream.  Package is any serialized
     * representation of the item, at the discretion of the implementing
     * class.  It does not have to include content bitstreams.
     * <br>
     * Use the <code>params</code> parameter list to adjust the way the
     * package is made, e.g. including a "<code>metadataOnly</code>"
     * parameter might make the package a bare manifest in XML
     * instead of a Zip file including manifest and contents.
     * <br>
     * Throws an exception of the chosen object is not acceptable or there is
     * a failure creating the package.
     *
     * @param context  DSpace context.
     * @param object  DSpace object (item, collection, etc)
     * @param params Properties-style list of options specific to this packager
     * @param out  output stream on which to write package
     * @throws PackageValidationException if package cannot be created or there is
     *  a fatal error in creating it.
     */
    void disseminate(Context context, DSpaceObject object,
                     PackageParameters params, OutputStream out)
        throws PackageException, CrosswalkException,
               AuthorizeException, SQLException, IOException;

    /**
     * Identifies the MIME-type of this package, e.g. <code>"application/zip"</code>.
     * Required when sending the package via HTTP, to
     * provide the Content-Type header.
     *
     * @return the MIME type (content-type header) of the package to be returned
     */
    String getMIMEType(PackageParameters params);
}
