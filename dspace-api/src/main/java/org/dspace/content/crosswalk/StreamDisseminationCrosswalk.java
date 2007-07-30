/*
 * StreamDisseminationCrosswalk
 *
 * Version: $Revision: 1.4 $
 *
 * Date: $Date: 2006/04/10 04:11:09 $
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

package org.dspace.content.crosswalk;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;


/**
 * A class implementing this interface crosswalk metadata directly
 * from a DSpace Object to an output stream, in a specific format.
 * <p>
 * Stream-oriented crosswalks are intended to be used for metadata
 * formats which are either (a) not XML-based, or (b) too bulky for the
 * DOM-ish in-memory model developed for the METS and IMSCP packagers.
 * The METS packagers (all subclasses of AbstractMETSDisseminator / AbstractMETSIngester
 * are equipped to call these crosswalks as well as the XML-based ones,
 * just refer to the desired crosswalk by its plugin name.
 *
 * @author  Larry Stone
 * @version $Revision: 1.0 $
 */
public interface StreamDisseminationCrosswalk
{
    /**
     * Predicate: Can this disseminator crosswalk the given object.
     *
     * @param dso  dspace object, e.g. an <code>Item</code>.
     * @return true when disseminator is capable of producing metadata.
     */
    public boolean canDisseminate(Context context, DSpaceObject dso);

    /**
     * Execute crosswalk on the given object, sending output to the stream.
     *
     * @param context the DSpace context
     * @param dso the  DSpace Object whose metadata to export.
     * @param out output stream to write to
     *
     * @throws CrosswalkInternalException (<code>CrosswalkException</code>) failure of the crosswalk itself.
     * @throws CrosswalkObjectNotSupported (<code>CrosswalkException</code>) Cannot crosswalk this kind of DSpace object.
     * @throws IOException  I/O failure in services this calls
     * @throws SQLException  Database failure in services this calls
     * @throws AuthorizeException current user not authorized for this operation.
     */
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException;

    public String getMIMEType();
}
