/*
 * PackageIngester.java
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
import java.io.InputStream;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Context;
               

/**
 * Plugin Interface to interpret a Submission Information Package (SIP)
 * and create (or replace) a DSpace item from its contents.
 * <p>
 * A package is a single data stream
 * containing enough information to construct an Item.  It can be
 * anything from an archive like a Zip file with a manifest and metadata,
 * to a simple manifest containing external references to the content,
 * to a self-contained file such as a PDF.  The interpretation
 * of the package is entirely at the discretion of the implementing class.
 * <p>
 * The ingest methods are also given an attribute-value
 * list of "parameters"  which may modify their actions.
 * The parameters list is a generalized mechanism to pass parameters
 * from the requestor to the packager, since different packagers will
 * understand different sets of parameters.
 *
 * @author Larry Stone
 * @version $Revision$
 * @see PackageParameters
 */
public interface PackageIngester
{
    /**
     * Create new Item out of the ingested package.
     * The item will belong to the indicated
     * collection.  This creates a <code>WorkspaceItem</code>, so it is
     * up to the caller to decide whether to install it or submit
     * it to normal DSpace Workflow.
     * <p>
     * The deposit license is passed explicitly as a string since there
     * is no place for it in many package formats.  It is optional and may
     * be given as <code>null</code>.
     *
     * @param context  DSpace context.
     * @param collection  collection under which to create new item.
     * @param in  input stream containing package to ingest.
     * @param params Properties-style list of options (interpreted by each packager).
     * @param license  may be null, which takes default license.
     * @return workspace item created by ingest.
     *
     * @throws PackageValidationException if package is unacceptable or there is
     *  a fatal error turning it into an Item.
     */
    WorkspaceItem ingest(Context context, Collection collection, InputStream in,
                         PackageParameters params, String license)
        throws PackageException, CrosswalkException,
               AuthorizeException, SQLException, IOException;

    /**
     * Replace an existing Item with contents of the ingested package.
     * The packager <em>may</em> choose not to implement <code>replace</code>,
     * since it somewhat contradicts the archival nature of DSpace.
     * The exact function of this method is highly implementation-dependent.
     *
     * @param context  DSpace context.
     * @param item  existing item to be replaced
     * @param in  input stream containing package to ingest.
     * @param params Properties-style list of options specific to this packager
     * @return item re-created by ingest.
     *
     * @throws PackageValidationException if package is unacceptable or there is
     *  a fatal error turning it into an Item.
     * @throws UnsupportedOperationException if this packager does not
     *  implement <code>replace</code>.
     */
    Item replace(Context context, Item item, InputStream in,
                 PackageParameters params)
        throws PackageException, UnsupportedOperationException,
               CrosswalkException, AuthorizeException,
               SQLException, IOException;
               
}
