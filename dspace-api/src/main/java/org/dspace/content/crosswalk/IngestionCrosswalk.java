/*
 * IngestionCrosswalk.java
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

package org.dspace.content.crosswalk;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.jdom.Element;

/**
 * Ingestion Crosswalk plugin -- translate an external metadata format
 * into DSpace native metadata.
 * <p>
 * This describes a plugin that translates an external XML
 * metadata format (e.g.  MODS) into the DSpace internal metadata
 * representation.  A crosswalk plugin may operate on different kinds of
 * DSpace Objects, so the concept of "metadata" encompasses the
 * qualified Dublin Core fields on Items, properties of Bitstreams, and
 * metadata on Collections and Communities.
 * <p>
 *
 * @author Larry Stone
 * @version $Revision$
 */
public interface IngestionCrosswalk
{
    /**
     * Crosswalk metadata from external XML representation to DSpace
     * internal representations.  This version accepts metadata as a
     * <code>List</code> of JDOM XML elements.  It interprets the
     * contents of each element and adds the appropriate values to the
     * DSpace Object's internal metadata represenation.
     * <p>
     * Note that this method may be called several times for the same target
     * Item, if the metadata comes as several lists of elements, so it should
     * not add fixed metadata values on each or they may appear multiples times.
     * <p>
     * NOTE:  <br>
     * Most XML metadata standards (e.g.  MODS) are defined as a "root"
     * element which contains a sequence of "fields" that have the
     * descriptive information.  Some metadata containers have a
     * "disembodied" list of fields, rather than the root element, so
     * this <code>ingest</code> method is intended to accept that bare
     * list of fields.  However, it must also accept a list containing
     * only the "root" element for the metadata structure (e.g.  the
     * "mods:mods" wrapper in a MODS expression) as a member of the
     * list.  It can handle this case by calling the single-element
     * version of ingest() on the "root" element.
     * <p>
     * Some callers of the crosswalk plugin may not be careful about (or
     * capable of) choosing whether the list or element version should
     * be called.
     * <p>
     * @param context  DSpace context.
     * @param dso DSpace Object (Item, Bitstream, etc) to which new metadata gets attached.
     * @param metadata  List of XML Elements of metadata
     *
     * @throws CrosswalkInternalException (<code>CrosswalkException</code>) failure of the crosswalk itself.
     * @throws CrosswalkObjectNotSupported (<code>CrosswalkException</code>) Cannot crosswalk into this kind of DSpace object.
     * @throws MetadataValidationException (<code>CrosswalkException</code>)  metadata format was not acceptable or missing required elements.
     * @throws IOException  I/O failure in services this calls
     * @throws SQLException  Database failure in services this calls
     * @throws AuthorizeException current user not authorized for this operation.
     */
    public void ingest(Context context, DSpaceObject dso, List metadata)
        throws CrosswalkException, IOException, SQLException, AuthorizeException;

    /**
     * Crosswalk metadata from external XML representation to DSpace
     * internal representations.  This version accepts a single "root"
     * element of the XML metadata.
     * <p>
     * It is otherwise just like the <code>List</code> form of
     * <code>ingest()</code> above.
     * <p>
     * @param context  DSpace context.
     * @param dso DSpace Object (usually an Item) to which new metadata gets attached.
     * @param root root Element of metadata document.
     *
     * @throws CrosswalkInternalException (<code>CrosswalkException</code>) failure of the crosswalk itself.
     * @throws CrosswalkObjectNotSupported (<code>CrosswalkException</code>) Cannot crosswalk into this kind of DSpace object.
     * @throws MetadataValidationException (<code>CrosswalkException</code>)  metadata format was not acceptable or missing required elements.
     * @throws IOException  I/O failure in services this calls
     * @throws SQLException  Database failure in services this calls
     * @throws AuthorizeException current user not authorized for this operation.
     */
    public void ingest(Context context, DSpaceObject dso, Element root)
        throws CrosswalkException, IOException, SQLException, AuthorizeException;
}
